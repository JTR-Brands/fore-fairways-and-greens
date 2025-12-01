package com.fore.game.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerActionRequest {

    @NotNull(message = "Player ID is required")
    private UUID playerId;

    @NotNull(message = "Action type is required")
    private ActionType actionType;

    // Optional fields based on action type
    private UUID targetPropertyId;
    private TradeOfferRequest tradeOffer;

    public enum ActionType {
        ROLL_DICE,
        PURCHASE_PROPERTY,
        IMPROVE_PROPERTY,
        PROPOSE_TRADE,
        ACCEPT_TRADE,
        REJECT_TRADE,
        END_TURN
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TradeOfferRequest {
        private UUID receivingPlayerId;
        private Set<UUID> offeredPropertyIds;
        private long offeredCurrencyCents;
        private Set<UUID> requestedPropertyIds;
        private long requestedCurrencyCents;
    }
}
