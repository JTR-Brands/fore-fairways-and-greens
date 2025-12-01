package com.fore.game.api.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fore.game.application.dto.ActionResultResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Lightweight notification sent via WebSocket.
 * Clients fetch full state via REST API after receiving notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUpdateNotification {

    private UUID gameId;
    private GameUpdateMessage.UpdateType updateType;
    private UUID triggeredByPlayerId;
    private int turnNumber;
    private UUID currentPlayerId;
    private String turnPhase;
    private String gameStatus;
    private ActionResultResponse.DiceRollDto diceRoll;
    private List<ActionResultResponse.GameEventDto> events;
    private Instant timestamp;

    public static GameUpdateNotification from(GameUpdateMessage fullMessage) {
        var gameState = fullMessage.getGameState();
        return GameUpdateNotification.builder()
                .gameId(fullMessage.getGameId())
                .updateType(fullMessage.getUpdateType())
                .triggeredByPlayerId(fullMessage.getTriggeredByPlayerId())
                .turnNumber(gameState != null ? gameState.getTurnNumber() : 0)
                .currentPlayerId(gameState != null ? gameState.getCurrentPlayerId() : null)
                .turnPhase(gameState != null ? gameState.getTurnPhase() : null)
                .gameStatus(gameState != null ? gameState.getStatus() : null)
                .diceRoll(fullMessage.getDiceRoll())
                .events(fullMessage.getEvents())
                .timestamp(fullMessage.getTimestamp())
                .build();
    }
}
