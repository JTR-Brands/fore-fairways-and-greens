package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.Difficulty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.*;

/**
 * Represents a player's state within a game session.
 */
@Getter
@ToString
public class PlayerState {

    private final UUID playerId;
    private final String displayName;
    private final boolean npc;
    private final Difficulty npcDifficulty; // null for human players

    // Mutable game state
    private int position;
    private Money currency;
    private final Set<UUID> ownedPropertyIds;
    private boolean bankrupt;
    private int turnsInSandTrap;
    private int consecutiveDoubles;

    @Builder
    public PlayerState(
            UUID playerId,
            String displayName,
            boolean npc,
            Difficulty npcDifficulty,
            Money startingCurrency) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.npc = npc;
        this.npcDifficulty = npc ? (npcDifficulty != null ? npcDifficulty : Difficulty.MEDIUM) : null;
        this.position = 0;
        this.currency = startingCurrency;
        this.ownedPropertyIds = new HashSet<>();
        this.bankrupt = false;
        this.turnsInSandTrap = 0;
        this.consecutiveDoubles = 0;
    }

    public void moveTo(int newPosition) {
        this.position = newPosition;
    }

    public void addCurrency(Money amount) {
        this.currency = this.currency.add(amount);
    }

    public void subtractCurrency(Money amount) {
        this.currency = this.currency.subtract(amount);
        if (this.currency.isNegative()) {
            throw new IllegalStateException("Player " + displayName + " cannot have negative currency");
        }
    }

    public boolean canAfford(Money amount) {
        return this.currency.isGreaterThanOrEqual(amount);
    }

    public void addProperty(UUID propertyId) {
        this.ownedPropertyIds.add(propertyId);
    }

    public void removeProperty(UUID propertyId) {
        this.ownedPropertyIds.remove(propertyId);
    }

    public boolean ownsProperty(UUID propertyId) {
        return ownedPropertyIds.contains(propertyId);
    }

    public Set<UUID> getOwnedPropertyIds() {
        return Collections.unmodifiableSet(ownedPropertyIds);
    }

    public int getPropertyCount() {
        return ownedPropertyIds.size();
    }

    public void declareBankrupt() {
        this.bankrupt = true;
    }

    public void enterSandTrap() {
        this.turnsInSandTrap = 3; // Must wait 3 turns or pay to escape
    }

    public void decrementSandTrapTurns() {
        if (turnsInSandTrap > 0) {
            turnsInSandTrap--;
        }
    }

    public void escapeSandTrap() {
        this.turnsInSandTrap = 0;
    }

    public boolean isInSandTrap() {
        return turnsInSandTrap > 0;
    }

    public void incrementConsecutiveDoubles() {
        this.consecutiveDoubles++;
    }

    public void resetConsecutiveDoubles() {
        this.consecutiveDoubles = 0;
    }

    public boolean hasRolledThreeDoubles() {
        return consecutiveDoubles >= 3;
    }

    /**
     * Calculate total asset value (currency + property values + improvements)
     */
    public Money calculateNetWorth(Board board) {
        Money propertyValue = Money.zero();
        for (UUID propertyId : ownedPropertyIds) {
            Property property = board.getProperty(propertyId);
            propertyValue = propertyValue.add(property.getPurchasePrice());

            // Add improvement value
            int improvements = property.getImprovementLevel().getLevel();
            if (improvements > 0) {
                propertyValue = propertyValue.add(
                    property.getImprovementCost().multiply(improvements)
                );
            }
        }
        return currency.add(propertyValue);
    }

    public boolean isHuman() {
        return !npc;
    }
}
