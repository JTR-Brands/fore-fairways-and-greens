package com.fore.game.domain.model.enums;

public enum GameStatus {
    WAITING,      // Waiting for second player to join
    IN_PROGRESS,  // Game is active
    COMPLETED     // Game has ended (winner declared or draw)
}
