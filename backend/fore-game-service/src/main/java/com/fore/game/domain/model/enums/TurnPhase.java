package com.fore.game.domain.model.enums;

public enum TurnPhase {
    ROLL,      // Player must roll dice
    ACTION,    // Player can buy, improve, trade, or end turn
    TRADE,     // Player is in active trade negotiation
    END_TURN   // Turn cleanup, transitioning to next player
}
