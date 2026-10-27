package com.example.loginapp;
public class GameResponse {
    private Long id; private String roomCode, winner, phase, status; private boolean started, finished; private int currentRound, round;
    // Public result of the last night / council, identical for every player (server-authoritative).
    private int lastEventSeq; private String lastEventType, lastEventMessage, lastEliminatedName;
    public Long getId(){return id;} public String getRoomCode(){return roomCode;} public String getWinner(){return winner;}
    public String getPhase(){return phase;} public String getStatus(){return status;} public boolean isStarted(){return started;}
    public boolean isFinished(){return finished;} public int getCurrentRound(){return currentRound > 0 ? currentRound : round;}
    public int getLastEventSeq(){return lastEventSeq;} public String getLastEventType(){return lastEventType;}
    public String getLastEventMessage(){return lastEventMessage;} public String getLastEliminatedName(){return lastEliminatedName;}
}
