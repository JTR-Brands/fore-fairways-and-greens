package com.fore.game.domain.model;

import com.fore.common.types.Money;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

/**
 * Represents a trade offer between two players.
 */
@Getter
@Builder
@ToString
public class TradeOffer {

    private final UUID offerId;
    private final UUID offeringPlayerId;
    private final UUID receivingPlayerId;

    // What the offering player gives
    private final Set<UUID> offeredPropertyIds;
    private final Money offeredCurrency;

    // What the offering player wants
    private final Set<UUID> requestedPropertyIds;
    private final Money requestedCurrency;

    private final TradeStatus status;

    public enum TradeStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        CANCELLED,
        EXPIRED
    }

    public TradeOffer accept() {
        if (status != TradeStatus.PENDING) {
            throw new IllegalStateException("Can only accept pending trades");
        }
        return toBuilder().status(TradeStatus.ACCEPTED).build();
    }

    public TradeOffer reject() {
        if (status != TradeStatus.PENDING) {
            throw new IllegalStateException("Can only reject pending trades");
        }
        return toBuilder().status(TradeStatus.REJECTED).build();
    }

    public TradeOffer cancel() {
        if (status != TradeStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending trades");
        }
        return toBuilder().status(TradeStatus.CANCELLED).build();
    }

    public boolean isPending() {
        return status == TradeStatus.PENDING;
    }

    private TradeOfferBuilder toBuilder() {
        return TradeOffer.builder()
                .offerId(offerId)
                .offeringPlayerId(offeringPlayerId)
                .receivingPlayerId(receivingPlayerId)
                .offeredPropertyIds(offeredPropertyIds)
                .offeredCurrency(offeredCurrency)
                .requestedPropertyIds(requestedPropertyIds)
                .requestedCurrency(requestedCurrency)
                .status(status);
    }
}
