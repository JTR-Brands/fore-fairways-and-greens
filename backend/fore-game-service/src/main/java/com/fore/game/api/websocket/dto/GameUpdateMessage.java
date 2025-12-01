package com.fore.game.api.websocket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fore.game.application.dto.ActionResultResponse;
import com.fore.game.application.dto.GameStateResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Message sent to all subscribers when game state changes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameUpdateMessage {

    private UUID gameId;
    private UpdateType updateType;
    private UUID triggeredByPlayerId;
    private GameStateResponse gameState;
    private List<ActionResultResponse.GameEventDto> events;
    private ActionResultResponse.DiceRollDto diceRoll;
    private Instant timestamp;

    public enum UpdateType {
        GAME_CREATED,
        PLAYER_JOINED,
        GAME_STARTED,
        DICE_ROLLED,
        PLAYER_MOVED,
        PROPERTY_PURCHASED,
        PROPERTY_IMPROVED,
        RENT_PAID,
        TRADE_PROPOSED,
        TRADE_ACCEPTED,
        TRADE_REJECTED,
        TURN_ENDED,
        PLAYER_BANKRUPT,
        GAME_ENDED,
        PLAYER_CONNECTED,
        PLAYER_DISCONNECTED
    }

    public static GameUpdateMessage fromActionResult(
            UUID gameId,
            UUID playerId,
            UpdateType updateType,
            ActionResultResponse result) {
        
        return GameUpdateMessage.builder()
                .gameId(gameId)
                .updateType(updateType)
                .triggeredByPlayerId(playerId)
                .gameState(result.getGameState())
                .events(result.getEvents())
                .diceRoll(result.getDiceRoll())
                .timestamp(Instant.now())
                .build();
    }

    public static GameUpdateMessage gameStateUpdate(
            UUID gameId,
            UUID playerId,
            UpdateType updateType,
            GameStateResponse gameState) {
        
        return GameUpdateMessage.builder()
                .gameId(gameId)
                .updateType(updateType)
                .triggeredByPlayerId(playerId)
                .gameState(gameState)
                .timestamp(Instant.now())
                .build();
    }
}
