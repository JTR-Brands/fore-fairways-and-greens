package com.fore.game.api.websocket;

import com.fore.game.api.websocket.dto.PlayerPresenceMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks WebSocket connections and publishes presence events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final GameEventPublisher eventPublisher;

    // Track session -> (gameId, playerId) mapping
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket connected: sessionId={}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        SessionInfo sessionInfo = activeSessions.remove(sessionId);
        if (sessionInfo != null) {
            log.info("Player disconnected: sessionId={}, gameId={}, playerId={}", 
                    sessionId, sessionInfo.gameId, sessionInfo.playerId);
            
            eventPublisher.publishPlayerPresence(PlayerPresenceMessage.builder()
                    .gameId(sessionInfo.gameId)
                    .playerId(sessionInfo.playerId)
                    .playerName(sessionInfo.playerName)
                    .presenceType(PlayerPresenceMessage.PresenceType.DISCONNECTED)
                    .timestamp(Instant.now())
                    .build());
        }
    }

    @EventListener
    public void handleSubscription(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/game/")) {
            // Extract gameId from destination: /topic/game/{gameId}
            String gameIdStr = destination.substring("/topic/game/".length());
            
            // Try to get playerId and playerName from headers
            String playerIdHeader = accessor.getFirstNativeHeader("playerId");
            String playerNameHeader = accessor.getFirstNativeHeader("playerName");
            
            if (playerIdHeader != null) {
                UUID gameId = UUID.fromString(gameIdStr);
                UUID playerId = UUID.fromString(playerIdHeader);
                String playerName = playerNameHeader != null ? playerNameHeader : "Unknown";
                
                activeSessions.put(sessionId, new SessionInfo(gameId, playerId, playerName));
                
                log.info("Player subscribed: sessionId={}, gameId={}, playerId={}", 
                        sessionId, gameId, playerId);
                
                eventPublisher.publishPlayerPresence(PlayerPresenceMessage.builder()
                        .gameId(gameId)
                        .playerId(playerId)
                        .playerName(playerName)
                        .presenceType(PlayerPresenceMessage.PresenceType.CONNECTED)
                        .timestamp(Instant.now())
                        .build());
            }
        }
    }

    /**
     * Check if a player is currently connected to a game.
     */
    public boolean isPlayerConnected(UUID gameId, UUID playerId) {
        return activeSessions.values().stream()
                .anyMatch(info -> info.gameId.equals(gameId) && info.playerId.equals(playerId));
    }

    /**
     * Get count of connected players for a game.
     */
    public long getConnectedPlayerCount(UUID gameId) {
        return activeSessions.values().stream()
                .filter(info -> info.gameId.equals(gameId))
                .count();
    }

    private record SessionInfo(UUID gameId, UUID playerId, String playerName) {}
}
