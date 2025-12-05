package com.fore.game.application.npc;

import com.fore.common.types.Money;
import com.fore.game.domain.model.*;
import com.fore.game.domain.model.enums.Difficulty;
import com.fore.game.domain.model.enums.TurnPhase;
import lombok.Builder;
import lombok.Data;

import java.util.*;

/**
 * Snapshot of game state provided to NPC engine for decision-making.
 * Immutable view - NPC cannot modify game state directly.
 */
@Data
@Builder
public class GameContext {

    private final UUID gameId;
    private final TurnPhase turnPhase;
    private final int turnNumber;

    // NPC's state
    private final UUID npcPlayerId;
    private final String npcName;
    private final Difficulty difficulty;
    private final int npcPosition;
    private final Money npcCurrency;
    private final Set<UUID> npcOwnedPropertyIds;
    private final boolean npcInSandTrap;

    // Opponent's visible state
    private final UUID opponentPlayerId;
    private final String opponentName;
    private final int opponentPosition;
    private final Money opponentCurrency;
    private final Set<UUID> opponentOwnedPropertyIds;

    // Board state
    private final List<PropertyInfo> allProperties;
    private final PropertyInfo currentTileProperty; // null if not on a property
    private final TradeOffer pendingTrade; // null if no pending trade

    // Computed helpers
    private final Set<String> npcCompleteGroups;
    private final Set<String> opponentCompleteGroups;

    /**
     * Property information visible to NPC.
     */
    @Data
    @Builder
    public static class PropertyInfo {
        private final UUID propertyId;
        private final String name;
        private final String courseGroup;
        private final int position;
        private final Money purchasePrice;
        private final Money baseRent;
        private final Money currentRent;
        private final Money improvementCost;
        private final UUID ownerId;
        private final String improvementLevel;
        private final boolean mortgaged;
        private final boolean canBePurchased;
        private final boolean canBeImproved;
    }

    /**
     * Create context from a game session for the NPC player.
     */
    public static GameContext fromGame(GameSession game, UUID npcPlayerId) {
        PlayerState npc = game.getPlayer(npcPlayerId);
        PlayerState opponent = game.getOpponent(npcPlayerId);
        Board board = game.getBoard();

        // Get current tile's property if applicable
        Tile currentTile = board.getTileAt(npc.getPosition());
        PropertyInfo currentTileProperty = currentTile.getProperty()
                .map(p -> mapProperty(p, board, npc.getPlayerId()))
                .orElse(null);

        // Map all properties
        List<PropertyInfo> allProperties = board.getAllProperties().stream()
                .map(p -> mapProperty(p, board, npc.getPlayerId()))
                .toList();

        // Find complete groups
        Set<String> npcCompleteGroups = board.getCompleteGroupsOwnedBy(npcPlayerId).stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> opponentCompleteGroups = board.getCompleteGroupsOwnedBy(opponent.getPlayerId()).stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet());

        return GameContext.builder()
                .gameId(game.getGameId())
                .turnPhase(game.getTurnPhase())
                .turnNumber(game.getTurnNumber())
                .npcPlayerId(npcPlayerId)
                .npcName(npc.getDisplayName())
                .difficulty(npc.getNpcDifficulty())
                .npcPosition(npc.getPosition())
                .npcCurrency(npc.getCurrency())
                .npcOwnedPropertyIds(new HashSet<>(npc.getOwnedPropertyIds()))
                .npcInSandTrap(npc.isInSandTrap())
                .opponentPlayerId(opponent.getPlayerId())
                .opponentName(opponent.getDisplayName())
                .opponentPosition(opponent.getPosition())
                .opponentCurrency(opponent.getCurrency())
                .opponentOwnedPropertyIds(new HashSet<>(opponent.getOwnedPropertyIds()))
                .allProperties(allProperties)
                .currentTileProperty(currentTileProperty)
                .pendingTrade(game.getPendingTrade())
                .npcCompleteGroups(npcCompleteGroups)
                .opponentCompleteGroups(opponentCompleteGroups)
                .build();
    }

    private static PropertyInfo mapProperty(Property p, Board board, UUID npcPlayerId) {
        boolean ownerHasGroup = p.isOwned() && board.ownsCompleteGroup(p.getOwnerId(), p.getCourseGroup());
        
        return PropertyInfo.builder()
                .propertyId(p.getPropertyId())
                .name(p.getName())
                .courseGroup(p.getCourseGroup().name())
                .position(p.getTilePosition())
                .purchasePrice(p.getPurchasePrice())
                .baseRent(p.getBaseRent())
                .currentRent(p.isOwned() ? p.calculateRent(ownerHasGroup) : p.getBaseRent())
                .improvementCost(p.getImprovementCost())
                .ownerId(p.getOwnerId())
                .improvementLevel(p.getImprovementLevel().name())
                .mortgaged(p.isMortgaged())
                .canBePurchased(!p.isOwned())
                .canBeImproved(p.isOwnedBy(npcPlayerId) 
                        && p.canBeImproved() 
                        && board.ownsCompleteGroup(npcPlayerId, p.getCourseGroup()))
                .build();
    }

    // Convenience methods for NPC logic

    public boolean canAfford(Money amount) {
        return npcCurrency.isGreaterThan(amount) || npcCurrency.equals(amount);
    }

    public boolean isOnUnownedProperty() {
        return currentTileProperty != null && currentTileProperty.getOwnerId() == null;
    }

    public boolean canPurchaseCurrentProperty() {
        return currentTileProperty != null 
                && currentTileProperty.isCanBePurchased()
                && canAfford(currentTileProperty.getPurchasePrice());
    }

    public List<PropertyInfo> getImprovableProperties() {
        return allProperties.stream()
                .filter(PropertyInfo::isCanBeImproved)
                .filter(p -> canAfford(p.getImprovementCost()))
                .toList();
    }

    public List<PropertyInfo> getNpcProperties() {
        return allProperties.stream()
                .filter(p -> npcPlayerId.equals(p.getOwnerId()))
                .toList();
    }

    public List<PropertyInfo> getOpponentProperties() {
        return allProperties.stream()
                .filter(p -> opponentPlayerId.equals(p.getOwnerId()))
                .toList();
    }

    public List<PropertyInfo> getUnownedProperties() {
        return allProperties.stream()
                .filter(p -> p.getOwnerId() == null)
                .toList();
    }
}
