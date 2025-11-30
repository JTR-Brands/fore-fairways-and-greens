package com.fore.game.domain.events;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PlayerBankruptEvent extends GameEvent {
    private final UUID playerId;
    private final UUID creditorId;

    @Override
    public String getEventType() {
        return "PLAYER_BANKRUPT";
    }
}
