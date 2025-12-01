package com.fore.game.domain.exceptions;

public class InvalidActionException extends GameException {

    public InvalidActionException(String message) {
        super("INVALID_ACTION", message, true);
    }
}
