package com.shadowrealm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    private Role winner;

    @Column(nullable = false)
    private String phase;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean started;

    @Column(nullable = false)
    private boolean finished;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private int currentRound;

    @JsonIgnore
    @Column(length = 120)
    private String secretWord;

    @JsonIgnore
    @Column(length = 250)
    private String secretHint;

    // Public, server-authoritative "what just happened" information so that
    // EVERY client can show the same night / vote result. Nullable so that
    // Hibernate can add the columns safely to an existing table.
    @Column(name = "last_event_seq")
    private Integer lastEventSeq;

    @Column(name = "last_event_type", length = 20)
    private String lastEventType;

    @Column(name = "last_event_message", length = 255)
    private String lastEventMessage;

    @Column(name = "last_eliminated_name", length = 100)
    private String lastEliminatedName;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Game() {

        this.createdAt = LocalDateTime.now();

        this.phase = "LOBBY";

        this.status = "WAITING";

        this.started = false;

        this.finished = false;
        this.currentRound = 1;
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }


    public Role getWinner() {
        return winner;
    }

    public void setWinner(Role winner) {
        this.winner = winner;
    }


    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
    }


    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }


    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }

    public String getSecretWord() { return secretWord; }
    public void setSecretWord(String secretWord) { this.secretWord = secretWord; }

    public String getSecretHint() { return secretHint; }
    public void setSecretHint(String secretHint) { this.secretHint = secretHint; }

    public int getLastEventSeq() { return lastEventSeq == null ? 0 : lastEventSeq; }
    public void setLastEventSeq(Integer lastEventSeq) { this.lastEventSeq = lastEventSeq; }

    public String getLastEventType() { return lastEventType; }
    public void setLastEventType(String lastEventType) { this.lastEventType = lastEventType; }

    public String getLastEventMessage() { return lastEventMessage; }
    public void setLastEventMessage(String lastEventMessage) { this.lastEventMessage = lastEventMessage; }

    public String getLastEliminatedName() { return lastEliminatedName; }
    public void setLastEliminatedName(String lastEliminatedName) { this.lastEliminatedName = lastEliminatedName; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}