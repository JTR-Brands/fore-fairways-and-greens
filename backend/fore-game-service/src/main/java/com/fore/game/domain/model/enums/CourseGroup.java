package com.fore.game.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CourseGroup {
    LINKS_NINE("#8B4513", "Links Nine", 3),           // Brown - cheapest
    PRAIRIE_NINE("#87CEEB", "Prairie Nine", 3),       // Light blue
    HIGHLAND_NINE("#DDA0DD", "Highland Nine", 3),     // Pink/Plum
    COASTAL_NINE("#FFA500", "Coastal Nine", 3),       // Orange
    CHAMPIONSHIP_NINE("#DC143C", "Championship Nine", 3), // Red
    MASTERS_NINE("#0000CD", "Masters Nine", 3);       // Blue - most expensive

    private final String hexColor;
    private final String displayName;
    private final int propertiesInGroup;
}
