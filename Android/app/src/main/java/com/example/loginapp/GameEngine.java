package com.example.loginapp;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Self-contained local game engine used by the presentation/demo mode.
 * It mirrors the backend concepts: room, players, roles, secret word,
 * night action, discussion, voting, elimination and winner detection.
 */
public final class GameEngine {
    public static final class Player {
        public final String name;
        public final String role;
        public boolean alive = true;
        public Player(String name, String role) { this.name = name; this.role = role; }
    }

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String[][] WORDS = {
            {"MOON", "night sky"}, {"CASTLE", "ancient fortress"}, {"FOREST", "many trees"},
            {"OCEAN", "deep water"}, {"CANDLE", "small flame"}, {"LIBRARY", "place of books"},
            {"THUNDER", "loud storm sound"}, {"MIRROR", "shows a reflection"},
            {"DRAGON", "mythical creature"}, {"DESERT", "dry landscape"}, {"VOLCANO", "mountain that erupts"},
            {"CLOCK", "measures passing time"}, {"MASK", "covers the face"}, {"RAVEN", "dark-feathered bird"}
    };
    private static final String[] NAMES = {"Mira", "Kael", "Riven", "Nova", "Iris", "Ash"};
    private static GameEngine instance;

    private final List<Player> players = new ArrayList<>();
    private final Map<String, String> votes = new HashMap<>();
    private final List<String> chat = new ArrayList<>();
    private final Set<String> protectedTonight = new HashSet<>();
    private String roomCode;
    private String word;
    private String hint;
    private int round = 1;
    private String phase = "NIGHT";
    private String winner = null;
    private String lastNightMessage = "The realm waits for the first move.";
    private String lastEliminated = null;      // eliminated by the council vote
    private String lastNightEliminated = null; // taken by the night
    private String seerMessage = null;         // private Seer result (human Seer only)
    private boolean lastVoteTie = false;

    private GameEngine() {}

    public static GameEngine get() {
        if (instance == null) instance = new GameEngine();
        return instance;
    }

    public void start(String playerName) {
        players.clear(); votes.clear(); chat.clear(); protectedTonight.clear();
        winner = null; lastEliminated = null; lastNightEliminated = null; seerMessage = null;
        lastVoteTie = false; lastNightMessage = "The realm waits for the first move.";
        round = 1; phase = "NIGHT";
        roomCode = makeRoomCode();
        int wordIndex = RANDOM.nextInt(WORDS.length);
        word = WORDS[wordIndex][0];
        hint = WORDS[wordIndex][1];

        List<String> roles = new ArrayList<>();
        roles.add("TRAITOR");
        roles.add("SEER");
        roles.add("DOCTOR");
        roles.add("VILLAGER");
        roles.add("VILLAGER");
        roles.add("VILLAGER");
        roles.add("VILLAGER");
        Collections.shuffle(roles, RANDOM);

        players.add(new Player(playerName == null || playerName.trim().isEmpty() ? "Night Walker" : playerName.trim(), roles.remove(0)));
        for (int i = 0; i < NAMES.length; i++) players.add(new Player(NAMES[i], roles.remove(0)));

        chat.add("SYSTEM  •  The gates are sealed. Seven players remain.");
        chat.add("Mira  •  I have a strange feeling about this round.");
        chat.add("Kael  •  Nobody reveal the secret word. Not even by accident.");
    }

    public String roomCode() { return roomCode; }
    public String word() { return word; }
    public String hint() { return hint; }
    public int round() { return round; }
    public String phase() { return phase; }
    public String winner() { return winner; }
    public String lastNightMessage() { return lastNightMessage; }
    public String lastEliminated() { return lastEliminated; }
    public String lastNightEliminated() { return lastNightEliminated; }
    public String seerMessage() { return seerMessage; }
    public boolean lastVoteWasTie() { return lastVoteTie; }
    public boolean humanHasNightAction() {
        Player me = me();
        return me != null && me.alive && winner == null
                && ("TRAITOR".equals(me.role) || "DOCTOR".equals(me.role) || "SEER".equals(me.role));
    }
    public List<Player> players() { return Collections.unmodifiableList(players); }

