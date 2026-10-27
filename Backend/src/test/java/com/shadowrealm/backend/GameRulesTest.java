package com.shadowrealm.backend;

import com.shadowrealm.backend.entity.Role;
import com.shadowrealm.backend.service.GameRules;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure-logic tests: no database, no Spring context needed. */
class GameRulesTest {

    @Test
    void villagersWinWhenNoTraitorIsAlive() {
        assertEquals(Role.VILLAGER, GameRules.winnerFor(0, 5));
    }

    @Test
    void traitorsWinWhenTheyEqualOrOutnumberOthers() {
        assertEquals(Role.TRAITOR, GameRules.winnerFor(1, 1));
        assertEquals(Role.TRAITOR, GameRules.winnerFor(2, 1));
    }

    @Test
    void matchContinuesOtherwise() {
        assertNull(GameRules.winnerFor(1, 6));
        assertNull(GameRules.winnerFor(1, 2));
    }

    @Test
    void clearMajorityEliminatesThatPlayer() {
        assertEquals(7L, GameRules.tallyVotes(Arrays.asList(7L, 7L, 3L)));
    }

    @Test
    void tieEliminatesNobody() {
        assertNull(GameRules.tallyVotes(Arrays.asList(7L, 3L)));
        assertNull(GameRules.tallyVotes(Arrays.asList(7L, 7L, 3L, 3L)));
    }

    @Test
    void noVotesEliminatesNobody() {
        assertNull(GameRules.tallyVotes(Collections.emptyList()));
        assertNull(GameRules.tallyVotes(null));
    }

    @Test
    void singleVoteWins() {
        assertEquals(4L, GameRules.tallyVotes(Collections.singletonList(4L)));
    }

    @Test
    void doctorProtectionCancelsTheKill() {
        assertNull(GameRules.nightKill(5L, Arrays.asList(5L)));
    }

    @Test
    void unprotectedTargetDies() {
        assertEquals(5L, GameRules.nightKill(5L, Arrays.asList(6L)));
        assertEquals(5L, GameRules.nightKill(5L, Collections.emptyList()));
    }

    @Test
    void noAttackMeansNoKill() {
        assertNull(GameRules.nightKill(null, Arrays.asList(1L)));
    }

    @Test
    void nightWaitsUntilEveryRequiredActorActed() {
        List<Long> required = Arrays.asList(1L, 2L, 3L);
        assertFalse(GameRules.allActed(required, Arrays.asList(1L, 2L)));
        assertTrue(GameRules.allActed(required, Arrays.asList(3L, 2L, 1L)));
        assertFalse(GameRules.allActed(Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    void onlySpecialRolesHaveNightActions() {
        assertTrue(GameRules.hasNightAction(Role.TRAITOR));
        assertTrue(GameRules.hasNightAction(Role.DOCTOR));
        assertTrue(GameRules.hasNightAction(Role.SEER));
        assertFalse(GameRules.hasNightAction(Role.VILLAGER));
        assertFalse(GameRules.hasNightAction(null));
    }
}
