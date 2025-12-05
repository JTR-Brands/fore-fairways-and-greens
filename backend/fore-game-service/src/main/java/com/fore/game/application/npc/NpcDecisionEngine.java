package com.fore.game.application.npc;

/**
 * Strategy interface for NPC decision-making.
 * Implementations can be rule-based, LLM-powered, or hybrid.
 */
public interface NpcDecisionEngine {

    /**
     * Decide what action the NPC should take given the current game state.
     *
     * @param context Current game state from NPC's perspective
     * @return The action to execute
     */
    NpcAction decideAction(GameContext context);

    /**
     * Evaluate whether to accept a trade offer from opponent.
     *
     * @param context Current game state
     * @return true to accept, false to reject
     */
    boolean evaluateTradeOffer(GameContext context);

    /**
     * Get the engine type for logging/debugging.
     */
    String getEngineType();
}
