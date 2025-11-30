package com.fore.game.infrastructure.persistence.repository;

import com.fore.game.infrastructure.persistence.entity.GameEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaGameEventRepository extends JpaRepository<GameEventEntity, UUID> {

    List<GameEventEntity> findByGameIdOrderBySequenceNumAsc(UUID gameId);

    List<GameEventEntity> findByGameIdAndSequenceNumGreaterThanOrderBySequenceNumAsc(UUID gameId, long sequenceNum);

    @Query("SELECT COALESCE(MAX(e.sequenceNum), 0) FROM GameEventEntity e WHERE e.gameId = :gameId")
    long findMaxSequenceNumByGameId(@Param("gameId") UUID gameId);
}
