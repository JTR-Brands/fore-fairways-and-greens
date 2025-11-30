package com.fore.game.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "game_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSessionEntity {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "current_player_id")
    private UUID currentPlayerId;

    @Column(name = "turn_phase", nullable = false, length = 20)
    private String turnPhase;

    @Column(name = "turn_number", nullable = false)
    private int turnNumber;

    @Column(name = "winner_id")
    private UUID winnerId;

    /**
     * JSONB column storing the complete game state snapshot.
     * This includes board state, player states, property ownership, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "game_state_snapshot", columnDefinition = "jsonb", nullable = false)
    private GameStateSnapshot gameStateSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private Set<GameParticipantEntity> participants = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (lastActivityAt == null) lastActivityAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        lastActivityAt = Instant.now();
    }

    public void addParticipant(GameParticipantEntity participant) {
        participants.add(participant);
        participant.setGameSession(this);
    }
}
