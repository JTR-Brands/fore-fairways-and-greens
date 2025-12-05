package com.fore.game.application.npc;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.Difficulty;
import com.fore.game.domain.model.enums.TurnPhase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Rule-based NPC decision engine.
 * No external dependencies - fast, predictable, great for testing.
 * 
 * Difficulty affects:
 * - Purchase threshold (how much cash to keep in reserve)
 * - Improvement priority
 * - Trade evaluation strictness
 */
@Slf4j
@Component
public class DeterministicNpcEngine implements NpcDecisionEngine {

    private final Random random = new Random();

    @Override
    public String getEngineType() {
        return "DETERMINISTIC";
    }

    @Override
    public NpcAction decideAction(GameContext context) {
        log.debug("NPC {} deciding action in phase {} (difficulty: {})",
                context.getNpcName(), context.getTurnPhase(), context.getDifficulty());

        return switch (context.getTurnPhase()) {
            case ROLL -> NpcAction.rollDice();
            case ACTION -> decideActionPhase(context);
            case TRADE -> decideTrade(context);
            case END_TURN -> NpcAction.endTurn();
        };
    }

    private NpcAction decideActionPhase(GameContext context) {
        Difficulty difficulty = context.getDifficulty();

        // 1. Consider purchasing current property
        if (context.canPurchaseCurrentProperty()) {
            if (shouldPurchase(context)) {
                return NpcAction.purchaseProperty(
                        context.getCurrentTileProperty().getPropertyId(),
                        "Property available and within budget"
                );
            }
        }

        // 2. Consider improving properties (harder difficulties prioritize this)
        List<GameContext.PropertyInfo> improvable = context.getImprovableProperties();
        if (!improvable.isEmpty() && shouldImprove(context, difficulty)) {
            GameContext.PropertyInfo toImprove = selectPropertyToImprove(improvable, context);
            if (toImprove != null) {
                return NpcAction.improveProperty(
                        toImprove.getPropertyId(),
                        "Improving " + toImprove.getName() + " for higher rent"
                );
            }
        }

        // 3. End turn
        return NpcAction.endTurn();
    }

    private boolean shouldPurchase(GameContext context) {
        Difficulty difficulty = context.getDifficulty();
        GameContext.PropertyInfo property = context.getCurrentTileProperty();
        Money price = property.getPurchasePrice();
        Money currentCash = context.getNpcCurrency();

        // Calculate reserve based on difficulty
        Money reserve = calculateReserve(difficulty, context);

        // Always buy if we can complete a group
        if (wouldCompleteGroup(property, context)) {
            log.debug("Purchasing {} to complete group", property.getName());
            return true;
        }

        // Check if we can afford it while maintaining reserve
        Money afterPurchase = currentCash.subtract(price);
        if (afterPurchase.isGreaterThan(reserve) || afterPurchase.equals(reserve)) {
            // Higher difficulties are more aggressive buyers
            double buyProbability = switch (difficulty) {
                case EASY -> 0.5;
                case MEDIUM -> 0.7;
                case HARD -> 0.85;
                case RUTHLESS -> 0.95;
            };

            boolean willBuy = random.nextDouble() < buyProbability;
            log.debug("Purchase decision for {}: {} (probability: {})", 
                    property.getName(), willBuy, buyProbability);
            return willBuy;
        }

        log.debug("Cannot afford {} while maintaining reserve", property.getName());
        return false;
    }

    private boolean wouldCompleteGroup(GameContext.PropertyInfo property, GameContext context) {
        String group = property.getCourseGroup();
        
        // Count how many of this group we own
        long ownedInGroup = context.getNpcProperties().stream()
                .filter(p -> p.getCourseGroup().equals(group))
                .count();

        // Each group has 3 properties
        return ownedInGroup == 2;
    }

    private Money calculateReserve(Difficulty difficulty, GameContext context) {
        // Reserve enough to pay potential rent on opponent's best property
        Money maxOpponentRent = context.getOpponentProperties().stream()
                .map(GameContext.PropertyInfo::getCurrentRent)
                .max(Comparator.comparingLong(Money::toCents))
                .orElse(Money.zero());

        // Multiply by safety factor based on difficulty
        double safetyFactor = switch (difficulty) {
            case EASY -> 3.0;    // Very conservative
            case MEDIUM -> 2.0;  // Moderate
            case HARD -> 1.5;    // Aggressive
            case RUTHLESS -> 1.0; // Maximum aggression
        };

        return maxOpponentRent.multiply(safetyFactor);
    }

    private boolean shouldImprove(GameContext context, Difficulty difficulty) {
        // Higher difficulties improve more aggressively
        double improveProbability = switch (difficulty) {
            case EASY -> 0.2;
            case MEDIUM -> 0.4;
            case HARD -> 0.6;
            case RUTHLESS -> 0.8;
        };

        return random.nextDouble() < improveProbability;
    }

    private GameContext.PropertyInfo selectPropertyToImprove(
            List<GameContext.PropertyInfo> improvable, 
            GameContext context) {
        
        if (improvable.isEmpty()) return null;

        // Prioritize properties that opponent is likely to land on
        // Simple heuristic: properties closer to opponent's position
        int opponentPos = context.getOpponentPosition();

        return improvable.stream()
                .min(Comparator.comparingInt(p -> {
                    int distance = p.getPosition() - opponentPos;
                    if (distance < 0) distance += 24; // Wrap around
                    return distance;
                }))
                .orElse(improvable.get(0));
    }

    private NpcAction decideTrade(GameContext context) {
        if (evaluateTradeOffer(context)) {
            return NpcAction.acceptTrade("Trade is favorable");
        } else {
            return NpcAction.rejectTrade("Trade is not favorable");
        }
    }

    @Override
    public boolean evaluateTradeOffer(GameContext context) {
        var trade = context.getPendingTrade();
        if (trade == null) return false;

        Difficulty difficulty = context.getDifficulty();

        // Calculate value of what we're giving vs receiving
        long givingValue = calculateTradeValue(
                trade.getOfferedPropertyIds(), 
                trade.getOfferedCurrency(),
                context
        );

        long receivingValue = calculateTradeValue(
                trade.getRequestedPropertyIds(),
                trade.getRequestedCurrency(),
                context
        );

        // Apply difficulty-based threshold
        double acceptanceThreshold = switch (difficulty) {
            case EASY -> 0.8;     // Accept if getting 80% of value
            case MEDIUM -> 1.0;   // Accept if fair
            case HARD -> 1.2;     // Need 20% advantage
            case RUTHLESS -> 1.5; // Need 50% advantage
        };

        boolean accept = receivingValue >= (givingValue * acceptanceThreshold);
        
        log.debug("Trade evaluation: giving={}, receiving={}, threshold={}, accept={}",
                givingValue, receivingValue, acceptanceThreshold, accept);

        return accept;
    }

    private long calculateTradeValue(Set<UUID> propertyIds, Money currency, GameContext context) {
        long propertyValue = context.getAllProperties().stream()
                .filter(p -> propertyIds.contains(p.getPropertyId()))
                .mapToLong(p -> {
                    // Base value is purchase price
                    long value = p.getPurchasePrice().toCents();
                    
                    // Add premium if part of a group we're collecting
                    // or if it would complete opponent's group
                    value = (long) (value * 1.2);
                    
                    return value;
                })
                .sum();

        return propertyValue + currency.toCents();
    }
}
