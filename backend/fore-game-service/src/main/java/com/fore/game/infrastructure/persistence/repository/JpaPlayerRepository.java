package com.fore.game.infrastructure.persistence.repository;

import com.fore.game.infrastructure.persistence.entity.PlayerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaPlayerRepository extends JpaRepository<PlayerEntity, UUID> {

    Optional<PlayerEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
