package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.core.Locatable;
import com.kraken.api.service.tile.GameArea;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.widgets.Widget;

public class NpcEntity extends AbstractEntity<NPC> implements Locatable {
    public NpcEntity(Context ctx, NPC raw) {
        super(ctx, raw);
    }

    @Override
    public WorldPoint getWorldLocation() {
        NPC n = raw();
        return n != null ? n.getWorldLocation() : null;
    }

    @Override
    public String getName() {
        NPC n = raw();
        return n != null ? ctx.runOnClientThread(n::getName) : null;
    }

    @Override
    public int getId() {
        NPC n = raw();
        return n != null ? ctx.runOnClientThread(n::getId) : -1;
    }

    /**
     * Gets the health percentage of the NPC. The client only reports health for NPCs whose health bar
     * has been shown recently; both ratio and scale are -1 before then.
     * @return Health percentage (0-100), or -1 if unknown
     */
    public double getHealthPercentage() {
        NPC raw = raw();
        if (raw == null) return -1;
        return ctx.runOnClientThread(() -> {
            int ratio = raw.getHealthRatio();
            int scale = raw.getHealthScale();
            if (ratio < 0 || scale <= 0) return -1.0;
            return ratio / (double) scale * 100.0;
        }, -1.0);
    }

    /**
     * Retrieves the protection prayer head icon shown above the NPC, if any.
     * <p>
     * NPCs can display several overhead sprites at once, each drawn from a sprite archive. Only sprites
     * from the prayer head-icon archive map onto {@link HeadIcon} (by sprite index); sprites from any
     * other archive, such as Nex's deflect icons drawn from custom archives, are ignored.
     * </p>
     *
     * @return The {@code HeadIcon} for the NPC, or {@code null} if it shows no supported prayer icon.
     */
    public HeadIcon getHeadIcon() {
        NPC raw = raw();
        if (raw == null) return null;
        return ctx.runOnClientThread(() -> {
            int[] archiveIds = raw.getOverheadArchiveIds();
            short[] spriteIds = raw.getOverheadSpriteIds();
            if (archiveIds == null || spriteIds == null) return null;

            HeadIcon[] icons = HeadIcon.values();
            for (int i = 0; i < Math.min(archiveIds.length, spriteIds.length); i++) {
                if (archiveIds[i] != SpriteID.HEADICONS_PRAYER) continue;
                int index = spriteIds[i];
                if (index >= 0 && index < icons.length) return icons[index];
            }
            return null;
        }, null);
    }

    /**
     * Calculates the distance between the NPC and the local player within the game world.
     * <p>
     * The method retrieves the NPC's local location and the local player's location
     * from the game client, then computes the distance between the two positions.
     * If the distance cannot be calculated (e.g., due to a timeout on the client thread),
     * {@code Integer.MAX_VALUE} is returned as a fallback.
     * </p>
     *
     * <ul>
     *   <li>The computation is performed on the game's client thread to ensure thread safety unless the result is unavailable.</li>
     * </ul>
     *
     * @return The distance between the NPC's location and the local player's location in the game world,
     *         or {@code Integer.MAX_VALUE} if the distance cannot be determined.
     */
    public int getDistanceFromPlayer() {
        NPC raw = raw();
        return ctx.runOnClientThreadOptional(() -> raw.getLocalLocation().distanceTo(
                ctx.getClient().getLocalPlayer().getLocalLocation())).orElse(Integer.MAX_VALUE);
    }

    /**
     * Checks if the NPC's current location is within the game area.
     * @param area The {@link GameArea} to check.
     * @return True if the NPC location is within the game area and false otherwise.
     */
    public boolean isInArea(GameArea area) {
        if (area == null) return false;
        return area.contains(raw().getWorldLocation());
    }


    @Override
    public boolean interact(String action) {
        NPC raw = raw();
        if (raw == null) return false;
        return ctx.getInteractionManager().interact(raw, action);
    }

    /**
     * Attacks an NPC. This is a shallow wrapper around the interact() method.
     * @return True if the attack interaction was successful and false otherwise
     */
    public boolean attack() {
        return interact("attack");
    }

    /**
     * Uses a specified widget on the NPC (i.e. Casting Crumble Undead Spell on the Vorkaths Spawn)
     * @param widget The widget to use on the NPC
     * @return True if the interaction was successful and false otherwise
     */
    public boolean useWidget(Widget widget) {
        NPC raw = raw();
        if (raw == null) return false;
        return ctx.getInteractionManager().interact(widget, raw);
    }
}