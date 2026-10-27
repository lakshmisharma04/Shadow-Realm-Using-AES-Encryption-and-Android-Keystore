package com.shadowrealm.backend.service;

import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.entity.Role;
import com.shadowrealm.backend.repository.PlayerRepository;
import com.shadowrealm.backend.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RoomRepository roomRepository;

    // =========================================================
    // ADD CURRENT LOGGED-IN USER AS PLAYER
    // =========================================================

    @Transactional
    public Player addCurrentPlayer(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalStateException("Room code is required.");
        }
        String normalizedRoomCode = roomCode.trim().toUpperCase(java.util.Locale.ROOT);
        var room = roomRepository.findLockedByRoomCode(normalizedRoomCode)
                .orElseThrow(() -> new com.shadowrealm.backend.exception.ResourceNotFoundException("Room not found"));
        if (!"WAITING".equals(room.getStatus())) {
            throw new IllegalStateException("The game has already started.");
        }

        // Get the currently logged-in user from the JWT
        var currentUser =
                authenticationService.getCurrentUser();

        Long userId = currentUser.getId();


        // -----------------------------------------------------
        // Prevent the same user from joining the same room twice
        // -----------------------------------------------------

        if (playerRepository
                .findByUserIdAndRoomCode(
                        userId,
                        normalizedRoomCode
                )
                .isPresent()) {

            throw new IllegalStateException(
                    "User is already a player in this room"
            );
        }

        long playerCount = playerRepository.countByRoomCode(normalizedRoomCode);
        if (playerCount >= room.getMaxPlayers()) {
            throw new IllegalStateException("Room is full");
        }

        // -----------------------------------------------------
        // Create player
        // -----------------------------------------------------

        Player player = new Player();

        player.setUserId(userId);
        player.setRoomCode(normalizedRoomCode);
        player.setHost(userId.equals(room.getHostId()));
        player.setAlive(true);
        player.setRole(null);

        Player saved = playerRepository.save(player);
        room.setCurrentPlayers((int) playerCount + 1);
        roomRepository.save(room);
        return saved;
    }


    // =========================================================
    // GET ALL PLAYERS IN ROOM
    // =========================================================

    public List<Player> getPlayers(
            String roomCode
    ) {

        return playerRepository
                .findByRoomCode(roomCode);
    }


    // =========================================================
    // GET ALL ALIVE PLAYERS
    // =========================================================

    public List<Player> getAlivePlayers(
            String roomCode
    ) {

        return playerRepository
                .findByRoomCodeAndAlive(
                        roomCode,
                        true
                );
    }


    // =========================================================
    // GET PLAYER BY ID
    // =========================================================

    public Player getPlayer(
            Long id
    ) {

        return playerRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Player not found"
                        )
                );
    }


    // =========================================================
    // UPDATE PLAYER
    // =========================================================

    public Player updatePlayer(
            Player player
    ) {

        return playerRepository.save(player);
    }


    // =========================================================
    // DELETE PLAYER
    // =========================================================

    public void deletePlayer(
            Long id
    ) {

        playerRepository.deleteById(id);
    }


    // =========================================================
    // COUNT PLAYERS
    // =========================================================

    public long countPlayers(
            String roomCode
    ) {

        return playerRepository
                .countByRoomCode(roomCode);
    }


    // =========================================================
    // ASSIGN ROLES RANDOMLY
    // =========================================================

    public void assignRoles(
            String roomCode
    ) {

        List<Player> players =
                playerRepository
                        .findByRoomCode(roomCode);


        // -----------------------------------------------------
        // Minimum 4 players required
        // -----------------------------------------------------

        if (players.size() < 4) {

            throw new RuntimeException(
                    "At least 4 players are required to start the game."
            );
        }


        // -----------------------------------------------------
        // Randomize players
        // -----------------------------------------------------

        Collections.shuffle(players);


        // -----------------------------------------------------
        // Assign roles
        // -----------------------------------------------------

        players.get(0)
                .setRole(Role.TRAITOR);

        players.get(1)
                .setRole(Role.SEER);

        players.get(2)
                .setRole(Role.DOCTOR);


        // -----------------------------------------------------
        // Remaining players become villagers
        // -----------------------------------------------------

        for (
                int i = 3;
                i < players.size();
                i++
        ) {

            players.get(i)
                    .setRole(Role.VILLAGER);
        }


        playerRepository.saveAll(players);
    }


    // =========================================================
    // ELIMINATE PLAYER
    // =========================================================

    public Player eliminatePlayer(
            Long id
    ) {

        Player player =
                playerRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Player not found"
                                )
                        );


        // -----------------------------------------------------
        // Prevent eliminating an already dead player
        // -----------------------------------------------------

        if (!player.isAlive()) {

            throw new IllegalStateException(
                    "Player is already eliminated"
            );
        }


        // -----------------------------------------------------
        // Mark player as dead
        // -----------------------------------------------------

        player.setAlive(false);

        return playerRepository.save(player);
    }
}