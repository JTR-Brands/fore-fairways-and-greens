package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PlayerSentToSandTrapEvent extends GameEvent {
    private final UUID playerId;

    @Override
    public String getEventType() {
        return "PLAYER_SENT_TO_SAND_TRAP";
    }
}
