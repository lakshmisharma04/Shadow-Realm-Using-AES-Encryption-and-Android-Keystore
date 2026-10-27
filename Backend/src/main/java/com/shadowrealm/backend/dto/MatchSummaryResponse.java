package com.shadowrealm.backend.dto;

public class MatchSummaryResponse {
    private Long matchId;
    private String roomCode;
    private String winner;
    private int rounds;
    private String finishedAt;
    private String myRole;
    private boolean survived;
    private boolean won;
    private int playerCount;

    public MatchSummaryResponse() {}
    public MatchSummaryResponse(Long matchId, String roomCode, String winner, int rounds, String finishedAt,
                                String myRole, boolean survived, boolean won, int playerCount) {
        this.matchId = matchId; this.roomCode = roomCode; this.winner = winner; this.rounds = rounds;
        this.finishedAt = finishedAt; this.myRole = myRole; this.survived = survived; this.won = won;
        this.playerCount = playerCount;
    }
    public Long getMatchId() { return matchId; }
    public String getRoomCode() { return roomCode; }
    public String getWinner() { return winner; }
    public int getRounds() { return rounds; }
    public String getFinishedAt() { return finishedAt; }
    public String getMyRole() { return myRole; }
    public boolean isSurvived() { return survived; }
    public boolean isWon() { return won; }
    public int getPlayerCount() { return playerCount; }
}
