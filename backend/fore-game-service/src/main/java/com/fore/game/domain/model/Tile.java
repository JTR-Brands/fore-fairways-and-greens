package com.fore.game.domain.model;

import com.fore.game.domain.model.enums.TileType;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a single tile on the game board.
 */
@Getter
@ToString
public class Tile {

    private final UUID tileId;
    private final int position;
    private final TileType type;
    private final String name;
    private final Property property; // Only present if type == PROPERTY

    @Builder
    public Tile(UUID tileId, int position, TileType type, String name, Property property) {
        this.tileId = tileId != null ? tileId : UUID.randomUUID();
        this.position = position;
        this.type = type;
        this.name = name;

        if (type == TileType.PROPERTY && property == null) {
            throw new IllegalArgumentException("Property tile must have a property");
        }
        if (type != TileType.PROPERTY && property != null) {
            throw new IllegalArgumentException("Non-property tile cannot have a property");
        }
        this.property = property;
    }

    public Optional<Property> getProperty() {
        return Optional.ofNullable(property);
    }

    public boolean isProperty() {
        return type == TileType.PROPERTY;
    }

    public boolean isStartTile() {
        return type == TileType.CLUBHOUSE_HQ;
    }

    public boolean isSafeTile() {
        return type == TileType.MEMBERS_LOUNGE || type == TileType.CLUBHOUSE_HQ;
    }

    public boolean requiresPayment() {
        return type == TileType.WATER_HAZARD;
    }

    public boolean causesTurnLoss() {
        return type == TileType.SAND_TRAP;
    }
}
