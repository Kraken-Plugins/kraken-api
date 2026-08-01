package unit.com.kraken.api.dps;

import com.google.gson.Gson;
import com.kraken.api.service.util.dps.data.DpsDataStore;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.GearCategory;
import com.kraken.api.service.util.dps.model.GearSlot;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.PlayerSkills;
import com.kraken.api.service.util.dps.search.GearOptimizer;
import com.kraken.api.service.util.dps.search.GearSearchConfig;
import com.kraken.api.service.util.dps.search.GearSearchResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the smart gear search: style selection, special-item handling (salve amulet),
 * and set-effect probing (void knight equipment).
 */
class GearOptimizerTest {

    private static final DpsDataStore DATA = new DpsDataStore(new Gson());

    private static List<EquipmentItem> items(int... ids) {
        List<EquipmentItem> out = new ArrayList<>();
        for (int id : ids) {
            EquipmentItem item = DATA.equipment(id);
            assertNotNull(item, "missing equipment id " + id);
            out.add(item);
        }
        return out;
    }

    @Test
    void picksRangedAgainstMeleeImmuneMonster() {
        // whip + melee armour vs twisted bow + ranged armour against Zulrah (immune to melee)
        List<EquipmentItem> candidates = items(
                4151,  // Abyssal whip
                11832, // Bandos chestplate
                11834, // Bandos tassets
                19553, // Amulet of torture
                20997, // Twisted bow
                11212, // Dragon arrow
                27238, // Masori body (f)
                27241, // Masori chaps (f)
                19547  // Necklace of anguish
        );

        MonsterData zulrah = DATA.monster(2042);
        GearSearchResult result = new GearOptimizer(DATA).search(candidates, zulrah, GearSearchConfig.defaults(), null);

        assertNotNull(result.getBest(), "search should find a usable loadout");
        assertEquals(20997, result.getBest().getLoadout().get(GearSlot.WEAPON).getId(), "twisted bow should win vs melee-immune Zulrah");
        assertTrue(result.getBest().getDps() > 0);

        // the melee loadout must be reported as zero dps (immune) or omitted entirely
        if (result.getBestPerStyle().containsKey(GearCategory.MELEE)) {
            assertEquals(0.0, result.getBestPerStyle().get(GearCategory.MELEE).getDps(), 1e-9);
        }
    }

    @Test
    void picksSalveOverTortureAgainstUndead() {
        List<EquipmentItem> candidates = items(
                26219, // Osmumten's fang
                19553, // Amulet of torture
                12018, // Salve amulet(ei)
                26382, // Torva full helm
                26384, // Torva platebody
                26386  // Torva platelegs
        );

        MonsterData vorkath = DATA.monster(8059);
        GearSearchConfig config = GearSearchConfig.builder()
                .skills(PlayerSkills.builder().attack(118).strength(118).build())
                .useBestPrayers(true)
                .styles(java.util.Collections.singletonList(GearCategory.MELEE))
                .build();

        GearSearchResult result = new GearOptimizer(DATA).search(candidates, vorkath, config, null);

        assertNotNull(result.getBest());
        EquipmentItem neck = result.getBest().getLoadout().get(GearSlot.NECK);
        assertNotNull(neck, "search should fill the neck slot");
        assertEquals("Salve amulet(ei)", neck.getName(), "salve amulet should beat torture against undead Vorkath");
    }

    @Test
    void findsVoidSetViaProbe() {
        // void pieces have no stats individually; only the set probe can discover the set bonus
        List<EquipmentItem> candidates = items(
                26219, // Osmumten's fang
                11665, // Void melee helm
                13072, // Elite void top
                13073, // Elite void robe
                8842   // Void knight gloves
        );

        MonsterData demon = DATA.monster(415);
        GearSearchConfig config = GearSearchConfig.builder()
                .skills(PlayerSkills.builder().attack(118).strength(118).build())
                .useBestPrayers(true)
                .styles(java.util.Collections.singletonList(GearCategory.MELEE))
                .build();

        GearSearchResult result = new GearOptimizer(DATA).search(candidates, demon, config, null);

        assertNotNull(result.getBest());
        EquipmentItem head = result.getBest().getLoadout().get(GearSlot.HEAD);
        EquipmentItem body = result.getBest().getLoadout().get(GearSlot.BODY);
        assertNotNull(head, "void helm should be equipped");
        assertNotNull(body, "void top should be equipped");
        assertEquals("Void melee helm", head.getName());
        assertEquals("Elite void top", body.getName());
    }
}
