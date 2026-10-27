package com.shadowrealm.backend.service;

import com.shadowrealm.backend.dto.MatchDetailResponse;
import com.shadowrealm.backend.dto.MatchPlayerResponse;
import com.shadowrealm.backend.dto.MatchSummaryResponse;
import com.shadowrealm.backend.entity.Game;
import com.shadowrealm.backend.entity.MatchHistory;
import com.shadowrealm.backend.entity.MatchParticipant;
import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.entity.Role;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.exception.ResourceNotFoundException;
import com.shadowrealm.backend.repository.MatchHistoryRepository;
import com.shadowrealm.backend.repository.MatchParticipantRepository;
import com.shadowrealm.backend.repository.PlayerRepository;
import com.shadowrealm.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchHistoryService {

    private final MatchHistoryRepository matches;
    private final MatchParticipantRepository participants;
    private final PlayerRepository players;
    private final UserRepository users;
    private final AuthenticationService auth;

    public MatchHistoryService(MatchHistoryRepository matches, MatchParticipantRepository participants,
                               PlayerRepository players, UserRepository users, AuthenticationService auth) {
        this.matches = matches;
        this.participants = participants;
        this.players = players;
        this.users = users;
        this.auth = auth;
    }

    /** Saves a finished match exactly once (safe to call repeatedly). */
    @Transactional
    public void recordFinishedMatch(Game game, String endedBy) {
        String room = game.getRoomCode();
        if (matches.existsByRoomCode(room)) {
            return;
        }
        MatchHistory m = new MatchHistory();
        m.setRoomCode(room);
        m.setWinner(game.getWinner() == null ? null : game.getWinner().name());
        m.setRounds(game.getCurrentRound());
        m.setEndedBy(endedBy);
        m = matches.save(m);

        for (Player p : players.findByRoomCode(room)) {
            MatchParticipant mp = new MatchParticipant();
            mp.setMatchId(m.getId());
            mp.setUserId(p.getUserId());
            mp.setUsername(users.findById(p.getUserId()).map(User::getUsername).orElse("Player " + p.getUserId()));
            mp.setRole(p.getRole() == null ? null : p.getRole().name());
            mp.setSurvived(p.isAlive());
            participants.save(mp);
        }
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> historyForCurrentUser() {
        Long userId = auth.getCurrentUser().getId();
        List<MatchSummaryResponse> out = new ArrayList<>();
        for (MatchParticipant mp : participants.findByUserId(userId)) {
            MatchHistory m = matches.findById(mp.getMatchId()).orElse(null);
            if (m == null) continue;
            out.add(new MatchSummaryResponse(m.getId(), m.getRoomCode(), m.getWinner(), m.getRounds(),
                    m.getFinishedAt().toString(), mp.getRole(), mp.isSurvived(),
                    didWin(m.getWinner(), mp.getRole()), participants.findByMatchId(m.getId()).size()));
        }
        out.sort(Comparator.comparing(MatchSummaryResponse::getFinishedAt).reversed());
        return out;
    }

    /** Only participants of the match may read it (roles are public once a match is over). */
    @Transactional(readOnly = true)
    public MatchDetailResponse detail(Long matchId) {
        Long userId = auth.getCurrentUser().getId();
        MatchHistory m = matches.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        participants.findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        return toDetail(m);
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse detailForRoom(String roomCode) {
        MatchHistory m = matches.findByRoomCode(roomCode)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        return detail(m.getId());
    }

    private MatchDetailResponse toDetail(MatchHistory m) {
        List<MatchPlayerResponse> list = new ArrayList<>();
        for (MatchParticipant p : participants.findByMatchId(m.getId())) {
            list.add(new MatchPlayerResponse(p.getUsername(), p.getRole(), p.isSurvived()));
        }
        return new MatchDetailResponse(m.getId(), m.getRoomCode(), m.getWinner(), m.getRounds(),
                m.getFinishedAt().toString(), m.getEndedBy(), list);
    }

    static boolean didWin(String winner, String role) {
        if (winner == null || role == null) return false;
        return Role.TRAITOR.name().equals(role) ? Role.TRAITOR.name().equals(winner)
                : Role.VILLAGER.name().equals(winner);
    }
}
