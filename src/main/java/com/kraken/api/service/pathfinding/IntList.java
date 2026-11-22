package com.kraken.api.service.pathfinding;

import java.util.Arrays;

/**
 * A growable list of {@code int} values that allocates nothing per element. The planner reuses one
 * for the neighbours of each node it expands.
 */
final class IntList {

    private int[] elements;
    private int size;

    IntList(int initialCapacity) {
        elements = new int[Math.max(1, initialCapacity)];
    }

    void add(int value) {
        if (size == elements.length) {
            elements = Arrays.copyOf(elements, elements.length << 1);
        }
        elements[size++] = value;
    }

    int get(int index) {
        return elements[index];
    }

    int size() {
        return size;
    }

    void clear() {
        size = 0;
    }
}
