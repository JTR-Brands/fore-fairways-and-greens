package com.fore.game.domain.events;

import com.fore.game.domain.model.DiceRoll;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class DiceRolledEvent extends GameEvent {
    private final UUID playerId;
    private final DiceRoll roll;

    @Override
    public String getEventType() {
        return "DICE_ROLLED";
    }
}
