package com.fore.game.api.websocket;

import com.fore.game.application.dto.GameStateResponse;
import com.fore.game.application.usecases.GetGameUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * WebSocket controller for client-initiated messages.
 * Most game actions go through REST API, but this allows for
 * direct WebSocket communication if needed.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {

    private final GetGameUseCase getGameUseCase;

    /**
     * Client can request current game state via WebSocket.
     * Send to: /app/game/{gameId}/state
     * Response: /topic/game/{gameId}
     */
    @MessageMapping("/game/{gameId}/state")
    @SendTo("/topic/game/{gameId}")
    public GameStateResponse requestGameState(@DestinationVariable UUID gameId) {
        log.debug("WebSocket state request for game: {}", gameId);
        return getGameUseCase.getById(gameId);
    }
}
