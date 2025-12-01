package com.fore.game.api.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Message for player connection/disconnection events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerPresenceMessage {

    private UUID gameId;
    private UUID playerId;
    private String playerName;
    private PresenceType presenceType;
    private Instant timestamp;

    public enum PresenceType {
        CONNECTED,
        DISCONNECTED,
        RECONNECTED
    }
}
