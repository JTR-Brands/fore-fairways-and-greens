package com.fore.game.infrastructure.persistence;

import com.fore.game.application.ports.outbound.PlayerRepository;
import com.fore.game.infrastructure.persistence.entity.PlayerEntity;
import com.fore.game.infrastructure.persistence.repository.JpaPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PlayerRepositoryAdapter implements PlayerRepository {

    private final JpaPlayerRepository jpaRepository;

    @Override
    @Transactional
    public PlayerRecord save(PlayerRecord player) {
        log.debug("Saving player: {}", player.playerId());

        PlayerEntity entity = PlayerEntity.builder()
                .playerId(player.playerId() != null ? player.playerId() : UUID.randomUUID())
                .email(player.email())
                .displayName(player.displayName())
                .passwordHash(player.passwordHash())
                .build();

        PlayerEntity saved = jpaRepository.save(entity);

        return new PlayerRecord(
                saved.getPlayerId(),
                saved.getEmail(),
                saved.getDisplayName(),
                saved.getPasswordHash()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerRecord> findById(UUID playerId) {
        return jpaRepository.findById(playerId)
                .map(e -> new PlayerRecord(e.getPlayerId(), e.getEmail(), e.getDisplayName(), e.getPasswordHash()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerRecord> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(e -> new PlayerRecord(e.getPlayerId(), e.getEmail(), e.getDisplayName(), e.getPasswordHash()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void deleteById(UUID playerId) {
        jpaRepository.deleteById(playerId);
    }
}
