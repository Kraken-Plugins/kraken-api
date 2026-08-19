package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import shortestpath.transport.Transport;
import shortestpath.transport.TransportType;
import shortestpath.transport.parser.VarRequirement;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Reads the parts of the player's state a particular transport cares about.
 *
 * <p>Only what the transport actually requires is read. Quest state in particular is not cheap to
 * query, and a door needs none of it.</p>
 */
@Slf4j
@Singleton
public class PlayerStateReader {

    /** The first wilderness level starts just north of this y coordinate. */
    private static final int WILDERNESS_SOUTH_EDGE = 3520;

    /** Wilderness levels are eight tiles tall. */
    private static final int TILES_PER_WILDERNESS_LEVEL = 8;

    /** The underground wilderness mirrors the surface from this y coordinate. */
    private static final int WILDERNESS_UNDERGROUND_SOUTH_EDGE = 9920;

    @Inject
    private Context ctx;

    /**
     * Builds a snapshot covering the requirements of one transport.
     *
     * @param transport the transport about to be executed
     * @return the state to check its requirements against
     */
    public TransportRequirements.PlayerState read(Transport transport) {
        if (transport == null) {
            return TransportRequirements.PlayerState.builder()
                    .skillLevels(readSkillLevels())
                    .wildernessLevel(readWildernessLevel())
                    .build();
        }

        Map<Integer, Integer> varbits = new HashMap<>();
        Map<Integer, Integer> varPlayers = new HashMap<>();
        readVars(transport, varbits, varPlayers);

        // Fairy rings need a staff unless the Lumbridge elite diary is done, and neither the staff
        // nor the diary appears in the transport's own requirements, so both are read explicitly.
        if (transport.getType() == TransportType.FAIRY_RING) {
            varbits.put(TransportRequirements.FAIRY_RING_DIARY_VARBIT,
                    ctx.getVarbitValue(TransportRequirements.FAIRY_RING_DIARY_VARBIT));
        }

        return TransportRequirements.PlayerState.builder()
                .skillLevels(readSkillLevels())
                .wildernessLevel(readWildernessLevel())
                .completedQuests(readCompletedQuests(transport))
                .itemQuantities(readItems(transport))
                .varbitValues(varbits)
                .varPlayerValues(varPlayers)
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

    /**
     * Works out how deep into the wilderness the player is.
     *
     * <p>Returns zero outside it, which is what the dataset's wilderness ceilings compare against.</p>
     */
    private int readWildernessLevel() {
        WorldPoint location = ctx.players().local().location();
        if (location == null) {
            return 0;
        }

        int y = location.getY();
        if (y > WILDERNESS_UNDERGROUND_SOUTH_EDGE) {
            return ((y - WILDERNESS_UNDERGROUND_SOUTH_EDGE) / TILES_PER_WILDERNESS_LEVEL) + 1;
        }

        if (y > WILDERNESS_SOUTH_EDGE) {
            return ((y - WILDERNESS_SOUTH_EDGE) / TILES_PER_WILDERNESS_LEVEL) + 1;
        }

        return 0;
    }

    private Set<Quest> readCompletedQuests(Transport transport) {
        Set<Quest> required = transport.getQuests();
        if (required == null || required.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Quest> completed = new HashSet<>();
        for (Quest quest : required) {
            QuestState state = ctx.runOnClientThread(() -> quest.getState(ctx.getClient()), null);
            if (state == QuestState.FINISHED) {
                completed.add(quest);
            }
        }

        return completed;
    }

    /**
     * Totals the transport's candidate items across inventory and equipment.
     *
     * <p>Only ids the transport mentions are counted, so this stays a small map however full the
     * player's bags are.</p>
     */
    private Map<Integer, Integer> readItems(Transport transport) {
        if (transport.getItemRequirements() == null && transport.getType() != TransportType.FAIRY_RING) {
            return Collections.emptyMap();
        }

        Map<Integer, Integer> totals = new HashMap<>();

        for (InventoryEntity entity : ctx.inventory().list()) {
            add(totals, entity.raw());
        }

        for (EquipmentEntity entity : ctx.equipment().list()) {
            add(totals, entity.raw());
        }

        return totals;
    }

    private void add(Map<Integer, Integer> totals, ContainerItem item) {
        if (item == null) {
            return;
        }
        totals.merge(item.getId(), Math.max(item.getQuantity(), 1), Integer::sum);
    }

    private void readVars(Transport transport, Map<Integer, Integer> varbits, Map<Integer, Integer> varPlayers) {
        Set<VarRequirement> required = transport.getVarRequirements();
        if (required == null || required.isEmpty()) {
            return;
        }

        for (VarRequirement requirement : required) {
            if (requirement.isVarbit()) {
                varbits.put(requirement.getId(), ctx.getVarbitValue(requirement.getId()));
            } else {
                varPlayers.put(requirement.getId(), ctx.getVarpValue(requirement.getId()));
            }
        }
    }
}
