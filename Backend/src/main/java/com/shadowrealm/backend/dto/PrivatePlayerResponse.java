package com.shadowrealm.backend.dto;

public class PrivatePlayerResponse {
    private Long id;
    private Long userId;
    private String username;
    private String role;
    private boolean alive;
    private boolean host;
    private boolean nightActionSubmitted;
    private boolean voteSubmitted;

    public PrivatePlayerResponse() {}

    public PrivatePlayerResponse(Long id, Long userId, String username, String role, boolean alive, boolean host,
                                 boolean nightActionSubmitted, boolean voteSubmitted) {
        this(id, userId, username, role, alive, host);
        this.nightActionSubmitted = nightActionSubmitted;
        this.voteSubmitted = voteSubmitted;
    }

    public PrivatePlayerResponse(Long id, Long userId, String username, String role, boolean alive, boolean host) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.alive = alive;
        this.host = host;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isAlive() { return alive; }
    public boolean isHost() { return host; }
    public boolean isNightActionSubmitted() { return nightActionSubmitted; }
    public boolean isVoteSubmitted() { return voteSubmitted; }
}
