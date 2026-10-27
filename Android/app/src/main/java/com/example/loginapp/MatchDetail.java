package com.example.loginapp;
import java.util.List;
public class MatchDetail {
    public static class MatchPlayer {
        private String username, role; private boolean survived;
        public String getUsername(){return username;} public String getRole(){return role;} public boolean isSurvived(){return survived;}
    }
    private Long matchId; private String roomCode, winner, finishedAt, endedBy; private int rounds; private List<MatchPlayer> players;
    public Long getMatchId(){return matchId;} public String getRoomCode(){return roomCode;} public String getWinner(){return winner;}
    public String getFinishedAt(){return finishedAt;} public String getEndedBy(){return endedBy;} public int getRounds(){return rounds;}
    public List<MatchPlayer> getPlayers(){return players;}
}
