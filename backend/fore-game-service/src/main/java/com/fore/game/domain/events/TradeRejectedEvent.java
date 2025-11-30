package com.fore.game.domain.events;

import com.fore.game.domain.model.TradeOffer;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class TradeRejectedEvent extends GameEvent {
    private final TradeOffer offer;

    @Override
    public String getEventType() {
        return "TRADE_REJECTED";
    }
}
