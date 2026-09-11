package com.kraken.api.core;

/**
 * A view backed by a live game object, not an immutable snapshot of its state.
 * Read view getters and raw-object state on the client thread unless a method explicitly documents
 * its own handoff. Query result collections copy membership only. For worker processing, capture
 * immutable values with {@link AbstractQuery#snapshot(java.util.function.Function)}.
 *
 * @param <T> The backing object type.
 */
public interface EntityView<T> {
    /**
     * Returns the live backing object without copying or scheduling access to its state.
     * @return The backing object.
     */
    T raw();
}
