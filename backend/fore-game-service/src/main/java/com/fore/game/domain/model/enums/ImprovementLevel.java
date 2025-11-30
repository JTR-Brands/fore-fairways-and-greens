package com.fore.game.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImprovementLevel {
    NONE(0, "Unimproved"),
    CLUBHOUSE(1, "Clubhouse"),
    RESORT(2, "Resort");

    private final int level;
    private final String displayName;

    public boolean canUpgrade() {
        return this != RESORT;
    }

    public ImprovementLevel nextLevel() {
        return switch (this) {
            case NONE -> CLUBHOUSE;
            case CLUBHOUSE -> RESORT;
            case RESORT -> throw new IllegalStateException("Cannot upgrade beyond RESORT");
        };
    }
}
