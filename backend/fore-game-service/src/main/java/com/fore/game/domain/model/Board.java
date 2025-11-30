package com.fore.game.domain.model;

import com.fore.game.domain.model.enums.CourseGroup;
import com.fore.game.domain.model.enums.TileType;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the game board with all tiles.
 * Immutable structure, but tiles contain mutable Property state.
 */
@Getter
public class Board {

    public static final int TOTAL_TILES = 24;
    public static final int START_POSITION = 0;

    private final List<Tile> tiles;
    private final Map<UUID, Property> propertiesById;
    private final Map<Integer, Tile> tilesByPosition;

    public Board(List<Tile> tiles) {
        if (tiles.size() != TOTAL_TILES) {
            throw new IllegalArgumentException("Board must have exactly " + TOTAL_TILES + " tiles");
        }

        this.tiles = List.copyOf(tiles);
        this.tilesByPosition = tiles.stream()
                .collect(Collectors.toUnmodifiableMap(Tile::getPosition, t -> t));

        this.propertiesById = tiles.stream()
                .filter(Tile::isProperty)
                .map(t -> t.getProperty().orElseThrow())
                .collect(Collectors.toUnmodifiableMap(Property::getPropertyId, p -> p));
    }

    public Tile getTileAt(int position) {
        Tile tile = tilesByPosition.get(position);
        if (tile == null) {
            throw new IllegalArgumentException("Invalid tile position: " + position);
        }
        return tile;
    }

    public Property getProperty(UUID propertyId) {
        Property property = propertiesById.get(propertyId);
        if (property == null) {
            throw new IllegalArgumentException("Property not found: " + propertyId);
        }
        return property;
    }

    public Optional<Property> getPropertyAt(int position) {
        return getTileAt(position).getProperty();
    }

    public List<Property> getAllProperties() {
        return List.copyOf(propertiesById.values());
    }

    public List<Property> getPropertiesInGroup(CourseGroup group) {
        return propertiesById.values().stream()
                .filter(p -> p.getCourseGroup() == group)
                .toList();
    }

    public List<Property> getPropertiesOwnedBy(UUID playerId) {
        return propertiesById.values().stream()
                .filter(p -> p.isOwnedBy(playerId))
                .toList();
    }

    public boolean ownsCompleteGroup(UUID playerId, CourseGroup group) {
        List<Property> groupProperties = getPropertiesInGroup(group);
        return groupProperties.stream().allMatch(p -> p.isOwnedBy(playerId));
    }

    public Set<CourseGroup> getCompleteGroupsOwnedBy(UUID playerId) {
        return Arrays.stream(CourseGroup.values())
                .filter(group -> ownsCompleteGroup(playerId, group))
                .collect(Collectors.toSet());
    }

    public int calculateNewPosition(int currentPosition, int diceTotal) {
        return (currentPosition + diceTotal) % TOTAL_TILES;
    }

    public boolean passedStart(int oldPosition, int newPosition) {
        // Passed start if we wrapped around
        return newPosition < oldPosition || (oldPosition == 0 && newPosition == 0);
    }

    /**
     * Count properties owned by player in a specific group
     */
    public int countPropertiesInGroupOwnedBy(UUID playerId, CourseGroup group) {
        return (int) getPropertiesInGroup(group).stream()
                .filter(p -> p.isOwnedBy(playerId))
                .count();
    }
}
