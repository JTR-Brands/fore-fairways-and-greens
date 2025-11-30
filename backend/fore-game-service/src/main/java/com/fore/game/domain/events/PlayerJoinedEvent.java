package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PlayerJoinedEvent extends GameEvent {
    private final UUID playerId;
    private final String playerName;

    @Override
    public String getEventType() {
        return "PLAYER_JOINED";
    }
}
