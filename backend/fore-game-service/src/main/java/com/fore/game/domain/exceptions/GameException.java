package com.fore.game.domain.exceptions;

import lombok.Getter;

@Getter
public class GameException extends RuntimeException {

    private final String errorCode;
    private final boolean recoverable;

    public GameException(String errorCode, String message, boolean recoverable) {
        super(message);
        this.errorCode = errorCode;
        this.recoverable = recoverable;
    }

    public GameException(String errorCode, String message) {
        this(errorCode, message, true);
    }
}
