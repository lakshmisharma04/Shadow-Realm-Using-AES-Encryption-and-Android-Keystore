package com.shadowrealm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One row per finished match. Created once by MatchHistoryService. */
@Entity
@Table(name = "match_history")
public class MatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", nullable = false, unique = true)
    private String roomCode;

    /** TRAITOR, VILLAGER, or null when the host ended the match early. */
    @Column(length = 20)
    private String winner;

    private int rounds;

    @Column(name = "ended_by", length = 20)
    private String endedBy;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getRoomCode() { return roomCode; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
    public int getRounds() { return rounds; }
    public void setRounds(int rounds) { this.rounds = rounds; }
    public String getEndedBy() { return endedBy; }
    public void setEndedBy(String endedBy) { this.endedBy = endedBy; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
