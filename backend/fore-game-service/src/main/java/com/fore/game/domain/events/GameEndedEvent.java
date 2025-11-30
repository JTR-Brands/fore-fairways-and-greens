package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class GameEndedEvent extends GameEvent {
    private final UUID winnerId;
    private final String reason;

    @Override
    public String getEventType() {
        return "GAME_ENDED";
    }
}
