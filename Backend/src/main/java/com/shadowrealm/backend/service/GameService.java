package com.shadowrealm.backend.service;

import com.shadowrealm.backend.entity.Game;
import com.shadowrealm.backend.entity.Role;
import com.shadowrealm.backend.entity.Room;
import com.shadowrealm.backend.exception.DuplicateResourceException;
import com.shadowrealm.backend.exception.ResourceNotFoundException;
import com.shadowrealm.backend.entity.NightAction;
import com.shadowrealm.backend.entity.Vote;
import com.shadowrealm.backend.repository.GameRepository;
import com.shadowrealm.backend.repository.NightActionRepository;
import com.shadowrealm.backend.repository.VoteRepository;
import com.shadowrealm.backend.repository.PlayerRepository;
import com.shadowrealm.backend.repository.RoomRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GameService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private SecretCryptoService secretCryptoService;

    @Autowired
    private NightActionRepository nightActionRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private MatchHistoryService matchHistoryService;


    // =========================================================
    // SECRET WORDS
    // =========================================================

    private static final String[][] SECRET_PAIRS = {

            {"CLOCK", "measures passing time"},
            {"CASTLE", "a fortified royal home"},
            {"THUNDER", "a loud sound after lightning"},
            {"OCEAN", "a vast body of salt water"},
            {"CANDLE", "gives light when it burns"},
            {"FOREST", "a place filled with trees"},
            {"MIRROR", "shows your reflection"},
            {"COMPASS", "helps you find direction"},
            {"VOLCANO", "a mountain that can erupt"},
            {"KEY", "opens a lock"},
            {"SHADOW", "follows you in the light"},
            {"CROWN", "worn by a monarch"},
            {"DRAGON", "a legendary fire-breathing creature"},
            {"LIBRARY", "a place full of books"}

    };


    // =========================================================
    // START GAME
    // =========================================================

    @Transactional
    public Game startGame(String roomCode) {

        // -----------------------------------------------------
        // Find room
        // -----------------------------------------------------

        Room room =
                roomRepository.findLockedByRoomCode(roomCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Room not found: " + roomCode
                                )
                        );


        // -----------------------------------------------------
        // Get logged-in user
        // -----------------------------------------------------

        Long currentUserId =
                authenticationService
                        .getCurrentUser()
                        .getId();

        // -----------------------------------------------------
        // Verify host using ROOM hostId
        // -----------------------------------------------------

        if (!currentUserId.equals(room.getHostId())) {

            throw new IllegalStateException(
                    "Only the room host can start the game."
            );
        }


        // -----------------------------------------------------
        // Make sure room is still waiting
        // -----------------------------------------------------

        if (!"WAITING".equals(room.getStatus())) {

            throw new IllegalStateException(
                    "This room has already started."
            );
        }


        // -----------------------------------------------------
        // Get players
        // -----------------------------------------------------

        List<com.shadowrealm.backend.entity.Player> players =
                playerRepository.findByRoomCode(roomCode);


        // -----------------------------------------------------
        // Minimum 4 players
        // -----------------------------------------------------

        if (players.size() < 4) {

            throw new IllegalStateException(
                    "At least 4 players are required to start the game."
            );
        }


        // -----------------------------------------------------
        // Prevent duplicate game
        // -----------------------------------------------------

        if (gameRepository.findByRoomCode(roomCode).isPresent()) {

            throw new DuplicateResourceException(
                    "A game already exists for this room."
            );
        }


        // -----------------------------------------------------
        // Assign roles
        // -----------------------------------------------------

        playerService.assignRoles(roomCode);


        // -----------------------------------------------------
        // Create game
        // -----------------------------------------------------

        Game game = new Game();

        game.setRoomCode(roomCode);
        game.setStarted(true);
        game.setFinished(false);
        game.setStatus("RUNNING");
        game.setPhase("NIGHT");
        game.setWinner(null);
        game.setCurrentRound(1);
        game.setLastEventSeq(0);


        // -----------------------------------------------------
        // Generate encrypted secret
        // -----------------------------------------------------

        rotateSecret(game);


        // -----------------------------------------------------
        // Update room
        // -----------------------------------------------------

        room.setStatus("RUNNING");

        roomRepository.save(room);


        // -----------------------------------------------------
        // Save game
        // -----------------------------------------------------

        Game savedGame =
                gameRepository.save(game);


        return savedGame;
    }


    // =========================================================
    // GET GAME
    // =========================================================

    public Game getGame(String roomCode) {

        return gameRepository
                .findByRoomCode(roomCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Game not found for room: "
                                        + roomCode
                        )
                );
    }


    /** Loads the game with a write lock (use inside @Transactional methods). */
    public Game getGameForUpdate(String roomCode) {
        return gameRepository
                .findForUpdateByRoomCode(roomCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Game not found for room: " + roomCode
                        )
                );
    }

    /** Game state for a player who belongs to the room. */
    public Game getGameForMember(String roomCode) {
        authenticationService.getCurrentPlayer(roomCode);
        return getGame(roomCode);
    }

    // =========================================================
    // NEXT PHASE
    // =========================================================

    @Transactional
    public Game nextPhase(String roomCode) {

        requireHost(roomCode);

        Game game = getGameForUpdate(roomCode);

        if (!game.isStarted() || game.isFinished()) {
            throw new IllegalStateException("Game is not active.");
        }

        if (!"RUNNING".equals(game.getStatus())) {
            throw new IllegalStateException("Game is not running.");
        }

        switch (game.getPhase()) {

            // The host decides when the discussion ends.
            case "DAY" -> game.setPhase("VOTING");

            // NIGHT ends only by resolving the night actions, and VOTING
            // only by resolving the votes. Skipping them by hand would leave
            // stale actions/votes behind and break the next round.
            case "NIGHT" ->
                    throw new IllegalStateException(
                            "Night ends when all night actions are submitted.");

            case "VOTING" ->
                    throw new IllegalStateException(
                            "Voting ends when every living player has voted.");

            // Legacy phase: the current flow never enters it, but a game that
            // is already in ELIMINATION can still be moved on safely.
            case "ELIMINATION" -> {
                evaluateWinner(game);
                if (!game.isFinished()) {
                    beginNextRound(game);
                }
            }

            default ->
                    throw new IllegalStateException(
                            "Invalid game phase: " + game.getPhase());
        }

        return gameRepository.save(game);
    }


    // =========================================================
    // END GAME
    // =========================================================

    @Transactional
    public Game endGame(String roomCode) {

        requireHost(roomCode);

        Game game = getGameForUpdate(roomCode);

        if (!game.isFinished()) {
            finishGame(game, null, "HOST");
        }

        return gameRepository.save(game);
    }


    // =========================================================
    // CHECK WINNER
    // =========================================================

    @Transactional
    public Game checkWinner(String roomCode) {

        authenticationService.getCurrentPlayer(roomCode);

        Game game = getGameForUpdate(roomCode);

        evaluateWinner(game);

        return gameRepository.save(game);
    }

    /**
     * Applies the win conditions to the given (already loaded) game.
     * Does nothing if the match is already over, so it is safe to call twice.
     * The caller is responsible for saving the game.
     */
    public boolean evaluateWinner(Game game) {

        if (game.isFinished()) {
            return true;
        }

        String roomCode = game.getRoomCode();

        long traitors = playerRepository.countByRoomCodeAndAliveAndRole(
                roomCode, true, Role.TRAITOR);

        long others = playerRepository.countByRoomCodeAndAliveAndRoleNot(
                roomCode, true, Role.TRAITOR);

        Role winner = GameRules.winnerFor(traitors, others);

        if (winner != null) {
            finishGame(game, winner, "WIN_CONDITION");
            return true;
        }

        return false;
    }

    /** Marks the match finished, closes the room and saves the match history. */
    public void finishGame(Game game, Role winner, String endedBy) {

        game.setWinner(winner);
        game.setFinished(true);
        game.setStatus("FINISHED");

        roomRepository.findByRoomCode(game.getRoomCode()).ifPresent(room -> {
            room.setStatus("FINISHED");
            roomRepository.save(room);
        });

        matchHistoryService.recordFinishedMatch(game, endedBy);
    }

    /** Stores a public result that every client can display. */
    public void recordEvent(Game game, String type, String message, String eliminatedName) {
        game.setLastEventSeq(game.getLastEventSeq() + 1);
        game.setLastEventType(type);
        game.setLastEventMessage(message);
        game.setLastEliminatedName(eliminatedName);
    }


    // =========================================================
    // UPDATE GAME
    // =========================================================

    public Game updateGame(Game game) {

        return gameRepository.save(game);
    }


    // =========================================================
    // RESOLVE ELIMINATION
    // =========================================================

    @Transactional
    public Game resolveElimination(String roomCode) {

        requireHost(roomCode);

        Game game = getGameForUpdate(roomCode);


        if (!"RUNNING".equals(game.getStatus())) {

            throw new IllegalStateException(
                    "Game is not running."
            );
        }


        if (!"ELIMINATION".equals(game.getPhase())) {

            throw new IllegalStateException(
                    "Elimination is not active."
            );
        }


        evaluateWinner(game);


        if (!game.isFinished()) {

            beginNextRound(game);
        }


        return gameRepository.save(game);
    }


    // =========================================================
    // BEGIN NEXT ROUND
    // =========================================================

    public Game beginNextRound(Game game) {

        game.setCurrentRound(
                game.getCurrentRound() + 1
        );

        rotateSecret(game);

        game.setPhase("NIGHT");

        // Start the new round with no stale night actions or votes.
        List<NightAction> staleActions =
                nightActionRepository.findByRoomCodeAndResolved(game.getRoomCode(), false);
        nightActionRepository.deleteAll(staleActions);

        List<Vote> staleVotes = new java.util.ArrayList<>();
        for (Vote v : voteRepository.findByRoomCode(game.getRoomCode())) {
            if (!v.isCounted()) staleVotes.add(v);
        }
        voteRepository.deleteAll(staleVotes);

        return game;
    }


    // =========================================================
    // REQUIRE HOST
    // =========================================================

    public void requireHost(String roomCode) {

        Room room =
                roomRepository
                        .findByRoomCode(roomCode)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Room not found: "
                                                + roomCode
                                )
                        );


        Long currentUserId =
                authenticationService
                        .getCurrentUser()
                        .getId();


        if (!currentUserId.equals(room.getHostId())) {

            throw new IllegalStateException(
                    "Only the room host can perform this action."
            );
        }
    }


    // =========================================================
    // ROTATE SECRET
    // =========================================================

    private void rotateSecret(Game game) {

        int index =
                ThreadLocalRandom.current()
                        .nextInt(
                                SECRET_PAIRS.length
                        );


        String word =
                SECRET_PAIRS[index][0];

        String hint =
                SECRET_PAIRS[index][1];


        game.setSecretWord(
                secretCryptoService.encrypt(word)
        );

        game.setSecretHint(
                secretCryptoService.encrypt(hint)
        );
    }
}