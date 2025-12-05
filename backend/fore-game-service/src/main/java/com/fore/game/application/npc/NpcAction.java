package com.fore.game.application.npc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a decision made by the NPC engine.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NpcAction {

    private ActionType actionType;
    private UUID targetPropertyId;
    private TradeOfferDecision tradeOffer;
    private String reasoning; // Optional explanation for debugging/display

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
    public static class TradeOfferDecision {
        private UUID targetPlayerId;
        private Set<UUID> offeredPropertyIds;
        private long offeredCurrencyCents;
        private Set<UUID> requestedPropertyIds;
        private long requestedCurrencyCents;
    }

    // Factory methods for common actions
    public static NpcAction rollDice() {
        return NpcAction.builder()
                .actionType(ActionType.ROLL_DICE)
                .build();
    }

    public static NpcAction purchaseProperty(UUID propertyId, String reasoning) {
        return NpcAction.builder()
                .actionType(ActionType.PURCHASE_PROPERTY)
                .targetPropertyId(propertyId)
                .reasoning(reasoning)
                .build();
    }

    public static NpcAction improveProperty(UUID propertyId, String reasoning) {
        return NpcAction.builder()
                .actionType(ActionType.IMPROVE_PROPERTY)
                .targetPropertyId(propertyId)
                .reasoning(reasoning)
                .build();
    }

    public static NpcAction endTurn() {
        return NpcAction.builder()
                .actionType(ActionType.END_TURN)
                .build();
    }

    public static NpcAction acceptTrade(String reasoning) {
        return NpcAction.builder()
                .actionType(ActionType.ACCEPT_TRADE)
                .reasoning(reasoning)
                .build();
    }

    public static NpcAction rejectTrade(String reasoning) {
        return NpcAction.builder()
                .actionType(ActionType.REJECT_TRADE)
                .reasoning(reasoning)
                .build();
    }
}
