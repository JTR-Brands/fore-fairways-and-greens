package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class GameCreatedEvent extends GameEvent {
    private final UUID creatorId;
    private final boolean vsNpc;

    @Override
    public String getEventType() {
        return "GAME_CREATED";
    }
}
