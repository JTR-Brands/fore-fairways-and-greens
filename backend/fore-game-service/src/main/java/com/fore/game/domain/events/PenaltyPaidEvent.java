package com.fore.game.domain.events;

import com.fore.common.types.Money;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PenaltyPaidEvent extends GameEvent {
    private final UUID playerId;
    private final Money amount;
    private final String reason;

    @Override
    public String getEventType() {
        return "PENALTY_PAID";
    }
}
