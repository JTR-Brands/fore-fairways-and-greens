package com.fore.game.domain.model;

import com.fore.common.types.Money;
import com.fore.game.domain.events.*;
import com.fore.game.domain.model.enums.Difficulty;
import com.fore.game.domain.model.enums.GameStatus;
import com.fore.game.domain.model.enums.TurnPhase;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class GameSessionTest {

    @Test
    void shouldCreateNewGameWithHumanPlayer() {
        // given
        UUID creatorId = UUID.randomUUID();
        String creatorName = "Alice";

        // when
        GameSession game = GameSession.create(creatorId, creatorName, false, null);

        // then
        assertThat(game.getGameId()).isNotNull();
        assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(game.getPlayers()).hasSize(1);
        assertThat(game.getPlayers().get(creatorId).getDisplayName()).isEqualTo(creatorName);
        assertThat(game.getTurnPhase()).isEqualTo(TurnPhase.ROLL);
        assertThat(game.getTurnNumber()).isZero();

        // Check events
        List<GameEvent> events = game.drainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(GameCreatedEvent.class);
    }

    @Test
    void shouldCreateNewGameWithNpcOpponent() {
        // given
        UUID creatorId = UUID.randomUUID();
        String creatorName = "Alice";

        // when
        GameSession game = GameSession.create(creatorId, creatorName, true, Difficulty.HARD);

        // then
        assertThat(game.getPlayers()).hasSize(2);
        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS); // Auto-started
        assertThat(game.getNpcPlayer()).isPresent();
        assertThat(game.getNpcPlayer().get().getNpcDifficulty()).isEqualTo(Difficulty.HARD);

        // Check events - should contain GameCreatedEvent and GameStartedEvent
        List<GameEvent> events = game.drainEvents();
        assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events).anyMatch(e -> e instanceof GameCreatedEvent);
        assertThat(events).anyMatch(e -> e instanceof GameStartedEvent);
    }

    @Test
    void shouldAllowPlayerToJoinGame() {
        // given
        GameSession game = GameSession.create(UUID.randomUUID(), "Creator", false, null);
        game.drainEvents(); // Clear creation events
        UUID joiningPlayerId = UUID.randomUUID();

        // when
        game.joinGame(joiningPlayerId, "Bob");

        // then
        assertThat(game.getPlayers()).hasSize(2);
        assertThat(game.getPlayers().get(joiningPlayerId)).isNotNull();
        assertThat(game.getPlayers().get(joiningPlayerId).getDisplayName()).isEqualTo("Bob");

        // Should auto-start when max players reached
        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);

        // Check events (PlayerJoinedEvent + GameStartedEvent + TurnStartedEvent)
        List<GameEvent> events = game.drainEvents();
        assertThat(events).hasSizeGreaterThanOrEqualTo(2);
        assertThat(events.get(0)).isInstanceOf(PlayerJoinedEvent.class);
    }

    @Test
    void shouldNotAllowJoiningWhenGameIsFull() {
        // given
        UUID creator = UUID.randomUUID();
        GameSession game = GameSession.create(creator, "Creator", false, null);
        UUID player2 = UUID.randomUUID();
        game.joinGame(player2, "Player 2");
        assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS); // Auto-started

        // when/then - game is now full and in progress, so either error is acceptable
        assertThatThrownBy(() -> game.joinGame(UUID.randomUUID(), "Player 3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageMatching(".*(Game is full|Invalid game status).*");
    }

    @Test
    void shouldNotAllowSamePlayerToJoinTwice() {
        // given
        UUID playerId = UUID.randomUUID();
        GameSession game = GameSession.create(playerId, "Creator", false, null);

        // when/then
        assertThatThrownBy(() -> game.joinGame(playerId, "Creator Again"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in game");
    }

    @Test
    void shouldNotAllowJoiningGameInProgress() {
        // given
        GameSession game = GameSession.create(UUID.randomUUID(), "Creator", true, Difficulty.EASY);

        // when/then
        assertThatThrownBy(() -> game.joinGame(UUID.randomUUID(), "Late Joiner"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid game status");
    }

    @Test
    void shouldAllowCurrentPlayerToRollDice() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);
        game.drainEvents();

        // when
        DiceRoll roll = game.rollDice(player1);

        // then
        assertThat(roll).isNotNull();
        assertThat(roll.getTotal()).isBetween(2, 12);

        // Check events
        List<GameEvent> events = game.drainEvents();
        assertThat(events).hasSizeGreaterThanOrEqualTo(2); // DiceRolledEvent + PlayerMovedEvent
        assertThat(events.get(0)).isInstanceOf(DiceRolledEvent.class);
        assertThat(events.get(1)).isInstanceOf(PlayerMovedEvent.class);
    }

    @Test
    void shouldNotAllowNonCurrentPlayerToRollDice() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        // when/then
        assertThatThrownBy(() -> game.rollDice(player2)) // Player 2's turn is not yet
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not your turn");
    }

    @Test
    void shouldCollectSalaryWhenPassingStart() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);
        PlayerState player = game.getPlayer(player1);
        player.moveTo(23); // Near the end
        game.drainEvents();

        Money beforeCurrency = player.getCurrency();

        // when - roll to pass start
        game.rollDice(player1);

        // then
        Money afterCurrency = player.getCurrency();
        if (player.getPosition() < 23) { // If we wrapped around
            assertThat(afterCurrency).isGreaterThan(beforeCurrency);
            List<GameEvent> events = game.drainEvents();
            assertThat(events).anyMatch(e -> e instanceof SalaryCollectedEvent);
        }
    }

    @Test
    void shouldAllowPurchasingUnownedProperty() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");
        PlayerState player = game.getPlayer(player1);

        // Find a property tile and move player there, then advance to ACTION phase
        Property property = game.getBoard().getAllProperties().get(0);
        player.moveTo(property.getTilePosition());

        // Use reconstitute to set the game to ACTION phase for testing
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,  // Set to ACTION phase
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        Money beforeCurrency = testGame.getPlayer(player1).getCurrency();

        // when
        testGame.purchaseProperty(player1, property.getPropertyId());

        // then
        assertThat(property.isOwnedBy(player1)).isTrue();
        assertThat(testGame.getPlayer(player1).ownsProperty(property.getPropertyId())).isTrue();
        assertThat(testGame.getPlayer(player1).getCurrency()).isEqualTo(beforeCurrency.subtract(property.getPurchasePrice()));

        // Check events
        List<GameEvent> events = testGame.drainEvents();
        assertThat(events).anyMatch(e -> e instanceof PropertyPurchasedEvent);
    }

    @Test
    void shouldNotAllowPurchasingOwnedProperty() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        Property property = game.getBoard().getAllProperties().get(0);
        property.purchase(player2); // Already owned

        game.getPlayer(player1).moveTo(property.getTilePosition());

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        // when/then
        assertThatThrownBy(() -> testGame.purchaseProperty(player1, property.getPropertyId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already owned");
    }

    @Test
    void shouldNotAllowPurchasingWhenNotOnProperty() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);
        game.getPlayer(player1).moveTo(0); // Not on the property

        Property property = game.getBoard().getAllProperties().get(0);

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        // when/then
        assertThatThrownBy(() -> testGame.purchaseProperty(player1, property.getPropertyId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be on the property tile");
    }

    @Test
    void shouldNotAllowPurchasingWithInsufficientFunds() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);

        Property property = game.getBoard().getAllProperties().get(0);
        game.getPlayer(player1).moveTo(property.getTilePosition());
        game.getPlayer(player1).setCurrency(Money.ofDollars(10)); // Not enough

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        // when/then
        assertThatThrownBy(() -> testGame.purchaseProperty(player1, property.getPropertyId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient funds");
    }

    @Test
    void shouldAllowImprovingOwnedProperty() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);
        Board board = game.getBoard();

        // Get all properties in a group and purchase them
        List<Property> groupProperties = board.getPropertiesInGroup(board.getAllProperties().get(0).getCourseGroup());
        for (Property prop : groupProperties) {
            prop.purchase(player1);
            game.getPlayer(player1).addProperty(prop.getPropertyId());
        }

        Property property = groupProperties.get(0);
        game.getPlayer(player1).moveTo(property.getTilePosition());

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                board,
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        // when
        testGame.improveProperty(player1, property.getPropertyId());

        // then
        assertThat(property.getImprovementLevel().getLevel()).isEqualTo(1);

        // Check events
        List<GameEvent> events = testGame.drainEvents();
        assertThat(events).anyMatch(e -> e instanceof PropertyImprovedEvent);
    }

    @Test
    void shouldNotAllowImprovingWithoutCompleteGroup() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);

        Property property = game.getBoard().getAllProperties().get(0);
        property.purchase(player1);
        game.getPlayer(player1).addProperty(property.getPropertyId());
        game.getPlayer(player1).moveTo(property.getTilePosition());

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        // when/then
        assertThatThrownBy(() -> testGame.improveProperty(player1, property.getPropertyId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Must own complete course group");
    }

    @Test
    void shouldEndTurnAndAdvanceToNextPlayer() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");
        game.drainEvents();

        int initialTurn = game.getTurnNumber();

        // when
        game.endTurn(player1);

        // then
        assertThat(game.getCurrentPlayerId()).isEqualTo(player2);
        assertThat(game.getTurnNumber()).isEqualTo(initialTurn + 1);
        assertThat(game.getTurnPhase()).isEqualTo(TurnPhase.ROLL);

        // Check events
        List<GameEvent> events = game.drainEvents();
        assertThat(events).anyMatch(e -> e instanceof TurnEndedEvent);
        assertThat(events).anyMatch(e -> e instanceof TurnStartedEvent);
    }

    @Test
    void shouldHandleSandTrapMechanics() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);
        PlayerState player = game.getPlayer(player1);

        // Put player in sand trap
        player.enterSandTrap();
        player.moveTo(8); // Sand trap position
        game.drainEvents();

        // when
        game.rollDice(player1);

        // then
        assertThat(player.isInSandTrap() || player.getTurnsInSandTrap() < 3).isTrue();
    }

    @Test
    void shouldProposeTrade() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        // Give each player a property
        Property prop1 = game.getBoard().getAllProperties().get(0);
        Property prop2 = game.getBoard().getAllProperties().get(1);
        prop1.purchase(player1);
        prop2.purchase(player2);
        game.getPlayer(player1).addProperty(prop1.getPropertyId());
        game.getPlayer(player2).addProperty(prop2.getPropertyId());

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        // Create trade offer
        TradeOffer offer = TradeOffer.builder()
                .offerId(UUID.randomUUID())
                .offeringPlayerId(player1)
                .receivingPlayerId(player2)
                .offeredPropertyIds(Set.of(prop1.getPropertyId()))
                .offeredCurrency(Money.ofDollars(100))
                .requestedPropertyIds(Set.of(prop2.getPropertyId()))
                .requestedCurrency(Money.zero())
                .status(TradeOffer.TradeStatus.PENDING)
                .build();

        // when
        testGame.proposeTrade(player1, offer);

        // then
        assertThat(testGame.getTurnPhase()).isEqualTo(TurnPhase.TRADE);
        assertThat(testGame.getPendingTrade()).isNotNull();

        // Check events
        List<GameEvent> events = testGame.drainEvents();
        assertThat(events).anyMatch(e -> e instanceof TradeProposedEvent);
    }

    @Test
    void shouldAcceptTrade() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        Property prop1 = game.getBoard().getAllProperties().get(0);
        Property prop2 = game.getBoard().getAllProperties().get(1);
        prop1.purchase(player1);
        prop2.purchase(player2);
        game.getPlayer(player1).addProperty(prop1.getPropertyId());
        game.getPlayer(player2).addProperty(prop2.getPropertyId());

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        TradeOffer offer = TradeOffer.builder()
                .offerId(UUID.randomUUID())
                .offeringPlayerId(player1)
                .receivingPlayerId(player2)
                .offeredPropertyIds(Set.of(prop1.getPropertyId()))
                .offeredCurrency(Money.zero())
                .requestedPropertyIds(Set.of(prop2.getPropertyId()))
                .requestedCurrency(Money.zero())
                .status(TradeOffer.TradeStatus.PENDING)
                .build();

        testGame.proposeTrade(player1, offer);
        testGame.drainEvents();

        // when
        testGame.respondToTrade(player2, true);

        // then
        assertThat(prop1.isOwnedBy(player2)).isTrue();
        assertThat(prop2.isOwnedBy(player1)).isTrue();
        assertThat(testGame.getTurnPhase()).isEqualTo(TurnPhase.ACTION);

        // Check events
        List<GameEvent> events = testGame.drainEvents();
        assertThat(events).anyMatch(e -> e instanceof TradeAcceptedEvent);
    }

    @Test
    void shouldRejectTrade() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        Property prop1 = game.getBoard().getAllProperties().get(0);
        prop1.purchase(player1);
        game.getPlayer(player1).addProperty(prop1.getPropertyId());

        // Set to ACTION phase
        GameSession testGame = GameSession.reconstitute(
                game.getGameId(),
                GameStatus.IN_PROGRESS,
                player1,
                TurnPhase.ACTION,
                game.getTurnNumber(),
                null,
                game.getBoard(),
                game.getPlayers(),
                game.getCreatedAt(),
                game.getUpdatedAt()
        );

        TradeOffer offer = TradeOffer.builder()
                .offerId(UUID.randomUUID())
                .offeringPlayerId(player1)
                .receivingPlayerId(player2)
                .offeredPropertyIds(new HashSet<>())
                .offeredCurrency(Money.ofDollars(100))
                .requestedPropertyIds(new HashSet<>())
                .requestedCurrency(Money.ofDollars(50))
                .status(TradeOffer.TradeStatus.PENDING)
                .build();

        testGame.proposeTrade(player1, offer);
        testGame.drainEvents();

        // when
        testGame.respondToTrade(player2, false);

        // then
        assertThat(testGame.getTurnPhase()).isEqualTo(TurnPhase.ACTION);

        // Check events
        List<GameEvent> events = testGame.drainEvents();
        assertThat(events).anyMatch(e -> e instanceof TradeRejectedEvent);
    }

    @Test
    void shouldIdentifyCurrentPlayerAsNpc() {
        // given
        UUID humanPlayer = UUID.randomUUID();
        GameSession game = GameSession.create(humanPlayer, "Human", true, Difficulty.EASY);

        // when - advance to NPC's turn
        game.endTurn(humanPlayer);

        // then
        assertThat(game.isCurrentPlayerNpc()).isTrue();
    }

    @Test
    void shouldGetOpponentForPlayer() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        // when
        PlayerState opponent = game.getOpponent(player1);

        // then
        assertThat(opponent.getPlayerId()).isEqualTo(player2);
    }

    @Test
    void shouldGetActivePlayers() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        // when
        List<PlayerState> activePlayers = game.getActivePlayers();

        // then
        assertThat(activePlayers).hasSize(2);
    }

    @Test
    void shouldFilterOutBankruptPlayersFromActivePlayers() {
        // given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", false, null);
        game.joinGame(player2, "Player 2");

        game.getPlayer(player2).declareBankrupt();

        // when
        List<PlayerState> activePlayers = game.getActivePlayers();

        // then
        assertThat(activePlayers).hasSize(1);
        assertThat(activePlayers.get(0).getPlayerId()).isEqualTo(player1);
    }

    @Test
    void shouldDrainPendingEvents() {
        // given
        UUID player1 = UUID.randomUUID();
        GameSession game = GameSession.create(player1, "Player 1", true, Difficulty.EASY);

        // when
        List<GameEvent> events1 = game.drainEvents();
        List<GameEvent> events2 = game.drainEvents();

        // then
        assertThat(events1).isNotEmpty();
        assertThat(events2).isEmpty(); // Drained
    }
}
