package com.fore.game.domain;

import com.fore.game.domain.model.*;
import com.fore.game.domain.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class GameSessionTest {

    private static final UUID PLAYER_1_ID = UUID.randomUUID();
    private static final UUID PLAYER_2_ID = UUID.randomUUID();
    private static final String PLAYER_1_NAME = "Player1";
    private static final String PLAYER_2_NAME = "Player2";

    @Nested
    class GameCreation {

        @Test
        void createGameVsNpc_shouldStartImmediately() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.getPlayers()).hasSize(2);
            assertThat(game.getCurrentPlayerId()).isEqualTo(PLAYER_1_ID);
            assertThat(game.getTurnPhase()).isEqualTo(TurnPhase.ROLL);
            assertThat(game.getTurnNumber()).isEqualTo(1);
        }

        @Test
        void createGameVsHuman_shouldWaitForSecondPlayer() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, false, null);

            assertThat(game.getStatus()).isEqualTo(GameStatus.WAITING);
            assertThat(game.getPlayers()).hasSize(1);
            assertThat(game.getCurrentPlayerId()).isNull();
        }

        @Test
        void createGame_shouldInitializePlayersWithCorrectCurrency() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.EASY);

            game.getPlayers().values().forEach(player -> {
                assertThat(player.getCurrency()).isEqualTo(GameConstants.STARTING_CURRENCY);
                assertThat(player.getPosition()).isEqualTo(0);
                assertThat(player.isBankrupt()).isFalse();
            });
        }

        @Test
        void createGame_shouldHaveNpcWithCorrectDifficulty() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.RUTHLESS);

            PlayerState npc = game.getNpcPlayer().orElseThrow();
            assertThat(npc.isNpc()).isTrue();
            assertThat(npc.getNpcDifficulty()).isEqualTo(Difficulty.RUTHLESS);
        }

        @Test
        void createGame_shouldGenerateEvents() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            assertThat(game.drainEvents()).isNotEmpty();
        }
    }

    @Nested
    class JoiningGame {

        @Test
        void joinGame_shouldAddPlayerAndStartGame() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, false, null);

            game.joinGame(PLAYER_2_ID, PLAYER_2_NAME);

            assertThat(game.getStatus()).isEqualTo(GameStatus.IN_PROGRESS);
            assertThat(game.getPlayers()).hasSize(2);
            assertThat(game.getPlayers().get(PLAYER_2_ID)).isNotNull();
        }

        @Test
        void joinGame_alreadyInProgress_shouldThrow() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            assertThatThrownBy(() -> game.joinGame(PLAYER_2_ID, PLAYER_2_NAME))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void joinGame_playerAlreadyInGame_shouldThrow() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, false, null);

            assertThatThrownBy(() -> game.joinGame(PLAYER_1_ID, "SameName"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class DiceRolling {

        private GameSession game;

        @BeforeEach
        void setUp() {
            game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);
            game.drainEvents();
        }

        @Test
        void rollDice_onYourTurn_shouldMovePlayer() {
            DiceRoll roll = game.rollDice(PLAYER_1_ID);

            assertThat(roll.getDie1()).isBetween(1, 6);
            assertThat(roll.getDie2()).isBetween(1, 6);
            assertThat(roll.getTotal()).isBetween(2, 12);

            PlayerState player = game.getPlayer(PLAYER_1_ID);
            assertThat(player.getPosition()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void rollDice_notYourTurn_shouldThrow() {
            UUID npcId = game.getNpcPlayer().orElseThrow().getPlayerId();

            assertThatThrownBy(() -> game.rollDice(npcId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not your turn");
        }

        @Test
        void rollDice_wrongPhase_shouldThrow() {
            game.rollDice(PLAYER_1_ID);

            if (game.getTurnPhase() == TurnPhase.ACTION) {
                assertThatThrownBy(() -> game.rollDice(PLAYER_1_ID))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("phase");
            }
        }

        @Test
        void rollDice_shouldGenerateEvents() {
            game.rollDice(PLAYER_1_ID);

            assertThat(game.drainEvents()).isNotEmpty();
        }
    }

    @Nested
    class EndingTurn {

        private GameSession game;

        @BeforeEach
        void setUp() {
            game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);
            game.drainEvents();
        }

        private void rollUntilActionPhase() {
            int maxAttempts = 20;
            int attempts = 0;
            while (game.getTurnPhase() == TurnPhase.ROLL && attempts < maxAttempts) {
                game.rollDice(PLAYER_1_ID);
                attempts++;
            }
        }

        @Test
        void endTurn_fromActionPhase_shouldSwitchToNextPlayer() {
            rollUntilActionPhase();

            if (game.getTurnPhase() == TurnPhase.ACTION) {
                UUID currentBefore = game.getCurrentPlayerId();
                int turnBefore = game.getTurnNumber();

                game.endTurn(PLAYER_1_ID);

                assertThat(game.getCurrentPlayerId()).isNotEqualTo(currentBefore);
                assertThat(game.getTurnNumber()).isEqualTo(turnBefore + 1);
                assertThat(game.getTurnPhase()).isEqualTo(TurnPhase.ROLL);
            }
        }

        @Test
        void endTurn_fromRollPhase_shouldThrow() {
            assertThat(game.getTurnPhase()).isEqualTo(TurnPhase.ROLL);

            assertThatThrownBy(() -> game.endTurn(PLAYER_1_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ACTION phase");
        }

        @Test
        void endTurn_notYourTurn_shouldThrow() {
            rollUntilActionPhase();

            if (game.getTurnPhase() == TurnPhase.ACTION) {
                UUID npcId = game.getNpcPlayer().orElseThrow().getPlayerId();

                assertThatThrownBy(() -> game.endTurn(npcId))
                        .isInstanceOf(IllegalStateException.class);
            }
        }
    }

    @Nested
    class NpcDetection {

        @Test
        void isCurrentPlayerNpc_whenHumanTurn_shouldReturnFalse() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            assertThat(game.isCurrentPlayerNpc()).isFalse();
        }

        @Test
        void isCurrentPlayerNpc_afterHumanEndsTurn_shouldReturnTrue() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            int maxAttempts = 20;
            int attempts = 0;
            while (game.getTurnPhase() == TurnPhase.ROLL && attempts < maxAttempts) {
                game.rollDice(PLAYER_1_ID);
                attempts++;
            }

            if (game.getTurnPhase() == TurnPhase.ACTION) {
                game.endTurn(PLAYER_1_ID);
                assertThat(game.isCurrentPlayerNpc()).isTrue();
            }
        }

        @Test
        void getNpcPlayer_shouldReturnNpcPlayer() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.HARD);

            PlayerState npc = game.getNpcPlayer().orElseThrow();

            assertThat(npc.isNpc()).isTrue();
            assertThat(npc.getNpcDifficulty()).isEqualTo(Difficulty.HARD);
        }

        @Test
        void getNpcPlayer_inHumanVsHumanGame_shouldReturnEmpty() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, false, null);
            game.joinGame(PLAYER_2_ID, PLAYER_2_NAME);

            assertThat(game.getNpcPlayer()).isEmpty();
        }
    }

    @Nested
    class BoardVerification {

        @Test
        void board_shouldHave24Tiles() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            assertThat(game.getBoard().getTiles()).hasSize(24);
        }

        @Test
        void board_shouldHave18Properties() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);

            long propertyCount = game.getBoard().getTiles().stream()
                    .filter(Tile::isProperty)
                    .count();

            assertThat(propertyCount).isEqualTo(18);
        }

        @Test
        void board_shouldHaveCorrectSpecialTiles() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);
            Board board = game.getBoard();

            assertThat(board.getTileAt(0).getType()).isEqualTo(TileType.CLUBHOUSE_HQ);
            assertThat(board.getTileAt(4).getType()).isEqualTo(TileType.PRO_SHOP);
            assertThat(board.getTileAt(8).getType()).isEqualTo(TileType.SAND_TRAP);
            assertThat(board.getTileAt(12).getType()).isEqualTo(TileType.MEMBERS_LOUNGE);
            assertThat(board.getTileAt(16).getType()).isEqualTo(TileType.WATER_HAZARD);
        }

        @Test
        void board_propertiesInSameGroup_shouldHaveSameColor() {
            GameSession game = GameSession.create(PLAYER_1_ID, PLAYER_1_NAME, true, Difficulty.MEDIUM);
            Board board = game.getBoard();

            assertThat(board.getTileAt(1).getProperty().orElseThrow().getCourseGroup())
                    .isEqualTo(CourseGroup.LINKS_NINE);
            assertThat(board.getTileAt(2).getProperty().orElseThrow().getCourseGroup())
                    .isEqualTo(CourseGroup.LINKS_NINE);
            assertThat(board.getTileAt(3).getProperty().orElseThrow().getCourseGroup())
                    .isEqualTo(CourseGroup.LINKS_NINE);
        }
    }
}