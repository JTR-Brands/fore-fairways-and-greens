package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.CourseGroup;
import com.fore.game.domain.model.enums.TileType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory for creating a standard game board.
 */
public final class BoardFactory {

    private BoardFactory() {}

    public static Board createStandardBoard() {
        List<Tile> tiles = new ArrayList<>();

        // Position 0: Fairway Start (Clubhouse HQ)
        tiles.add(createSpecialTile(0, TileType.CLUBHOUSE_HQ, "Fairway Start"));

        // Links Nine (Brown) - Positions 1-3
        tiles.add(createPropertyTile(1, "Dunes End Hole 1", CourseGroup.LINKS_NINE,
                60, 2, 10, 30, 50));
        tiles.add(createPropertyTile(2, "Dunes End Hole 2", CourseGroup.LINKS_NINE,
                60, 4, 20, 60, 50));
        tiles.add(createPropertyTile(3, "Dunes End Hole 3", CourseGroup.LINKS_NINE,
                80, 6, 30, 90, 50));

        // Position 4: Pro Shop
        tiles.add(createSpecialTile(4, TileType.PRO_SHOP, "Pro Shop"));

        // Prairie Nine (Light Blue) - Positions 5-7
        tiles.add(createPropertyTile(5, "Meadow Creek Hole 4", CourseGroup.PRAIRIE_NINE,
                100, 8, 40, 120, 50));
        tiles.add(createPropertyTile(6, "Meadow Creek Hole 5", CourseGroup.PRAIRIE_NINE,
                100, 8, 40, 120, 50));
        tiles.add(createPropertyTile(7, "Meadow Creek Hole 6", CourseGroup.PRAIRIE_NINE,
                120, 10, 50, 150, 50));

        // Position 8: Sand Trap (Corner)
        tiles.add(createSpecialTile(8, TileType.SAND_TRAP, "Bunker Beach"));

        // Highland Nine (Pink) - Positions 9-11
        tiles.add(createPropertyTile(9, "Eagle Ridge Hole 7", CourseGroup.HIGHLAND_NINE,
                140, 12, 60, 180, 100));
        tiles.add(createPropertyTile(10, "Eagle Ridge Hole 8", CourseGroup.HIGHLAND_NINE,
                140, 12, 60, 180, 100));
        tiles.add(createPropertyTile(11, "Eagle Ridge Hole 9", CourseGroup.HIGHLAND_NINE,
                160, 14, 70, 210, 100));

        // Position 12: Members Lounge (Safe)
        tiles.add(createSpecialTile(12, TileType.MEMBERS_LOUNGE, "Members Lounge"));

        // Coastal Nine (Orange) - Positions 13-15
        tiles.add(createPropertyTile(13, "Oceanview Hole 10", CourseGroup.COASTAL_NINE,
                180, 16, 80, 240, 100));
        tiles.add(createPropertyTile(14, "Oceanview Hole 11", CourseGroup.COASTAL_NINE,
                180, 16, 80, 240, 100));
        tiles.add(createPropertyTile(15, "Oceanview Hole 12", CourseGroup.COASTAL_NINE,
                200, 18, 90, 270, 100));

        // Position 16: Water Hazard (Corner)
        tiles.add(createSpecialTile(16, TileType.WATER_HAZARD, "Lake Penalty"));

        // Championship Nine (Red) - Positions 17-19
        tiles.add(createPropertyTile(17, "Champion Oaks Hole 13", CourseGroup.CHAMPIONSHIP_NINE,
                220, 20, 100, 300, 150));
        tiles.add(createPropertyTile(18, "Champion Oaks Hole 14", CourseGroup.CHAMPIONSHIP_NINE,
                220, 20, 100, 300, 150));
        tiles.add(createPropertyTile(19, "Champion Oaks Hole 15", CourseGroup.CHAMPIONSHIP_NINE,
                240, 22, 110, 330, 150));

        // Position 20: Pro Shop (Second)
        tiles.add(createSpecialTile(20, TileType.PRO_SHOP, "Tournament Pro Shop"));

        // Masters Nine (Blue) - Positions 21-23
        tiles.add(createPropertyTile(21, "Grand Pines Hole 16", CourseGroup.MASTERS_NINE,
                260, 24, 120, 360, 200));
        tiles.add(createPropertyTile(22, "Grand Pines Hole 17", CourseGroup.MASTERS_NINE,
                280, 26, 130, 390, 200));
        tiles.add(createPropertyTile(23, "Grand Pines Hole 18", CourseGroup.MASTERS_NINE,
                300, 30, 150, 450, 200));

        return new Board(tiles);
    }

    private static Tile createSpecialTile(int position, TileType type, String name) {
        return Tile.builder()
                .tileId(UUID.randomUUID())
                .position(position)
                .type(type)
                .name(name)
                .property(null)
                .build();
    }

    private static Tile createPropertyTile(
            int position,
            String name,
            CourseGroup courseGroup,
            int purchasePriceDollars,
            int baseRentDollars,
            int rentWithClubhouseDollars,
            int rentWithResortDollars,
            int improvementCostDollars) {

        Property property = Property.builder()
                .propertyId(UUID.randomUUID())
                .name(name)
                .courseGroup(courseGroup)
                .tilePosition(position)
                .purchasePrice(Money.ofDollars(purchasePriceDollars))
                .baseRent(Money.ofDollars(baseRentDollars))
                .rentWithClubhouse(Money.ofDollars(rentWithClubhouseDollars))
                .rentWithResort(Money.ofDollars(rentWithResortDollars))
                .improvementCost(Money.ofDollars(improvementCostDollars))
                .build();

        return Tile.builder()
                .tileId(UUID.randomUUID())
                .position(position)
                .type(TileType.PROPERTY)
                .name(name)
                .property(property)
                .build();
    }
}
