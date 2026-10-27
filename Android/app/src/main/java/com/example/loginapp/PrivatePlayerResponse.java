package com.example.loginapp;
public class PrivatePlayerResponse {
    private Long id, userId; private String username, role; private boolean alive, host, nightActionSubmitted, voteSubmitted;
    public Long getId(){return id;} public Long getUserId(){return userId;} public String getUsername(){return username;}
    public String getRole(){return Roles.normalize(role);} public boolean isAlive(){return alive;} public boolean isHost(){return host;}
    /** Server truth: has this player already used their night action / vote in the current round? */
    public boolean isNightActionSubmitted(){return nightActionSubmitted;} public boolean isVoteSubmitted(){return voteSubmitted;}
}
