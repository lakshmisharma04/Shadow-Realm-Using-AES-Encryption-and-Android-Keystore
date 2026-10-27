package com.shadowrealm.backend.dto;

public class MatchPlayerResponse {
    private String username;
    private String role;
    private boolean survived;

    public MatchPlayerResponse() {}
    public MatchPlayerResponse(String username, String role, boolean survived) {
        this.username = username; this.role = role; this.survived = survived;
    }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isSurvived() { return survived; }
}
