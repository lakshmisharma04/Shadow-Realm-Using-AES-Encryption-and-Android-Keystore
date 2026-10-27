package com.shadowrealm.backend.service;

import com.shadowrealm.backend.entity.Role;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Pure game rules (no database, no Spring). Keeping the rules here makes them
 * easy to unit-test and guarantees that every service uses the same logic.
 */
public final class GameRules {

    private GameRules() {
    }

    /**
     * Win conditions (unchanged from the original GameService):
     *  - no living Traitor            -> VILLAGER side wins
     *  - Traitors >= everyone else    -> TRAITOR side wins
     *  - otherwise the match continues (returns null)
     */
    public static Role winnerFor(long aliveTraitors, long aliveOthers) {
        if (aliveTraitors <= 0) {
            return Role.VILLAGER;
        }
        if (aliveTraitors >= aliveOthers) {
            return Role.TRAITOR;
        }
        return null;
    }

    /**
     * Returns the id with strictly the most votes.
     * Returns null when there are no votes OR when the top spot is tied
     * (a tied council eliminates nobody).
     */
    public static Long tallyVotes(Collection<Long> votedPlayerIds) {
        if (votedPlayerIds == null || votedPlayerIds.isEmpty()) {
            return null;
        }
        Map<Long, Integer> counts = new HashMap<>();
        for (Long id : votedPlayerIds) {
            if (id != null) {
                counts.merge(id, 1, Integer::sum);
            }
        }
        Long best = null;
        int bestCount = 0;
        boolean tie = false;
        for (Map.Entry<Long, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
                tie = false;
            } else if (e.getValue() == bestCount) {
                tie = true;
            }
        }
        return tie ? null : best;
    }

    /**
     * The Traitor's target dies unless the Doctor protected that same player.
     * Returns the id of the player who dies, or null when nobody dies.
     */
    public static Long nightKill(Long attackTargetId, Collection<Long> protectedIds) {
        if (attackTargetId == null) {
            return null;
        }
        if (protectedIds != null && protectedIds.contains(attackTargetId)) {
            return null;
        }
        return attackTargetId;
    }

    /** True when every required actor id appears in the acted set. */
    public static boolean allActed(Collection<Long> requiredActorIds, Collection<Long> actedActorIds) {
        if (requiredActorIds == null || requiredActorIds.isEmpty()) {
            return false;
        }
        return actedActorIds != null && actedActorIds.containsAll(requiredActorIds);
    }

    /** Roles that have a night action. */
    public static boolean hasNightAction(Role role) {
        return role == Role.TRAITOR || role == Role.DOCTOR || role == Role.SEER;
    }
}
