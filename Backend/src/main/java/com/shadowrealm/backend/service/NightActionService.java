package com.shadowrealm.backend.service;

import com.shadowrealm.backend.dto.ActionResponse;
import com.shadowrealm.backend.dto.InvestigationResponse;
import com.shadowrealm.backend.entity.Game;
import com.shadowrealm.backend.entity.NightAction;
import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.entity.Role;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.repository.NightActionRepository;
import com.shadowrealm.backend.repository.PlayerRepository;
import com.shadowrealm.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Night phase. The server is authoritative:
 *  - every action is validated (room membership, alive, role, phase, target),
 *  - the game row is locked so a round can never be resolved twice,
 *  - the night resolves automatically as soon as every living night-role
 *    player (Traitor, Doctor, Seer) has acted. The host may also force dawn
 *    once the Traitor has acted (useful if the Doctor/Seer is away).
 */
@Service
public class NightActionService {
    private final NightActionRepository nightActionRepository;
    private final PlayerRepository playerRepository;
    private final GameService gameService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    public NightActionService(NightActionRepository repo, PlayerRepository players, GameService games, AuthenticationService auth, UserRepository users) {
        this.nightActionRepository = repo;
        this.playerRepository = players;
        this.gameService = games;
        this.authenticationService = auth;
        this.userRepository = users;
    }

    @Transactional
    public InvestigationResponse seerInvestigate(String roomCode, Long targetId) {
        Game game = gameService.getGameForUpdate(roomCode);
        ensureNight(game);
        Player actor = currentActor(roomCode, Role.SEER);
        Player target = target(targetId, roomCode);
        if (actor.getId().equals(target.getId())) throw new IllegalStateException("You cannot investigate yourself");
        ensureAlive(target);
        ensureOneAction(roomCode, actor.getId(), Role.SEER);
        save(roomCode, actor.getId(), target.getId(), Role.SEER);
        InvestigationResponse result = new InvestigationResponse(displayName(target),
                target.getRole() == null ? "UNKNOWN" : target.getRole().name());
        autoResolveIfReady(game, roomCode);
        return result;
    }

    @Transactional
    public ActionResponse traitorAttack(String roomCode, Long targetId) {
        Game game = gameService.getGameForUpdate(roomCode);
        ensureNight(game);
        Player actor = currentActor(roomCode, Role.TRAITOR);
        Player target = target(targetId, roomCode);
        if (actor.getId().equals(target.getId())) throw new IllegalStateException("You cannot attack yourself");
        ensureAlive(target);
        ensureOneAction(roomCode, actor.getId(), Role.TRAITOR);
        save(roomCode, actor.getId(), target.getId(), Role.TRAITOR);
        autoResolveIfReady(game, roomCode);
        return new ActionResponse("ACTION_RECORDED", "Your night action has been recorded.");
    }

    @Transactional
    public ActionResponse doctorProtect(String roomCode, Long targetId) {
        Game game = gameService.getGameForUpdate(roomCode);
        ensureNight(game);
        Player actor = currentActor(roomCode, Role.DOCTOR);
        Player target = target(targetId, roomCode);
        ensureAlive(target);
        ensureOneAction(roomCode, actor.getId(), Role.DOCTOR);
        save(roomCode, actor.getId(), target.getId(), Role.DOCTOR);
        autoResolveIfReady(game, roomCode);
        return new ActionResponse("ACTION_RECORDED", "Your night action has been recorded.");
    }

    /** Host-only "force dawn". Requires the Traitor to have chosen a target. */
    @Transactional
    public Player resolveNight(String roomCode) {
        gameService.requireHost(roomCode);
        Game game = gameService.getGameForUpdate(roomCode);
        ensureNight(game);
        List<NightAction> actions = nightActionRepository.findByRoomCodeAndResolved(roomCode, false);
        boolean traitorActed = actions.stream().anyMatch(a -> a.getActionRole() == Role.TRAITOR);
        if (!traitorActed) throw new IllegalStateException("The Traitor must choose a target before dawn.");
        return resolveNightInternal(game, roomCode, actions);
    }

