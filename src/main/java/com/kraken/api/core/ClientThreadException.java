package com.kraken.api.core;

/**
 * Thrown when work handed to the RuneLite client thread could not be completed.
 *
 * <p>This means the <em>question could not be asked</em> — the client thread did not answer within the
 * budget, the work threw, or the waiting thread was interrupted. It does not mean the answer was
 * "nothing": a query that legitimately finds no match returns an empty result, and a widget that does
 * not exist is still reported as {@code null}.
 *
 * <p>Callers that would rather degrade than fail have two options that never throw:
 * {@link com.kraken.api.Context#runOnClientThreadOptional(java.util.concurrent.Callable)} and
 * {@link com.kraken.api.Context#runOnClientThread(java.util.concurrent.Callable, Object)}.</p>
 */
public class ClientThreadException extends RuntimeException {

    /**
     * Creates a new exception describing a failed client-thread hand-off.
     * @param message What was being attempted and how it failed.
     */
    public ClientThreadException(String message) {
        super(message);
    }

    /**
     * Creates a new exception describing a failed client-thread hand-off.
     * @param message What was being attempted and how it failed.
     * @param cause The underlying timeout, interruption, or thrown exception.
     */
    public ClientThreadException(String message, Throwable cause) {
        super(message, cause);
    }
}
