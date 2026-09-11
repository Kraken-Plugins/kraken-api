package com.kraken.api.core;

import lombok.Getter;

/**
 * Reports client-thread rejection, execution failure or an interrupted/expired wait.
 * When {@code #isOutcomeUnknown()} is true, execution began and may still finish; callers
 * must not interpret the exception as proof that no side effect occurred.
 */
public class ClientThreadException extends RuntimeException {

    @Getter
    private final boolean outcomeUnknown;

    /**
     * Creates a failure with an explicit execution-outcome flag.
     * @param message Failure description.
     * @param cause Underlying failure.
     * @param outcomeUnknown Whether effects cannot be ruled out, including an in-flight action or compromised transport.
     */
    public ClientThreadException(String message, Throwable cause, boolean outcomeUnknown) {
        super(message, cause);
        this.outcomeUnknown = outcomeUnknown;
    }


    /**
     * Creates a new exception describing a failed client-thread hand-off.
     * @param message What was being attempted and how it failed.
     */
    public ClientThreadException(String message) {
        this(message, null, false);
    }

    /**
     * Creates a new exception describing a failed client-thread hand-off.
     * @param message What was being attempted and how it failed.
     * @param cause The underlying timeout, interruption, or thrown exception.
     */
    public ClientThreadException(String message, Throwable cause) {
        this(message, cause, false);
    }
}
