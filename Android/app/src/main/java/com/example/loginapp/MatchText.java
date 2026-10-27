package com.example.loginapp;

/** Turns a finished-match record into readable text (used by the history list and the results screen). */
public final class MatchText {
    private MatchText() {}

    public static String describe(MatchDetail d) {
        StringBuilder sb = new StringBuilder();
        sb.append(d.getWinner() == null ? "The host ended this match early." : Roles.winnerLabel(d.getWinner()) + " won.");
        sb.append("\nRounds: ").append(d.getRounds()).append("\n\nPlayers and hidden roles:");
        if (d.getPlayers() != null) {
            for (MatchDetail.MatchPlayer p : d.getPlayers()) {
                sb.append("\n• ").append(p.getUsername() == null ? "Player" : p.getUsername())
                  .append(" — ").append(p.getRole() == null ? "?" : Roles.normalize(p.getRole()))
                  .append(p.isSurvived() ? " (survived)" : " (eliminated)");
            }
        }
        return sb.toString();
    }
}
