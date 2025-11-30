package com.fore.game.domain.model.enums;

public enum TileType {
    PROPERTY,        // Purchasable course tile
    CLUBHOUSE_HQ,    // Start tile - collect salary when passing
    PRO_SHOP,        // Draw a card (chance equivalent)
    SAND_TRAP,       // Lose a turn or pay to escape
    WATER_HAZARD,    // Pay penalty
    MEMBERS_LOUNGE   // Safe tile - no action
}
