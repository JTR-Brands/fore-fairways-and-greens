package com.fore.game.domain.exceptions;

public class InvalidGameStateException extends GameException {

    public InvalidGameStateException(String message) {
        super("INVALID_GAME_STATE", message, true);
    }
}
