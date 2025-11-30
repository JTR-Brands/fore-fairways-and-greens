package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class GameStartedEvent extends GameEvent {
    private final UUID firstPlayerId;

    @Override
    public String getEventType() {
        return "GAME_STARTED";
    }
}
