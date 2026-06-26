package com.ledgercore.exception;

/**
 * Thrown when a referenced account does not exist.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