    public Player me() { return players.isEmpty() ? null : players.get(0); }
    public boolean isAlive(String name) {
        for (Player p : players) if (p.name.equals(name)) return p.alive;
        return false;
    }

    public List<Player> aliveOthers() {
        List<Player> result = new ArrayList<>();
        for (int i = 1; i < players.size(); i++) if (players.get(i).alive) result.add(players.get(i));
        return result;
    }

    public String secretForMe() {
        return "TRAITOR".equals(me().role) ? "SHADOW HINT: " + hint : "SECRET WORD: " + word;
    }

    public boolean isTraitor() { return me() != null && "TRAITOR".equals(me().role); }

    /** Kept for compatibility: a night with no human action (bots act on their own). */
    public void enterDay() {
        resolveNight(null);
    }

    private void addBotDiscussion() {
        String[] lines = {
                "Nova  •  Someone is being too careful with their answers.",
                "Riven  •  I think the quiet player knows more than they say.",
                "Iris  •  We need evidence, not panic.",
                "Ash  •  Watch who changes their story when the pressure rises."
        };
        int start = Math.min(round - 1, lines.length - 1);
        chat.add(lines[start]);
    }

    public List<String> chat() { return Collections.unmodifiableList(chat); }

    public void addChat(String name, String message) {
        if (message == null || message.trim().isEmpty()) return;
        chat.add(name + "  •  " + message.trim());
    }

    /**
     * Resolves one night. The human acts only if alive and holding a night role
     * (targetName is their choice); every bot role acts automatically.
     * Does nothing if the match is over or it is not night.
     */
    public void resolveNight(String targetName) {
        if (winner != null || !"NIGHT".equals(phase)) return;
        runNight(targetName);
    }

    private void runNight(String humanTarget) {
        Player me = me();
        boolean humanActs = me != null && me.alive;
        String attackName = null;
        String protectName = null;
        seerMessage = null;
        lastNightEliminated = null;

        Player traitor = findRole("TRAITOR");
        if (traitor != null) {
            if (humanActs && traitor == me) {
                Player t = find(humanTarget);
                if (t != null && t.alive && !"TRAITOR".equals(t.role)) attackName = t.name;
            } else {
                attackName = randomAliveName(true);
            }
        }

        Player doctor = findRole("DOCTOR");
        if (doctor != null) {
            if (humanActs && doctor == me) {
                Player t = find(humanTarget);
                if (t != null && t.alive) protectName = t.name;
            } else {
                protectName = randomAliveName(false);
            }
        }

        if (humanActs && "SEER".equals(me.role)) {
            Player t = find(humanTarget);
            if (t != null && t.alive && t != me) {
                seerMessage = t.name + " is " + t.role + ". Only you can see this truth.";
            }
        }

        Player victim = find(attackName);
        if (victim != null && victim.alive && !victim.name.equals(protectName)) {
            victim.alive = false;
            lastNightEliminated = victim.name;
            lastNightMessage = victim.name + " vanished into the shadows during the night.";
        } else {
            // Same public wording whether protected or not attacked: the Doctor's success never leaks.
            lastNightMessage = "Dawn breaks. Nobody was eliminated tonight.";
        }

        checkWinner();
        if (winner == null) {
            phase = "DAY";
            chat.add("SYSTEM  •  Night is over. Review the clues before the council votes.");
            addBotDiscussion();
        }
    }

    private String randomAliveName(boolean excludeTraitor) {
        List<Player> candidates = new ArrayList<>();
        for (Player p : players) {
            if (p.alive && !(excludeTraitor && "TRAITOR".equals(p.role))) candidates.add(p);
        }
        return candidates.isEmpty() ? null : candidates.get(RANDOM.nextInt(candidates.size())).name;
    }

    /**
     * Records a vote. Returns false (and changes nothing) when the vote is invalid:
     * wrong phase, match over, dead/unknown voter, dead/unknown target,
     * voting for yourself, or voting twice in the same round.
     */
    public boolean vote(String voter, String target) {
        if (winner != null || !"DAY".equals(phase)) return false;
        Player v = find(voter);
        Player t = find(target);
        if (v == null || t == null || !v.alive || !t.alive || v == t) return false;
        if (votes.containsKey(voter)) return false;
        votes.put(voter, target);
        return true;
    }

