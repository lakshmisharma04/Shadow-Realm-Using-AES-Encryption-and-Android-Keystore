package com.shadowrealm.backend.repository;

import com.shadowrealm.backend.entity.Game;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findByRoomCode(String roomCode);

    /**
     * Loads the game row with a database write lock. Night/vote resolution use it
     * so two simultaneous requests can never resolve the same round twice.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Game g where g.roomCode = :roomCode")
    Optional<Game> findForUpdateByRoomCode(@Param("roomCode") String roomCode);
}
