package com.fore.game.infrastructure.persistence;

import com.fore.game.application.ports.outbound.GameRepository;
import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.enums.GameStatus;
import com.fore.game.infrastructure.persistence.entity.GameSessionEntity;
import com.fore.game.infrastructure.persistence.mapper.GameSessionMapper;
import com.fore.game.infrastructure.persistence.repository.JpaGameSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class GameRepositoryAdapter implements GameRepository {

    private final JpaGameSessionRepository jpaRepository;
    private final GameSessionMapper mapper;

    @Override
    @Transactional
    public GameSession save(GameSession gameSession) {
        log.debug("Saving game session: {}", gameSession.getGameId());
        
        GameSessionEntity entity = mapper.toEntity(gameSession);
        GameSessionEntity saved = jpaRepository.save(entity);
        
        log.debug("Saved game session: {} with status {}", saved.getGameId(), saved.getStatus());
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameSession> findById(UUID gameId) {
        log.debug("Finding game session: {}", gameId);
        return jpaRepository.findById(gameId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSession> findByStatus(GameStatus status) {
        return jpaRepository.findByStatus(status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSession> findByPlayerId(UUID playerId) {
        return jpaRepository.findByPlayerId(playerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameSession> findActiveGamesByPlayerId(UUID playerId) {
        return jpaRepository.findActiveGamesByPlayerId(playerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID gameId) {
        log.debug("Deleting game session: {}", gameId);
        jpaRepository.deleteById(gameId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID gameId) {
        return jpaRepository.existsById(gameId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(GameStatus status) {
        return jpaRepository.countByStatus(status.name());
    }
}
