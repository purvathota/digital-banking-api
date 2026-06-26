package com.ledgercore.exception;

/**
 * Thrown when a duplicate email is detected during registration.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
