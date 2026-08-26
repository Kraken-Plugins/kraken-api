package com.kraken.api.service.walker.transport;

import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.query.equipment.EquipmentEntity;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
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
                    .wildernessLevel(WildernessLevels.of(ctx.players().local().location()))
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

        // The Al Kharid gate is a free Open in the dataset, so coins, varp 273 and Prince Ali
        // Rescue are not listed on the transport. They still decide whether the live gate opens.
        if (AlKharidGate.matches(transport)) {
            varPlayers.put(AlKharidGate.GATE_VARP, ctx.getVarpValue(AlKharidGate.GATE_VARP));
        }

        return TransportRequirements.PlayerState.builder()
                .skillLevels(readSkillLevels())
                .wildernessLevel(WildernessLevels.of(ctx.players().local().location()))
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

    private Set<Quest> readCompletedQuests(Transport transport) {
        Set<Quest> required = transport.getQuests();
        boolean alKharid = AlKharidGate.matches(transport);
        if ((required == null || required.isEmpty()) && !alKharid) {
            return Collections.emptySet();
        }

        Set<Quest> completed = new HashSet<>();
        if (required != null) {
            for (Quest quest : required) {
                addIfFinished(completed, quest);
            }
        }
        if (alKharid) {
            addIfFinished(completed, Quest.PRINCE_ALI_RESCUE);
        }

        return completed;
    }

    private void addIfFinished(Set<Quest> completed, Quest quest) {
        QuestState state = ctx.runOnClientThread(() -> quest.getState(ctx.getClient()), null);
        if (state == QuestState.FINISHED) {
            completed.add(quest);
        }
    }

    /**
     * Totals the transport's candidate items across inventory and equipment.
     *
     * <p>Only ids the transport mentions are counted, so this stays a small map however full the
     * player's bags are.</p>
     */
    private Map<Integer, Integer> readItems(Transport transport) {
        if (transport.getItemRequirements() == null
                && transport.getType() != TransportType.FAIRY_RING
                && !AlKharidGate.matches(transport)) {
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
