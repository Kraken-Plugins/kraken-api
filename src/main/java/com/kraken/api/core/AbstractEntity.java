package com.kraken.api.core;

import com.kraken.api.Context;
import java.util.Objects;

/**
 * Base class for live, interactable entity views in a scene. Query materialization does not
 * freeze their state; see {@link EntityView} for the client-thread read contract.
 * @param <T> Raw RuneLite type (NPC, TileObject, Widget, etc...)
 */
public abstract class AbstractEntity<T> implements Interactable<T> {

    /** The context used to reach the client thread and the services that act on this entity. */
    protected final Context ctx;
    /** The wrapped RuneLite object. */
    protected final T raw;

    /**
     * Creates a view over a RuneLite object.
     * @param ctx The context used to act on this entity.
     * @param raw The RuneLite object to wrap.
     */
    public AbstractEntity(Context ctx, T raw) {
        this.ctx = ctx;
        this.raw = raw;
    }

    public T raw() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractEntity<?> that = (AbstractEntity<?>) o;
        return Objects.equals(raw, that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }
}