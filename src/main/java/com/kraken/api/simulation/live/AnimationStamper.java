package com.kraken.api.simulation.live;

import com.kraken.api.simulation.data.MechanicsData;
import com.kraken.api.simulation.data.NpcDef;
import com.kraken.api.simulation.geom.Coords;
import com.kraken.api.simulation.geom.Footprints;
import com.kraken.api.simulation.hits.Style;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generic attack animation stamping: animation changes are buffered as raw facts (tiles, distance, prayer at
 * launch) on the client thread and folded into a {@link CooldownTracker} at the start of the next tick, after the
 * tick's death events are complete, so a death animation never stamps an attack. Defend animations (npcs.json
 * {@code defendAnims}) never stamp; an animation the type does not classify counts as an attack (logged once). The
 * pack decides through a {@link Hook} which of its own animations stamp the cadence and which ignore prayer, and
 * hears every classified launch (its own trackers, the trace feed).
 */
@Slf4j
public final class AnimationStamper {
    /** A content pack's part of animation stamping. */
    public interface Hook {
        /**
         * Whether a classified attack animation stamps the cooldown tracker (false for a charge animation that only
         * announces a later launch).
         * @param def the NPC's type
         * @param slot the NPC's slot
         * @param animId the animation
         * @param tick the client tick
         * @return true to stamp the cadence
         */
        default boolean stampsCooldown(NpcDef def, int slot, int animId, int tick) {
            return true;
        }

        /**
         * Whether a launch ignores prayer (a tile targeted special): stamped as blocked so in flight re seeding stays
         * silent; the pack tracks its impact itself.
         * @param def the NPC's type
         * @param animId the animation
         * @return true for a prayer ignoring launch
         */
        default boolean ignoresPrayer(NpcDef def, int animId) {
            return false;
        }

        /**
         * A classified attack animation was folded.
         * @param npcIndex the game's NPC index
         * @param slot the NPC's slot
         * @param def the NPC's type
         * @param animId the animation
         * @param playerTile the player's packed tile when it played
         * @param tick the client tick
         */
        default void onLaunch(int npcIndex, int slot, NpcDef def, int animId, int playerTile, int tick) {
        }

        /**
         * The player's attack animation was folded.
         * @param animId the animation
         * @param tick the client tick
         */
        default void onPlayerAttack(int animId, int tick) {
        }
    }

    private static final class Fact {
        boolean player;
        int npcIndex;
        int npcId;
        int animId;
        int chebyshevDistance;
        int playerTile;
        boolean blocked;
    }

    private final MechanicsData data;
    private final List<Fact> facts = new ArrayList<>();
    private final Set<Integer> loggedUnclassified = new HashSet<>();

    /**
     * A stamper over a pack's tables.
     * @param data the tables (type lookup by live id, attack and defend animations, hit delays, the animation lag)
     */
    public AnimationStamper(MechanicsData data) {
        this.data = data;
    }

    /**
     * Buffers an {@code AnimationChanged} actor's animation: the local player's attack animation (while it is
     * interacting), or a known NPC's animation with its server tile, the player's, their distance and whether the
     * overhead active now blocks the type's style. Call on the client thread.
     * @param client the client
     * @param actor the actor whose animation changed
     * @param anchor the anchored window
     */
    public void observe(Client client, Actor actor, RegionAnchor anchor) {
        Player local = client.getLocalPlayer();
        if (actor == local) {
            if (local != null && local.getAnimation() != -1 && local.getInteracting() != null) {
                playerAnimation(local.getAnimation());
            }
            return;
        }
        if (!(actor instanceof NPC)) {
            return;
        }
        NPC npc = (NPC) actor;
        int typeIdx = data.typeIndexForNpcId(npc.getId());
        if (typeIdx < 0 || npc.getAnimation() == -1 || npc.isDead() || local == null || !anchor.anchored()) {
            return;
        }
        int npcTile = anchor.local(LiveTiles.serverTile(client, npc), 0);
        int playerTile = anchor.local(LiveTiles.serverTile(client, local), 0);
        if (npcTile < 0 || playerTile < 0) {
            return;
        }
        NpcDef def = data.npcs[typeIdx];
        boolean blocked = def.styleCode >= 0 && overheadBlocks(client, def.styleCode);
        npcAnimation(npc.getIndex(), npc.getId(), npc.getAnimation(), npcTile, playerTile, blocked);
    }

