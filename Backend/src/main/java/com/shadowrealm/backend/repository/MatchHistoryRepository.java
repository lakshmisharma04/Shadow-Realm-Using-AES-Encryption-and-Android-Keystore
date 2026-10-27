package com.shadowrealm.backend.repository;

import com.shadowrealm.backend.entity.MatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchHistoryRepository extends JpaRepository<MatchHistory, Long> {
    boolean existsByRoomCode(String roomCode);
    Optional<MatchHistory> findByRoomCode(String roomCode);
}
