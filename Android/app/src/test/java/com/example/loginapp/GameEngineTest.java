package com.example.loginapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** Plain JVM tests for the offline/demo engine (no Android, no backend). */
public class GameEngineTest {

    private static int count(GameEngine g, String role) {
        int n = 0;
        for (GameEngine.Player p : g.players()) if (role.equals(p.role)) n++;
        return n;
    }

    private static int alive(GameEngine g) {
        int n = 0;
        for (GameEngine.Player p : g.players()) if (p.alive) n++;
        return n;
    }

    /** Plays one full match like a human would; returns the engine at the end. */
    private static GameEngine playFullMatch() {
        GameEngine g = GameEngine.get();
        g.start("Tester");
        int guard = 0;
        while (g.winner() == null && guard++ < 40) {
            if (g.me().alive) {
                String target = null;
                for (GameEngine.Player p : g.aliveOthers()) {
                    if (g.isTraitor() && "TRAITOR".equals(p.role)) continue;
                    target = p.name;
                    break;
                }
                g.resolveNight(target);
                if (g.winner() != null) break;
                if (!g.me().alive) continue; // killed during the night: next loop spectates
                String voteTarget = g.aliveOthers().get(0).name;
                assertTrue(g.vote(g.me().name, voteTarget));
                g.resolveVotes();
            } else {
                g.spectateToEnd();
            }
        }
        return g;
    }

    @Test
    public void startCreatesSevenPlayersWithOneOfEachSpecialRole() {
        GameEngine g = GameEngine.get();
        g.start("Tester");
        assertEquals(7, g.players().size());
        assertEquals(1, count(g, "TRAITOR"));
        assertEquals(1, count(g, "SEER"));
        assertEquals(1, count(g, "DOCTOR"));
        assertEquals(4, count(g, "VILLAGER"));
        assertEquals("NIGHT", g.phase());
        assertEquals(1, g.round());
        assertNull(g.winner());
    }

    @Test
    public void nightMovesToDayWithoutAnyBackend() {
        GameEngine g = GameEngine.get();
        g.start("Tester");
        g.resolveNight(null);
        assertTrue("DAY".equals(g.phase()) || g.winner() != null);
        assertNotNull(g.lastNightMessage());
    }

    @Test
    public void humanCanBeEliminatedAtNight() {
        boolean humanDied = false;
        for (int i = 0; i < 600 && !humanDied; i++) {
            GameEngine g = GameEngine.get();
            g.start("Tester");
            if (g.isTraitor()) continue;
            g.resolveNight(null);
            if (!g.me().alive) humanDied = true;
        }
        assertTrue("the human was never attacked in 600 nights", humanDied);
    }

    @Test
    public void tieEliminatesNobody() {
        assertNull(GameEngine.tally(Arrays.asList("A", "B")));
        assertNull(GameEngine.tally(Arrays.asList("A", "A", "B", "B")));
        assertNull(GameEngine.tally(Collections.<String>emptyList()));
        assertEquals("A", GameEngine.tally(Arrays.asList("A", "A", "B")));
    }

    @Test
    public void invalidVotesAreRejected() {
        GameEngine g = GameEngine.get();
        g.start("Tester");
        g.resolveNight(null);
        if (g.winner() != null) return;
        String me = g.me().name;
        String other = g.aliveOthers().get(0).name;
        assertFalse("cannot vote for yourself", g.vote(me, me));
        assertFalse("unknown target", g.vote(me, "Nobody"));
        assertTrue(g.vote(me, other));
        assertFalse("duplicate vote", g.vote(me, g.aliveOthers().get(1).name));
    }

    @Test
    public void votingOnlyWorksDuringTheDay() {
        GameEngine g = GameEngine.get();
        g.start("Tester");
        assertEquals("NIGHT", g.phase());
        assertFalse(g.vote(g.me().name, g.aliveOthers().get(0).name));
    }

    @Test
    public void everyMatchEndsWithACorrectWinner() {
        for (int i = 0; i < 200; i++) {
            GameEngine g = playFullMatch();
            assertNotNull("match must end", g.winner());
            int traitors = 0, others = 0;
            for (GameEngine.Player p : g.players()) {
                if (!p.alive) continue;
                if ("TRAITOR".equals(p.role)) traitors++; else others++;
            }
            if ("TRAITOR".equals(g.winner())) assertTrue(traitors >= others && traitors > 0);
            else {
                assertEquals("VILLAGERS", g.winner());
                assertEquals(0, traitors);
            }
        }
    }

    @Test
    public void noActionsAfterTheMatchHasEnded() {
        GameEngine g = playFullMatch();
        String phase = g.phase();
        int round = g.round();
        int aliveBefore = alive(g);
        g.resolveNight(null);
        g.resolveVotes();
        assertEquals(phase, g.phase());
        assertEquals(round, g.round());
        assertEquals(aliveBefore, alive(g));
    }

    @Test
    public void newRoundStartsClean() {
        for (int i = 0; i < 100; i++) {
            GameEngine g = GameEngine.get();
            g.start("Tester");
            g.resolveNight(null);
            if (g.winner() != null || !g.me().alive) continue;
            assertTrue(g.vote(g.me().name, g.aliveOthers().get(0).name));
            g.resolveVotes();
            if (g.winner() == null) {
                assertEquals("NIGHT", g.phase());
                assertEquals(2, g.round());
                assertFalse("votes must be cleared", g.hasVoted(g.me().name));
                return;
            }
        }
    }
}
