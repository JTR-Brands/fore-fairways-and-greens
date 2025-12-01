package com.fore.game.domain.exceptions;

import java.util.UUID;

public class NotYourTurnException extends GameException {

    public NotYourTurnException(UUID playerId) {
        super("NOT_YOUR_TURN", "It is not your turn. Player: " + playerId, true);
    }
}
