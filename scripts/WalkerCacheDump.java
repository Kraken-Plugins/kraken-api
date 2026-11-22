import net.runelite.cache.EntityOpsDefinition;
import net.runelite.cache.ItemManager;
import net.runelite.cache.NpcManager;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.definitions.ItemDefinition;
import net.runelite.cache.definitions.LocationsDefinition;
import net.runelite.cache.definitions.MapDefinition;
import net.runelite.cache.definitions.NpcDefinition;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dumps the parts of the game cache that scripts/update_walker_transports.py checks transports
 * against: every object, NPC and item definition, where interactive objects are placed on the map,
 * and which tiles are bridges.
 *
 * <p>This file is not part of the Gradle build. It is a single file program run against RuneLite's
 * cache library, which the API does not otherwise depend on. Use the cache library version that
 * matches {@code runeLiteVersion} in build.gradle, from https://repo.runelite.net, with gson, guava,
 * slf4j-api, commons-compress, antlr4-runtime and jna beside it.</p>
 *
 * <pre>
 *   # the game client keeps the live cache open, so read a copy
 *   cp ~/.runelite/jagexcache/oldschool/LIVE/main_file_cache.* cache-copy/
 *   java -Xmx3g -cp "libs/*" scripts/WalkerCacheDump.java cache-copy cache-dump
 * </pre>
 *
 * <p>Output, tab separated with one header line starting with {@code #}:</p>
 * <ul>
 *   <li>{@code objects.tsv}: id, name, the five menu actions joined by {@code |}, sizeX, sizeY,
 *       varbitId, varpId, the ids the object can transform into, mapSceneId</li>
 *   <li>{@code npcs.tsv}: id, name, actions, varbitId, varpId, transforms</li>
 *   <li>{@code items.tsv}: id, name, inventory actions, ground actions, members, noted template,
 *       placeholder template, sub actions written as {@code slot:first;second} groups joined by
 *       {@code |}</li>
 *   <li>{@code locs.tsv}: id, type, orientation, x, y, plane of every placed object that offers a
 *       menu action itself or through a transform. The plane is the one the map stores.</li>
 *   <li>{@code bridges.tsv}: x, y of every tile flagged as a bridge. The game shows what stands on
 *       such a tile one plane lower than the map stores it.</li>
 * </ul>
 */
public class WalkerCacheDump {

    private static final int ACTION_SLOTS = 5;
    private static final int REGIONS_PER_AXIS = 256;
    private static final int REGION_SIZE = 64;
    private static final int BRIDGE_FLAG = 2;

    public static void main(String[] arguments) throws IOException {
        if (arguments.length != 2) {
            System.err.println("usage: WalkerCacheDump <cache directory> <output directory>");
            System.exit(2);
        }

        Path output = Paths.get(arguments[1]);
        Files.createDirectories(output);

        try (Store store = new Store(new File(arguments[0]))) {
            store.load();
            Map<Integer, ObjectDefinition> objects = dumpObjects(store, output);
            dumpNpcs(store, output);
            dumpItems(store, output);
            dumpPlacements(store, objects, output);
        }
    }

    private static Map<Integer, ObjectDefinition> dumpObjects(Store store, Path output) throws IOException {
        ObjectManager manager = new ObjectManager(store);
        manager.load();
        List<ObjectDefinition> definitions = new ArrayList<>(manager.getObjects());
        definitions.sort(Comparator.comparingInt(ObjectDefinition::getId));

        Map<Integer, ObjectDefinition> byId = new HashMap<>();
        try (BufferedWriter writer = open(output.resolve("objects.tsv"),
                "#id\tname\tactions\tsizeX\tsizeY\tvarbitId\tvarpId\ttransforms\tmapSceneId")) {
            for (ObjectDefinition definition : definitions) {
                byId.put(definition.getId(), definition);
                writeRow(writer,
                        definition.getId(),
                        clean(definition.getName()),
                        actions(definition.getOps()),
                        definition.getSizeX(),
                        definition.getSizeY(),
                        definition.getVarbitID(),
                        definition.getVarpID(),
                        join(definition.getConfigChangeDest()),
                        definition.getMapSceneID() >= 0 ? String.valueOf(definition.getMapSceneID()) : "");
            }
        }
        System.out.println("objects " + definitions.size());
        return byId;
    }

    private static void dumpNpcs(Store store, Path output) throws IOException {
        NpcManager manager = new NpcManager(store);
        manager.load();
        List<NpcDefinition> definitions = new ArrayList<>(manager.getNpcs());
        definitions.sort(Comparator.comparingInt(NpcDefinition::getId));

        try (BufferedWriter writer = open(output.resolve("npcs.tsv"),
                "#id\tname\tactions\tvarbitId\tvarpId\ttransforms")) {
            for (NpcDefinition definition : definitions) {
                writeRow(writer,
                        definition.getId(),
                        clean(definition.getName()),
                        actions(definition.getOps()),
                        definition.getVarbitId(),
                        definition.getVarpIndex(),
                        join(definition.getConfigs()));
            }
        }
        System.out.println("npcs " + definitions.size());
    }

    private static void dumpItems(Store store, Path output) throws IOException {
        ItemManager manager = new ItemManager(store);
        manager.load();
        List<ItemDefinition> definitions = new ArrayList<>(manager.getItems());
        definitions.sort(Comparator.comparingInt(ItemDefinition::getId));

        try (BufferedWriter writer = open(output.resolve("items.tsv"),
                "#id\tname\tinventoryActions\tgroundActions\tmembers\tnotedTemplate\tplaceholderTemplate\tsubops")) {
            for (ItemDefinition definition : definitions) {
                writeRow(writer,
                        definition.getId(),
                        clean(definition.getName()),
                        joinText(definition.getInterfaceOptions(), "|", true),
                        actions(definition.getGroundOps()),
                        definition.isMembers(),
                        definition.getNotedTemplate(),
                        definition.getPlaceholderTemplateId(),
                        subActions(definition.getSubops()));
            }
        }
        System.out.println("items " + definitions.size());
    }

    /**
     * Writes where interactive objects stand and which tiles are bridges, region by region.
     *
     * <p>The map index carries no archive names, so regions cannot be listed. Every possible region
     * id is tried and the ones without a map are skipped. Current maps are not encrypted, so the
     * loader is given no keys.</p>
     */
    private static void dumpPlacements(Store store, Map<Integer, ObjectDefinition> objects, Path output)
            throws IOException {
        RegionLoader loader = new RegionLoader(store, regionId -> null);
        Map<Integer, Boolean> interactive = new HashMap<>();
        int loaded = 0;
        int failed = 0;
        long written = 0;

        try (BufferedWriter placements = open(output.resolve("locs.tsv"), "#id\ttype\torientation\tx\ty\tplane");
             BufferedWriter bridges = open(output.resolve("bridges.tsv"), "#x\ty")) {
            for (int regionX = 0; regionX < REGIONS_PER_AXIS; regionX++) {
                for (int regionY = 0; regionY < REGIONS_PER_AXIS; regionY++) {
                    int regionId = (regionX << 8) | regionY;
                    try {
                        MapDefinition map = loader.loadMapDef(regionId);
                        if (map == null) {
                            continue;
                        }

                        Region region = new Region(regionId);
                        region.loadTerrain(map);
                        LocationsDefinition locations = loader.loadLocDef(regionId);
                        if (locations != null) {
                            region.loadLocations(locations);
                        }
                        loaded++;

                        for (int x = 0; x < REGION_SIZE; x++) {
                            for (int y = 0; y < REGION_SIZE; y++) {
                                if ((region.getTileSetting(1, x, y) & BRIDGE_FLAG) != 0) {
                                    writeRow(bridges, region.getBaseX() + x, region.getBaseY() + y);
                                }
                            }
                        }

                        for (Location location : region.getLocations()) {
                            if (!interactive.computeIfAbsent(location.getId(), id -> offersAnAction(id, objects))) {
                                continue;
                            }
                            Position position = location.getPosition();
                            writeRow(placements, location.getId(), location.getType(), location.getOrientation(),
                                    position.getX(), position.getY(), position.getZ());
                            written++;
                        }
                    } catch (RuntimeException e) {
                        failed++;
                        System.out.println("region " + regionId + " (" + regionX + "," + regionY + ") failed: " + e);
                    }
                }
            }
        }
        System.out.println("regions loaded " + loaded + ", failed " + failed + ", placements written " + written);
    }

    /** Reports whether an object, or anything it can transform into, has a menu action. */
    private static boolean offersAnAction(int objectId, Map<Integer, ObjectDefinition> objects) {
        ObjectDefinition definition = objects.get(objectId);
        if (definition == null) {
            return false;
        }
        if (hasAction(definition)) {
            return true;
        }

        int[] transforms = definition.getConfigChangeDest();
        if (transforms != null) {
            for (int transformId : transforms) {
                ObjectDefinition transform = objects.get(transformId);
                if (transform != null && hasAction(transform)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAction(ObjectDefinition definition) {
        EntityOpsDefinition operations = definition.getOps();
        if (operations == null) {
            return false;
        }
        if (operations.getOps() != null) {
            for (EntityOpsDefinition.Op operation : operations.getOps()) {
                if (operation != null && operation.text != null) {
                    return true;
                }
            }
        }
        if (operations.getConditionalOps() != null) {
            for (List<EntityOpsDefinition.ConditionalOp> conditional : operations.getConditionalOps()) {
                if (conditional != null && !conditional.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Joins an entity's menu actions with {@code |}, leaving an empty slot empty. */
    private static String actions(EntityOpsDefinition operations) {
        List<String> slots = new ArrayList<>();
        if (operations != null && operations.getOps() != null) {
            for (EntityOpsDefinition.Op operation : operations.getOps()) {
                slots.add(operation == null ? "" : clean(operation.text));
            }
        }
        while (slots.size() < ACTION_SLOTS) {
            slots.add("");
        }
        return String.join("|", slots);
    }

    private static String subActions(String[][] subActions) {
        if (subActions == null) {
            return "";
        }

        List<String> groups = new ArrayList<>();
        for (int slot = 0; slot < subActions.length; slot++) {
            String joined = joinText(subActions[slot], ";", false);
            if (!joined.isEmpty()) {
                groups.add(slot + ":" + joined);
            }
        }
        return String.join("|", groups);
    }

    private static String joinText(String[] values, String separator, boolean keepEmptySlots) {
        if (values == null) {
            return "";
        }

        List<String> kept = new ArrayList<>();
        for (String value : values) {
            if (value != null || keepEmptySlots) {
                kept.add(clean(value));
            }
        }
        return String.join(separator, kept);
    }

    private static String join(int[] values) {
        if (values == null) {
            return "";
        }

        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                joined.append(',');
            }
            joined.append(values[i]);
        }
        return joined.toString();
    }

    private static String clean(String text) {
        return text == null ? "" : text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ');
    }

    private static BufferedWriter open(Path path, String header) throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8);
        writer.write(header);
        writer.write('\n');
        return writer;
    }

    private static void writeRow(BufferedWriter writer, Object... cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                writer.write('\t');
            }
            writer.write(String.valueOf(cells[i]));
        }
        writer.write('\n');
    }
}
