package com.fore.game.infrastructure.persistence.repository;

import com.fore.game.infrastructure.persistence.entity.GameSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaGameSessionRepository extends JpaRepository<GameSessionEntity, UUID> {

    List<GameSessionEntity> findByStatus(String status);

    @Query("SELECT g FROM GameSessionEntity g JOIN g.participants p WHERE p.playerId = :playerId")
    List<GameSessionEntity> findByPlayerId(@Param("playerId") UUID playerId);

    @Query("SELECT g FROM GameSessionEntity g JOIN g.participants p WHERE p.playerId = :playerId AND g.status IN ('WAITING', 'IN_PROGRESS')")
    List<GameSessionEntity> findActiveGamesByPlayerId(@Param("playerId") UUID playerId);

    long countByStatus(String status);
}
