package com.kraken.api.service.util.dps.search;

import com.kraken.api.service.util.dps.model.GearCategory;
import com.kraken.api.service.util.dps.model.PlayerSkills;
import com.kraken.api.service.util.dps.model.PrayerBoost;
import com.kraken.api.service.util.dps.model.SpellData;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Options controlling the smart gear search.
 */
@Data
@Builder(toBuilder = true)
public class GearSearchConfig {

    /** Base skill levels used for every evaluated loadout. */
    @Builder.Default
    private PlayerSkills skills = PlayerSkills.maxed();

    /** Skill boosts applied on top of base levels. */
    @Builder.Default
    private PlayerSkills boosts = PlayerSkills.none();

    /**
     * When true, the search assumes the best offensive prayer for each combat style
     * (Piety / Rigour / Augury). When false, the prayers list below is used as-is.
     */
    @Builder.Default
    private boolean useBestPrayers = false;

    /** Explicit prayers to use when {@code useBestPrayers} is false. */
    @Builder.Default
    private List<PrayerBoost> prayers = new ArrayList<>();

    /** Whether the player is on a slayer task against the target. */
    @Builder.Default
    private boolean onSlayerTask = false;

    /** Whether the player is in the wilderness (revenant weapon bonuses). */
    @Builder.Default
    private boolean inWilderness = false;

    /** Spell to use for autocast staves. When null the best available elemental spell is chosen. */
    private SpellData spell;

    /** Dart id assumed to be loaded in a blowpipe, when one is a candidate weapon. */
    private Integer blowpipeDartId;

    /** The combat style classes to search. Defaults to all three. */
    @Builder.Default
    private List<GearCategory> styles = new ArrayList<>(Arrays.asList(GearCategory.MELEE, GearCategory.RANGED, GearCategory.MAGIC));

    /** Maximum number of candidate weapons fully evaluated per combat style. */
    @Builder.Default
    private int maxWeaponsPerStyle = 8;

    /**
     * Creates a config with sensible defaults.
     * @return GearSearchConfig defaults
     */
    public static GearSearchConfig defaults() {
        return GearSearchConfig.builder().build();
    }
}
