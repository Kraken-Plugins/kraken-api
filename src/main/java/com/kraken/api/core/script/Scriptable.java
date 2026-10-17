package com.kraken.api.core.script;

/**
 * Interface representing a script that can be executed.
 * Implementations of this interface define the main loop of a script.
 */
public interface Scriptable {

    /**
     * Gracefully stops a running asynchronous loop().
     * @param callback Callback function to execute once the the loop() is stopped.
     */
    void stop(Runnable callback);

    /**
     * Initializes the script before it accepts ticks. Called once per run, before the loop starts.
     */
    void onStart();

    /**
     * Cleans up after the script's last iteration. Called once per run, only if its initialization succeeded.
     */
    void onStop();
}
