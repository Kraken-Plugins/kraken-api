package com.kraken.api.service.pathfinding;

/**
 * A growable first in, first out queue of node ids, kept in a ring buffer so the search frontier
 * holds no boxed values. Used by one search on one thread.
 */
final class IntDeque {

    private int[] elements;
    private int head;
    private int tail;
    private int size;

    IntDeque(int initialCapacity) {
        elements = new int[Math.max(1, initialCapacity)];
    }

    boolean isEmpty() {
        return size == 0;
    }

    void addLast(int value) {
        if (size == elements.length) {
            grow();
        }
        elements[tail] = value;
        tail = tail + 1 == elements.length ? 0 : tail + 1;
        size++;
    }

    /**
     * Returns the first element without removing it.
     *
     * @return the first element, or {@link NodeGraph#NO_NODE} when the queue is empty
     */
    int peekFirst() {
        return size == 0 ? NodeGraph.NO_NODE : elements[head];
    }

    /**
     * Removes and returns the first element.
     *
     * @return the first element, or {@link NodeGraph#NO_NODE} when the queue is empty
     */
    int pollFirst() {
        if (size == 0) {
            return NodeGraph.NO_NODE;
        }
        int value = elements[head];
        head = head + 1 == elements.length ? 0 : head + 1;
        size--;
        return value;
    }

    void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    private void grow() {
        int oldCapacity = elements.length;
        int[] grown = new int[oldCapacity << 1];
        for (int i = 0; i < size; i++) {
            grown[i] = elements[(head + i) % oldCapacity];
        }
        elements = grown;
        head = 0;
        tail = size;
    }
}
