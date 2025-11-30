package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.model.enums.CourseGroup;
import com.fore.game.domain.model.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BoardTest {

    @Test
    void shouldCreateBoardWithCorrectNumberOfTiles() {
        // given
        List<Tile> tiles = createTestTiles();

        // when
        Board board = new Board(tiles);

        // then
        assertThat(board.getTiles()).hasSize(Board.TOTAL_TILES);
    }

    @Test
    void shouldRejectBoardWithIncorrectNumberOfTiles() {
        // given
        List<Tile> tiles = createTestTiles();
        tiles.remove(0);

        // when/then
        assertThatThrownBy(() -> new Board(tiles))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must have exactly 24 tiles");
    }

    @Test
    void shouldGetTileAtPosition() {
        // given
        Board board = new Board(createTestTiles());

        // when
        Tile tile = board.getTileAt(0);

        // then
        assertThat(tile).isNotNull();
        assertThat(tile.getPosition()).isEqualTo(0);
    }

    @Test
    void shouldThrowExceptionForInvalidTilePosition() {
        // given
        Board board = new Board(createTestTiles());

        // when/then
        assertThatThrownBy(() -> board.getTileAt(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid tile position");
    }

    @Test
    void shouldGetPropertyById() {
        // given
        Property property = createTestProperty("Test Property", 1);
        List<Tile> tiles = createTestTilesWithProperty(property);
        Board board = new Board(tiles);

        // when
        Property found = board.getProperty(property.getPropertyId());

        // then
        assertThat(found).isEqualTo(property);
    }

    @Test
    void shouldThrowExceptionForNonExistentProperty() {
        // given
        Board board = new Board(createTestTiles());

        // when/then
        assertThatThrownBy(() -> board.getProperty(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Property not found");
    }

    @Test
    void shouldGetPropertyAtPosition() {
        // given
        Property property = createTestProperty("Test Property", 1);
        List<Tile> tiles = createTestTilesWithProperty(property);
        Board board = new Board(tiles);

        // when
        var foundProperty = board.getPropertyAt(1);

        // then
        assertThat(foundProperty).isPresent();
        assertThat(foundProperty.get()).isEqualTo(property);
    }

    @Test
    void shouldReturnEmptyOptionalForNonPropertyTile() {
        // given
        Board board = new Board(createTestTiles());

        // when
        var property = board.getPropertyAt(0); // Start tile

        // then
        assertThat(property).isEmpty();
    }

    @Test
    void shouldGetAllProperties() {
        // given
        Property property1 = createTestProperty("Property 1", 1);
        Property property2 = createTestProperty("Property 2", 2);
        List<Tile> tiles = createTestTilesWithProperties(property1, property2);
        Board board = new Board(tiles);

        // when
        List<Property> properties = board.getAllProperties();

        // then
        assertThat(properties).hasSize(2);
        assertThat(properties).containsExactlyInAnyOrder(property1, property2);
    }

    @Test
    void shouldGetPropertiesInGroup() {
        // given
        Property property1 = createTestPropertyInGroup("Property 1", 1, CourseGroup.LINKS_NINE);
        Property property2 = createTestPropertyInGroup("Property 2", 2, CourseGroup.LINKS_NINE);
        Property property3 = createTestPropertyInGroup("Property 3", 3, CourseGroup.PRAIRIE_NINE);
        List<Tile> tiles = createTestTilesWithProperties(property1, property2, property3);
        Board board = new Board(tiles);

        // when
        List<Property> linksNine = board.getPropertiesInGroup(CourseGroup.LINKS_NINE);

        // then
        assertThat(linksNine).hasSize(2);
        assertThat(linksNine).containsExactlyInAnyOrder(property1, property2);
    }

    @Test
    void shouldGetPropertiesOwnedByPlayer() {
        // given
        UUID playerId = UUID.randomUUID();
        Property property1 = createTestProperty("Property 1", 1);
        Property property2 = createTestProperty("Property 2", 2);
        Property property3 = createTestProperty("Property 3", 3);

        property1.purchase(playerId);
        property2.purchase(playerId);
        property3.purchase(UUID.randomUUID()); // Different owner

        List<Tile> tiles = createTestTilesWithProperties(property1, property2, property3);
        Board board = new Board(tiles);

        // when
        List<Property> ownedProperties = board.getPropertiesOwnedBy(playerId);

        // then
        assertThat(ownedProperties).hasSize(2);
        assertThat(ownedProperties).containsExactlyInAnyOrder(property1, property2);
    }

    @Test
    void shouldCheckIfPlayerOwnsCompleteGroup() {
        // given
        UUID playerId = UUID.randomUUID();
        Property property1 = createTestPropertyInGroup("Property 1", 1, CourseGroup.LINKS_NINE);
        Property property2 = createTestPropertyInGroup("Property 2", 2, CourseGroup.LINKS_NINE);
        Property property3 = createTestPropertyInGroup("Property 3", 3, CourseGroup.LINKS_NINE);

        property1.purchase(playerId);
        property2.purchase(playerId);
        property3.purchase(playerId);

        List<Tile> tiles = createTestTilesWithProperties(property1, property2, property3);
        Board board = new Board(tiles);

        // when
        boolean ownsCompleteGroup = board.ownsCompleteGroup(playerId, CourseGroup.LINKS_NINE);

        // then
        assertThat(ownsCompleteGroup).isTrue();
    }

    @Test
    void shouldReturnFalseForIncompleteGroup() {
        // given
        UUID playerId = UUID.randomUUID();
        Property property1 = createTestPropertyInGroup("Property 1", 1, CourseGroup.LINKS_NINE);
        Property property2 = createTestPropertyInGroup("Property 2", 2, CourseGroup.LINKS_NINE);
        Property property3 = createTestPropertyInGroup("Property 3", 3, CourseGroup.LINKS_NINE);

        property1.purchase(playerId);
        property2.purchase(playerId);
        property3.purchase(UUID.randomUUID()); // Different owner

        List<Tile> tiles = createTestTilesWithProperties(property1, property2, property3);
        Board board = new Board(tiles);

        // when
        boolean ownsCompleteGroup = board.ownsCompleteGroup(playerId, CourseGroup.LINKS_NINE);

        // then
        assertThat(ownsCompleteGroup).isFalse();
    }

    @Test
    void shouldGetCompleteGroupsOwnedByPlayer() {
        // given - use the standard board which has all properties in all groups
        UUID playerId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        Board board = BoardFactory.createStandardBoard();

        // Get all properties in LINKS_NINE and purchase them for the player
        List<Property> linksNineProps = board.getPropertiesInGroup(CourseGroup.LINKS_NINE);
        for (Property prop : linksNineProps) {
            prop.purchase(playerId);
        }

        // Get first property from PRAIRIE_NINE for the player (incomplete group)
        List<Property> prairieNineProps = board.getPropertiesInGroup(CourseGroup.PRAIRIE_NINE);
        prairieNineProps.get(0).purchase(playerId);
        prairieNineProps.get(1).purchase(otherPlayerId);
        prairieNineProps.get(2).purchase(otherPlayerId);

        // when
        var completeGroups = board.getCompleteGroupsOwnedBy(playerId);

        // then
        assertThat(completeGroups).containsExactly(CourseGroup.LINKS_NINE);
    }

    @Test
    void shouldCalculateNewPosition() {
        // given
        Board board = new Board(createTestTiles());

        // when/then
        assertThat(board.calculateNewPosition(0, 5)).isEqualTo(5);
        assertThat(board.calculateNewPosition(20, 5)).isEqualTo(1); // Wraps around
        assertThat(board.calculateNewPosition(23, 1)).isEqualTo(0); // Wraps to start
    }

    @Test
    void shouldDetectPassingStart() {
        // given
        Board board = new Board(createTestTiles());

        // when/then
        assertThat(board.passedStart(23, 1)).isTrue(); // Wrapped around
        assertThat(board.passedStart(20, 2)).isTrue(); // Wrapped around
        assertThat(board.passedStart(5, 10)).isFalse(); // Normal movement
        assertThat(board.passedStart(0, 0)).isTrue(); // Stayed at start
    }

    @Test
    void shouldCountPropertiesInGroupOwnedByPlayer() {
        // given
        UUID playerId = UUID.randomUUID();
        Property property1 = createTestPropertyInGroup("Property 1", 1, CourseGroup.LINKS_NINE);
        Property property2 = createTestPropertyInGroup("Property 2", 2, CourseGroup.LINKS_NINE);
        Property property3 = createTestPropertyInGroup("Property 3", 3, CourseGroup.LINKS_NINE);

        property1.purchase(playerId);
        property2.purchase(playerId);
        property3.purchase(UUID.randomUUID()); // Different owner

        List<Tile> tiles = createTestTilesWithProperties(property1, property2, property3);
        Board board = new Board(tiles);

        // when
        int count = board.countPropertiesInGroupOwnedBy(playerId, CourseGroup.LINKS_NINE);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void shouldCreateStandardBoard() {
        // when
        Board board = BoardFactory.createStandardBoard();

        // then
        assertThat(board.getTiles()).hasSize(Board.TOTAL_TILES);
        assertThat(board.getAllProperties()).isNotEmpty();
        assertThat(board.getTileAt(0).getType()).isEqualTo(TileType.CLUBHOUSE_HQ);
    }

    private List<Tile> createTestTiles() {
        List<Tile> tiles = new ArrayList<>();
        // Create 24 tiles - start with clubhouse HQ, rest as safe tiles
        tiles.add(Tile.builder()
                .position(0)
                .type(TileType.CLUBHOUSE_HQ)
                .name("Clubhouse HQ")
                .build());

        for (int i = 1; i < Board.TOTAL_TILES; i++) {
            tiles.add(Tile.builder()
                    .position(i)
                    .type(TileType.MEMBERS_LOUNGE)
                    .name("Safe Tile " + i)
                    .build());
        }
        return tiles;
    }

    private List<Tile> createTestTilesWithProperty(Property property) {
        List<Tile> tiles = new ArrayList<>();
        tiles.add(Tile.builder()
                .position(0)
                .type(TileType.CLUBHOUSE_HQ)
                .name("Clubhouse HQ")
                .build());

        tiles.add(Tile.builder()
                .position(property.getTilePosition())
                .type(TileType.PROPERTY)
                .name(property.getName())
                .property(property)
                .build());

        for (int i = 2; i < Board.TOTAL_TILES; i++) {
            tiles.add(Tile.builder()
                    .position(i)
                    .type(TileType.MEMBERS_LOUNGE)
                    .name("Safe Tile " + i)
                    .build());
        }
        return tiles;
    }

    private List<Tile> createTestTilesWithProperties(Property... properties) {
        List<Tile> tiles = new ArrayList<>();
        tiles.add(Tile.builder()
                .position(0)
                .type(TileType.CLUBHOUSE_HQ)
                .name("Clubhouse HQ")
                .build());

        for (Property property : properties) {
            tiles.add(Tile.builder()
                    .position(property.getTilePosition())
                    .type(TileType.PROPERTY)
                    .name(property.getName())
                    .property(property)
                    .build());
        }

        // Fill remaining positions
        for (int i = tiles.size(); i < Board.TOTAL_TILES; i++) {
            tiles.add(Tile.builder()
                    .position(i)
                    .type(TileType.MEMBERS_LOUNGE)
                    .name("Safe Tile " + i)
                    .build());
        }
        return tiles;
    }

    private Property createTestProperty(String name, int position) {
        return Property.builder()
                .propertyId(UUID.randomUUID())
                .name(name)
                .courseGroup(CourseGroup.LINKS_NINE)
                .tilePosition(position)
                .purchasePrice(Money.ofDollars(300))
                .baseRent(Money.ofDollars(50))
                .rentWithClubhouse(Money.ofDollars(200))
                .rentWithResort(Money.ofDollars(500))
                .improvementCost(Money.ofDollars(100))
                .build();
    }

    private Property createTestPropertyInGroup(String name, int position, CourseGroup group) {
        return Property.builder()
                .propertyId(UUID.randomUUID())
                .name(name)
                .courseGroup(group)
                .tilePosition(position)
                .purchasePrice(Money.ofDollars(300))
                .baseRent(Money.ofDollars(50))
                .rentWithClubhouse(Money.ofDollars(200))
                .rentWithResort(Money.ofDollars(500))
                .improvementCost(Money.ofDollars(100))
                .build();
    }
}
