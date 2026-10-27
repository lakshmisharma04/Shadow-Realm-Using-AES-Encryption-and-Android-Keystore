package com.shadowrealm.backend.service;

import com.shadowrealm.backend.dto.ActionResponse;
import com.shadowrealm.backend.entity.Game;
import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.entity.Vote;
import com.shadowrealm.backend.repository.PlayerRepository;
import com.shadowrealm.backend.repository.UserRepository;
import com.shadowrealm.backend.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Voting phase. One vote per living player per round, resolved automatically
 * (server side, under a database lock) when the last living player votes.
 * A tie or an empty tally eliminates nobody.
 */
@Service
public class VoteService {
    private final VoteRepository voteRepository;
    private final PlayerRepository playerRepository;
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    public VoteService(VoteRepository votes, PlayerRepository players, GameService games,
                       AuthenticationService auth, UserRepository users) {
        this.voteRepository = votes; this.playerRepository = players; this.gameService = games;
        this.authenticationService = auth; this.userRepository = users;
    }

    @Transactional
    public ActionResponse castVote(String roomCode, Long votedPlayerId) {
        if (roomCode == null || roomCode.isBlank()) throw new IllegalStateException("roomCode is required");
        if (votedPlayerId == null) throw new IllegalStateException("A vote target is required");

        Game game = gameService.getGameForUpdate(roomCode);
        ensureVoting(game);

        // The voter is ALWAYS the authenticated player; any voterId sent by the client is ignored.
        Player voter = authenticationService.getCurrentPlayer(roomCode);
        if (!voter.isAlive()) throw new IllegalStateException("Dead players cannot vote");
        Player target = playerRepository.findById(votedPlayerId).orElseThrow(() -> new IllegalStateException("Target not found"));
        if (!roomCode.equals(target.getRoomCode()) || !target.isAlive()) throw new IllegalStateException("Invalid vote target");
        if (voter.getId().equals(target.getId())) throw new IllegalStateException("You cannot vote for yourself");

        if (voteRepository.existsByRoomCodeAndVoterIdAndCounted(roomCode, voter.getId(), false))
            throw new IllegalStateException("You have already voted this round");

        Vote vote = new Vote();
        vote.setRoomCode(roomCode);
        vote.setVoterId(voter.getId());
        vote.setVotedPlayerId(target.getId());
        vote.setCounted(false);
        voteRepository.save(vote);

        if (everyoneHasVoted(roomCode)) {
            resolveVotingInternal(game, roomCode);
        }
        return new ActionResponse("VOTE_RECORDED", "Your vote has been recorded.");
    }

    /** Votes that have already been counted (i.e. earlier, resolved rounds). Live votes stay secret. */
    public List<Vote> getVotes(String roomCode) {
        Player caller = authenticationService.getCurrentPlayer(roomCode);
        // Never expose other players' choices, even after a round resolves.
        return voteRepository.findByRoomCode(roomCode).stream()
                .filter(Vote::isCounted)
                .filter(vote -> caller.getId().equals(vote.getVoterId()))
                .collect(Collectors.toList());
    }

    /** Current leader. Not available while voting is open, so live votes cannot be spied on. */
    public Player getMostVotedPlayer(String roomCode) {
        authenticationService.getCurrentPlayer(roomCode);
        Game game = gameService.getGame(roomCode);
        if ("VOTING".equals(game.getPhase()) && !game.isFinished())
            throw new IllegalStateException("Votes are secret until the council resolves");
        return null;
    }

    /** Host fallback; normally the last vote resolves automatically, but the host can resolve a stalled/AFK council. */
    @Transactional
    public Player resolveVoting(String roomCode) {
        gameService.requireHost(roomCode);
        Game game = gameService.getGameForUpdate(roomCode);
        ensureVoting(game);
        // Host may manually resolve a stalled council when a player is AFK.
        // Only votes actually submitted are counted; missing votes count as abstentions.
        return resolveVotingInternal(game, roomCode);
    }

    /** True if this player already voted in the current voting round. */
    public boolean hasVoted(String roomCode, Long playerId) {
        return voteRepository.existsByRoomCodeAndVoterIdAndCounted(roomCode, playerId, false);
    }

    // ------------------------------------------------------------------

    private void ensureVoting(Game game) {
        if (game.isFinished() || !"RUNNING".equals(game.getStatus()) || !"VOTING".equals(game.getPhase()))
            throw new IllegalStateException("Voting is not active");
    }

    private List<Vote> currentVotes(String roomCode) {
        return voteRepository.findByRoomCode(roomCode).stream().filter(v -> !v.isCounted()).collect(Collectors.toList());
    }

    private int countCurrentVotes(String roomCode) {
        Set<Long> voters = new HashSet<>();
        for (Vote v : currentVotes(roomCode)) voters.add(v.getVoterId());
        return voters.size();
    }

    private boolean everyoneHasVoted(String roomCode) {
        Set<Long> voters = new HashSet<>();
        for (Vote v : currentVotes(roomCode)) voters.add(v.getVoterId());
        List<Long> aliveIds = new ArrayList<>();
        for (Player p : playerRepository.findByRoomCodeAndAlive(roomCode, true)) aliveIds.add(p.getId());
        return GameRules.allActed(aliveIds, voters);
    }

    private Player resolveVotingInternal(Game game, String roomCode) {
        List<Vote> votes = currentVotes(roomCode);
        // Only votes cast by players who are still alive count.
        List<Long> targets = new ArrayList<>();
        for (Vote v : votes) {
            Player voter = playerRepository.findById(v.getVoterId()).orElse(null);
            if (voter != null && voter.isAlive()) targets.add(v.getVotedPlayerId());
        }

        Long eliminatedId = GameRules.tallyVotes(targets);
        Player eliminated = null;
        if (eliminatedId != null) {
            eliminated = playerRepository.findById(eliminatedId).orElse(null);
            if (eliminated != null && eliminated.isAlive()) {
                eliminated.setAlive(false);
                playerRepository.save(eliminated);
            } else {
                eliminated = null;
            }
        }

        votes.forEach(v -> v.setCounted(true));
        voteRepository.saveAll(votes);

        if (eliminated != null) {
            String name = userRepository.findById(eliminated.getUserId()).map(User::getUsername)
                    .orElse("Player " + eliminated.getUserId());
            gameService.recordEvent(game, "VOTE", name + " was eliminated by the council.", name);
        } else {
            gameService.recordEvent(game, "VOTE", "The council was split. Nobody was eliminated.", null);
        }

        gameService.evaluateWinner(game);
        if (!game.isFinished()) {
            gameService.beginNextRound(game);
        }
        gameService.updateGame(game);
        return eliminated;
    }
}
