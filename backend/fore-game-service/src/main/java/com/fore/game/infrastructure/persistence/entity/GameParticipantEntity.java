package com.fore.game.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_participants")
@IdClass(GameParticipantId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameParticipantEntity {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Id
    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "is_npc", nullable = false)
    private boolean npc;

    @Column(name = "npc_difficulty", length = 20)
    private String npcDifficulty;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", insertable = false, updatable = false)
    private GameSessionEntity gameSession;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }
}
