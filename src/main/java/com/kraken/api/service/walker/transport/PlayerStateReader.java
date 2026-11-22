package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.service.pathfinding.PathfinderLiveConfig;
import com.kraken.api.service.walker.transport.registry.DefinitionRequirements;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reads the parts of the player's state that transport requirements care about.
 *
 * <p>Only what the requirements at hand name is read. Quest state in particular is not cheap to
 * query, and a door needs none of it.</p>
 */
@Slf4j
@Singleton
public class PlayerStateReader {

    private static final int[] RUNE_POUCHES = {
            ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
            ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER
    };

    private static final int[] RUNE_POUCH_RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
            VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
    };

    private static final int[] RUNE_POUCH_AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
            VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
    };

    @Inject
    private Context ctx;

    /**
     * Builds one snapshot covering the requirements of several transports.
     *
     * <p>Skills, items and the world type are each read once however many transports ask for them,
     * and only the quests and vars they name are queried.</p>
     *
     * @param requirements the requirements about to be checked
     * @return the state to check them against
     */
    public TransportRequirements.PlayerState read(Collection<DefinitionRequirements> requirements) {
        return read(Needs.of(requirements));
    }

    /**
     * Builds one snapshot covering a set of needs that was worked out beforehand. A caller that
     * checks the same requirements again and again, as a search checks the dataset's, works the
     * needs out once and reads with them each time.
     *
     * @param needs what to read
     * @return the state to check the requirements against
     */
    public TransportRequirements.PlayerState read(Needs needs) {
        Set<Quest> completed = new HashSet<>();
        for (Quest quest : needs.quests) {
            addIfFinished(completed, quest);
        }

        Map<Integer, Integer> varbits = new HashMap<>();
        for (int id : needs.varbits) {
            varbits.put(id, ctx.getVarbitValue(id));
        }
        Map<Integer, Integer> varPlayers = new HashMap<>();
        for (int id : needs.varPlayers) {
            varPlayers.put(id, ctx.getVarpValue(id));
        }

        return TransportRequirements.PlayerState.builder()
                .skillLevels(needs.skills ? readSkillLevels() : new int[0])
                .combatLevel(needs.levels ? readCombatLevel() : 0)
                .totalLevel(needs.levels ? readTotalLevel() : 0)
                .completedQuests(completed)
                .itemQuantities(needs.items ? readHeldItems() : Collections.emptyMap())
                .varbitValues(varbits)
                .varPlayerValues(varPlayers)
                .membersWorld(needs.worldType && PathfinderLiveConfig.isMembersWorld(ctx))
                .epochMinutes(System.currentTimeMillis() / 60_000L)
                .build();
    }

    private int[] readSkillLevels() {
        Skill[] skills = Skill.values();
        return ctx.runOnClientThread(() -> {
            int[] levels = new int[skills.length];
            for (int i = 0; i < skills.length; i++) {
                levels[i] = ctx.getClient().getBoostedSkillLevel(skills[i]);
            }
            return levels;
        }, new int[skills.length]);
    }

    private int readCombatLevel() {
        return ctx.runOnClientThread(() -> {
            Player player = ctx.getClient().getLocalPlayer();
            return player != null ? player.getCombatLevel() : 0;
        }, 0);
    }

    private int readTotalLevel() {
        return ctx.runOnClientThread(() -> ctx.getClient().getTotalLevel(), 0);
    }

    private void addIfFinished(Set<Quest> completed, Quest quest) {
        QuestState state = ctx.runOnClientThread(() -> quest.getState(ctx.getClient()), null);
        if (state == QuestState.FINISHED) {
            completed.add(quest);
        }
    }

    /**
     * Totals every item the player carries or wears, by item id, and adds the runes inside a rune
     * pouch when one is carried. A spell can be cast from the pouch, so its runes count as held.
     */
    private Map<Integer, Integer> readHeldItems() {
        Map<Integer, Integer> totals = new HashMap<>();

        for (InventoryEntity entity : ctx.inventory().list()) {
            add(totals, entity.raw());
        }

        for (EquipmentEntity entity : ctx.equipment().list()) {
            add(totals, entity.raw());
        }

        for (int pouch : RUNE_POUCHES) {
            if (totals.containsKey(pouch)) {
                readRunePouch().forEach((rune, amount) -> totals.merge(rune, amount, Integer::sum));
                break;
            }
        }

        return totals;
    }

    /**
     * Reads the runes inside the rune pouch. Each slot has a varbit for the kind of rune, which an
     * enum of the game turns into an item id, and a varbit for how many there are.
     */
    private Map<Integer, Integer> readRunePouch() {
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            Map<Integer, Integer> runes = new HashMap<>();
            EnumComposition runeItems = client.getEnum(EnumID.RUNEPOUCH_RUNE);
            if (runeItems == null) {
                return runes;
            }
            for (int slot = 0; slot < RUNE_POUCH_RUNE_VARBITS.length; slot++) {
                int kind = client.getVarbitValue(RUNE_POUCH_RUNE_VARBITS[slot]);
                int amount = client.getVarbitValue(RUNE_POUCH_AMOUNT_VARBITS[slot]);
                int rune = kind > 0 ? runeItems.getIntValue(kind) : 0;
                if (rune > 0 && amount > 0) {
                    runes.merge(rune, amount, Integer::sum);
                }
            }
            return runes;
        }, Collections.emptyMap());
    }

    private void add(Map<Integer, Integer> totals, ContainerItem item) {
        if (item == null) {
            return;
        }
        totals.merge(item.getId(), Math.max(item.getQuantity(), 1), Integer::sum);
    }

    /**
     * The parts of the player's state a set of requirements asks about.
     */
    public static final class Needs {

        private final Set<Quest> quests = new HashSet<>();
        private final Set<Integer> varbits = new HashSet<>();
        private final Set<Integer> varPlayers = new HashSet<>();
        private boolean skills;
        private boolean levels;
        private boolean items;
        private boolean worldType;

        private Needs() {
        }

        /**
         * Works out what a set of requirements asks about.
         *
         * @param requirements the requirements, which may hold nulls
         * @return what has to be read to check them
         */
        public static Needs of(Collection<DefinitionRequirements> requirements) {
            Needs needs = new Needs();
            for (DefinitionRequirements required : requirements) {
                needs.add(required);
            }
            return needs;
        }

        /**
         * Joins these needs with others.
         *
         * @param other the needs to add
         * @return needs that cover both
         */
        public Needs and(Needs other) {
            Needs joined = new Needs();
            for (Needs source : new Needs[]{this, other}) {
                joined.quests.addAll(source.quests);
                joined.varbits.addAll(source.varbits);
                joined.varPlayers.addAll(source.varPlayers);
                joined.skills |= source.skills;
                joined.levels |= source.levels;
                joined.items |= source.items;
                joined.worldType |= source.worldType;
            }
            return joined;
        }

        private void add(DefinitionRequirements required) {
            if (required == null || required.isEmpty()) {
                return;
            }

            skills |= !required.getSkillLevels().isEmpty();
            levels |= required.getCombatLevel() != 0 || required.getTotalLevel() != 0;
            items |= !required.getItems().isEmpty();
            worldType |= required.isMembersOnly();
            quests.addAll(required.getQuests());

            for (DefinitionRequirements.RequiredVar varbit : required.getVarbits()) {
                varbits.add(varbit.getId());
            }
            for (DefinitionRequirements.RequiredVar varPlayer : required.getVarPlayers()) {
                varPlayers.add(varPlayer.getId());
            }
            for (DefinitionRequirements.RequiredItem item : required.getItems()) {
                if (item.getWaivedByVarbit() != null) {
                    varbits.add(item.getWaivedByVarbit().getId());
                }
            }
        }
    }
}
