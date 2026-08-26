package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.MagicService;
import com.kraken.api.service.magic.spellbook.Ancient;
import com.kraken.api.service.magic.spellbook.Arceuus;
import com.kraken.api.service.magic.spellbook.Lunar;
import com.kraken.api.service.magic.spellbook.Standard;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.HomeTeleportPlan;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportHandler;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Casts a teleport spell.
 *
 * <p>Teleport spells carry no object info at all — the dataset identifies them only by display info
 * such as {@code "Varrock Teleport"} — so the spell is matched by name against the spellbook enums the
 * magic service already models.</p>
 */
@Slf4j
public class SpellTeleportHandler implements TransportHandler {

    /** How long to wait for the teleport to land. */
    private static final long TELEPORT_TIMEOUT_MS = 10_000;

    @Override
    public boolean execute(TransportContext context) {
        CastableSpell spell = findSpell(context.getDisplayInfo());
        if (spell == null) {
            log.debug("No spell matches '{}'", context.getDisplayInfo());
            return false;
        }

        MagicService magic = context.getCtx().getService(MagicService.class);
        if (!magic.canCast(spell)) {
            log.debug("Cannot cast {}", spell.getName());
            return false;
        }

        WorldPoint before = context.playerLocation();
        if (!magic.cast(spell)) {
            return false;
        }

        long timeout = HomeTeleportPlan.isHomeTeleport(spell)
                ? HomeTeleportPlan.CAST_TIMEOUT_MS
                : TELEPORT_TIMEOUT_MS;
        SleepService.sleepUntil(() -> {
            WorldPoint now = context.playerLocation();
            return now != null && before != null && now.distanceTo(before) > 10;
        }, timeout);

        return true;
    }

    /**
     * Spells the dataset names differently from the spellbook enums.
     *
     * <p>Only genuine naming differences belong here. Teleports the spellbooks do not model at all —
     * the Ancient city teleports among them — are deliberately absent, so a route through one fails
     * with a reason rather than casting something else.</p>
     */
    private static final Map<String, CastableSpell> ALIASES = buildAliases();

    private static Map<String, CastableSpell> buildAliases() {
        Map<String, CastableSpell> aliases = new HashMap<>();
        aliases.put(normalise("Kourend Castle Teleport"), Standard.TELEPORT_TO_KOUREND);
        aliases.put(normalise("Lumbridge Home Teleport"), Standard.HOME_TELEPORT);
        return Collections.unmodifiableMap(aliases);
    }

    /**
     * Matches display info to a spell by name across every spellbook.
     *
     * <p>Display info qualifies the destination in a few ways the spellbook names do not: a colon
     * suffix as in {@code "Varrock Teleport: GE"}, and a parenthesised variant as in
     * {@code "Teleport to House (Inside)"}. Each qualifier is stripped in turn before giving up.</p>
     *
     * @param displayInfo the transport's display info
     * @return the matching spell, or null when none does
     */
    static CastableSpell findSpell(String displayInfo) {
        if (displayInfo == null || displayInfo.trim().isEmpty()) {
            return null;
        }

        String wanted = displayInfo.trim();

        CastableSpell exact = matchByName(wanted);
        if (exact != null) {
            return exact;
        }

        CastableSpell alias = ALIASES.get(normalise(wanted));
        if (alias != null) {
            return alias;
        }

        for (String candidate : withoutQualifiers(wanted)) {
            CastableSpell match = matchByName(candidate);
            if (match != null) {
                return match;
            }

            CastableSpell aliased = ALIASES.get(normalise(candidate));
            if (aliased != null) {
                return aliased;
            }
        }

        return null;
    }

    /** Yields the display info with each kind of qualifier removed, longest form first. */
    private static List<String> withoutQualifiers(String value) {
        List<String> candidates = new ArrayList<>();

        int parenthesis = value.indexOf('(');
        if (parenthesis > 0) {
            candidates.add(value.substring(0, parenthesis).trim());
        }

        int colon = value.indexOf(':');
        if (colon > 0) {
            candidates.add(value.substring(0, colon).trim());
        }

        return candidates;
    }

    private static CastableSpell matchByName(String name) {
        String normalised = normalise(name);

        for (CastableSpell spell : allSpells()) {
            if (normalise(spell.getName()).equals(normalised)) {
                return spell;
            }
        }

        return null;
    }

    private static List<CastableSpell> allSpells() {
        List<CastableSpell> spells = new ArrayList<>();
        spells.addAll(java.util.Arrays.asList(Standard.values()));
        spells.addAll(java.util.Arrays.asList(Ancient.values()));
        spells.addAll(java.util.Arrays.asList(Lunar.values()));
        spells.addAll(java.util.Arrays.asList(Arceuus.values()));
        return spells;
    }

    /** Spell names differ from display info in punctuation and spacing more than in wording. */
    private static String normalise(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (char c : value.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
