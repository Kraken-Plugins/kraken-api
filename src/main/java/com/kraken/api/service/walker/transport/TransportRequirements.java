package com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether the player can currently use a transport, and says why not when they cannot.
 *
 * <p>A search filters transports by requirement when it captures state, so a routed transport was
 * usable at planning time. State drifts though: a charter fare gets spent, a teleport tab gets
 * used, a stamina dose runs out. Checking again immediately before clicking turns a silent failure,
 * the walker clicking a boat that will not move, into an abort with a readable reason.</p>
 *
 * <p>Checks are made against a {@link PlayerState} snapshot rather than the live client so the logic
 * stays pure and unit testable.</p>
 */
public final class TransportRequirements {

    /**
     * Items a transport takes as a fare. A search refuses to spend more of one than its currency
     * threshold allows, even when the player holds enough.
     */
    private static final Set<Integer> CURRENCIES = Set.of(
            ItemID.COINS, ItemID.VILLAGE_TRADE_STICKS, ItemID.ECTOTOKEN, ItemID.WARGUILD_TOKENS);

    /** The highest level a skill reaches, which a total level requirement of "maximum" counts in. */
    private static final int HIGHEST_SKILL_LEVEL = 99;

    private TransportRequirements() {
    }

    /**
     * An immutable snapshot of the player state a transport's requirements are checked against.
     *
     * <p>Item quantities should combine inventory and equipment, since a requirement such as a ring of
     * dueling is satisfied whether it is worn or carried. Runes inside a carried rune pouch count
     * too.</p>
     */
    @Getter
    @Builder
    public static final class PlayerState {

        /** Boosted skill levels indexed by {@link Skill} ordinal. */
        private final int[] skillLevels;

        /** The player's combat level, or 0 when it was not read. */
        private final int combatLevel;

        /** The sum of the player's skill levels, or 0 when it was not read. */
        private final int totalLevel;

        /** Quests the player has completed. */
        @Singular("completedQuest")
        private final Set<Quest> completedQuests;

        /** Item id to total quantity held across inventory, equipment and a carried rune pouch. */
        @Singular("item")
        private final Map<Integer, Integer> itemQuantities;

        /** Varbit id to current value. */
        @Singular("varbit")
        private final Map<Integer, Integer> varbitValues;

        /** VarPlayer id to current value. */
        @Singular("varPlayer")
        private final Map<Integer, Integer> varPlayerValues;

        /** The player's current wilderness level, or 0 when outside the wilderness. */
        private final int wildernessLevel;

        /** Whether the player is logged in to a members world. */
        private final boolean membersWorld;

        /**
         * When the snapshot was taken, in minutes since 1970, which is what a cooldown is judged
         * against. Zero means the clock is read when the cooldown is checked.
         */
        private final long epochMinutes;
    }

    /**
     * Lists the reasons a transport cannot currently be used, with no limit on what it may spend.
     *
     * <p>A transport that has requirements is not usable when the state to check them against is
     * missing: a failed read must not open a gated transport.</p>
     *
     * @param requirements what the transport needs, may be null
     * @param state the player state to check against, may be null
     * @return the unmet requirements as readable text, empty when the transport is usable
     */
    public static List<String> unmetReasons(DefinitionRequirements requirements, PlayerState state) {
        return unmetReasons(requirements, state, Integer.MAX_VALUE);
    }

