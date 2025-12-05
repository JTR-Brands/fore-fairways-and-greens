package com.fore.game.application.npc;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * LLM-powered NPC decision engine.
 * Delegates to an LlmClient for actual API calls.
 * 
 * This is a template - actual implementation depends on chosen LLM provider.
 */
@Slf4j
@RequiredArgsConstructor
public class LlmNpcEngine implements NpcDecisionEngine {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final DeterministicNpcEngine fallbackEngine;

    @Override
    public String getEngineType() {
        return "LLM:" + llmClient.getProviderName();
    }

    @Override
    public NpcAction decideAction(GameContext context) {
        try {
            String prompt = buildPrompt(context);
            String response = llmClient.complete(prompt);
            return parseResponse(response, context);
        } catch (Exception e) {
            log.warn("LLM engine failed, falling back to deterministic: {}", e.getMessage());
            return fallbackEngine.decideAction(context);
        }
    }

    @Override
    public boolean evaluateTradeOffer(GameContext context) {
        try {
            String prompt = buildTradePrompt(context);
            String response = llmClient.complete(prompt);
            return parseTradeResponse(response);
        } catch (Exception e) {
            log.warn("LLM trade evaluation failed, falling back to deterministic: {}", e.getMessage());
            return fallbackEngine.evaluateTradeOffer(context);
        }
    }

    private String buildPrompt(GameContext context) {
        return """
            You are playing a property trading board game as %s (difficulty: %s).
            
            CURRENT STATE:
            - Turn Phase: %s
            - Your Position: Tile %d
            - Your Cash: $%d
            - Your Properties: %s
            - Opponent Cash: $%d
            - Opponent Properties: %s
            
            CURRENT TILE: %s
            
            AVAILABLE ACTIONS:
            %s
            
            Respond with ONLY a JSON object:
            {"action": "ROLL_DICE|PURCHASE_PROPERTY|IMPROVE_PROPERTY|END_TURN", "propertyId": "uuid-if-needed", "reasoning": "brief explanation"}
            """.formatted(
                context.getNpcName(),
                context.getDifficulty(),
                context.getTurnPhase(),
                context.getNpcPosition(),
                context.getNpcCurrency().toCents() / 100,
                summarizeProperties(context.getNpcProperties()),
                context.getOpponentCurrency().toCents() / 100,
                summarizeProperties(context.getOpponentProperties()),
                describeCurrentTile(context),
                listAvailableActions(context)
        );
    }

    private String buildTradePrompt(GameContext context) {
        var trade = context.getPendingTrade();
        return """
            You are evaluating a trade offer in a property trading game.
            Difficulty: %s
            
            TRADE OFFER:
            - You give: %s
            - You receive: %s
            
            YOUR CURRENT STATE:
            - Cash: $%d
            - Properties: %s
            
            Respond with ONLY: {"accept": true|false, "reasoning": "brief explanation"}
            """.formatted(
                context.getDifficulty(),
                describeTradeGive(trade, context),
                describeTradeReceive(trade, context),
                context.getNpcCurrency().toCents() / 100,
                summarizeProperties(context.getNpcProperties())
        );
    }

    private NpcAction parseResponse(String response, GameContext context) {
        try {
            // Extract JSON from response (LLMs sometimes add extra text)
            String json = extractJson(response);
            var node = objectMapper.readTree(json);
            
            String action = node.get("action").asText();
            String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : null;

            return switch (action) {
                case "ROLL_DICE" -> NpcAction.rollDice();
                case "PURCHASE_PROPERTY" -> {
                    String propId = node.get("propertyId").asText();
                    yield NpcAction.purchaseProperty(java.util.UUID.fromString(propId), reasoning);
                }
                case "IMPROVE_PROPERTY" -> {
                    String propId = node.get("propertyId").asText();
                    yield NpcAction.improveProperty(java.util.UUID.fromString(propId), reasoning);
                }
                case "END_TURN" -> NpcAction.endTurn();
                default -> {
                    log.warn("Unknown action from LLM: {}", action);
                    yield fallbackEngine.decideAction(context);
                }
            };
        } catch (Exception e) {
            log.warn("Failed to parse LLM response: {}", e.getMessage());
            return fallbackEngine.decideAction(context);
        }
    }

    private boolean parseTradeResponse(String response) {
        try {
            String json = extractJson(response);
            var node = objectMapper.readTree(json);
            return node.get("accept").asBoolean();
        } catch (Exception e) {
            log.warn("Failed to parse trade response: {}", e.getMessage());
            return false;
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }

    private String summarizeProperties(java.util.List<GameContext.PropertyInfo> properties) {
        if (properties.isEmpty()) return "None";
        return properties.stream()
                .map(p -> p.getName() + " (" + p.getCourseGroup() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("None");
    }

    private String describeCurrentTile(GameContext context) {
        var prop = context.getCurrentTileProperty();
        if (prop == null) return "Not a property tile";
        
        if (prop.getOwnerId() == null) {
            return "Unowned property: %s ($%d to buy)".formatted(
                    prop.getName(), prop.getPurchasePrice().toCents() / 100);
        } else if (prop.getOwnerId().equals(context.getNpcPlayerId())) {
            return "Your property: " + prop.getName();
        } else {
            return "Opponent's property: %s (rent: $%d)".formatted(
                    prop.getName(), prop.getCurrentRent().toCents() / 100);
        }
    }

    private String listAvailableActions(GameContext context) {
        var actions = new java.util.ArrayList<String>();
        
        if (context.getTurnPhase() == com.fore.game.domain.model.enums.TurnPhase.ROLL) {
            actions.add("ROLL_DICE - Roll the dice to move");
        }
        
        if (context.getTurnPhase() == com.fore.game.domain.model.enums.TurnPhase.ACTION) {
            if (context.canPurchaseCurrentProperty()) {
                actions.add("PURCHASE_PROPERTY - Buy " + context.getCurrentTileProperty().getName());
            }
            if (!context.getImprovableProperties().isEmpty()) {
                actions.add("IMPROVE_PROPERTY - Add clubhouse/resort to a property");
            }
            actions.add("END_TURN - End your turn");
        }
        
        return String.join("\n", actions);
    }

    private String describeTradeGive(com.fore.game.domain.model.TradeOffer trade, GameContext context) {
        // Implementation depends on whether NPC is offering or receiving
        return "Properties and/or cash";
    }

    private String describeTradeReceive(com.fore.game.domain.model.TradeOffer trade, GameContext context) {
        return "Properties and/or cash";
    }
}
