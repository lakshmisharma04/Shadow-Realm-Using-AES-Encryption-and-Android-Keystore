package com.shadowrealm.backend.service;

import com.shadowrealm.backend.entity.Player;
import com.shadowrealm.backend.entity.Room;
import com.shadowrealm.backend.exception.ResourceNotFoundException;
import com.shadowrealm.backend.repository.PlayerRepository;
import com.shadowrealm.backend.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class RoomService {
    private static final String ROOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private AuthenticationService authenticationService;


    // =========================================================
    // CREATE ROOM
    // =========================================================

    @Transactional
    public Room createRoom(int maxPlayers) {
        if (maxPlayers < 4 || maxPlayers > 7) {
            throw new IllegalStateException("Room size must be between 4 and 7 players.");
        }

        // Get currently logged-in user
        Long currentUserId =
                authenticationService
                        .getCurrentUser()
                        .getId();

        // Create room
        Room room = new Room();

        room.setHostId(currentUserId);
        room.setMaxPlayers(maxPlayers);
        room.setCurrentPlayers(1);
        room.setStatus("WAITING");
        room.setRoomCode(generateUniqueRoomCode());

        Room savedRoom =
                roomRepository.save(room);


        // =====================================================
        // AUTOMATICALLY ADD HOST AS PLAYER
        // =====================================================

        Player player = new Player();

        player.setUserId(currentUserId);
        player.setRoomCode(savedRoom.getRoomCode());
        player.setHost(true);
        player.setAlive(true);

        playerRepository.save(player);


        return savedRoom;
    }


    // =========================================================
    // JOIN ROOM
    // =========================================================

    @Transactional
    public Room joinRoom(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);

        // Find room
        Room room =
                roomRepository.findLockedByRoomCode(normalizedRoomCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Room not found"
                                )
                        );


        // Room must still be waiting
        if (!"WAITING".equals(room.getStatus())) {

            throw new IllegalStateException(
                    "Game has already started"
            );
        }


        // Get logged-in user
        Long currentUserId =
                authenticationService
                        .getCurrentUser()
                        .getId();


        // Check whether user is already in room
        boolean alreadyPlayer =
                playerRepository
                        .findByUserIdAndRoomCode(
                                currentUserId,
                                normalizedRoomCode
                        )
                        .isPresent();

        if (alreadyPlayer) {

            throw new IllegalStateException(
                    "User is already a player in this room"
            );
        }


        // Check room capacity
        long playerCount = playerRepository.countByRoomCode(normalizedRoomCode);
        if (playerCount >= room.getMaxPlayers()) {

            throw new IllegalStateException(
                    "Room is full"
            );
        }


        // =====================================================
        // ADD USER AS PLAYER
        // =====================================================

        Player player = new Player();

        player.setUserId(currentUserId);
        player.setRoomCode(normalizedRoomCode);
        player.setHost(false);
        player.setAlive(true);

        playerRepository.save(player);


        // Increase player count
        room.setCurrentPlayers((int) playerCount + 1);


        return roomRepository.save(room);
    }


    // =========================================================
    // LEAVE ROOM
    // =========================================================

    @Transactional
    public Room leaveRoom(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);

        Room room =
                roomRepository.findLockedByRoomCode(normalizedRoomCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Room not found"
                                )
                        );

        if (!"WAITING".equals(room.getStatus())) {
            throw new IllegalStateException("Players cannot leave after the game has started.");
        }

        Long currentUserId =
                authenticationService
                        .getCurrentUser()
                        .getId();


        Player player =
                playerRepository
                        .findByUserIdAndRoomCode(
                                currentUserId,
                                normalizedRoomCode
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "You are not a player in this room"
                                )
                        );

        if (player.isHost()) {
            throw new IllegalStateException("The host must delete the waiting room instead of leaving it.");
        }

        // Remove player
        playerRepository.delete(player);

        room.setCurrentPlayers((int) playerRepository.countByRoomCode(normalizedRoomCode));


        return roomRepository.save(room);
    }


    // =========================================================
    // DELETE ROOM
    // =========================================================

    @Transactional
    public void deleteRoom(String roomCode) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);

        Room room =
                roomRepository.findLockedByRoomCode(normalizedRoomCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Room not found"
                                )
                        );

        Long currentUserId = authenticationService.getCurrentUser().getId();
        if (!currentUserId.equals(room.getHostId())) {
            throw new IllegalStateException("Only the room host can delete the room");
        }
        if (!"WAITING".equals(room.getStatus())) {
            throw new IllegalStateException("A started or finished game's room cannot be deleted.");
        }
        playerRepository.deleteAll(playerRepository.findByRoomCode(normalizedRoomCode));
        roomRepository.delete(room);
    }


    // =========================================================
    // GENERATE UNIQUE ROOM CODE
    // =========================================================

    private String generateUniqueRoomCode() {

        String code;

        do {

            StringBuilder builder =
                    new StringBuilder();

            for (int i = 0; i < 6; i++) {

                builder.append(ROOM_CODE_CHARACTERS.charAt(
                        RANDOM.nextInt(ROOM_CODE_CHARACTERS.length())
                ));
            }

            code = builder.toString();

        } while (
                roomRepository.existsByRoomCode(code)
        );

        return code;
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalStateException("Room code is required.");
        }
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }
}