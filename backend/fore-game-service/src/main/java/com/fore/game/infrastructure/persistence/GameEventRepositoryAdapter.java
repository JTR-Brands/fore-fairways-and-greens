package com.fore.game.infrastructure.persistence;

import com.fore.game.application.ports.outbound.GameEventRepository;
import com.fore.game.domain.events.GameEvent;
import com.fore.game.infrastructure.persistence.entity.GameEventEntity;
import com.fore.game.infrastructure.persistence.mapper.GameEventMapper;
import com.fore.game.infrastructure.persistence.repository.JpaGameEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GameEventRepositoryAdapter implements GameEventRepository {

    private final JpaGameEventRepository jpaRepository;
    private final GameEventMapper mapper;

    @Override
    @Transactional
    public void appendEvents(UUID gameId, List<GameEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        log.debug("Appending {} events for game {}", events.size(), gameId);

        long nextSeq = getNextSequenceNumber(gameId);

        List<GameEventEntity> entities = events.stream()
                .map(event -> mapper.toEntity(event, nextSeq + events.indexOf(event)))
                .toList();

        jpaRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameEvent> findByGameId(UUID gameId) {
        // For now, return raw event data as we don't need full reconstruction
        // This would be enhanced if we need full event replay
        log.debug("Finding events for game {}", gameId);
        return List.of(); // Simplified - events stored but not fully reconstructed
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameEvent> findByGameIdAfterSequence(UUID gameId, long afterSequenceNum) {
        log.debug("Finding events for game {} after sequence {}", gameId, afterSequenceNum);
        return List.of(); // Simplified for MVP
    }

    @Override
    @Transactional(readOnly = true)
    public long getNextSequenceNumber(UUID gameId) {
        return jpaRepository.findMaxSequenceNumByGameId(gameId) + 1;
    }
}
