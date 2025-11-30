package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class TurnStartedEvent extends GameEvent {
    private final UUID playerId;
    private final int turnNumber;

    @Override
    public String getEventType() {
        return "TURN_STARTED";
    }
}