    public boolean hasVoted(String name) { return votes.containsKey(name); }

    /**
     * Returns the target with strictly the most votes, or null if there are
     * no votes or the top spot is tied (a tied council eliminates nobody).
     */
    public static String tally(java.util.Collection<String> targets) {
        if (targets == null || targets.isEmpty()) return null;
        Map<String, Integer> counts = new HashMap<>();
        for (String t : targets) if (t != null) counts.put(t, counts.getOrDefault(t, 0) + 1);
        String top = null; int topCount = 0; boolean tie = false;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > topCount) { top = e.getKey(); topCount = e.getValue(); tie = false; }
            else if (e.getValue() == topCount) tie = true;
        }
        return tie ? null : top;
    }

    public void resolveVotes() {
        if (winner != null || !"DAY".equals(phase)) return;
        Player me = me();
        // A living human must cast a vote first. If the human was eliminated, the AI council continues.
        if (me != null && me.alive && !votes.containsKey(me.name)) return;

        // Bots vote. The Traitor avoids voting for fellow Traitors; others are slightly suspicious of the Traitor.
        Player traitor = findRole("TRAITOR");
        for (Player bot : players) {
            if (!bot.alive || bot == me || votes.containsKey(bot.name)) continue;
            List<Player> candidates = new ArrayList<>();
            for (Player p : players) if (p.alive && p != bot) candidates.add(p);
            if (candidates.isEmpty()) continue;
            Player target;
            if ("TRAITOR".equals(bot.role)) {
                List<Player> safe = new ArrayList<>();
                for (Player p : candidates) if (!"TRAITOR".equals(p.role)) safe.add(p);
                target = safe.isEmpty() ? candidates.get(0) : safe.get(RANDOM.nextInt(safe.size()));
            } else if (traitor != null && traitor.alive && RANDOM.nextInt(100) < 48) {
                target = traitor;
            } else {
                target = candidates.get(RANDOM.nextInt(candidates.size()));
            }
            votes.put(bot.name, target.name);
        }

        // Only votes of living players count.
        List<String> counted = new ArrayList<>();
        for (Map.Entry<String, String> e : votes.entrySet()) if (isAlive(e.getKey())) counted.add(e.getValue());
        String top = tally(counted);

        lastEliminated = null;
        lastVoteTie = (top == null);
        if (top != null) {
            Player eliminated = find(top);
            if (eliminated != null && eliminated.alive) {
                eliminated.alive = false;
                lastEliminated = eliminated.name;
                chat.add("SYSTEM  •  " + eliminated.name + " was eliminated by the council. Role: " + eliminated.role + ".");
            }
        } else {
            chat.add("SYSTEM  •  The council was split. Nobody was eliminated.");
        }
        votes.clear();
        checkWinner();
        if (winner == null) {
            round++;
            phase = "NIGHT";
            seerMessage = null;
            lastNightMessage = "The realm darkens again. Choose your next move.";
        }
    }

    /** Finish the AI-vs-AI portion if the human player is eliminated. */
    public void spectateToEnd() {
        int guard = 0;
        while (winner == null && guard++ < 60) {
            if ("NIGHT".equals(phase)) {
                runNight(null);
            } else {
                resolveVotes();
            }
        }
    }

    public void checkWinner() {
        if (winner != null) return;
        int traitors = 0, others = 0;
        for (Player p : players) if (p.alive) {
            if ("TRAITOR".equals(p.role)) traitors++; else others++;
        }
        if (traitors == 0) winner = "VILLAGERS";
        else if (traitors >= others) winner = "TRAITOR";
    }

    public Player find(String name) { if (name == null) return null; for (Player p : players) if (p.name.equals(name)) return p; return null; }
    private Player findRole(String role) { for (Player p : players) if (p.alive && role.equals(p.role)) return p; return null; }
    public boolean isPlayerAlive(String name) { return isAlive(name); }

    private String makeRoomCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 6; i++) s.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        return s.toString();
    }
}
