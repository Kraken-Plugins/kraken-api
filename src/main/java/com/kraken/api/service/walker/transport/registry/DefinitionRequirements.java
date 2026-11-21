package com.kraken.api.service.walker.transport.registry;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the player needs before a registered transport can be used.
 *
 * <p>This is plain data. {@code TransportRequirements.unmetReasons} checks it against a player
 * state snapshot, once when a search captures state and again just before the walker operates the
 * transport.</p>
 */
@Getter
@Builder
public final class DefinitionRequirements {

    /** Requirements that are always met. */
    public static final DefinitionRequirements NONE = DefinitionRequirements.builder().build();

    /** Minimum boosted level per skill. */
    @Singular("skill")
    private final Map<Skill, Integer> skillLevels;

    /** Quests that must be finished. */
    @Singular("quest")
    private final Set<Quest> quests;

    /** Items that must be carried or worn. Every entry must be satisfied. */
    @Singular("item")
    private final List<RequiredItem> items;

    /** Varbit values that must hold. */
    @Singular("varbit")
    private final List<RequiredVar> varbits;

    /** VarPlayer values that must hold. */
    @Singular("varPlayer")
    private final List<RequiredVar> varPlayers;

    /** Whether the transport only exists on a members world. */
    private final boolean membersOnly;

    /**
     * Reports whether there is nothing to check.
     *
     * @return true when no requirement of any kind is set
     */
    public boolean isEmpty() {
        return !membersOnly
                && skillLevels.isEmpty()
                && quests.isEmpty()
                && items.isEmpty()
                && varbits.isEmpty()
                && varPlayers.isEmpty();
    }

    /**
     * An amount of one item, or of any one of several interchangeable items.
     */
    @Value
    public static class RequiredItem {

        /** Item ids that each satisfy the requirement on their own. */
        int[] anyOf;

        /** How many of one of those items must be held across inventory and equipment. */
        int quantity;

        /**
         * Requires one of a single item.
         *
         * @param itemId the item id
         * @return the requirement
         */
        public static RequiredItem of(int itemId) {
            return of(itemId, 1);
        }

        /**
         * Requires an amount of a single item, such as a coin fare.
         *
         * @param itemId the item id
         * @param quantity how many must be held
         * @return the requirement
         */
        public static RequiredItem of(int itemId, int quantity) {
            return new RequiredItem(new int[]{itemId}, quantity);
        }

        /**
         * Requires one of any of several interchangeable items.
         *
         * @param itemIds the item ids that each satisfy the requirement
         * @return the requirement
         */
        public static RequiredItem anyOf(int... itemIds) {
            return new RequiredItem(itemIds != null ? itemIds.clone() : new int[0], 1);
        }

        @Override
        public String toString() {
            return quantity + " of " + Arrays.toString(anyOf);
        }
    }

    /**
     * A test of one varbit or varplayer value.
     */
    @Value
    public static class RequiredVar {

        /** The varbit or varplayer id. */
        int id;

        /** How the live value is compared with {@link #value}. */
        Comparison comparison;

        /** The value to compare against, or the bit index for {@link Comparison#BIT_SET}. */
        int value;

        /**
         * Applies the test to a live value.
         *
         * @param actual the current value of the varbit or varplayer
         * @return true when the requirement holds
         */
        public boolean matches(int actual) {
            return comparison != null && comparison.test(actual, value);
        }

        @Override
        public String toString() {
            return id + " " + comparison + " " + value;
        }
    }

    /**
     * How a live varbit or varplayer value is compared with a required one.
     */
    public enum Comparison {
        EQUAL,
        NOT_EQUAL,
        AT_LEAST,
        AT_MOST,

        /** The bit at the required index is set in the live value. */
        BIT_SET;

        /**
         * Compares a live value with a required one.
         *
         * @param actual the live value
         * @param required the required value, or a bit index for {@link #BIT_SET}
         * @return true when the comparison holds
         */
        public boolean test(int actual, int required) {
            switch (this) {
                case EQUAL:
                    return actual == required;
                case NOT_EQUAL:
                    return actual != required;
                case AT_LEAST:
                    return actual >= required;
                case AT_MOST:
                    return actual <= required;
                case BIT_SET:
                    return required >= 0 && required < Integer.SIZE && (actual & (1 << required)) != 0;
                default:
                    return false;
            }
        }
    }
}
