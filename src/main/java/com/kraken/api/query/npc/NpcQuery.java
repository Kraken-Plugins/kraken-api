package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractSpatialQuery;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NpcQuery extends AbstractSpatialQuery<NpcEntity, NpcQuery, NPC> {

    public NpcQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<NpcEntity>> source() {
        // The terminal operation in AbstractQuery consumes this stream inside a single client-thread
        // block, so these field reads are already on the client thread; a per-NPC runOnClientThread
        // here only added a marshaling round-trip (and a Boolean-unboxing NPE on timeout) per NPC.
        return () -> ctx.getClient().getTopLevelWorldView().npcs().stream()
                .filter(Objects::nonNull)
                .filter(n -> n.getName() != null && n.getId() != -1)
                .map(rawNpc -> new NpcEntity(ctx, rawNpc));
    }

    /**
     * Filters for NPCs whose ids are present in the provided list of ids.
     * @param ids List of ids to check for
     * @return NpcQuery
     */
    public NpcQuery withIds(List<Integer> ids) {
        return filter(npc -> ids.contains(npc.raw().getId()));
    }

    /**
     * Filters for NPCs whose animation ids match the specified id.
     * @param animationId The id of the animation to match
     * @return NpcQuery
     */
    public NpcQuery withAnimation(int animationId) {
        return filter(npc -> npc.raw().getAnimation() == animationId);
    }

    /**
     * Returns Attackable NPC within the scene.
     * NPC's are considered attackable when:
     * - They are not dead
     * - Their menu options contain an "Attack" option
     * <p>
     * Combat level is not taken into consideration since there are many NPC's without a combat level that are attackable.
     * i.e. Yama's void flares
     * @return NpcQuery
     */
    public NpcQuery attackable() {
        return filter(npc -> {
            NPCComposition composition = npc.raw().getComposition();
            if(composition == null) return false;
            if(composition.getActions() == null || composition.getActions().length == 0) return false;

            List<String> actions = Arrays.stream(composition.getActions())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            return actions.contains("attack") && !npc.raw().isDead();
        });
    }

    /**
     * Filters for NPCs that are not interacting with anyone (null interaction).
     * This covers "not interacting with me" AND "not interacting with others".
     * @return NpcQuery
     */
    public NpcQuery idle() {
        return filter(npc -> npc.raw().getInteracting() == null);
    }

    /**
     * Filters the query for NPCs that have a specific menu option available.
     * <p>
     * This method checks the list of actions associated with each NPC's composition.
     * If the specified {@code option} matches any of the available actions (case-insensitive),
     * the NPC will be included in the resulting query.
     * </p>
     *
     * @param action The menu option to check for, e.g., {@literal @}Attack, {@literal @}Talk-to, {@literal @}Use, etc...
     *               The input is case-insensitive.
     * @return A filtered {@code NpcQuery} containing only the NPCs that match the specified menu option.
     */
    public NpcQuery withAction(String action) {
        return filter(npc -> {
            NPCComposition composition = npc.raw().getComposition();
            if(composition == null) return false;

            List<String> actions = Arrays.stream(composition.getActions())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            return actions.contains(action.toLowerCase());
        });
    }

    /**
     * Filters the query to include only NPCs that are currently interacting with the local player.
     * <p>
     * An NPC is considered to be interacting with the local player if the NPC's {@code interacting}
     * target is non-{@code null} and matches the client’s local player.
     * </p>
     *
     * <ul>
     *   <li>This method allows narrowing down the query to find NPCs that are actively engaged with
     *       the local player, whether by combat, dialogue, or other forms of interaction.</li>
     * </ul>
     *
     * @return A filtered {@code NpcQuery} containing only the NPCs that are interacting with the local player.
     */
    public NpcQuery interactingWithPlayer() {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target == ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters the query to include only NPCs that are currently interacting with any entity other than the local player.
     * <p>
     * An NPC is considered to be interacting if its {@code interacting} target is non-{@code null} and does not match
     * the local player. This includes NPCs that are engaged with other players, NPCs, or other entities in any form
     * of interaction (e.g., combat, dialogue, etc.).
     * </p>
     *
     * <ul>
     *   <li>This method helps identify NPCs that are actively engaged in an interaction within the game world,
     *   excluding those interacting directly with the local player.</li>
     * </ul>
     *
     * @return A filtered {@code NpcQuery} containing only the NPCs that are interacting with entities other than
     * the local player.
     */
    public NpcQuery interacting() {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target != ctx.getClient().getLocalPlayer();
        });
    }

    /**
     * Filters the query to include only NPCs that are currently interacting with the specified {@code actor}.
     * <p>
     * An NPC is considered to be interacting with the given actor if the NPC's {@code interacting} target
     * is non-{@code null} and matches the provided {@code actor}.
     * </p>
     * <ul>
     *   <li>This method is useful for identifying NPCs actively engaging with a specific actor, such as
     *   another player, NPC, or inanimate entity.</li>
     * </ul>
     *
     * @param actor The {@link Actor} that the NPCs being searched for should be interacting with.
     *              Passing {@code null} as the parameter is not allowed.
     * @return A filtered {@code NpcQuery} containing only the NPCs that are interacting with the specified actor.
     */
    public NpcQuery interactingWith(Actor actor) {
        return filter(npc -> {
            Actor target = npc.raw().getInteracting();
            return target != null && target == actor;
        });
    }

    /**
     * Filters the query to include only NPCs that are currently alive.
     * <p>
     * An NPC is considered to be alive if its internal state indicates it is not dead.
     * This method applies a filter to exclude NPCs marked as dead from the result set.
     * </p>
     *
     * <ul>
     *   <li>NPCs included in the resulting query are capable of interaction or action
     *       within the game environment.</li>
     *   <li>This filter helps narrow the query to focus only on viable, active NPCs.</li>
     * </ul>
     *
     * @return A filtered {@code NpcQuery} containing only the NPCs that are alive.
     */
    public NpcQuery alive() {
        return filter(npc -> !npc.raw().isDead());
    }
}
