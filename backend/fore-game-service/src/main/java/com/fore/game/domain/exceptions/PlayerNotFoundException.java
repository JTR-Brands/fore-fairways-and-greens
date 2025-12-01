package com.fore.game.domain.exceptions;

import java.util.UUID;

public class PlayerNotFoundException extends GameException {

    public PlayerNotFoundException(UUID playerId) {
        super("PLAYER_NOT_FOUND", "Player not found: " + playerId, false);
    }
}
