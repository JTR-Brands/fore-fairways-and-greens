package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Getter
@SuperBuilder
public abstract class GameEvent {
    private final UUID eventId = UUID.randomUUID();
    private final UUID gameId;
    private final Instant occurredAt = Instant.now();

    public abstract String getEventType();
}
