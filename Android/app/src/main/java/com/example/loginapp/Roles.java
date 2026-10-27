package com.example.loginapp;

/** One place that knows how role names are written, so HEALER / DOCTOR can never disagree again. */
public final class Roles {
    private Roles() {}

    /** Canonical role name. Older data/clients may say HEALER; the backend only knows DOCTOR. */
    public static String normalize(String role) {
        if (role == null) return null;
        String r = role.trim().toUpperCase(java.util.Locale.ROOT);
        return "HEALER".equals(r) ? "DOCTOR" : r;
    }

    public static boolean hasNightAction(String role) {
        String r = normalize(role);
        return "TRAITOR".equals(r) || "SEER".equals(r) || "DOCTOR".equals(r);
    }

    /** Text for the winner field ("VILLAGER" from the server, "VILLAGERS" offline). */
    public static String winnerLabel(String winner) {
        if (winner == null) return "NO ONE";
        String w = winner.toUpperCase(java.util.Locale.ROOT);
        return w.startsWith("VILLAGER") ? "THE VILLAGERS" : "THE TRAITOR";
    }
}
