package com.example.loginapp;
public class MatchSummary {
    private Long matchId; private String roomCode, winner, finishedAt, myRole; private int rounds, playerCount; private boolean survived, won;
    public Long getMatchId(){return matchId;} public String getRoomCode(){return roomCode;} public String getWinner(){return winner;}
    public String getFinishedAt(){return finishedAt;} public String getMyRole(){return myRole;} public int getRounds(){return rounds;}
    public int getPlayerCount(){return playerCount;} public boolean isSurvived(){return survived;} public boolean isWon(){return won;}
}
