package com.fore.game.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameStateResponse {

    private UUID gameId;
    private String status;
    private UUID currentPlayerId;
    private String turnPhase;
    private int turnNumber;
    private UUID winnerId;

    private List<PlayerStateDto> players;
    private BoardDto board;
    private TradeOfferDto pendingTrade;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerStateDto {
        private UUID playerId;
        private String displayName;
        private boolean npc;
        private String npcDifficulty;
        private int position;
        private long currencyCents;
        private List<UUID> ownedPropertyIds;
        private boolean bankrupt;
        private boolean inSandTrap;
        private int turnsInSandTrap;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoardDto {
        private List<TileDto> tiles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TileDto {
        private UUID tileId;
        private int position;
        private String type;
        private String name;
        private PropertyDto property;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PropertyDto {
        private UUID propertyId;
        private String name;
        private String courseGroup;
        private String courseGroupColor;
        private long purchasePriceCents;
        private long baseRentCents;
        private long currentRentCents;
        private long improvementCostCents;
        private UUID ownerId;
        private String ownerName;
        private String improvementLevel;
        private boolean mortgaged;
        private boolean canBePurchased;
        private boolean canBeImproved;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TradeOfferDto {
        private UUID offerId;
        private UUID offeringPlayerId;
        private String offeringPlayerName;
        private UUID receivingPlayerId;
        private String receivingPlayerName;
        private List<UUID> offeredPropertyIds;
        private long offeredCurrencyCents;
        private List<UUID> requestedPropertyIds;
        private long requestedCurrencyCents;
        private String status;
    }
}