    /** True if this player already submitted their night action in the current night. */
    public boolean hasActedThisNight(String roomCode, Player player) {
        if (player.getRole() == null || !GameRules.hasNightAction(player.getRole())) return false;
        return nightActionRepository.existsByRoomCodeAndActorIdAndActionRoleAndResolved(
                roomCode, player.getId(), player.getRole(), false);
    }

    // ------------------------------------------------------------------

    private void autoResolveIfReady(Game game, String roomCode) {
        List<NightAction> pending = nightActionRepository.findByRoomCodeAndResolved(roomCode, false);
        Set<Long> acted = new HashSet<>();
        for (NightAction a : pending) acted.add(a.getActorId());
        List<Long> required = new ArrayList<>();
        for (Player p : playerRepository.findByRoomCodeAndAlive(roomCode, true)) {
            if (GameRules.hasNightAction(p.getRole())) required.add(p.getId());
        }
        if (GameRules.allActed(required, acted)) {
            resolveNightInternal(game, roomCode, pending);
        }
    }

    private Player resolveNightInternal(Game game, String roomCode, List<NightAction> actions) {
        Long attackTarget = null;
        List<Long> protectedIds = new ArrayList<>();
        for (NightAction a : actions) {
            if (a.getActionRole() == Role.TRAITOR && attackTarget == null) attackTarget = a.getTargetId();
            if (a.getActionRole() == Role.DOCTOR) protectedIds.add(a.getTargetId());
        }

        Long killedId = GameRules.nightKill(attackTarget, protectedIds);
        Player killed = null;
        if (killedId != null) {
            killed = playerRepository.findById(killedId).orElse(null);
            if (killed != null && killed.isAlive()) {
                killed.setAlive(false);
                playerRepository.save(killed);
            } else {
                killed = null;
            }
        }

        actions.forEach(a -> a.setResolved(true));
        nightActionRepository.saveAll(actions);

        // Public wording is identical for "protected" and "no attack", so the
        // Doctor's success never leaks.
        if (killed != null) {
            String name = displayName(killed);
            gameService.recordEvent(game, "NIGHT", name + " was taken by the night.", name);
        } else {
            gameService.recordEvent(game, "NIGHT", "Dawn breaks. Nobody was eliminated tonight.", null);
        }

        gameService.evaluateWinner(game);
        if (!game.isFinished()) {
            game.setPhase("DAY");
        }
        gameService.updateGame(game);
        return killed;
    }

    private Player currentActor(String roomCode, Role role) {
        Player p = authenticationService.getCurrentPlayer(roomCode);
        if (!p.isAlive()) throw new IllegalStateException("Dead players cannot perform night actions");
        if (p.getRole() != role) throw new IllegalStateException("Only the " + role + " can perform this action");
        return p;
    }

    private void ensureNight(Game g) {
        if (g.isFinished() || !"RUNNING".equals(g.getStatus()) || !"NIGHT".equals(g.getPhase()))
            throw new IllegalStateException("This action is only available during NIGHT");
    }

    private Player target(Long id, String roomCode) {
        if (id == null) throw new IllegalStateException("A target is required");
        Player p = playerRepository.findById(id).orElseThrow(() -> new IllegalStateException("Target player not found"));
        if (!roomCode.equals(p.getRoomCode())) throw new IllegalStateException("Target is not in this room");
        return p;
    }

    private void ensureAlive(Player p) { if (!p.isAlive()) throw new IllegalStateException("Target is already eliminated"); }

    private void ensureOneAction(String roomCode, Long actorId, Role role) {
        if (nightActionRepository.existsByRoomCodeAndActorIdAndActionRoleAndResolved(roomCode, actorId, role, false))
            throw new IllegalStateException("You have already acted this night");
    }

    private NightAction save(String roomCode, Long actorId, Long targetId, Role role) {
        NightAction a = new NightAction();
        a.setRoomCode(roomCode); a.setActorId(actorId); a.setTargetId(targetId); a.setActionRole(role); a.setResolved(false);
        return nightActionRepository.save(a);
    }

    private String displayName(Player p) {
        return userRepository.findById(p.getUserId()).map(User::getUsername).orElse("Player " + p.getUserId());
    }
}
