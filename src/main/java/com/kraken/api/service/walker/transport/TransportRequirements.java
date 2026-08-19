package com.kraken.api.service.walker.transport;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import shortestpath.transport.TransportType;
import shortestpath.transport.Transport;
import shortestpath.transport.parser.VarRequirement;
import shortestpath.transport.requirement.ItemRequirement;
import shortestpath.transport.requirement.TransportItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether the player can currently use a transport, and says why not when they cannot.
 *
 * <p>The planner already filters transports by requirement when it refreshes, so a routed transport
 * was usable at planning time. State drifts though: a charter fare gets spent, a teleport tab gets
 * used, a stamina dose runs out. Re-checking immediately before clicking turns a silent failure — the
 * walker clicking a boat that will not move — into an abort with a readable reason.</p>
 *
 * <p>Checks are made against a {@link PlayerState} snapshot rather than the live client so the logic
 * stays pure and unit testable.</p>
 */
public final class TransportRequirements {

    /**
     * Staves that let a player use a fairy ring.
     *
     * <p>This requirement is not in the dataset — the pathfinder hardcodes it in its own config — so
     * it has to be applied here too, or a fairy ring route passes the check and then fails at the
     * ring.</p>
     */
    private static final int[] FAIRY_RING_STAVES = {
            ItemID.DRAMEN_STAFF,
            ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF
    };

    /** Completing the Lumbridge elite diary removes the need for a staff. */
    public static final int FAIRY_RING_DIARY_VARBIT = VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE;

    private TransportRequirements() {
    }

    /**
     * An immutable snapshot of the player state a transport's requirements are checked against.
     *
     * <p>Item quantities should combine inventory and equipment, since a requirement such as a ring of
     * dueling is satisfied whether it is worn or carried.</p>
     */
    @Getter
    @Builder
    public static final class PlayerState {

        /** Boosted skill levels indexed by {@link Skill} ordinal. */
        private final int[] skillLevels;

        /** Quests the player has completed. */
        @Singular("completedQuest")
        private final Set<Quest> completedQuests;

        /** Item id to total quantity held across inventory and equipment. */
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
    }

    /**
     * Reports whether every requirement of a transport is currently satisfied.
     *
     * @param transport the transport about to be executed
     * @param state the player state to check against
     * @return true when the transport can be used
     */
    public static boolean met(Transport transport, PlayerState state) {
        return unmetReasons(transport, state).isEmpty();
    }

    /**
     * Lists the reasons a transport cannot currently be used.
     *
     * <p>Skill levels beyond {@link Skill} are ignored: the dataset's level array carries three slots
     * past the known skills whose meaning is not part of the published API, so they are not enforced.
     * The effect is that such a transport is attempted rather than refused.</p>
     *
     * @param transport the transport about to be executed, may be null
     * @param state the player state to check against, may be null
     * @return the unmet requirements as readable text, empty when the transport is usable
     */
    public static List<String> unmetReasons(Transport transport, PlayerState state) {
        if (transport == null || state == null) {
            return Collections.emptyList();
        }

        List<String> reasons = new ArrayList<>();

        addSkillReasons(transport, state, reasons);
        addQuestReasons(transport, state, reasons);
        addItemReasons(transport, state, reasons);
        addVarReasons(transport, state, reasons);

        if (!transport.isUsableAtWildernessLevel(state.getWildernessLevel())) {
            reasons.add("not usable at wilderness level " + state.getWildernessLevel());
        }

        addFairyRingReasons(transport, state, reasons);

        return reasons;
    }

    /**
     * Applies the staff requirement that fairy rings carry but the dataset does not record.
     */
    private static void addFairyRingReasons(Transport transport, PlayerState state, List<String> reasons) {
        if (transport.getType() != TransportType.FAIRY_RING) {
            return;
        }

        Map<Integer, Integer> varbits = state.getVarbitValues();
        if (varbits != null && varbits.getOrDefault(FAIRY_RING_DIARY_VARBIT, 0) > 0) {
            return;
        }

        Map<Integer, Integer> held = state.getItemQuantities();
        if (held != null && holdsAny(held, FAIRY_RING_STAVES, 1)) {
            return;
        }

        reasons.add("needs a dramen or lunar staff, or the Lumbridge elite diary");
    }

    private static void addSkillReasons(Transport transport, PlayerState state, List<String> reasons) {
        int[] required = transport.getSkillLevels();
        if (required == null) {
            return;
        }

        int[] actual = state.getSkillLevels();
        Skill[] skills = Skill.values();
        int checkable = Math.min(required.length, skills.length);

        for (int i = 0; i < checkable; i++) {
            if (required[i] <= 0) {
                continue;
            }

            int have = actual != null && i < actual.length ? actual[i] : 0;
            if (have < required[i]) {
                reasons.add("needs " + required[i] + " " + skills[i].getName() + ", have " + have);
            }
        }
    }

    private static void addQuestReasons(Transport transport, PlayerState state, List<String> reasons) {
        Set<Quest> required = transport.getQuests();
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

    private static void addItemReasons(Transport transport, PlayerState state, List<String> reasons) {
        TransportItems items = transport.getItemRequirements();
        if (items == null) {
            return;
        }

        List<ItemRequirement> requirements = items.getRequirements();
        if (requirements == null) {
            return;
        }

        for (ItemRequirement requirement : requirements) {
            if (!satisfies(requirement, state)) {
                reasons.add("missing a required item (any of " + describe(requirement) + ")");
            }
        }
    }

    /**
     * A requirement is satisfied by holding enough of any listed item, or by a staff or offhand that
     * stands in for it — an elemental staff supplies its rune without the rune being carried.
     */
    private static boolean satisfies(ItemRequirement requirement, PlayerState state) {
        Map<Integer, Integer> held = state.getItemQuantities();
        if (held == null) {
            return false;
        }

        int quantity = Math.max(requirement.getQuantity(), 1);

        return holdsAny(held, requirement.getItemIds(), quantity)
                || holdsAny(held, requirement.getStaffIds(), 1)
                || holdsAny(held, requirement.getOffhandIds(), 1);
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

    private static void addVarReasons(Transport transport, PlayerState state, List<String> reasons) {
        Set<VarRequirement> required = transport.getVarRequirements();
        if (required == null || required.isEmpty()) {
            return;
        }

        Map<Integer, Integer> varbits = state.getVarbitValues() != null
                ? state.getVarbitValues() : Collections.emptyMap();
        Map<Integer, Integer> varPlayers = state.getVarPlayerValues() != null
                ? state.getVarPlayerValues() : Collections.emptyMap();

        for (VarRequirement requirement : required) {
            Map<Integer, Integer> values = requirement.isVarbit() ? varbits : varPlayers;
            if (!requirement.check(values)) {
                reasons.add("world state not met (" + requirement + ")");
            }
        }
    }

    private static String describe(ItemRequirement requirement) {
        int[] ids = requirement.getItemIds();
        if (ids == null || ids.length == 0) {
            return "no items listed";
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(ids[i]);
        }

        return builder.toString();
    }
}
