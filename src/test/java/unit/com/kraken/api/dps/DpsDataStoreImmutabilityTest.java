package unit.com.kraken.api.dps;

import com.google.gson.Gson;
import com.kraken.api.service.util.dps.data.DpsDataStore;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.SpellData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DpsDataStoreImmutabilityTest {

    private static final int BRONZE_DAGGER = 1205;
    private static final DpsDataStore DATA = new DpsDataStore(new Gson());

    private static List<String> publicMutators(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && m.getDeclaringClass() != Object.class)
                .map(Method::getName)
                .filter(n -> n.startsWith("set") || n.startsWith("put") || n.startsWith("add") || n.startsWith("clear") || n.startsWith("remove"))
                .collect(java.util.stream.Collectors.toList());
    }

    @Test
    void equipmentAndSpellDefinitionsExposeNoMutators() {
        for (Class<?> type : new Class<?>[] {EquipmentItem.class, EquipmentItem.Bonuses.class, EquipmentItem.Styles.class, SpellData.class}) {
            assertTrue(publicMutators(type).isEmpty(), type.getSimpleName() + " has mutators: " + publicMutators(type));
        }
    }

    @Test
    void repeatedLookupsSeeTheBundledDefinitionsUnchanged() {
        EquipmentItem dagger = DATA.equipment(BRONZE_DAGGER);
        assertEquals("Bronze dagger", dagger.getName());
        assertEquals(dagger, DATA.equipment(BRONZE_DAGGER));

        SpellData spell = DATA.spell("Fire Bolt");
        assertEquals(spell, DATA.spells().get("Fire Bolt"));
        assertThrows(UnsupportedOperationException.class, () -> DATA.spells().put("x", spell));
        assertThrows(UnsupportedOperationException.class, () -> DATA.spells().remove("Fire Bolt"));
    }

    @Test
    void monsterLookupsAreCopies() {
        MonsterData first = DATA.monstersByName("Goblin").get(0);
        MonsterData again = DATA.monster(first.getId());
        assertNotSame(first, again);
        assertEquals(first, again);
    }
}
