package com.fore.game.domain.events;

import com.fore.common.types.Money;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class RentPaidEvent extends GameEvent {
    private final UUID payerId;
    private final UUID receiverId;
    private final UUID propertyId;
    private final Money amount;

    @Override
    public String getEventType() {
        return "RENT_PAID";
    }
}
