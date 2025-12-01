package com.fore.game.domain.exceptions;

import java.util.UUID;

public class GameNotFoundException extends GameException {

    public GameNotFoundException(UUID gameId) {
        super("GAME_NOT_FOUND", "Game not found: " + gameId, false);
    }
}
