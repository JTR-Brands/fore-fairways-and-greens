package com.fore.game.domain.exceptions;

import com.fore.common.types.Money;

public class InsufficientFundsException extends GameException {

    public InsufficientFundsException(Money available, Money required) {
        super("INSUFFICIENT_FUNDS", 
              String.format("Insufficient funds. Available: %s, Required: %s", available, required),
              true);
    }
}