    /**
     * Buffers an NPC animation fact.
     * @param npcIndex the game's NPC index
     * @param npcId the live NPC id
     * @param animId the animation
     * @param npcTile the NPC's packed SW tile
     * @param playerTile the player's packed tile
     * @param blocked whether the overhead active now blocks the type's style
     */
    public void npcAnimation(int npcIndex, int npcId, int animId, int npcTile, int playerTile, boolean blocked) {
        int typeIdx = data.typeIndexForNpcId(npcId);
        int size = typeIdx >= 0 ? data.npcs[typeIdx].size : 1;
        Fact fact = new Fact();
        fact.npcIndex = npcIndex;
        fact.npcId = npcId;
        fact.animId = animId;
        fact.playerTile = playerTile;
        fact.chebyshevDistance = Footprints.toTile(Coords.x(npcTile), Coords.y(npcTile), size,
                Coords.x(playerTile), Coords.y(playerTile));
        fact.blocked = blocked;
        facts.add(fact);
    }

    /**
     * Buffers the local player's attack animation.
     * @param animId the animation
     */
    public void playerAnimation(int animId) {
        Fact fact = new Fact();
        fact.player = true;
        fact.animId = animId;
        facts.add(fact);
    }

    /**
     * Folds the buffered facts into the cooldown tracker and the pack's hook, then clears them. Facts of NPCs that
     * died this tick, of NPCs without a slot, and defend animations are dropped.
     * @param tick the client tick
     * @param diedThisTick game NPC indexes that died this tick
     * @param indexer the slot mapping
     * @param cooldowns the tracker to stamp
     * @param hook the pack's part
     */
    public void fold(int tick, Set<Integer> diedThisTick, NpcIndexer indexer, CooldownTracker cooldowns, Hook hook) {
        int lag = data.flags.attackAnimationLagTicks;
        for (Fact fact : facts) {
            if (fact.player) {
                cooldowns.stampPlayerAttack(tick);
                hook.onPlayerAttack(fact.animId, tick);
                continue;
            }
            if (diedThisTick.contains(fact.npcIndex)) {
                continue;
            }
            int slot = indexer.slotOf(fact.npcIndex);
            int typeIdx = data.typeIndexForNpcId(fact.npcId);
            if (slot < 0 || typeIdx < 0) {
                continue;
            }
            NpcDef def = data.npcs[typeIdx];
            if (def.isDefendAnim(fact.animId)) {
                continue;
            }
            if (!def.isAttackAnim(fact.animId) && loggedUnclassified.add(fact.animId)) {
                log.debug("unclassified animation {} on {} treated as an attack", fact.animId, def.key);
            }
            if (hook.stampsCooldown(def, slot, fact.animId, tick)) {
                cooldowns.stamp(slot, tick, lag, def.hitDelayTicks(fact.chebyshevDistance), def.styleCode,
                        fact.blocked || hook.ignoresPrayer(def, fact.animId));
            }
            hook.onLaunch(fact.npcIndex, slot, def, fact.animId, fact.playerTile, tick);
        }
        facts.clear();
    }

    /** Drops the buffered facts (a tick whose fold is skipped). */
    public void clear() {
        facts.clear();
    }

    /**
     * Whether the overhead active now blocks a style.
     * @param client the client
     * @param style the attack's style
     * @return true when the matching protection prayer is on
     */
    public static boolean overheadBlocks(Client client, int style) {
        Prayer prayer = style == Style.RANGED ? Prayer.PROTECT_FROM_MISSILES
                : style == Style.MAGIC ? Prayer.PROTECT_FROM_MAGIC : Prayer.PROTECT_FROM_MELEE;
        return client.getVarbitValue(prayer.getVarbit()) == 1;
    }
}
