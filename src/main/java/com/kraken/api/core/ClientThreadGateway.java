package com.kraken.api.core;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.LongSupplier;

/**
 * Owns admission, deadlines and cancellation for the commands submitted by one Context.
 */
@Slf4j
public final class ClientThreadGateway {
    private enum State { PENDING, EXECUTING, COMPLETED, CANCELLED, EXPIRED }

    private final Client client;
    private final ClientThread clientThread;
    private final long timeoutNanos;
    private final LongSupplier clock;
    private final Set<Command<?>> pending = new HashSet<>();
    private boolean closed;

    /**
     * Creates a command gateway. Closing it permanently revokes its pending commands.
     * @param client Client used to identify the game thread.
     * @param clientThread RuneLite callback queue.
     * @param timeoutMillis Maximum queue and synchronous wait budget in milliseconds.
     */
    public ClientThreadGateway(Client client, ClientThread clientThread, long timeoutMillis) {
        this(client, clientThread, TimeUnit.MILLISECONDS.toNanos(timeoutMillis), System::nanoTime);
    }

    ClientThreadGateway(Client client, ClientThread clientThread, long timeoutNanos, LongSupplier clock) {
        this.client = client;
        this.clientThread = clientThread;
        this.timeoutNanos = timeoutNanos;
        this.clock = clock;
    }

    /**
     * Runs work on the game thread and waits for its result.
     * @param callable Work to execute once, if still admitted before its deadline.
     * @param <T> Result type.
     * @return The callable's result, including null.
     * @throws ClientThreadException On rejection, failure, timeout or interruption. An unknown outcome
     * indicates execution began and the caller must not assume the action can safely be retried.
     */
    public <T> T call(Callable<T> callable) {
        Command<T> command = submit(callable);
        try {
            return command.result.get(Math.max(0, command.deadline - clock.getAsLong()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            throw abandon(command, State.EXPIRED, "Client-thread wait expired", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw abandon(command, State.CANCELLED, "Interrupted while waiting on the client thread", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ClientThreadException) {
                throw (ClientThreadException) e.getCause();
            }
            throw new ClientThreadException("Client-thread work threw an exception", e.getCause());
        }
    }

    /**
     * Queues fire-and-forget work, or runs it inline on the game thread.
     * @param runnable Work to execute before its deadline; pending work is revoked on close.
     */
    public void execute(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Client-thread work must not be null");
        }
        if (client.isClientThread()) {
            call(() -> {
                runnable.run();
                return null;
            });
            return;
        }
        submit(() -> {
            runnable.run();
            return null;
        }).result.whenComplete((ignored, failure) -> {
            if (failure != null) {
                log.warn("Asynchronous client-thread work failed: {}", failure.toString());
            }
        });
    }

    /** Cancels pending commands and rejects new submissions. Already executing work may finish. */
    public synchronized void close() {
        closed = true;
        for (Command<?> command : new HashSet<>(pending)) {
            cancel(command, State.CANCELLED, "Context shut down before command execution");
        }
    }

    private <T> Command<T> submit(Callable<T> callable) {
        if (callable == null) {
            throw new IllegalArgumentException("Client-thread work must not be null");
        }
        Command<T> command;
        synchronized (this) {
            if (closed) {
                throw new ClientThreadException("Context is shut down; command was not executed");
            }
            command = new Command<>(callable, clock.getAsLong() + timeoutNanos);
            pending.add(command);
        }
        try {
            if (client.isClientThread()) {
                run(command);
            } else {
                clientThread.invoke(() -> run(command));
            }
        } catch (RuntimeException e) {
            throw abandon(command, State.CANCELLED, "Client-thread submission failed", e);
        }
        return command;
    }

    private <T> void run(Command<T> command) {
        synchronized (this) {
            if (command.state != State.PENDING) {
                return;
            }
            if (closed || clock.getAsLong() - command.deadline >= 0) {
                cancel(command, closed ? State.CANCELLED : State.EXPIRED, "Command cancelled or expired before execution");
                return;
            }
            command.state = State.EXECUTING;
            pending.remove(command);
        }
        // Never hold the admission lock while executing arbitrary client work.
        try {
            command.result.complete(command.callable.call());
        } catch (Throwable e) {
            command.result.completeExceptionally(e);
        } finally {
            synchronized (this) {
                command.state = State.COMPLETED;
            }
        }
    }

    private synchronized ClientThreadException abandon(Command<?> command, State state, String message, Throwable cause) {
        boolean unknown = command.state == State.EXECUTING || command.state == State.COMPLETED;
        if (command.state == State.PENDING) {
            cancel(command, state, message);
        }
        return new ClientThreadException(message + (unknown
                ? "; execution started, outcome is unknown" : "; command will not execute"), cause, unknown);
    }

    private void cancel(Command<?> command, State state, String message) {
        command.state = state;
        pending.remove(command);
        command.result.completeExceptionally(new ClientThreadException(message));
    }

    private static final class Command<T> {
        private final Callable<T> callable;
        private final long deadline;
        private final CompletableFuture<T> result = new CompletableFuture<>();
        private State state = State.PENDING;

        private Command(Callable<T> callable, long deadline) {
            this.callable = callable;
            this.deadline = deadline;
        }
    }
}
