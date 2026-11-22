package com.kraken.api.service.pathfinding;

/**
 * A map from {@code int} keys to objects that boxes no key. A search looks up the transports
 * leaving a tile once for every tile it expands, so the lookup has to cost no allocation.
 *
 * <p>Keys are placed by open addressing with linear probing. Entries can be added and replaced but
 * not removed, which is all the transport table of a search needs.</p>
 *
 * @param <V> the type of the values
 */
final class IntObjectMap<V> {

    private int[] keys;
    private Object[] values;
    private int size;

    IntObjectMap(int expectedSize) {
        int capacity = 16;
        while (capacity < expectedSize * 2) {
            capacity <<= 1;
        }
        keys = new int[capacity];
        values = new Object[capacity];
    }

    /**
     * Stores a value under a key, replacing any value already there.
     *
     * @param key the key
     * @param value the value, which must not be null
     */
    void put(int key, V value) {
        if ((size + 1) * 2 > keys.length) {
            grow();
        }
        int slot = slotOf(key);
        if (values[slot] == null) {
            size++;
        }
        keys[slot] = key;
        values[slot] = value;
    }

    /**
     * Looks up a key.
     *
     * @param key the key
     * @param fallback what to return when the key has no value
     * @return the value stored under the key, or the fallback
     */
    @SuppressWarnings("unchecked")
    V getOrDefault(int key, V fallback) {
        Object value = values[slotOf(key)];
        return value != null ? (V) value : fallback;
    }

    int size() {
        return size;
    }

    /** Finds the slot a key is in, or the empty slot it would go in. */
    private int slotOf(int key) {
        int mask = keys.length - 1;
        int slot = mix(key) & mask;
        while (values[slot] != null && keys[slot] != key) {
            slot = (slot + 1) & mask;
        }
        return slot;
    }

    /** Spreads the bits of a packed tile, whose neighbours differ only in their lowest bits. */
    private static int mix(int key) {
        int hash = key * 0x9E3779B9;
        return hash ^ (hash >>> 16);
    }

    private void grow() {
        int[] oldKeys = keys;
        Object[] oldValues = values;
        keys = new int[oldKeys.length << 1];
        values = new Object[oldValues.length << 1];
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldValues[i] != null) {
                int slot = slotOf(oldKeys[i]);
                keys[slot] = oldKeys[i];
                values[slot] = oldValues[i];
            }
        }
    }
}
