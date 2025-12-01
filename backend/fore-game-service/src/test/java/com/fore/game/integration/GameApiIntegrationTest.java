package com.fore.game.integration;

import com.fore.game.application.dto.*;
import com.fore.game.application.dto.PlayerActionRequest.ActionType;
import com.fore.game.domain.model.enums.Difficulty;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GameApiIntegrationTest extends BaseIntegrationTest {

    private static final UUID PLAYER_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PLAYER_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PLAYER_1_NAME = "TestPlayer1";
    private static final String PLAYER_2_NAME = "TestPlayer2";

    // ==================== Health & Stats ====================

    @Test
    void healthCheck_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("fore-game-service"));
    }

    @Test
    void stats_shouldReturnGameCounts() throws Exception {
        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gamesWaiting").isNumber())
                .andExpect(jsonPath("$.gamesInProgress").isNumber())
                .andExpect(jsonPath("$.gamesCompleted").isNumber());
    }

    // ==================== Create Game ====================

    @Test
    void createGame_withNpc_shouldCreateAndStartGame() throws Exception {
        CreateGameRequest request = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .npcDifficulty(Difficulty.MEDIUM)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.turnPhase").value("ROLL"))
                .andExpect(jsonPath("$.turnNumber").value(1))
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andReturn();

        GameStateResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GameStateResponse.class
        );

        // Verify board has 24 tiles
        assertThat(response.getBoard().getTiles()).hasSize(24);

        // Verify players have starting currency ($1500 = 150000 cents)
        assertThat(response.getPlayers()).allSatisfy(player -> {
            assertThat(player.getCurrencyCents()).isEqualTo(150000L);
            assertThat(player.getPosition()).isEqualTo(0);
            assertThat(player.isBankrupt()).isFalse();
        });

        // Verify one player is NPC
        long npcCount = response.getPlayers().stream()
                .filter(GameStateResponse.PlayerStateDto::isNpc)
                .count();
        assertThat(npcCount).isEqualTo(1);
        
        // Current player should be the human player (creator)
        assertThat(response.getCurrentPlayerId()).isEqualTo(PLAYER_1_ID);
    }

    @Test
    void createGame_forHumanVsHuman_shouldCreateWaitingGame() throws Exception {
        CreateGameRequest request = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(false)
                .build();

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.players", hasSize(1)));
    }

    @Test
    void createGame_withMissingPlayerId_shouldReturn400() throws Exception {
        String invalidRequest = """
                {
                    "playerName": "Test",
                    "vsNpc": true
                }
                """;

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("playerId"));
    }

    @Test
    void createGame_withBlankPlayerName_shouldReturn400() throws Exception {
        CreateGameRequest request = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName("")
                .vsNpc(true)
                .build();

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ==================== Join Game ====================

    @Test
    void joinGame_shouldAddPlayerAndStartGame() throws Exception {
        // First create a waiting game
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(false)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );
        UUID gameId = created.getGameId();

        // Now join the game
        JoinGameRequest joinRequest = JoinGameRequest.builder()
                .playerId(PLAYER_2_ID)
                .playerName(PLAYER_2_NAME)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/join", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.turnNumber").value(1))
                .andExpect(jsonPath("$.currentPlayerId").isNotEmpty());
    }

    @Test
    void joinGame_nonExistentGame_shouldReturn404() throws Exception {
        UUID fakeGameId = UUID.randomUUID();
        JoinGameRequest joinRequest = JoinGameRequest.builder()
                .playerId(PLAYER_2_ID)
                .playerName(PLAYER_2_NAME)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/join", fakeGameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    @Test
    void joinGame_alreadyStartedGame_shouldReturn400() throws Exception {
        // Create a game vs NPC (auto-starts)
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );

        // Try to join an in-progress game
        JoinGameRequest joinRequest = JoinGameRequest.builder()
                .playerId(PLAYER_2_ID)
                .playerName(PLAYER_2_NAME)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/join", created.getGameId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(joinRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_GAME_STATE"));
    }

    // ==================== Get Game ====================

    @Test
    void getGame_existingGame_shouldReturnFullState() throws Exception {
        // Create a game
        CreateGameRequest request = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );

        // Get the game
        mockMvc.perform(get("/api/v1/games/{gameId}", created.getGameId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(created.getGameId().toString()))
                .andExpect(jsonPath("$.board.tiles", hasSize(24)))
                .andExpect(jsonPath("$.players", hasSize(2)));
    }

    @Test
    void getGame_nonExistentGame_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/games/{gameId}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ==================== Available Games ====================

    @Test
    void getAvailableGames_shouldReturnWaitingGames() throws Exception {
        // Create a waiting game
        CreateGameRequest request = CreateGameRequest.builder()
                .playerId(UUID.randomUUID())
                .playerName("WaitingPlayer")
                .vsNpc(false)
                .build();

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get available games
        mockMvc.perform(get("/api/v1/games/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games").isArray())
                .andExpect(jsonPath("$.games", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalCount").isNumber());
    }

    // ==================== Player Actions ====================

    @Test
    void rollDice_onPlayerTurn_shouldMovePlayer() throws Exception {
        // Create a game
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );

        // Roll dice
        PlayerActionRequest rollRequest = PlayerActionRequest.builder()
                .playerId(PLAYER_1_ID)
                .actionType(ActionType.ROLL_DICE)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/actions", created.getGameId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.actionType").value("ROLL_DICE"))
                .andExpect(jsonPath("$.diceRoll").isNotEmpty())
                .andExpect(jsonPath("$.diceRoll.die1").isNumber())
                .andExpect(jsonPath("$.diceRoll.die2").isNumber())
                .andExpect(jsonPath("$.diceRoll.total").isNumber())
                .andExpect(jsonPath("$.events").isArray())
                .andExpect(jsonPath("$.gameState").isNotEmpty());
    }

    @Test
    void rollDice_notYourTurn_shouldReturn400() throws Exception {
        // Create a game where PLAYER_1 is current player
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );

        // Find the NPC player ID
        UUID npcId = created.getPlayers().stream()
                .filter(GameStateResponse.PlayerStateDto::isNpc)
                .findFirst()
                .map(GameStateResponse.PlayerStateDto::getPlayerId)
                .orElseThrow();

        // Try to roll as NPC when it's PLAYER_1's turn
        PlayerActionRequest rollRequest = PlayerActionRequest.builder()
                .playerId(npcId)
                .actionType(ActionType.ROLL_DICE)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/actions", created.getGameId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ACTION"));
    }

    @Test
    void endTurn_afterRolling_shouldSwitchPlayer() throws Exception {
        // Create a game
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );
        UUID gameId = created.getGameId();

        // Roll dice first
        PlayerActionRequest rollRequest = PlayerActionRequest.builder()
                .playerId(PLAYER_1_ID)
                .actionType(ActionType.ROLL_DICE)
                .build();

        MvcResult rollResult = mockMvc.perform(post("/api/v1/games/{gameId}/actions", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ActionResultResponse rollResponse = objectMapper.readValue(
                rollResult.getResponse().getContentAsString(),
                ActionResultResponse.class
        );

        // Only try to end turn if we're in ACTION phase (not ROLL due to doubles)
        if ("ACTION".equals(rollResponse.getGameState().getTurnPhase())) {
            PlayerActionRequest endTurnRequest = PlayerActionRequest.builder()
                    .playerId(PLAYER_1_ID)
                    .actionType(ActionType.END_TURN)
                    .build();

            MvcResult endTurnResult = mockMvc.perform(post("/api/v1/games/{gameId}/actions", gameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(endTurnRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andReturn();

            ActionResultResponse response = objectMapper.readValue(
                    endTurnResult.getResponse().getContentAsString(),
                    ActionResultResponse.class
            );

            // Current player should have changed
            assertThat(response.getGameState().getCurrentPlayerId())
                    .isNotEqualTo(PLAYER_1_ID);
            assertThat(response.getGameState().getTurnNumber()).isEqualTo(2);
        }
        // If doubles were rolled, we're still in ROLL phase, which is valid behavior
    }

    @Test
    void endTurn_beforeRolling_shouldFail() throws Exception {
        // Create a game
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andReturn();

        GameStateResponse created = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );

        // Verify we're in ROLL phase
        assertThat(created.getTurnPhase()).isEqualTo("ROLL");

        // Try to end turn without rolling - should fail
        PlayerActionRequest endTurnRequest = PlayerActionRequest.builder()
                .playerId(PLAYER_1_ID)
                .actionType(ActionType.END_TURN)
                .build();

        // The game allows END_TURN from ROLL phase based on current implementation
        // Let's check what actually happens
        mockMvc.perform(post("/api/v1/games/{gameId}/actions", created.getGameId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(endTurnRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeAction_nonExistentGame_shouldReturn404() throws Exception {
        PlayerActionRequest rollRequest = PlayerActionRequest.builder()
                .playerId(PLAYER_1_ID)
                .actionType(ActionType.ROLL_DICE)
                .build();

        mockMvc.perform(post("/api/v1/games/{gameId}/actions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GAME_NOT_FOUND"));
    }

    // ==================== Full Game Flow ====================

    @Test
    void fullGameFlow_createRollEndTurn_shouldWork() throws Exception {
        // 1. Create game vs NPC
        CreateGameRequest createRequest = CreateGameRequest.builder()
                .playerId(PLAYER_1_ID)
                .playerName(PLAYER_1_NAME)
                .vsNpc(true)
                .npcDifficulty(Difficulty.EASY)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn();

        GameStateResponse game = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                GameStateResponse.class
        );
        UUID gameId = game.getGameId();

        // Verify initial state
        assertThat(game.getTurnPhase()).isEqualTo("ROLL");
        assertThat(game.getCurrentPlayerId()).isEqualTo(PLAYER_1_ID);

        // 2. Roll dice
        PlayerActionRequest rollRequest = PlayerActionRequest.builder()
                .playerId(PLAYER_1_ID)
                .actionType(ActionType.ROLL_DICE)
                .build();

        MvcResult rollResult = mockMvc.perform(post("/api/v1/games/{gameId}/actions", gameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollRequest)))
                .andExpect(status().isOk())
                .andReturn();

        ActionResultResponse rollResponse = objectMapper.readValue(
                rollResult.getResponse().getContentAsString(),
                ActionResultResponse.class
        );

        // Verify dice roll
        assertThat(rollResponse.getDiceRoll()).isNotNull();
        assertThat(rollResponse.getDiceRoll().getDie1()).isBetween(1, 6);
        assertThat(rollResponse.getDiceRoll().getDie2()).isBetween(1, 6);

        // Player should have moved
        GameStateResponse afterRoll = rollResponse.getGameState();
        GameStateResponse.PlayerStateDto player1 = afterRoll.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(PLAYER_1_ID))
                .findFirst()
                .orElseThrow();

        // Position should match dice roll (mod 24)
        int expectedPosition = rollResponse.getDiceRoll().getTotal() % 24;
        assertThat(player1.getPosition()).isEqualTo(expectedPosition);

        // 3. End turn (if in ACTION phase - might be in ROLL if doubles)
        if ("ACTION".equals(afterRoll.getTurnPhase())) {
            PlayerActionRequest endTurnRequest = PlayerActionRequest.builder()
                    .playerId(PLAYER_1_ID)
                    .actionType(ActionType.END_TURN)
                    .build();

            MvcResult endResult = mockMvc.perform(post("/api/v1/games/{gameId}/actions", gameId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(endTurnRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            ActionResultResponse endResponse = objectMapper.readValue(
                    endResult.getResponse().getContentAsString(),
                    ActionResultResponse.class
            );

            // Should be turn 2 now
            assertThat(endResponse.getGameState().getTurnNumber()).isEqualTo(2);
            // Should be NPC's turn
            assertThat(endResponse.getGameState().getCurrentPlayerId()).isNotEqualTo(PLAYER_1_ID);
        }

        // 4. Verify game can still be retrieved
        mockMvc.perform(get("/api/v1/games/{gameId}", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(gameId.toString()));
    }
}
