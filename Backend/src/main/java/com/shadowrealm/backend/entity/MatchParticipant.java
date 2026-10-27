package com.shadowrealm.backend.entity;

import jakarta.persistence.*;

/** One row per player of a finished match. */
@Entity
@Table(name = "match_participant")
public class MatchParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", nullable = false)
    private Long matchId;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 100)
    private String username;

    @Column(length = 20)
    private String role;

    private boolean survived;

    public Long getId() { return id; }
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isSurvived() { return survived; }
    public void setSurvived(boolean survived) { this.survived = survived; }
}
