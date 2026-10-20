package com.kraken.api.service.prayer;

import com.kraken.api.Context;
import com.kraken.api.core.Services;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.annotations.Component;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;

import java.util.List;

/**
 * The prayers that can be turned on and off from the prayer interface. Each constant holds the prayer's display
 * name, its widget index, the level needed to use it, its drain rate, its activation varbit and its quick prayer
 * index.
 */
@Getter
@RequiredArgsConstructor
public enum InteractablePrayer {
    /** The Thick Skin prayer, which boosts Defence. */
    THICK_SKIN("Thick Skin", 35454985, 1, 5.0, VarbitID.PRAYER_THICKSKIN, 0),
    /** The Burst of Strength prayer, which boosts Strength. */
    BURST_STRENGTH("Burst of Strength", 35454986, 4, 5.0, VarbitID.PRAYER_BURSTOFSTRENGTH, 1),
    /** The Clarity of Thought prayer, which boosts Attack. */
    CLARITY_THOUGHT("Clarity of Thought", 35454987, 7, 5.0, VarbitID.PRAYER_CLARITYOFTHOUGHT, 2),
    /** The Sharp Eye prayer, which boosts Ranged. */
    SHARP_EYE("Sharp Eye", 35455003, 8, 5.0, VarbitID.PRAYER_SHARPEYE, 18),
    /** The Mystic Will prayer, which boosts Magic. */
    MYSTIC_WILL("Mystic Will", 35455006, 9, 5.0, VarbitID.PRAYER_MYSTICWILL, 19),
    /** The Rock Skin prayer, which boosts Defence. */
    ROCK_SKIN("Rock Skin", 35454988, 10, 10.0, VarbitID.PRAYER_ROCKSKIN, 3),
    /** The Superhuman Strength prayer, which boosts Strength. */
    SUPERHUMAN_STRENGTH("Superhuman Strength", 35454989, 13, 10.0, VarbitID.PRAYER_SUPERHUMANSTRENGTH, 4),
    /** The Improved Reflexes prayer, which boosts Attack. */
    IMPROVED_REFLEXES("Improved Reflexes", 35454990, 16, 10.0, VarbitID.PRAYER_IMPROVEDREFLEXES, 5),
    /** The Rapid Restore prayer, which restores drained skill levels faster. */
    RAPID_RESTORE("Rapid Restore", 35454991, 19, 60.0 / 36.0, VarbitID.PRAYER_RAPIDRESTORE, 6),
    /** The Rapid Heal prayer, which heals hitpoints faster. */
    RAPID_HEAL("Rapid heal", 35454992, 22, 60.0 / 18, VarbitID.PRAYER_RAPIDHEAL, 7),
    /** The Protect Item prayer, which keeps one extra item on death. */
    PROTECT_ITEM("Protect Item", 35454993, 25, 60.0 / 18,  VarbitID.PRAYER_PROTECTITEM, 8),
    /** The Hawk Eye prayer, which boosts Ranged. */
    HAWK_EYE("Hawk eye", 35455004, 26, 10.0, VarbitID.PRAYER_HAWKEYE, 20),
    /** The Mystic Lore prayer, which boosts Magic. */
    MYSTIC_LORE("Mystic Lore", 35455007, 27, 10.0, VarbitID.PRAYER_MYSTICLORE, 21),
    /** The Steel Skin prayer, which boosts Defence. */
    STEEL_SKIN("Steel Skin", 35454994, 28, 20.0, VarbitID.PRAYER_STEELSKIN, 9),
    /** The Ultimate Strength prayer, which boosts Strength. */
    ULTIMATE_STRENGTH("Ultimate Strength", 35454995, 31, 20.0, VarbitID.PRAYER_ULTIMATESTRENGTH, 10),
    /** The Incredible Reflexes prayer, which boosts Attack. */
    INCREDIBLE_REFLEXES("Incredible Reflexes", 35454996, 34, 20.0, VarbitID.PRAYER_INCREDIBLEREFLEXES, 11),
    /** The Protect from Magic prayer, which protects the player from magic attacks. */
    PROTECT_MAGIC("Protect From Magic", 35454997, 37, 20.0, VarbitID.PRAYER_PROTECTFROMMAGIC, 12),
    /** The Protect from Missiles prayer, which protects the player from ranged attacks. */
    PROTECT_RANGE("Protect From Missiles", 35454998, 40, 20.0, VarbitID.PRAYER_PROTECTFROMMISSILES, 13),
    /** The Protect from Melee prayer, which protects the player from melee attacks. */
    PROTECT_MELEE("Protect From Melee", 35454999, 43, 20.0, VarbitID.PRAYER_PROTECTFROMMELEE, 14),
    /** The Eagle Eye prayer, which boosts Ranged. */
    EAGLE_EYE("Eagle Eye", 35455005, 44, 20.0, VarbitID.PRAYER_EAGLEEYE, 22),
    /** The Mystic Might prayer, which boosts Magic. */
    MYSTIC_MIGHT("Mystic Might", 35455008, 45, 20.0, VarbitID.PRAYER_MYSTICMIGHT, 23),
    /** The Retribution prayer, which damages nearby enemies when the player dies. */
    RETRIBUTION("Retribution", 35455000, 46, 5.0, VarbitID.PRAYER_RETRIBUTION, 15),
    /** The Redemption prayer, which heals the player when their hitpoints run low. */
    REDEMPTION("Redemption", 35455001, 49, 10.0, VarbitID.PRAYER_REDEMPTION, 16),
    /** The Smite prayer, which drains the prayer points of an enemy that damages the player. */
    SMITE("Smite", 35455002, 52, 30.0, VarbitID.PRAYER_SMITE, 17),
    /** The Preserve prayer, which slows the decay of boosted skill levels. */
    PRESERVE("Preserve", 35455013, 55, 60.0 / 18.0, VarbitID.PRAYER_PRESERVE, 28),
    /** The Chivalry prayer, which boosts Attack, Strength and Defence. */
    CHIVALRY("Chivalry", 35455010,60, 40.0, VarbitID.PRAYER_CHIVALRY, 25),
    /** The Piety prayer, which boosts Attack, Strength and Defence. */
    PIETY("Piety", 35455011, 70, 40.0, VarbitID.PRAYER_PIETY, 26),
    /** The Rigour prayer, which boosts Ranged and Defence. */
    RIGOUR("Rigour", 35455009, 74, 40.0, VarbitID.PRAYER_RIGOUR, 24),
    /** The Augury prayer, which boosts Magic and Defence. */
    AUGURY("Augury", 35455012, 77, 40.0, VarbitID.PRAYER_AUGURY, 27);

