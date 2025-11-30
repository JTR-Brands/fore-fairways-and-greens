package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PlayerMovedEvent extends GameEvent {
    private final UUID playerId;
    private final int fromPosition;
    private final int toPosition;
    private final boolean passedStart;

    @Override
    public String getEventType() {
        return "PLAYER_MOVED";
    }
}
