package com.shadowrealm.backend.controller;

import com.shadowrealm.backend.dto.ActionResponse;
import com.shadowrealm.backend.dto.PlayerResponse;
import com.shadowrealm.backend.dto.VoteRequest;
import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.entity.Vote;
import com.shadowrealm.backend.repository.UserRepository;
import com.shadowrealm.backend.service.VoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vote")
public class VoteController {

    @Autowired
    private VoteService voteService;

    @Autowired
    private UserRepository userRepository;

    private PlayerResponse toPublic(Player p) {
        if (p == null) return null;
        String displayName = userRepository.findById(p.getUserId()).map(User::getUsername).orElse("Player " + p.getUserId());
        return new PlayerResponse(p.getId(), p.getUserId(), displayName, p.isAlive(), p.isHost());
    }

    /**
     * Cast a vote. The voter is taken from the JWT; a voterId in the body is ignored.
     */
    @PostMapping("/cast")
    public ResponseEntity<?> castVote(@RequestBody VoteRequest request) {
        try {
            ActionResponse result = voteService.castVote(request.getRoomCode(), request.getVotedPlayerId());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Votes of already-resolved rounds (members of the room only). */
    @GetMapping("/{roomCode}")
    public List<Vote> getVotes(@PathVariable String roomCode) {
        return voteService.getVotes(roomCode);
    }

    @GetMapping("/{roomCode}/most-voted")
    public ResponseEntity<?> getMostVoted(@PathVariable String roomCode) {
        try {
            Player player = voteService.getMostVotedPlayer(roomCode);
            if (player == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(toPublic(player));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Host fallback. Normally the last vote resolves the round automatically.
     * The public result is stored on the game (lastEvent*).
     */
    @PostMapping("/{roomCode}/resolve")
    public ResponseEntity<?> resolveVoting(@PathVariable String roomCode) {
        try {
            Player eliminated = voteService.resolveVoting(roomCode);
            return ResponseEntity.ok(new ActionResponse("VOTE_RESOLVED",
                    eliminated == null ? "Nobody was eliminated." : toPublic(eliminated).getDisplayName() + " was eliminated."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
