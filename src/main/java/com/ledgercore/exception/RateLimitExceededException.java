package com.ledgercore.exception;

/**
 * Thrown when a user exceeds the per-second rate limit on transaction endpoints.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