    private final String name;

    // The Widget ID for the Prayer
    @Component
    private final int index;
    private final int level;
    private final double drainRate;
    @Varbit
    private final int varbit;
    private final int quickPrayerIndex;

    /** The prayers that count as overhead: the three protection prayers and Retribution, Redemption and Smite. */
    public static final InteractablePrayer[] OVERHEAD_PRAYERS = {
            PROTECT_MAGIC,
            PROTECT_MELEE,
            PROTECT_RANGE,
            RETRIBUTION,
            REDEMPTION,
            SMITE
    };

    /**
     * Checks whether the current {@literal @InteractablePrayer} is active.
     *
     * <p>A prayer is considered active if its corresponding {@code varbit} value
     * equals {@code 1} in the game's internal context.
     *
     * @return {@code true} if the prayer is active; {@code false} otherwise.
     */
    public boolean isActive() {
        Context ctx = Services.get(Context.class);
        return ctx.getVarbitValue(getVarbit()) == 1;
    }


    /**
     * Returns the Kraken prayer (from this class) given a RuneLite API Prayer enum or null if the prayer doesn't exist.
     * @param p RuneLite prayer
     * @return Kraken Prayer (Prayers)
     */
    public static InteractablePrayer of(Prayer p) {
        for (InteractablePrayer krakenPrayer : InteractablePrayer.values()) {
            if (p.getVarbit() == krakenPrayer.getVarbit()) {
                return krakenPrayer;
            }
        }
        return null;
    }

    /**
     * Maps an {@literal @InteractablePrayer} to its corresponding {@literal @Prayer} from the RuneLite API.
     * If no matching prayer is found, returns {@code null}.
     *
     * <p>This method iterates through all values of the {@literal @Prayer} enum and matches them
     * based on their {@code varbit} value.
     *
     * @param p The {@literal @InteractablePrayer} instance whose corresponding {@literal @Prayer}
     *          needs to be determined. Must not be null.
     * @return The {@literal @Prayer} instance matching the specified {@literal @InteractablePrayer}.
     *         Returns {@code null} if no match is found.
     */
    public static Prayer runelitePrayerFor(InteractablePrayer p) {
        for(Prayer runelitePrayer : Prayer.values()) {
            if(runelitePrayer.getVarbit() == p.getVarbit()) {
                return runelitePrayer;
            }
        }
        return null;
    }

    /**
     * Determines whether the specified {@literal @InteractablePrayer} is classified
     * as an overhead prayer.
     *
     * <p>Overhead prayers are typically characterized by specific protections or effects
     * activated during player interaction.
     *
     * @param p The {@literal @InteractablePrayer} to be checked. Must not be {@code null}.
     * @return {@code true} if the given prayer is an overhead prayer; {@code false} otherwise.
     */
    public static boolean isOverhead(InteractablePrayer p) {
        return List.of(OVERHEAD_PRAYERS).contains(p);
    }
}
