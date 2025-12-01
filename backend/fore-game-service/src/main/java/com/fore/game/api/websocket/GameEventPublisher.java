package com.fore.game.api.websocket;

import com.fore.game.api.websocket.dto.GameUpdateMessage;
import com.fore.game.api.websocket.dto.GameUpdateMessage.UpdateType;
import com.fore.game.api.websocket.dto.GameUpdateNotification;
import com.fore.game.api.websocket.dto.PlayerPresenceMessage;
import com.fore.game.application.dto.ActionResultResponse;
import com.fore.game.application.dto.GameStateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes game events to WebSocket subscribers.
 * Sends lightweight notifications; clients fetch full state via REST.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String GAME_TOPIC = "/topic/game/";

    public void publishActionResult(
            UUID gameId,
            UUID playerId,
            UpdateType updateType,
            ActionResultResponse result) {
        
        GameUpdateMessage fullMessage = GameUpdateMessage.fromActionResult(
                gameId, playerId, updateType, result);
        
        GameUpdateNotification notification = GameUpdateNotification.from(fullMessage);
        publishNotification(gameId, notification);
    }

    public void publishGameState(
            UUID gameId,
            UUID playerId,
            UpdateType updateType,
            GameStateResponse gameState) {
        
        GameUpdateNotification notification = GameUpdateNotification.builder()
                .gameId(gameId)
                .updateType(updateType)
                .triggeredByPlayerId(playerId)
                .turnNumber(gameState.getTurnNumber())
                .currentPlayerId(gameState.getCurrentPlayerId())
                .turnPhase(gameState.getTurnPhase())
                .gameStatus(gameState.getStatus())
                .timestamp(Instant.now())
                .build();
        
        publishNotification(gameId, notification);
    }

    public void publishPlayerPresence(PlayerPresenceMessage presence) {
        String destination = GAME_TOPIC + presence.getGameId();
        log.debug("Publishing presence to {}: {} {}", 
                destination, presence.getPlayerName(), presence.getPresenceType());
        
        messagingTemplate.convertAndSend(destination, presence);
    }

    public void sendToPlayer(UUID gameId, UUID playerId, Object message) {
        String destination = "/queue/game/" + gameId + "/player/" + playerId;
        log.debug("Sending to player {}: {}", playerId, destination);
        
        messagingTemplate.convertAndSend(destination, message);
    }

    private void publishNotification(UUID gameId, GameUpdateNotification notification) {
        String destination = GAME_TOPIC + gameId;
        log.info("Publishing {} to {}", notification.getUpdateType(), destination);
        
        messagingTemplate.convertAndSend(destination, notification);
    }
}
