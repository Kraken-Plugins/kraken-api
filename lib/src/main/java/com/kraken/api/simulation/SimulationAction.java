package com.kraken.api.simulation;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Player action used by the simulation tick.
 */
@Getter
public final class SimulationAction {
    public static final SimulationAction WAIT = new SimulationAction(0, 0, false);
    public static final SimulationAction NORTH = new SimulationAction(0, 1, false);
    public static final SimulationAction SOUTH = new SimulationAction(0, -1, false);
    public static final SimulationAction EAST = new SimulationAction(1, 0, false);
    public static final SimulationAction WEST = new SimulationAction(-1, 0, false);
    public static final SimulationAction NORTH_EAST = new SimulationAction(1, 1, false);
    public static final SimulationAction NORTH_WEST = new SimulationAction(-1, 1, false);
    public static final SimulationAction SOUTH_EAST = new SimulationAction(1, -1, false);
    public static final SimulationAction SOUTH_WEST = new SimulationAction(-1, -1, false);

    private static final List<SimulationAction> STANDARD_WALK_ACTIONS = Collections.unmodifiableList(Arrays.asList(
            WAIT,
            NORTH,
            SOUTH,
            EAST,
            WEST,
            NORTH_EAST,
            NORTH_WEST,
            SOUTH_EAST,
            SOUTH_WEST
    ));

    private final int dx;
    private final int dy;
    private final boolean run;

    private SimulationAction(int dx, int dy, boolean run) {
        if (dx < -1 || dx > 1 || dy < -1 || dy > 1) {
            throw new IllegalArgumentException("dx and dy must be in range [-1, 1]");
        }
        this.dx = dx;
        this.dy = dy;
        this.run = run;
    }

    public static SimulationAction move(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return WAIT;
        }
        if (dx == NORTH.dx && dy == NORTH.dy) {
            return NORTH;
        }
        if (dx == SOUTH.dx && dy == SOUTH.dy) {
            return SOUTH;
        }
        if (dx == EAST.dx && dy == EAST.dy) {
            return EAST;
        }
        if (dx == WEST.dx && dy == WEST.dy) {
            return WEST;
        }
        if (dx == NORTH_EAST.dx && dy == NORTH_EAST.dy) {
            return NORTH_EAST;
        }
        if (dx == NORTH_WEST.dx && dy == NORTH_WEST.dy) {
            return NORTH_WEST;
        }
        if (dx == SOUTH_EAST.dx && dy == SOUTH_EAST.dy) {
            return SOUTH_EAST;
        }
        if (dx == SOUTH_WEST.dx && dy == SOUTH_WEST.dy) {
            return SOUTH_WEST;
        }
        return new SimulationAction(dx, dy, false);
    }

    public static SimulationAction run(int dx, int dy) {
        if (dx == 0 && dy == 0) {
            return WAIT;
        }
        return new SimulationAction(dx, dy, true);
    }

    public static List<SimulationAction> standardWalkActions() {
        return STANDARD_WALK_ACTIONS;
    }

    public WorldPoint destinationFrom(WorldPoint origin) {
        if (origin == null) {
            throw new IllegalArgumentException("origin cannot be null");
        }
        int steps = run ? 2 : 1;
        return new WorldPoint(origin.getX() + (dx * steps), origin.getY() + (dy * steps), origin.getPlane());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SimulationAction)) {
            return false;
        }
        SimulationAction that = (SimulationAction) other;
        return dx == that.dx && dy == that.dy && run == that.run;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dx, dy, run);
    }

    @Override
    public String toString() {
        return "SimulationAction{"
                + "dx=" + dx
                + ", dy=" + dy
                + ", run=" + run
                + '}';
    }
}
