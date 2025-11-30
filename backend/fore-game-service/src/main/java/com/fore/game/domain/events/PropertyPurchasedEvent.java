package com.fore.game.domain.events;

import com.fore.common.types.Money;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PropertyPurchasedEvent extends GameEvent {
    private final UUID playerId;
    private final UUID propertyId;
    private final String propertyName;
    private final Money price;

    @Override
    public String getEventType() {
        return "PROPERTY_PURCHASED";
    }
}
