package com.kraken.api.service.util.dps.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kraken.api.service.util.dps.model.EquipmentItem;
import com.kraken.api.service.util.dps.model.MonsterData;
import com.kraken.api.service.util.dps.model.SpellData;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads and indexes the bundled OSRS wiki combat data (equipment stats, monster stats,
 * spells, and equipment aliases) used by the DPS calculator. Data is loaded lazily on
 * first access and cached for the lifetime of the client.
 */
@Slf4j
@Singleton
public class DpsDataStore {

    private final Gson gson;

    private volatile boolean loaded;
    private final Map<Integer, EquipmentItem> equipmentById = new HashMap<>();
    private final Map<Integer, Integer> canonicalItemIds = new HashMap<>();
    private final Map<Integer, MonsterData> monstersById = new HashMap<>();
    private final Map<String, List<MonsterData>> monstersByName = new HashMap<>();
    private final Map<String, SpellData> spellsByName = new HashMap<>();

    @Inject
    public DpsDataStore(Gson gson) {
        this.gson = gson;
    }

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (this) {
            if (loaded) return;
            loadEquipment();
            loadMonsters();
            loadSpells();
            loaded = true;
            log.debug("DPS data loaded: {} equipment, {} aliases, {} monsters, {} spells",
                    equipmentById.size(), canonicalItemIds.size(), monstersById.size(), spellsByName.size());
        }
    }

    private <T> T readResource(String path, Type type) {
        InputStream in = DpsDataStore.class.getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing bundled DPS data resource: " + path);
        }
        try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse bundled DPS data resource: " + path, e);
        }
    }

    private void loadEquipment() {
        Type listType = new TypeToken<List<EquipmentItem>>() {}.getType();
        List<EquipmentItem> items = readResource("/dps/equipment.json", listType);
        for (EquipmentItem item : items) {
            equipmentById.putIfAbsent(item.getId(), item);
        }

        // maps variant item id -> canonical (base) item id
        Type aliasType = new TypeToken<Map<String, Integer>>() {}.getType();
        Map<String, Integer> aliases = readResource("/dps/equipment_aliases.json", aliasType);
        for (Map.Entry<String, Integer> entry : aliases.entrySet()) {
            try {
                canonicalItemIds.put(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void loadMonsters() {
        Type listType = new TypeToken<List<MonsterData>>() {}.getType();
        List<MonsterData> monsters = readResource("/dps/monsters.json", listType);
        for (MonsterData m : monsters) {
            monstersById.putIfAbsent(m.getId(), m);
            monstersByName.computeIfAbsent(m.getName().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(m);
        }
    }

    private void loadSpells() {
        Type listType = new TypeToken<List<SpellData>>() {}.getType();
        List<SpellData> spells = readResource("/dps/spells.json", listType);
        for (SpellData s : spells) {
            spellsByName.putIfAbsent(s.getName(), s);
        }
    }

    /**
     * Resolves an item id to its canonical (base) id. Locked, degraded, and cosmetic
     * variants all resolve to the same base item.
     * @param itemId The item id to canonicalize
     * @return int the canonical item id (the input id when no alias exists)
     */
    public int canonicalItemId(int itemId) {
        ensureLoaded();
        return canonicalItemIds.getOrDefault(itemId, itemId);
    }

    /**
     * Looks up an item's combat stats by item id, resolving cosmetic/locked variants
     * to their base item.
     * @param itemId The item id
     * @return EquipmentItem or null when the item is not equippable / unknown
     */
    public EquipmentItem equipment(int itemId) {
        ensureLoaded();
        EquipmentItem item = equipmentById.get(canonicalItemId(itemId));
        if (item == null) {
            item = equipmentById.get(itemId);
        }
        return item;
    }

    /**
     * Looks up a monster's combat stats by npc id.
     * @param npcId The npc id
     * @return MonsterData copy safe to mutate, or null when unknown
     */
    public MonsterData monster(int npcId) {
        ensureLoaded();
        MonsterData m = monstersById.get(npcId);
        return m == null ? null : m.copy();
    }

    /**
     * Looks up monsters by name (case-insensitive). Multiple versions of the same monster
     * (e.g. Zulrah's forms) are all returned.
     * @param name The monster name
     * @return List of matching monsters (copies safe to mutate); empty when none match
     */
    public List<MonsterData> monstersByName(String name) {
        ensureLoaded();
        List<MonsterData> found = monstersByName.get(name == null ? "" : name.toLowerCase(Locale.ROOT));
        if (found == null) return Collections.emptyList();
        List<MonsterData> copies = new ArrayList<>(found.size());
        for (MonsterData m : found) {
            copies.add(m.copy());
        }
        return copies;
    }

    /**
     * Looks up a spell by its exact name, e.g. "Fire Surge" or "Ice Barrage".
     * @param name The spell name
     * @return SpellData or null when unknown
     */
    public SpellData spell(String name) {
        ensureLoaded();
        return spellsByName.get(name);
    }

    /**
     * Gets all loaded spells.
     * @return Map of spell name to spell data
     */
    public Map<String, SpellData> spells() {
        ensureLoaded();
        return Collections.unmodifiableMap(spellsByName);
    }

    /**
     * Computes a spell's max hit at the given magic level. Elemental spells whose damage
     * scales with level (Strike/Bolt/Blast/Wave/Surge) resolve to the strongest element
     * unlocked at that level, mirroring the wiki calculator.
     * @param spell The spell being cast
     * @param magicLevel The player's visible magic level (base + boost)
     * @return int the spell's base max hit
     */
    public int spellMaxHit(SpellData spell, int magicLevel) {
        ensureLoaded();
        if (spell == null) return 0;
        if (spell.getElement() == null) {
            return spell.getMaxHit();
        }

        String[] parts = spell.getName().split(" ");
        if (parts.length < 2) return spell.getMaxHit();
        String spellClass = parts[1];

        int fire;
        int earth;
        int water;
        switch (spellClass) {
            case "Strike":
                fire = 13; earth = 9; water = 5;
                break;
            case "Bolt":
                fire = 35; earth = 29; water = 23;
                break;
            case "Blast":
                fire = 59; earth = 53; water = 47;
                break;
            case "Wave":
                fire = 75; earth = 70; water = 65;
                break;
            case "Surge":
                fire = 95; earth = 90; water = 85;
                break;
            default:
                return spell.getMaxHit();
        }

        String element;
        if (magicLevel >= fire) {
            element = "Fire";
        } else if (magicLevel >= earth) {
            element = "Earth";
        } else if (magicLevel >= water) {
            element = "Water";
        } else {
            element = "Wind";
        }

        SpellData resolved = spellsByName.get(element + " " + spellClass);
        return resolved != null ? resolved.getMaxHit() : spell.getMaxHit();
    }
}
