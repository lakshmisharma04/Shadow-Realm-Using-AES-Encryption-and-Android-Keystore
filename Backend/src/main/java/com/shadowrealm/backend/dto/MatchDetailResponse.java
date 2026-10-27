package com.shadowrealm.backend.dto;

import java.util.List;

public class MatchDetailResponse {
    private Long matchId;
    private String roomCode;
    private String winner;
    private int rounds;
    private String finishedAt;
    private String endedBy;
    private List<MatchPlayerResponse> players;

    public MatchDetailResponse() {}
    public MatchDetailResponse(Long matchId, String roomCode, String winner, int rounds, String finishedAt,
                               String endedBy, List<MatchPlayerResponse> players) {
        this.matchId = matchId; this.roomCode = roomCode; this.winner = winner; this.rounds = rounds;
        this.finishedAt = finishedAt; this.endedBy = endedBy; this.players = players;
    }
    public Long getMatchId() { return matchId; }
    public String getRoomCode() { return roomCode; }
    public String getWinner() { return winner; }
    public int getRounds() { return rounds; }
    public String getFinishedAt() { return finishedAt; }
    public String getEndedBy() { return endedBy; }
    public List<MatchPlayerResponse> getPlayers() { return players; }
}
