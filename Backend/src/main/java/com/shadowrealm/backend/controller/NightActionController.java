package com.shadowrealm.backend.controller;

import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.dto.ActionResponse;
import com.shadowrealm.backend.dto.PlayerResponse;
import com.shadowrealm.backend.entity.User;
import com.shadowrealm.backend.repository.UserRepository;
import com.shadowrealm.backend.service.NightActionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/night")
public class NightActionController {
    private final NightActionService service;
    private final UserRepository userRepository;
    public NightActionController(NightActionService service, UserRepository userRepository) { this.service = service; this.userRepository = userRepository; }

    private PlayerResponse toPublic(Player p) {
        if (p == null) return null;
        String displayName = userRepository.findById(p.getUserId()).map(User::getUsername).orElse("Player " + p.getUserId());
        return new PlayerResponse(p.getId(), p.getUserId(), displayName, p.isAlive(), p.isHost());
    }

    @PostMapping("/attack")
    public ResponseEntity<?> attack(@RequestParam String roomCode, @RequestParam Long targetId) {
        try { return ResponseEntity.ok(service.traitorAttack(roomCode, targetId)); }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/protect")
    public ResponseEntity<?> protect(@RequestParam String roomCode, @RequestParam Long targetId) {
        try { return ResponseEntity.ok(service.doctorProtect(roomCode, targetId)); }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/investigate")
    public ResponseEntity<?> investigate(@RequestParam String roomCode, @RequestParam Long targetId) {
        try { return ResponseEntity.ok(service.seerInvestigate(roomCode, targetId)); }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/resolve")
    public ResponseEntity<?> resolve(@RequestParam String roomCode) {
        try {
            Player eliminated = service.resolveNight(roomCode);
            // Always a JSON body (an empty 200 body cannot be parsed by Retrofit/Gson).
            // The public result for every player is stored on the game (lastEvent*).
            return ResponseEntity.ok(new ActionResponse("NIGHT_RESOLVED",
                    eliminated == null ? "Nobody was eliminated." : toPublic(eliminated).getDisplayName() + " was eliminated."));
        }
        catch (IllegalStateException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}
