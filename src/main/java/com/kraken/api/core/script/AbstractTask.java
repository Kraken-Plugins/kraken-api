package com.kraken.api.core.script;

import com.google.inject.Inject;
import com.kraken.api.Context;


/**
 * Abstract base class for Script tasks.
 * Provides a {@link Context} instance injected by Guice.
 */
public abstract class AbstractTask implements Task {
    /** The context injected by Guice, used by the task to reach the game and its services. */
    @Inject
    protected Context ctx;
}
