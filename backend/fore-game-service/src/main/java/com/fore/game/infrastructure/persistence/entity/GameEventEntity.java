package com.fore.game.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "game_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameEventEntity {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "sequence_num", nullable = false)
    private long sequenceNum;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_payload", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventPayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
