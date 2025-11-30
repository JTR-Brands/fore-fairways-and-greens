package com.fore.game.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Difficulty {
    EASY("Casual Caddie", 0.2, 0.8, 0.3),
    MEDIUM("Club Pro", 0.5, 0.5, 0.1),
    HARD("Tour Veteran", 0.7, 0.3, 0.0),
    RUTHLESS("Championship Mind", 0.95, 0.1, 0.0);

    private final String displayName;
    private final double aggression;       // How aggressively to acquire/improve
    private final double tradeWillingness; // Likelihood to accept trades
    private final double mistakeRate;      // Chance of suboptimal play
}
