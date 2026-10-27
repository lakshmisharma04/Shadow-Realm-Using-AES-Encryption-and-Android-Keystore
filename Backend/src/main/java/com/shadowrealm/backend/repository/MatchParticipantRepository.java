package com.shadowrealm.backend.repository;

import com.shadowrealm.backend.entity.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {
    List<MatchParticipant> findByUserId(Long userId);
    List<MatchParticipant> findByMatchId(Long matchId);
    Optional<MatchParticipant> findByMatchIdAndUserId(Long matchId, Long userId);
}