    /**
     * Lists the reasons a transport cannot currently be used.
     *
     * @param requirements what the transport needs, may be null
     * @param state the player state to check against, may be null
     * @param currencyThreshold the most of one currency the transport may ask for. A fare above it
     *                          counts as unmet even when the player holds enough
     * @return the unmet requirements as readable text, empty when the transport is usable
     */
    public static List<String> unmetReasons(DefinitionRequirements requirements, PlayerState state,
                                            int currencyThreshold) {
        if (requirements == null || requirements.isEmpty()) {
            return Collections.emptyList();
        }
        if (state == null) {
            return Collections.singletonList("player state could not be read");
        }

        List<String> reasons = new ArrayList<>();

        if (requirements.isMembersOnly() && !state.isMembersWorld()) {
            reasons.add("needs a members world");
        }

        for (Map.Entry<Skill, Integer> level : requirements.getSkillLevels().entrySet()) {
            addSkillReason(level.getKey(), level.getValue(), state, reasons);
        }
        addLevelReasons(requirements, state, reasons);
        addQuestReasons(requirements.getQuests(), state, reasons);
        addItemReasons(requirements.getItems(), state, currencyThreshold, reasons);
        addVarReasons(requirements.getVarbits(), state.getVarbitValues(), state, reasons);
        addVarReasons(requirements.getVarPlayers(), state.getVarPlayerValues(), state, reasons);

        return reasons;
    }

    private static void addVarReasons(List<DefinitionRequirements.RequiredVar> required,
                                      Map<Integer, Integer> values,
                                      PlayerState state,
                                      List<String> reasons) {
        for (DefinitionRequirements.RequiredVar requirement : required) {
            if (!holds(requirement, values, state)) {
                reasons.add("world state not met (" + requirement + ")");
            }
        }
    }

    private static boolean holds(DefinitionRequirements.RequiredVar requirement, Map<Integer, Integer> values,
                                 PlayerState state) {
        int actual = values != null ? values.getOrDefault(requirement.getId(), 0) : 0;
        return state.getEpochMinutes() > 0
                ? requirement.matches(actual, state.getEpochMinutes())
                : requirement.matches(actual);
    }

    private static void addSkillReason(Skill skill, int required, PlayerState state, List<String> reasons) {
        int[] actual = state.getSkillLevels();
        int index = skill.ordinal();
        int have = actual != null && index < actual.length ? actual[index] : 0;
        if (have < required) {
            reasons.add("needs " + required + " " + skill.getName() + ", have " + have);
        }
    }

    private static void addLevelReasons(DefinitionRequirements requirements, PlayerState state, List<String> reasons) {
        if (state.getCombatLevel() < requirements.getCombatLevel()) {
            reasons.add("needs combat level " + requirements.getCombatLevel() + ", have " + state.getCombatLevel());
        }

        int totalLevel = requirements.getTotalLevel() == DefinitionRequirements.MAX_TOTAL_LEVEL
                ? highestTotalLevel() : requirements.getTotalLevel();
        if (state.getTotalLevel() < totalLevel) {
            reasons.add("needs total level " + totalLevel + ", have " + state.getTotalLevel());
        }
    }

    /**
     * Returns the total level of a player with every skill at its highest level.
     *
     * @return the highest total level the game allows
     */
    public static int highestTotalLevel() {
        int skills = 0;
        for (Skill skill : Skill.values()) {
            // RuneLite lists the hiscore's overall rank among the skills.
            if (!"OVERALL".equals(skill.name())) {
                skills++;
            }
        }
        return skills * HIGHEST_SKILL_LEVEL;
    }

    private static void addQuestReasons(Set<Quest> required, PlayerState state, List<String> reasons) {
        if (required == null || required.isEmpty()) {
            return;
        }

        Set<Quest> completed = state.getCompletedQuests();
        for (Quest quest : required) {
            if (completed == null || !completed.contains(quest)) {
                reasons.add("needs quest " + quest.getName());
            }
        }
    }

