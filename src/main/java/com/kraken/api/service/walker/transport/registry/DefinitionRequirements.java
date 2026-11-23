package com.kraken.api.service.walker.transport.registry;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import net.runelite.api.Quest;
import net.runelite.api.Skill;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the player needs before a transport can be used.
 *
 * <p>This is plain data. {@code TransportRequirements.unmetReasons} checks it against a player
 * state snapshot, once when a search captures state and again just before the walker operates the
 * transport.</p>
 */
@Getter
@Builder(toBuilder = true)
public final class DefinitionRequirements {

    /** Requirements that are always met. */
    public static final DefinitionRequirements NONE = DefinitionRequirements.builder().build();

    /** The value of {@link #totalLevel} that asks for every skill at its highest level. */
    public static final int MAX_TOTAL_LEVEL = -1;

    /** Minimum boosted level per skill. */
    @Singular("skill")
    private final Map<Skill, Integer> skillLevels;

    /** Minimum combat level, or zero for none. */
    private final int combatLevel;

    /**
     * Minimum total level, zero for none, or {@link #MAX_TOTAL_LEVEL} for every skill at its
     * highest level, however many skills the game has.
     */
    private final int totalLevel;

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
                && combatLevel == 0
                && totalLevel == 0
                && skillLevels.isEmpty()
                && quests.isEmpty()
                && items.isEmpty()
                && varbits.isEmpty()
                && varPlayers.isEmpty();
    }

    /**
     * Joins these requirements with others into one set that asks for both. Where both name a
     * level, the higher one is kept.
     *
     * @param other the requirements to add, may be null
     * @return requirements that hold when both of the originals do
     */
    public DefinitionRequirements and(DefinitionRequirements other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }

        var merged = toBuilder()
                .combatLevel(Math.max(combatLevel, other.combatLevel))
                .totalLevel(totalLevel == MAX_TOTAL_LEVEL || other.totalLevel == MAX_TOTAL_LEVEL
                        ? MAX_TOTAL_LEVEL : Math.max(totalLevel, other.totalLevel))
                .membersOnly(membersOnly || other.membersOnly);

        for (Map.Entry<Skill, Integer> level : other.skillLevels.entrySet()) {
            merged.skill(level.getKey(), Math.max(level.getValue(), skillLevels.getOrDefault(level.getKey(), 0)));
        }
        other.quests.forEach(merged::quest);
        other.items.stream().filter(item -> !items.contains(item)).forEach(merged::item);
        other.varbits.stream().filter(varbit -> !varbits.contains(varbit)).forEach(merged::varbit);
        other.varPlayers.stream().filter(varPlayer -> !varPlayers.contains(varPlayer)).forEach(merged::varPlayer);
        return merged.build();
    }

    /**
     * An amount of one item, or of any one of several interchangeable items.
     *
     * <p>The simple form is a list of item ids and a quantity. A requirement can also name other
     * ways to meet it: further alternatives that each bring their own quantity, a staff or an
     * offhand that stands in for a rune, and a varbit that waives it altogether.</p>
     */
    @Getter
    public static final class RequiredItem {

        /** Item ids that each satisfy the requirement on their own. */
        private final int[] anyOf;

        /**
         * How many of one of those items must be held across inventory and equipment. Zero turns
         * the requirement around: none of the items may be held.
         */
        private final int quantity;

        /**
         * Staves that each stand in for the items however many are needed, the way an elemental
         * staff supplies its rune. One staff has to cover every requirement of a transport that is
         * met by a staff, since only one can be wielded.
         */
        private final int[] staves;

        /**
         * Offhand items that each stand in for the items, the way a tome supplies its rune. An
         * offhand covers one requirement of a transport.
         */
        private final int[] offhands;

        /**
         * Other ways to meet the requirement, each with its own items and quantity: one Shantay
         * pass, or five coins to buy one. An alternative carries no alternatives of its own.
         */
        private final List<RequiredItem> alternatives;

        /** A varbit test that meets the requirement without any item, or null when there is none. */
        private final RequiredVar waivedByVarbit;

        /**
         * Requires an amount of one of several interchangeable items.
         *
         * @param anyOf the item ids that each satisfy the requirement
         * @param quantity how many must be held, or zero when none may be held
         */
        public RequiredItem(int[] anyOf, int quantity) {
            this(anyOf, quantity, null, null, null, null);
        }

        /**
         * Requires an amount of one of several interchangeable items, with other ways to meet the
         * requirement.
         *
         * @param anyOf the item ids that each satisfy the requirement
         * @param quantity how many must be held, or zero when none may be held
         * @param staves staves that each stand in for the items, may be null
         * @param offhands offhand items that each stand in for the items, may be null
         * @param alternatives other ways to meet the requirement, may be null
         * @param waivedByVarbit a varbit test that meets the requirement without an item, may be null
         */
        @Builder(toBuilder = true)
        public RequiredItem(int[] anyOf, int quantity, int[] staves, int[] offhands,
                            List<RequiredItem> alternatives, RequiredVar waivedByVarbit) {
            this.anyOf = anyOf != null ? anyOf.clone() : new int[0];
            this.quantity = Math.max(quantity, 0);
            this.staves = staves != null ? staves.clone() : new int[0];
            this.offhands = offhands != null ? offhands.clone() : new int[0];
            this.alternatives = alternatives != null ? List.copyOf(alternatives) : Collections.emptyList();
            this.waivedByVarbit = waivedByVarbit;
        }

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
            return new RequiredItem(itemIds, 1);
        }

        /**
         * Reports whether the requirement is only a list of items and a quantity, with no other
         * way to meet it.
         *
         * @return true when it names no staff, offhand, alternative or waiver
         */
        public boolean isPlain() {
            return staves.length == 0 && offhands.length == 0 && alternatives.isEmpty() && waivedByVarbit == null;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RequiredItem)) {
                return false;
            }
            RequiredItem that = (RequiredItem) other;
            return quantity == that.quantity
                    && Arrays.equals(anyOf, that.anyOf)
                    && Arrays.equals(staves, that.staves)
                    && Arrays.equals(offhands, that.offhands)
                    && alternatives.equals(that.alternatives)
                    && java.util.Objects.equals(waivedByVarbit, that.waivedByVarbit);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(anyOf);
            result = 31 * result + quantity;
            result = 31 * result + Arrays.hashCode(staves);
            result = 31 * result + Arrays.hashCode(offhands);
            result = 31 * result + alternatives.hashCode();
            return 31 * result + java.util.Objects.hashCode(waivedByVarbit);
        }

        @Override
        public String toString() {
            StringBuilder text = new StringBuilder(quantity + " of " + Arrays.toString(anyOf));
            for (RequiredItem alternative : alternatives) {
                text.append(" or ").append(alternative);
            }
            return text.toString();
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

        /**
         * The value to compare against. For {@link Comparison#BIT_SET} it is a bit index, for
         * {@link Comparison#ANY_BIT_OF} a mask, and for {@link Comparison#COOLDOWN_MINUTES} a
         * number of minutes.
         */
        int value;

        /**
         * Applies the test to a live value, reading the clock for a cooldown.
         *
         * @param actual the current value of the varbit or varplayer
         * @return true when the requirement holds
         */
        public boolean matches(int actual) {
            return matches(actual, System.currentTimeMillis() / 60_000L);
        }

        /**
         * Applies the test to a live value at a given time.
         *
         * @param actual the current value of the varbit or varplayer
         * @param epochMinutes the time to judge a cooldown at, in minutes since 1970
         * @return true when the requirement holds
         */
        public boolean matches(int actual, long epochMinutes) {
            return comparison != null && comparison.test(actual, value, epochMinutes);
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
        /** The live value equals the required value. */
        EQUAL,

        /** The live value differs from the required value. */
        NOT_EQUAL,

        /** The live value is greater than or equal to the required value. */
        AT_LEAST,

        /** The live value is less than or equal to the required value. */
        AT_MOST,

        /** The bit at the required index is set in the live value. */
        BIT_SET,

        /** At least one bit of the required mask is set in the live value. */
        ANY_BIT_OF,

        /**
         * The live value is the minute something was last used, counted from 1970, and more than
         * the required number of minutes have passed since. A home teleport is timed this way.
         */
        COOLDOWN_MINUTES;

        /**
         * Compares a live value with a required one, reading the clock for a cooldown.
         *
         * @param actual the live value
         * @param required the required value, a bit index for {@link #BIT_SET}, a mask for
         *                 {@link #ANY_BIT_OF} or a number of minutes for {@link #COOLDOWN_MINUTES}
         * @return true when the comparison holds
         */
        public boolean test(int actual, int required) {
            return test(actual, required, System.currentTimeMillis() / 60_000L);
        }

        /**
         * Compares a live value with a required one at a given time.
         *
         * @param actual the live value
         * @param required the required value, a bit index for {@link #BIT_SET}, a mask for
         *                 {@link #ANY_BIT_OF} or a number of minutes for {@link #COOLDOWN_MINUTES}
         * @param epochMinutes the time to judge a cooldown at, in minutes since 1970
         * @return true when the comparison holds
         */
        public boolean test(int actual, int required, long epochMinutes) {
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
                case ANY_BIT_OF:
                    return (actual & required) != 0;
                case COOLDOWN_MINUTES:
                    return epochMinutes - actual > required;
                default:
                    return false;
            }
        }
    }
}
