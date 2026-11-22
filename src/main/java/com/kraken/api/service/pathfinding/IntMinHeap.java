package com.kraken.api.service.pathfinding;

import java.util.Arrays;

/**
 * A binary heap of node ids that hands back the node with the lowest {@link NodeGraph#compareCost}
 * first. It holds the transports a search has found but not yet taken.
 *
 * <p>The cost of a node never changes once the node exists, so the heap needs no way to move an
 * entry up. A search that reaches a tile twice leaves both entries in the heap and ignores the
 * dearer one when it comes out.</p>
 */
final class IntMinHeap {

    private final NodeGraph graph;
    private int[] heap;
    private int size;

    IntMinHeap(NodeGraph graph, int initialCapacity) {
        this.graph = graph;
        this.heap = new int[Math.max(1, initialCapacity)];
    }

    boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the cheapest node without removing it.
     *
     * @return the node id, or {@link NodeGraph#NO_NODE} when the heap is empty
     */
    int peek() {
        return size == 0 ? NodeGraph.NO_NODE : heap[0];
    }

    void add(int id) {
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length << 1);
        }
        heap[size] = id;
        siftUp(size);
        size++;
    }

    /**
     * Removes and returns the cheapest node.
     *
     * @return the node id, or {@link NodeGraph#NO_NODE} when the heap is empty
     */
    int poll() {
        if (size == 0) {
            return NodeGraph.NO_NODE;
        }
        int top = heap[0];
        size--;
        if (size > 0) {
            heap[0] = heap[size];
            siftDown(0);
        }
        return top;
    }

    void clear() {
        size = 0;
    }

    private void siftUp(int index) {
        int id = heap[index];
        int key = graph.compareCost(id);
        while (index > 0) {
            int parent = (index - 1) >> 1;
            if (key >= graph.compareCost(heap[parent])) {
                break;
            }
            heap[index] = heap[parent];
            index = parent;
        }
        heap[index] = id;
    }

    private void siftDown(int index) {
        int id = heap[index];
        int key = graph.compareCost(id);
        int half = size >> 1;
        while (index < half) {
            int child = (index << 1) + 1;
            int childKey = graph.compareCost(heap[child]);
            int right = child + 1;
            if (right < size) {
                int rightKey = graph.compareCost(heap[right]);
                if (rightKey < childKey) {
                    child = right;
                    childKey = rightKey;
                }
            }
            if (key <= childKey) {
                break;
            }
            heap[index] = heap[child];
            index = child;
        }
        heap[index] = id;
    }
}
