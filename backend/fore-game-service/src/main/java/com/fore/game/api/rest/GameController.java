package com.fore.game.api.rest;

import com.fore.game.application.dto.*;
import com.fore.game.application.usecases.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/games")
@RequiredArgsConstructor
public class GameController {

    private final CreateGameUseCase createGameUseCase;
    private final JoinGameUseCase joinGameUseCase;
    private final GetGameUseCase getGameUseCase;
    private final ExecuteActionUseCase executeActionUseCase;

    /**
     * Create a new game.
     */
    @PostMapping
    public ResponseEntity<GameStateResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        log.info("POST /api/v1/games - Creating game for player: {}", request.getPlayerId());
        GameStateResponse response = createGameUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get game state by ID.
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateResponse> getGame(@PathVariable UUID gameId) {
        log.debug("GET /api/v1/games/{}", gameId);
        GameStateResponse response = getGameUseCase.getById(gameId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get available games (waiting for players).
     */
    @GetMapping("/available")
    public ResponseEntity<AvailableGamesResponse> getAvailableGames() {
        log.debug("GET /api/v1/games/available");
        AvailableGamesResponse response = getGameUseCase.getAvailableGames();
        return ResponseEntity.ok(response);
    }

    /**
     * Get games for a specific player.
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<GameStateResponse>> getPlayerGames(@PathVariable UUID playerId) {
        log.debug("GET /api/v1/games/player/{}", playerId);
        List<GameStateResponse> response = getGameUseCase.getGamesByPlayer(playerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Join an existing game.
     */
    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameStateResponse> joinGame(
            @PathVariable UUID gameId,
            @Valid @RequestBody JoinGameRequest request) {
        log.info("POST /api/v1/games/{}/join - Player: {}", gameId, request.getPlayerId());
        GameStateResponse response = joinGameUseCase.execute(gameId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Execute a player action.
     */
    @PostMapping("/{gameId}/actions")
    public ResponseEntity<ActionResultResponse> executeAction(
            @PathVariable UUID gameId,
            @Valid @RequestBody PlayerActionRequest request) {
        log.info("POST /api/v1/games/{}/actions - Action: {} by Player: {}", 
                gameId, request.getActionType(), request.getPlayerId());
        ActionResultResponse response = executeActionUseCase.execute(gameId, request);
        return ResponseEntity.ok(response);
    }
}