    /**
     * Checks the item requirements of one transport together, since a staff and an offhand are
     * shared between them.
     *
     * <p>Each requirement is first tried against the items held. One that is still open may be
     * covered by an offhand, which serves a single requirement. Whatever is open after that has to
     * be covered by one staff that serves all of it, because only one staff can be wielded.</p>
     */
    private static void addItemReasons(List<DefinitionRequirements.RequiredItem> required, PlayerState state,
                                       int currencyThreshold, List<String> reasons) {
        if (required.isEmpty()) {
            return;
        }

        Map<Integer, Integer> held = state.getItemQuantities() != null
                ? state.getItemQuantities() : Collections.emptyMap();
        List<DefinitionRequirements.RequiredItem> open = new ArrayList<>();
        boolean offhandUsed = false;

        for (DefinitionRequirements.RequiredItem item : required) {
            DefinitionRequirements.RequiredVar waiver = item.getWaivedByVarbit();
            if (waiver != null && holds(waiver, state.getVarbitValues(), state)) {
                continue;
            }

            Boolean met = metByItems(item, held, currencyThreshold);
            if (met == null) {
                reasons.add("would spend more of a currency than allowed (" + item + ")");
            } else if (!met) {
                if (!offhandUsed && holdsSubstitute(item, held, false)) {
                    offhandUsed = true;
                } else {
                    open.add(item);
                }
            }
        }

        if (!open.isEmpty() && !oneStaffCovers(open, held)) {
            for (DefinitionRequirements.RequiredItem item : open) {
                reasons.add("missing a required item (" + item + ")");
            }
        }
    }

    /**
     * Tries a requirement and its alternatives against the items held.
     *
     * @return true when one of them is met, false when none is, and null when the only ones that
     *         are met ask for more of a currency than the threshold allows
     */
    private static Boolean metByItems(DefinitionRequirements.RequiredItem item, Map<Integer, Integer> held,
                                      int currencyThreshold) {
        boolean overThreshold = false;
        for (DefinitionRequirements.RequiredItem way : ways(item)) {
            int quantity = way.getQuantity();
            if (quantity == 0) {
                if (!holdsAny(held, way.getAnyOf(), 1)) {
                    return true;
                }
                continue;
            }

            for (int id : way.getAnyOf()) {
                if (held.getOrDefault(id, 0) < quantity) {
                    continue;
                }
                if (CURRENCIES.contains(id) && quantity > currencyThreshold) {
                    overThreshold = true;
                    break;
                }
                return true;
            }
        }
        return overThreshold ? null : Boolean.FALSE;
    }

    /** Lists a requirement followed by its alternatives, which are tried in that order. */
    private static List<DefinitionRequirements.RequiredItem> ways(DefinitionRequirements.RequiredItem item) {
        if (item.getAlternatives().isEmpty()) {
            return Collections.singletonList(item);
        }
        List<DefinitionRequirements.RequiredItem> ways = new ArrayList<>(item.getAlternatives().size() + 1);
        ways.add(item);
        ways.addAll(item.getAlternatives());
        return ways;
    }

    /**
     * Reports whether a staff or an offhand that stands in for a requirement is held.
     *
     * @param staff true to look at the staves, false to look at the offhands
     */
    private static boolean holdsSubstitute(DefinitionRequirements.RequiredItem item, Map<Integer, Integer> held,
                                           boolean staff) {
        return !heldSubstitutes(item, held, staff).isEmpty();
    }

    private static Set<Integer> heldSubstitutes(DefinitionRequirements.RequiredItem item, Map<Integer, Integer> held,
                                                boolean staff) {
        Set<Integer> found = new HashSet<>();
        for (DefinitionRequirements.RequiredItem way : ways(item)) {
            // A substitute stands in for items that are needed, never for ones that must be absent.
            if (way.getQuantity() == 0) {
                continue;
            }
            for (int id : staff ? way.getStaves() : way.getOffhands()) {
                if (held.getOrDefault(id, 0) >= 1) {
                    found.add(id);
                }
            }
        }
        return found;
    }

    /** Reports whether a single held staff stands in for every one of the given requirements. */
    private static boolean oneStaffCovers(List<DefinitionRequirements.RequiredItem> open, Map<Integer, Integer> held) {
        Set<Integer> candidates = null;
        for (DefinitionRequirements.RequiredItem item : open) {
            Set<Integer> staves = heldSubstitutes(item, held, true);
            if (candidates == null) {
                candidates = staves;
            } else {
                candidates.retainAll(staves);
            }
            if (candidates.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean holdsAny(Map<Integer, Integer> held, int[] ids, int quantity) {
        if (ids == null) {
            return false;
        }

        for (int id : ids) {
            if (held.getOrDefault(id, 0) >= quantity) {
                return true;
            }
        }

        return false;
    }
}
