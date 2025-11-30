package com.fore.game.application.ports.outbound;

import com.fore.game.domain.model.GameSession;
import com.fore.game.domain.model.enums.GameStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for game persistence.
 * Domain layer depends on this interface; infrastructure provides implementation.
 */
public interface GameRepository {

    GameSession save(GameSession gameSession);

    Optional<GameSession> findById(UUID gameId);

    List<GameSession> findByStatus(GameStatus status);

    List<GameSession> findByPlayerId(UUID playerId);

    List<GameSession> findActiveGamesByPlayerId(UUID playerId);

    void deleteById(UUID gameId);

    boolean existsById(UUID gameId);

    long countByStatus(GameStatus status);
}
