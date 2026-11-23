package com.kraken.api.service.walker.transport.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.util.JsonResourceUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the hand written transports that ship inside the API jar, and reads and writes the row
 * format that they and the bundled dataset are stored in.
 *
 * <p>The hand written rows live in {@code src/main/resources/walker/transports.json} and are read
 * through the class loader, so they are available wherever the jar is. Only declarative
 * definitions can be written there, since a file cannot carry a handler or a condition. A row
 * without an {@code origin} is a teleport usable from anywhere, which an item ({@code itemIds}) or
 * a spell ({@code displayInfo}) provides. The far larger set generated from upstream shortest-path
 * is read by {@link TransportDataset}.</p>
 *
 * <p>{@link #parse(String)} and {@link #toJson(TransportDefinition)} are each other's inverse, so a
 * plugin can keep its own transports in a file of the same format and register what it parses.</p>
 *
 * <p>The file is read strictly. A row with a key this class does not know, a category or shape that
 * is not a known name, or a requirement it cannot resolve is logged and skipped, because reading
 * around a spelling mistake would load a gated transport without its gate. Each row is judged on
 * its own, so the rest still load.</p>
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BundledTransports {

    /** Where the bundled transports are read from on the classpath. */
    public static final String RESOURCE_PATH = "/walker/transports.json";

    private static final Gson GSON = new Gson();

    private static final Set<String> ROW_KEYS = fieldNames(Row.class);
    private static final Set<String> POINT_KEYS = fieldNames(Point.class);
    private static final Set<String> REQUIREMENTS_KEYS = fieldNames(RequirementsRow.class);
    private static final Set<String> ITEM_KEYS = fieldNames(ItemRow.class);
    private static final Set<String> ALTERNATIVE_KEYS = Set.of("anyOf", "quantity", "staves", "offhands");
    private static final Set<String> VAR_KEYS = fieldNames(VarRow.class);

    /**
     * Loads the hand written bundled transports. A missing or unreadable file yields no transports
     * rather than an exception, so the walker still works on the dataset alone.
     *
     * @return the definitions that parsed, in file order
     */
    public static List<TransportDefinition> load() {
        try {
            JsonArray rows = JsonResourceUtils.loadJsonResource(BundledTransports.class, RESOURCE_PATH, GSON, JsonArray.class);
            List<TransportDefinition> definitions = toDefinitions(rows);
            log.info("Loaded {} bundled walker transports", definitions.size());
            return definitions;
        } catch (IllegalStateException | JsonParseException e) {
            log.error("Could not read bundled walker transports from {}", RESOURCE_PATH, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parses transports from JSON text in the bundled file's format.
     *
     * @param json a JSON array of transport rows
     * @return the definitions that parsed, in order
     * @throws JsonParseException when the text is not a JSON array of rows
     */
    public static List<TransportDefinition> parse(String json) {
        return toDefinitions(GSON.fromJson(json, JsonArray.class));
    }

    /**
     * Writes a definition as one row of the bundled file's format, on a single line and spaced the
     * way the file's rows are. A value the loader fills in by default is left out, and
     * {@link #parse(String)} reads the row back into a definition that states the same.
     *
     * @param definition a declarative definition
     * @return the row as JSON text, without indentation or a trailing comma
     * @throws IllegalArgumentException when the definition carries a handler or a condition, or has
     *                                  a shape that needs a handler, none of which a file can hold
     */
    public static String toJson(TransportDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition must not be null");
        }

        String id = definition.getId();
        if (definition.getHandler() != null) {
            throw new IllegalArgumentException(id + ": a handler cannot be written to a data file");
        }
        if (definition.getAvailableWhen() != null) {
            throw new IllegalArgumentException(id + ": a condition cannot be written to a data file");
        }
        if (!TransportDefinition.DECLARATIVE_SHAPES.contains(definition.getShape())) {
            throw new IllegalArgumentException(id + ": shape " + definition.getShape()
                    + " needs a handler, which a data file cannot carry");
        }

        JsonObject row = new JsonObject();
        row.addProperty("id", id);
        if (definition.getOrigin() != null) {
            row.add("origin", toJson(definition.getOrigin()));
        }
        row.add("destination", toJson(definition.getDestination()));
        if (definition.getCategory() != TransportDefinition.categoryOf(definition.getShape())) {
            row.addProperty("category", definition.getCategory().name());
        }
        row.addProperty("shape", definition.getShape().name());
        addIfStated(row, "menuOption", definition.getMenuOption());
        addIfStated(row, "menuTarget", definition.getMenuTarget());
        if (definition.getEntityId() != ObjectInfo.NO_ID) {
            row.addProperty("entityId", definition.getEntityId());
        }
        if (definition.statesObjectInfo()) {
            row.addProperty("objectInfo", definition.getObjectInfo());
        }
        if (!definition.getItemIds().isEmpty()) {
            JsonArray itemIds = new JsonArray();
            definition.getItemIds().forEach(itemIds::add);
            row.add("itemIds", itemIds);
        }
        addIfStated(row, "displayInfo", definition.getDisplayInfo());
        if (definition.getDuration() != 1) {
            row.addProperty("duration", definition.getDuration());
        }
        if (definition.getMaxWildernessLevel() != TransportDefinition.DEFAULT_MAX_WILDERNESS_LEVEL) {
            row.addProperty("maxWildernessLevel", definition.getMaxWildernessLevel());
        }
        if (definition.isConsumable()) {
            row.addProperty("consumable", true);
        }
        if (definition.isBidirectional()) {
            row.addProperty("bidirectional", true);
        }
        if (definition.isReplaces()) {
            row.addProperty("replaces", true);
        }

        JsonObject requirements = toJson(definition.getRequirements(), definition.getItemIds());
        if (requirements.size() > 0) {
            row.add("requirements", requirements);
        }

        StringBuilder text = new StringBuilder();
        write(row, text);
        return text.toString();
    }

    private static void addIfStated(JsonObject row, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            row.addProperty(key, value);
        }
    }

    private static JsonObject toJson(WorldPoint point) {
        JsonObject json = new JsonObject();
        json.addProperty("x", point.getX());
        json.addProperty("y", point.getY());
        json.addProperty("plane", point.getPlane());
        return json;
    }

    /**
     * Writes requirements as the file states them.
     *
     * <p>A definition that lists item ids requires one of them to be held, and adds that rule to its
     * requirements itself when it is built. The rule is implied by {@code itemIds} in the file, so
     * it is not written a second time here.</p>
     */
    private static JsonObject toJson(DefinitionRequirements requirements, List<Integer> teleportItemIds) {
        JsonObject json = new JsonObject();

        if (!requirements.getSkillLevels().isEmpty()) {
            JsonObject skills = new JsonObject();
            requirements.getSkillLevels().forEach((skill, level) -> skills.addProperty(skill.name(), level));
            json.add("skills", skills);
        }
        if (requirements.getCombatLevel() != 0) {
            json.addProperty("combatLevel", requirements.getCombatLevel());
        }
        if (requirements.getTotalLevel() != 0) {
            json.addProperty("totalLevel", requirements.getTotalLevel());
        }

        if (!requirements.getQuests().isEmpty()) {
            JsonArray quests = new JsonArray();
            requirements.getQuests().forEach(quest -> quests.add(quest.name()));
            json.add("quests", quests);
        }

        List<DefinitionRequirements.RequiredItem> items = new ArrayList<>(requirements.getItems());
        int[] implied = teleportItemIds.stream().mapToInt(Integer::intValue).toArray();
        if (implied.length > 0 && !items.isEmpty()) {
            DefinitionRequirements.RequiredItem last = items.get(items.size() - 1);
            if (last.isPlain() && last.getQuantity() == 1 && Arrays.equals(last.getAnyOf(), implied)) {
                items.remove(items.size() - 1);
            }
        }
        if (!items.isEmpty()) {
            JsonArray written = new JsonArray();
            for (DefinitionRequirements.RequiredItem item : items) {
                JsonObject entry = toJson(item);
                if (!item.getAlternatives().isEmpty()) {
                    JsonArray alternatives = new JsonArray();
                    item.getAlternatives().forEach(alternative -> alternatives.add(toJson(alternative)));
                    entry.add("or", alternatives);
                }
                if (item.getWaivedByVarbit() != null) {
                    entry.add("waivedByVarbit", toJson(item.getWaivedByVarbit()));
                }
                written.add(entry);
            }
            json.add("items", written);
        }

        addVars(json, "varbits", requirements.getVarbits());
        addVars(json, "varPlayers", requirements.getVarPlayers());

        if (requirements.isMembersOnly()) {
            json.addProperty("membersOnly", true);
        }

        return json;
    }

    /**
     * Writes the items, quantity, staves and offhands of one way to meet an item requirement.
     */
    private static JsonObject toJson(DefinitionRequirements.RequiredItem item) {
        JsonObject entry = new JsonObject();
        entry.add("anyOf", toJson(item.getAnyOf()));
        if (item.getQuantity() != 1) {
            entry.addProperty("quantity", item.getQuantity());
        }
        if (item.getStaves().length > 0) {
            entry.add("staves", toJson(item.getStaves()));
        }
        if (item.getOffhands().length > 0) {
            entry.add("offhands", toJson(item.getOffhands()));
        }
        return entry;
    }

    private static JsonArray toJson(int[] values) {
        JsonArray array = new JsonArray();
        for (int value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject toJson(DefinitionRequirements.RequiredVar requirement) {
        JsonObject entry = new JsonObject();
        entry.addProperty("id", requirement.getId());
        entry.addProperty("comparison", requirement.getComparison().name());
        entry.addProperty("value", requirement.getValue());
        return entry;
    }

    private static void addVars(JsonObject json, String key, List<DefinitionRequirements.RequiredVar> required) {
        if (required.isEmpty()) {
            return;
        }

        JsonArray written = new JsonArray();
        for (DefinitionRequirements.RequiredVar requirement : required) {
            written.add(toJson(requirement));
        }
        json.add(key, written);
    }

    /**
     * Writes JSON on one line with a space after each comma and colon, which is how the bundled
     * file's rows are spaced. Gson's own writer offers either no spaces or one value per line.
     */
    private static void write(JsonElement element, StringBuilder text) {
        if (element.isJsonObject()) {
            text.append('{');
            String separator = "";
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                text.append(separator);
                writeString(entry.getKey(), text);
                text.append(": ");
                write(entry.getValue(), text);
                separator = ", ";
            }
            text.append('}');
        } else if (element.isJsonArray()) {
            text.append('[');
            String separator = "";
            for (JsonElement entry : element.getAsJsonArray()) {
                text.append(separator);
                write(entry, text);
                separator = ", ";
            }
            text.append(']');
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            writeString(element.getAsString(), text);
        } else {
            text.append(element);
        }
    }

    /**
     * Writes a JSON string, escaping everything outside printable ASCII so the file stays plain
     * ASCII whatever a name contains.
     */
    private static void writeString(String value, StringBuilder text) {
        text.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                text.append('\\').append(c);
            } else if (c == '\n') {
                text.append("\\n");
            } else if (c == '\r') {
                text.append("\\r");
            } else if (c == '\t') {
                text.append("\\t");
            } else if (c < 0x20 || c > 0x7e) {
                text.append(String.format("\\u%04x", (int) c));
            } else {
                text.append(c);
            }
        }
        text.append('"');
    }

    private static List<TransportDefinition> toDefinitions(JsonArray rows) {
        if (rows == null) {
            return Collections.emptyList();
        }

        List<TransportDefinition> definitions = new ArrayList<>(rows.size());
        for (JsonElement element : rows) {
            try {
                definitions.add(toDefinition(element, true));
            } catch (IllegalArgumentException | JsonParseException e) {
                log.warn("Skipping bundled walker transport: {}", e.getMessage());
            }
        }
        return definitions;
    }

    /**
     * Turns one row into a definition.
     *
     * <p>Gson reads an unknown key as nothing and an unknown enum name as null, so both are checked
     * here before the row is trusted. The constructor of the definition then rejects a row that
     * leaves out something required.</p>
     *
     * @param element the row
     * @param declarativeOnly true to refuse a shape whose built in handler cannot work from a row
     *                        alone, which is the rule for a file a person writes. The dataset
     *                        turns it off, because its gliders, balloons, mushtrees and quetzals
     *                        have the one shape whose handler also reads the row's category
     */
    static TransportDefinition toDefinition(JsonElement element, boolean declarativeOnly) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("a row must be a JSON object");
        }

        JsonObject object = element.getAsJsonObject();
        Row row = GSON.fromJson(object, Row.class);
        String id = row.id;

        requireKnownKeys(id, "row", object, ROW_KEYS);
        requireKnownKeys(id, "origin", object.get("origin"), POINT_KEYS);
        requireKnownKeys(id, "destination", object.get("destination"), POINT_KEYS);
        requireResolved(id, "category", object, row.category);
        requireResolved(id, "shape", object, row.shape);

        // A row that states no shape is operated the usual way for its category, and a row that
        // states no category either is an ordinary single click.
        TransportShape shape = row.shape != null ? row.shape
                : (row.category != null ? row.category : TransportCategory.GENERAL).getDefaultShape();
        if (declarativeOnly && !TransportDefinition.DECLARATIVE_SHAPES.contains(shape)) {
            throw new IllegalArgumentException(id + ": shape " + shape
                    + " needs a handler, which a data file cannot carry");
        }

        var builder = TransportDefinition.builder()
                .id(id)
                .origin(toWorldPoint(row.origin))
                .destination(toWorldPoint(row.destination))
                .category(row.category)
                .shape(shape)
                .menuOption(row.menuOption)
                .menuTarget(row.menuTarget)
                .entityId(row.entityId != null ? row.entityId : ObjectInfo.NO_ID)
                .objectInfo(row.objectInfo)
                .displayInfo(row.displayInfo)
                .duration(row.duration != null ? row.duration : 1)
                .bidirectional(row.bidirectional)
                .consumable(row.consumable)
                .replaces(row.replaces)
                .requirements(toRequirements(id, object.get("requirements"), row.requirements));

        if (row.itemIds != null) {
            for (int itemId : row.itemIds) {
                builder.itemId(itemId);
            }
        }
        if (row.maxWildernessLevel != null) {
            builder.maxWildernessLevel(row.maxWildernessLevel);
        }

        return builder.build();
    }

    private static WorldPoint toWorldPoint(Point point) {
        return point != null ? new WorldPoint(point.x, point.y, point.plane) : null;
    }

    /**
     * Reads the requirements object of a row.
     *
     * @param id the id of the row, for error text
     * @param json the requirements as written, may be null
     * @return the requirements, {@link DefinitionRequirements#NONE} when the row states none
     */
    static DefinitionRequirements toRequirements(String id, JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return DefinitionRequirements.NONE;
        }
        return toRequirements(id, json, GSON.fromJson(json, RequirementsRow.class));
    }

    private static DefinitionRequirements toRequirements(String id, JsonElement json, RequirementsRow row) {
        if (row == null) {
            return DefinitionRequirements.NONE;
        }

        requireKnownKeys(id, "requirements", json, REQUIREMENTS_KEYS);
        JsonObject object = json.getAsJsonObject();
        requireKnownKeysInEach(id, "items", object.get("items"), ITEM_KEYS);
        requireKnownKeysInEach(id, "varbits", object.get("varbits"), VAR_KEYS);
        requireKnownKeysInEach(id, "varPlayers", object.get("varPlayers"), VAR_KEYS);
        if (object.get("items") != null && object.get("items").isJsonArray()) {
            for (JsonElement item : object.get("items").getAsJsonArray()) {
                if (item.isJsonObject()) {
                    requireKnownKeysInEach(id, "items", item.getAsJsonObject().get("or"), ALTERNATIVE_KEYS);
                    requireKnownKeys(id, "items", item.getAsJsonObject().get("waivedByVarbit"), VAR_KEYS);
                }
            }
        }

        var builder = DefinitionRequirements.builder()
                .membersOnly(row.membersOnly)
                .combatLevel(row.combatLevel)
                .totalLevel(row.totalLevel);

        if (row.skills != null) {
            for (Map.Entry<Skill, Integer> entry : row.skills.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException(id + ": unknown skill or missing level in requirements");
                }
                builder.skill(entry.getKey(), entry.getValue());
            }
        }

        if (row.quests != null) {
            for (Quest quest : row.quests) {
                if (quest == null) {
                    throw new IllegalArgumentException(id + ": unknown quest in requirements");
                }
                builder.quest(quest);
            }
        }

        if (row.items != null) {
            for (ItemRow item : row.items) {
                builder.item(toRequiredItem(id, item));
            }
        }

        return builder
                .varbits(toRequiredVars(id, row.varbits))
                .varPlayers(toRequiredVars(id, row.varPlayers))
                .build();
    }

    /**
     * Reads one item requirement with its alternatives and its waiver. A quantity that is left out
     * means one. A quantity of zero is kept, since it says the items must not be held.
     */
    private static DefinitionRequirements.RequiredItem toRequiredItem(String id, ItemRow item) {
        if (item == null || item.anyOf == null || item.anyOf.length == 0) {
            throw new IllegalArgumentException(id + ": an item requirement lists no item ids");
        }

        List<DefinitionRequirements.RequiredItem> alternatives = new ArrayList<>();
        if (item.or != null) {
            for (ItemRow alternative : item.or) {
                if (alternative == null || alternative.anyOf == null || alternative.anyOf.length == 0) {
                    throw new IllegalArgumentException(id + ": an item alternative lists no item ids");
                }
                alternatives.add(new DefinitionRequirements.RequiredItem(alternative.anyOf, quantityOf(id, alternative),
                        alternative.staves, alternative.offhands, null, null));
            }
        }

        return new DefinitionRequirements.RequiredItem(item.anyOf, quantityOf(id, item), item.staves, item.offhands,
                alternatives, item.waivedByVarbit != null ? toRequiredVar(id, item.waivedByVarbit) : null);
    }

    private static int quantityOf(String id, ItemRow item) {
        if (item.quantity == null) {
            return 1;
        }
        if (item.quantity < 0) {
            throw new IllegalArgumentException(id + ": an item quantity cannot be negative");
        }
        return item.quantity;
    }

    private static List<DefinitionRequirements.RequiredVar> toRequiredVars(String id, List<VarRow> rows) {
        if (rows == null) {
            return Collections.emptyList();
        }

        List<DefinitionRequirements.RequiredVar> required = new ArrayList<>(rows.size());
        for (VarRow row : rows) {
            required.add(toRequiredVar(id, row));
        }
        return required;
    }

    private static DefinitionRequirements.RequiredVar toRequiredVar(String id, VarRow row) {
        if (row == null || row.comparison == null) {
            throw new IllegalArgumentException(id + ": a var requirement has no known comparison");
        }
        return new DefinitionRequirements.RequiredVar(row.id, row.comparison, row.value);
    }

    /**
     * Rejects a JSON object that carries a key the matching row class has no field for. Anything
     * that is not an object is left for Gson and the definition's own checks to judge.
     */
    private static void requireKnownKeys(String id, String where, JsonElement json, Set<String> knownKeys) {
        if (json == null || !json.isJsonObject()) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            if (!knownKeys.contains(entry.getKey())) {
                throw new IllegalArgumentException(id + ": unknown key '" + entry.getKey() + "' in " + where);
            }
        }
    }

    private static void requireKnownKeysInEach(String id, String where, JsonElement json, Set<String> knownKeys) {
        if (json == null || !json.isJsonArray()) {
            return;
        }

        for (JsonElement element : json.getAsJsonArray()) {
            requireKnownKeys(id, where, element, knownKeys);
        }
    }

    /**
     * Rejects a value that is written in the file but did not resolve to a known name, which Gson
     * reports only by leaving the field null.
     */
    private static void requireResolved(String id, String key, JsonObject object, Object resolved) {
        JsonElement written = object.get(key);
        if (resolved == null && written != null && !written.isJsonNull()) {
            throw new IllegalArgumentException(id + ": unknown " + key + " " + written);
        }
    }

    private static Set<String> fieldNames(Class<?> rowClass) {
        Set<String> names = new HashSet<>();
        for (Field field : rowClass.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                names.add(field.getName());
            }
        }
        return names;
    }

    /** One transport as the file writes it. Field names are the JSON keys. */
    private static final class Row {
        private String id;
        private Point origin;
        private Point destination;
        private TransportCategory category;
        private TransportShape shape;
        private String menuOption;
        private String menuTarget;
        private Integer entityId;
        private String objectInfo;
        private String displayInfo;
        private Integer duration;
        private boolean bidirectional;
        private boolean consumable;
        private boolean replaces;
        private RequirementsRow requirements;
        private int[] itemIds;
        private Integer maxWildernessLevel;
    }

    private static final class Point {
        private int x;
        private int y;
        private int plane;
    }

    private static final class RequirementsRow {
        private Map<Skill, Integer> skills;
        private int combatLevel;
        private int totalLevel;
        private List<Quest> quests;
        private List<ItemRow> items;
        private List<VarRow> varbits;
        private List<VarRow> varPlayers;
        private boolean membersOnly;
    }

    private static final class ItemRow {
        private int[] anyOf;
        private Integer quantity;
        private int[] staves;
        private int[] offhands;
        private List<ItemRow> or;
        private VarRow waivedByVarbit;
    }

    private static final class VarRow {
        private int id;
        private DefinitionRequirements.Comparison comparison;
        private int value;
    }
}
