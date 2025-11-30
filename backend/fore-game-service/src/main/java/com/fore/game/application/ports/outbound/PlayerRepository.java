package com.fore.game.application.ports.outbound;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for player account persistence.
 */
public interface PlayerRepository {

    PlayerRecord save(PlayerRecord player);

    Optional<PlayerRecord> findById(UUID playerId);

    Optional<PlayerRecord> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(UUID playerId);

    /**
     * Simple record for player account data.
     * Domain doesn't need a rich Player entity for MVP.
     */
    record PlayerRecord(
            UUID playerId,
            String email,
            String displayName,
            String passwordHash
    ) {}
}
