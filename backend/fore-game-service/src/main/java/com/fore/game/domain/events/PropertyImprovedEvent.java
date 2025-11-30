package com.fore.game.domain.events;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.ImprovementLevel;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@SuperBuilder
public class PropertyImprovedEvent extends GameEvent {
    private final UUID playerId;
    private final UUID propertyId;
    private final String propertyName;
    private final ImprovementLevel previousLevel;
    private final ImprovementLevel newLevel;
    private final Money cost;

    @Override
    public String getEventType() {
        return "PROPERTY_IMPROVED";
    }
}
