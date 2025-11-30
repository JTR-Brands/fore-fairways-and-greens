package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.CourseGroup;
import com.fore.game.domain.model.enums.ImprovementLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Represents a purchasable property on the board.
 * Mutable within the context of a game session.
 */
@Getter
@ToString
public class Property {

    private final UUID propertyId;
    private final String name;
    private final CourseGroup courseGroup;
    private final int tilePosition;

    // Pricing
    private final Money purchasePrice;
    private final Money baseRent;
    private final Money rentWithClubhouse;
    private final Money rentWithResort;
    private final Money improvementCost;

    // Mutable state
    private UUID ownerId;
    private ImprovementLevel improvementLevel;
    private boolean mortgaged;

    @Builder
    public Property(
            UUID propertyId,
            String name,
            CourseGroup courseGroup,
            int tilePosition,
            Money purchasePrice,
            Money baseRent,
            Money rentWithClubhouse,
            Money rentWithResort,
            Money improvementCost) {
        this.propertyId = propertyId != null ? propertyId : UUID.randomUUID();
        this.name = name;
        this.courseGroup = courseGroup;
        this.tilePosition = tilePosition;
        this.purchasePrice = purchasePrice;
        this.baseRent = baseRent;
        this.rentWithClubhouse = rentWithClubhouse;
        this.rentWithResort = rentWithResort;
        this.improvementCost = improvementCost;
        this.ownerId = null;
        this.improvementLevel = ImprovementLevel.NONE;
        this.mortgaged = false;
    }

    public boolean isOwned() {
        return ownerId != null;
    }

    public boolean isOwnedBy(UUID playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }

    public void purchase(UUID newOwnerId) {
        if (isOwned()) {
            throw new IllegalStateException("Property " + name + " is already owned");
        }
        this.ownerId = newOwnerId;
    }

    public void transferTo(UUID newOwnerId) {
        this.ownerId = newOwnerId;
    }

    public Money calculateRent(boolean ownerHasCompleteGroup) {
        if (mortgaged) {
            return Money.zero();
        }

        Money rent = switch (improvementLevel) {
            case NONE -> baseRent;
            case CLUBHOUSE -> rentWithClubhouse;
            case RESORT -> rentWithResort;
        };

        // Double rent if owner has complete group and no improvements
        if (ownerHasCompleteGroup && improvementLevel == ImprovementLevel.NONE) {
            rent = rent.multiply(2);
        }

        return rent;
    }

    public void improve() {
        if (!improvementLevel.canUpgrade()) {
            throw new IllegalStateException("Property " + name + " is already at maximum improvement");
        }
        if (mortgaged) {
            throw new IllegalStateException("Cannot improve mortgaged property " + name);
        }
        this.improvementLevel = improvementLevel.nextLevel();
    }

    public void mortgage() {
        if (mortgaged) {
            throw new IllegalStateException("Property " + name + " is already mortgaged");
        }
        if (improvementLevel != ImprovementLevel.NONE) {
            throw new IllegalStateException("Must sell improvements before mortgaging " + name);
        }
        this.mortgaged = true;
    }

    public Money getMortgageValue() {
        return purchasePrice.multiply(0.5);
    }

    public Money getUnmortgageCost() {
        return getMortgageValue().multiply(1.1); // 10% interest
    }

    public void unmortgage() {
        if (!mortgaged) {
            throw new IllegalStateException("Property " + name + " is not mortgaged");
        }
        this.mortgaged = false;
    }

    public boolean canBeImproved() {
        return !mortgaged && improvementLevel.canUpgrade();
    }
}
