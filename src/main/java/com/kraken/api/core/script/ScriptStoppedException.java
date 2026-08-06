package com.kraken.api.core.script;

/**
 * Thrown to unwind a script thread when its script has been stopped or the thread was interrupted.
 *
 * <p>Blocking helpers throw this from inside a wait so that a stopping script does not have to run to
 * completion before it releases. It signals an intentional stop, not a failure: catching it to log an
 * error will report a routine shutdown as a problem.</p>
 */
public class ScriptStoppedException extends RuntimeException {

    /**
     * Creates a new exception describing why the script thread is unwinding.
     * @param message What the thread was doing when the stop was observed.
     */
    public ScriptStoppedException(String message) {
        super(message);
    }
}
