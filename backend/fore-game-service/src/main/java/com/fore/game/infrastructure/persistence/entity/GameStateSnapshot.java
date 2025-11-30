package com.fore.game.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * JSON structure stored in the game_state_snapshot column.
 * Captures complete game state for snapshot persistence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameStateSnapshot implements Serializable {

    private List<PlayerSnapshot> players;
    private List<TileSnapshot> tiles;
    private TradeOfferSnapshot pendingTrade;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerSnapshot implements Serializable {
        private UUID playerId;
        private String displayName;
        private boolean npc;
        private String npcDifficulty; // null for humans
        private int position;
        private long currencyCents;
        private List<UUID> ownedPropertyIds;
        private boolean bankrupt;
        private int turnsInSandTrap;
        private int consecutiveDoubles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TileSnapshot implements Serializable {
        private UUID tileId;
        private int position;
        private String type;
        private String name;
        private PropertySnapshot property; // null for non-property tiles
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertySnapshot implements Serializable {
        private UUID propertyId;
        private String name;
        private String courseGroup;
        private int tilePosition;
        private long purchasePriceCents;
        private long baseRentCents;
        private long rentWithClubhouseCents;
        private long rentWithResortCents;
        private long improvementCostCents;
        private UUID ownerId;
        private String improvementLevel;
        private boolean mortgaged;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TradeOfferSnapshot implements Serializable {
        private UUID offerId;
        private UUID offeringPlayerId;
        private UUID receivingPlayerId;
        private List<UUID> offeredPropertyIds;
        private long offeredCurrencyCents;
        private List<UUID> requestedPropertyIds;
        private long requestedCurrencyCents;
        private String status;
    }
}
