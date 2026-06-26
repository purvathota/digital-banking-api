package com.ledgercore.exception;

/**
 * Thrown when a withdrawal or transfer exceeds the available account balance.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
