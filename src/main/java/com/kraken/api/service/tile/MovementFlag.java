package com.kraken.api.service.tile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.CollisionDataFlag;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a copy of the net.runelite.client.plugins.devtools.MovementFlag class which has private
 * access within the RuneLite client.
 */
@AllArgsConstructor
public enum MovementFlag {
    /** Blocks movement toward the north-west. */
    BLOCK_MOVEMENT_NORTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST),
    /** Blocks movement toward the north. */
    BLOCK_MOVEMENT_NORTH(CollisionDataFlag.BLOCK_MOVEMENT_NORTH),
    /** Blocks movement toward the north-east. */
    BLOCK_MOVEMENT_NORTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST),
    /** Blocks movement toward the east. */
    BLOCK_MOVEMENT_EAST(CollisionDataFlag.BLOCK_MOVEMENT_EAST),
    /** Blocks movement toward the south-east. */
    BLOCK_MOVEMENT_SOUTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST),
    /** Blocks movement toward the south. */
    BLOCK_MOVEMENT_SOUTH(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH),
    /** Blocks movement toward the south-west. */
    BLOCK_MOVEMENT_SOUTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST),
    /** Blocks movement toward the west. */
    BLOCK_MOVEMENT_WEST(CollisionDataFlag.BLOCK_MOVEMENT_WEST),

    /** Blocks movement because of a game object on the tile. */
    BLOCK_MOVEMENT_OBJECT(CollisionDataFlag.BLOCK_MOVEMENT_OBJECT),
    /** Blocks movement because of a floor decoration on the tile. */
    BLOCK_MOVEMENT_FLOOR_DECORATION(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION),
    /** Blocks movement because of the floor on the tile. */
    BLOCK_MOVEMENT_FLOOR(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR),
    /** Blocks movement across the whole tile. */
    BLOCK_MOVEMENT_FULL(CollisionDataFlag.BLOCK_MOVEMENT_FULL);

    @Getter
    private final int flag;

    /**
     * @param collisionData The tile collision flags.
     * @return The set of {@link net.runelite.client.plugins.devtools.MovementFlag}s that have been set.
     */
    public static Set<MovementFlag> getSetFlags(int collisionData) {
        return Arrays.stream(values())
                .filter(movementFlag -> (movementFlag.flag & collisionData) != 0)
                .collect(Collectors.toSet());
    }

    /**
     * Checks whether a flag is in an array of flags.
     *
     * @param flags The flags to search. Must not be {@code null}.
     * @param flagToCheck The flag to look for.
     * @return {@code true} if {@code flagToCheck} is one of {@code flags}, {@code false} otherwise.
     */
    public static boolean hasFlag(MovementFlag[] flags, MovementFlag flagToCheck) {
        return Arrays.stream(flags).anyMatch(flag -> flag == flagToCheck);
    }
}