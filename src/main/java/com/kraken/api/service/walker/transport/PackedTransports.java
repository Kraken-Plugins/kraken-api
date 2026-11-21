package com.kraken.api.service.walker.transport;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import shortestpath.PrimitiveIntHashMap;
import shortestpath.transport.Transport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Edits the planner's packed map of origin tile to the transport edges that leave it.
 *
 * <p>The maps belong to one search request and {@code refresh()} rebuilds them for the next one, so
 * an edit made here lasts for a single search and repeated edits do not accumulate.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PackedTransports {

    private static final Transport[] NONE = new Transport[0];

    /**
     * Removes the edges leaving one tile that match a test. Other edges on that tile stay in place.
     *
     * @param packed origin to transport array, may be null
     * @param origin the packed origin tile to edit
     * @param unwanted matches the edges to remove
     */
    public static void remove(PrimitiveIntHashMap<Transport[]> packed, int origin, Predicate<Transport> unwanted) {
        if (packed == null) {
            return;
        }

        Transport[] current = packed.get(origin);
        if (current == null || current.length == 0) {
            return;
        }

        List<Transport> kept = new ArrayList<>(current.length);
        for (Transport transport : current) {
            if (!unwanted.test(transport)) {
                kept.add(transport);
            }
        }

        if (kept.size() != current.length) {
            packed.put(origin, kept.toArray(NONE));
        }
    }

    /**
     * Adds an edge ahead of the edges already leaving its origin tile, so a lookup that takes the
     * first edge to a destination finds this one.
     *
     * @param packed origin to transport array, may be null
     * @param edge the edge to add, keyed by its own origin
     */
    public static void prepend(PrimitiveIntHashMap<Transport[]> packed, Transport edge) {
        if (packed == null || edge == null) {
            return;
        }

        Transport[] current = packed.get(edge.getOrigin());
        int existing = current != null ? current.length : 0;

        Transport[] combined = new Transport[existing + 1];
        combined[0] = edge;
        if (existing > 0) {
            System.arraycopy(current, 0, combined, 1, existing);
        }

        packed.put(edge.getOrigin(), combined);
    }
}
