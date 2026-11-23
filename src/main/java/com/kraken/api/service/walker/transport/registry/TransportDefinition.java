package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.TransportShape;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * One transport the walker can plan through and operate: where it starts, where it leads, what the
 * player needs, and how it is crossed.
 *
 * <p>A definition is crossed in one of two ways. A declarative definition names a
 * {@link TransportShape} and what to use (a menu option and target, the items that carry a teleport,
 * or a spell), and the handler for that shape operates it. A definition with its own
 * {@link TransportHandler} runs that handler instead. Only declarative definitions can be stored as
 * data.</p>
 *
 * <p>Every transport the walker knows is a definition: the ones in the dataset bundled with the
 * API, and the ones a plugin registers.</p>
 *
 * <p>A definition without an {@link #origin} is a teleport that can be used from wherever the player
 * stands, such as a teleport tablet or a spell. The planner considers it from every tile of a
 * search, up to its {@link #maxWildernessLevel}.</p>
 *
 * <p>Register a definition with {@link TransportRegistry#register(TransportDefinition)} and every
 * walk in the client plans through it from the next search on.</p>
 */
@Getter
@Builder
public final class TransportDefinition {

    /**
     * The shapes whose built in handlers work from what a definition states, so a definition can use
     * them without bringing a handler.
     */
    public static final Set<TransportShape> DECLARATIVE_SHAPES = Collections.unmodifiableSet(EnumSet.of(
            TransportShape.SINGLE_CLICK,
            TransportShape.CLEAR_THEN_WALK,
            TransportShape.WALK,
            TransportShape.CLICK_THEN_DIALOGUE,
            TransportShape.HUB_DIALOGUE,
            TransportShape.HUB_RESUME_PAUSE,
            TransportShape.FAIRY_RING,
            TransportShape.ITEM_SUBOP,
            TransportShape.SPELL,
            TransportShape.CANOE,
            TransportShape.GROUPING_TELEPORT));

    /**
     * The shapes that are operated without anything in the scene, which is what a teleport usable
     * from anywhere needs unless it brings its own handler.
     */
    public static final Set<TransportShape> ORIGIN_FREE_SHAPES = Collections.unmodifiableSet(EnumSet.of(
            TransportShape.ITEM_SUBOP,
            TransportShape.SPELL,
            TransportShape.GROUPING_TELEPORT));

    /** The wilderness level above which most teleports stop working. */
    public static final int DEFAULT_MAX_WILDERNESS_LEVEL = 20;

    /** World coordinates run from zero up to, but not including, this value on both axes. */
    private static final int WORLD_SIZE = 16384;

    /** The game has four planes, numbered from zero. */
    private static final int PLANES = 4;

    /**
     * Stable key written as {@code owner:name}, for example {@code my-plugin:temple-entrance}.
     * Registering the same id again replaces the earlier definition.
     */
    private final String id;

    /** The tile the transport is entered from, or null for a teleport usable from anywhere. */
    private final WorldPoint origin;

    /** The tile the transport leaves the player on. */
    private final WorldPoint destination;

    /**
     * The kind of transport, which decides the planning option that turns it on and off. When none
     * is given, an item teleport, a spell and a minigame teleport take the category of their shape,
     * and everything else is {@link TransportCategory#GENERAL}.
     */
    private final TransportCategory category;

    /**
     * How the transport is operated. A definition that names a {@link #category} and no shape takes
     * the usual shape of that category. Otherwise a shape is required unless a {@link #handler} is
     * given.
     *
     * <p>The handlers for the {@link #DECLARATIVE_SHAPES} work from what any definition states.
     * {@link TransportShape#CLICK_THEN_WIDGET} also reads the category to know which interface
     * opens, so a definition with that shape names one of the hub categories or brings a handler
     * of its own. A fairy ring needs a dramen or lunar staff in game, and a canoe an axe and a
     * woodcutting level; state those in {@link #requirements}, as the bundled transports do.</p>
     */
    private final TransportShape shape;

    /**
     * The menu action to use, such as {@code Open}. Blank lets the entity's own actions decide. For
     * {@link TransportShape#ITEM_SUBOP} it is the action on the item, such as {@code Rub} or
     * {@code Break}, and blank tries the usual teleport actions in turn.
     */
    private final String menuOption;

    /** The name of the object or NPC to click. */
    private final String menuTarget;

    /** The id of the object or NPC to click, or {@link ObjectInfo#NO_ID} to find it by name. */
    @Builder.Default
    private final int entityId = ObjectInfo.NO_ID;

    /**
     * What to click, written as one string of menu option, target name and id, such as
     * {@code Open Door 9398}. The bundled dataset states a click this way when it could not check
     * it against the game cache, and the walker works out where the option ends and the name
     * begins once it has found the object. A definition states either this or {@link #menuOption},
     * {@link #menuTarget} and {@link #entityId}.
     */
    private final String objectInfo;

    /**
     * Names the destination for transports that offer a choice: the chat option of a conversation,
     * a numbered hub entry such as {@code "4: Grand Exchange"}, a fairy ring code, or the stop an
     * item lists behind its menu action, such as {@code Edgeville}. For {@link TransportShape#SPELL}
     * it is the name of the spell, such as {@code Varrock Teleport}. For
     * {@link TransportShape#CANOE} it is the stop the canoe is paddled to, and for
     * {@link TransportShape#GROUPING_TELEPORT} the minigame as the grouping tab lists it, such as
     * {@code Castle Wars}, followed by a colon and a landing when the minigame has several.
     */
    private final String displayInfo;

    /** How long the crossing takes in game ticks, used as its planning cost and arrival timeout. */
    @Builder.Default
    private final int duration = 1;

    /** Whether the transport can also be taken from the destination back to the origin. */
    private final boolean bidirectional;

    /** Whether using the transport uses up an item or a charge, as a teleport tablet does. */
    private final boolean consumable;

    /**
     * Whether this definition is the authority for its edge. When true, the transports of the
     * bundled dataset between the same two tiles are removed from every search, including searches
     * where the requirements of this definition are not met.
     */
    private final boolean replaces;

    /**
     * What the player needs before the transport can be used. A definition that lists
     * {@link #itemIds} also requires one of those items to be worn or carried, and that requirement
     * is part of what this returns.
     */
    @Builder.Default
    private final DefinitionRequirements requirements = DefinitionRequirements.NONE;

    /**
     * Optional extra condition for what {@link #requirements} cannot express. It runs on the client
     * thread each time a search captures state, so it must be quick and must not block.
     */
    private final BooleanSupplier availableWhen;

    /** Optional handler that operates this transport in place of the handler for its shape. */
    private final TransportHandler handler;

    /**
     * The items that carry this teleport, for {@link TransportShape#ITEM_SUBOP}. Any one of them
     * works, worn or carried, so every charge level of a piece of jewellery can be listed.
     */
    @Singular("itemId")
    private final List<Integer> itemIds;

    /**
     * The deepest wilderness level a teleport usable from anywhere works at. Most teleports stop
     * working above level 20 and a few, such as a charged amulet of glory, work up to level 30.
     * A transport with an origin tile ignores this.
     */
    @Builder.Default
    private final int maxWildernessLevel = DEFAULT_MAX_WILDERNESS_LEVEL;

    private TransportDefinition(String id,
                                WorldPoint origin,
                                WorldPoint destination,
                                TransportCategory category,
                                TransportShape shape,
                                String menuOption,
                                String menuTarget,
                                int entityId,
                                String objectInfo,
                                String displayInfo,
                                int duration,
                                boolean bidirectional,
                                boolean consumable,
                                boolean replaces,
                                DefinitionRequirements requirements,
                                BooleanSupplier availableWhen,
                                TransportHandler handler,
                                List<Integer> itemIds,
                                int maxWildernessLevel) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("a transport definition needs an id");
        }
        if (destination == null) {
            throw new IllegalArgumentException(id + ": a destination tile is required");
        }
        if (origin != null && origin.equals(destination)) {
            throw new IllegalArgumentException(id + ": origin and destination are the same tile");
        }
        if ((origin != null && outsideWorld(origin)) || outsideWorld(destination)) {
            throw new IllegalArgumentException(id + ": origin and destination must be world tiles on planes 0 to 3");
        }
        if (duration < 0) {
            throw new IllegalArgumentException(id + ": duration cannot be negative");
        }
        if (maxWildernessLevel < 0) {
            throw new IllegalArgumentException(id + ": the wilderness level limit cannot be negative");
        }
        if (shape == null && category != null) {
            shape = category.getDefaultShape();
        }
        if (handler == null && shape == null) {
            throw new IllegalArgumentException(id + ": give a shape to use a built in handler, or a handler");
        }
        if (isStated(objectInfo) && (entityId != ObjectInfo.NO_ID || isStated(menuOption) || isStated(menuTarget))) {
            throw new IllegalArgumentException(id + ": state what to click either as object info or as "
                    + "a menu option, a menu target and an entity id, not both");
        }
        if (origin == null && (bidirectional || replaces)) {
            throw new IllegalArgumentException(id + ": a teleport usable from anywhere has no origin tile, "
                    + "so it cannot be bidirectional or replace a dataset transport");
        }
        if (origin == null && handler == null && !ORIGIN_FREE_SHAPES.contains(shape)) {
            throw new IllegalArgumentException(id + ": shape " + shape + " clicks something in the scene, "
                    + "so it needs an origin tile; a teleport usable from anywhere uses one of "
                    + ORIGIN_FREE_SHAPES + " or its own handler");
        }

        List<Integer> items = itemIds != null ? List.copyOf(itemIds) : List.of();
        if (items.stream().anyMatch(itemId -> itemId < 0)) {
            throw new IllegalArgumentException(id + ": item ids cannot be negative");
        }

        this.id = id.trim();
        this.origin = origin;
        this.destination = destination;
        this.category = category != null ? category : categoryOf(shape);
        this.shape = shape;
        this.menuOption = menuOption;
        this.menuTarget = menuTarget;
        this.entityId = entityId;
        this.objectInfo = isStated(objectInfo) ? objectInfo.trim() : null;
        this.displayInfo = displayInfo;
        this.duration = duration;
        this.bidirectional = bidirectional;
        this.consumable = consumable;
        this.replaces = replaces;
        this.requirements = requireOneOf(items, requirements != null ? requirements : DefinitionRequirements.NONE);
        this.availableWhen = availableWhen;
        this.handler = handler;
        this.itemIds = items;
        this.maxWildernessLevel = maxWildernessLevel;

        if (handler == null) {
            requireSomethingToUse();
        }
    }

    /**
     * Checks that a declarative definition says what its shape's handler works from: the items for
     * an item teleport, the spell for a spell, the activity for a minigame teleport, and a click
     * target for everything else. A canoe also names the stop it is paddled to.
     */
    private void requireSomethingToUse() {
        if (shape == TransportShape.ITEM_SUBOP) {
            if (itemIds.isEmpty()) {
                throw new IllegalArgumentException(id + ": list the item ids that carry the teleport");
            }
        } else if (shape == TransportShape.SPELL) {
            if (!isStated(displayInfo)) {
                throw new IllegalArgumentException(id + ": name the spell in the display info");
            }
        } else if (shape == TransportShape.GROUPING_TELEPORT) {
            if (!isStated(displayInfo)) {
                throw new IllegalArgumentException(id + ": name the minigame in the display info");
            }
        } else if (shape == TransportShape.WALK) {
            if (toObjectInfo() != null) {
                throw new IllegalArgumentException(id + ": a transport that is walked across clicks nothing");
            }
        } else if (!ORIGIN_FREE_SHAPES.contains(shape) && toObjectInfo() == null) {
            throw new IllegalArgumentException(id + ": name what to click with a menu target or an entity id");
        } else if (shape == TransportShape.CANOE && !isStated(displayInfo)) {
            throw new IllegalArgumentException(id + ": name the stop the canoe is paddled to in the display info");
        }
    }

    private static boolean isStated(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Adds the rule that one of the teleport's items must be held to the stated requirements.
     */
    private static DefinitionRequirements requireOneOf(List<Integer> itemIds, DefinitionRequirements stated) {
        if (itemIds.isEmpty()) {
            return stated;
        }

        int[] ids = itemIds.stream().mapToInt(Integer::intValue).toArray();
        return stated.toBuilder().item(DefinitionRequirements.RequiredItem.anyOf(ids)).build();
    }

    /**
     * Picks the category for a definition that names none, so that a teleport follows the planning
     * option for its kind without having to say so.
     *
     * @param shape how the definition is operated, may be null
     * @return the category a definition of that shape takes by default
     */
    static TransportCategory categoryOf(TransportShape shape) {
        if (shape == TransportShape.ITEM_SUBOP) {
            return TransportCategory.TELEPORTATION_ITEM;
        }
        if (shape == TransportShape.SPELL) {
            return TransportCategory.TELEPORTATION_SPELL;
        }
        if (shape == TransportShape.GROUPING_TELEPORT) {
            return TransportCategory.TELEPORTATION_MINIGAME;
        }
        return TransportCategory.GENERAL;
    }

    private static boolean outsideWorld(WorldPoint point) {
        return point.getX() < 0 || point.getX() >= WORLD_SIZE
                || point.getY() < 0 || point.getY() >= WORLD_SIZE
                || point.getPlane() < 0 || point.getPlane() >= PLANES;
    }

    /**
     * Starts a definition crossed by one click on a scene object, such as a door, a ladder or a cave
     * entrance.
     *
     * @param id the definition's stable key
     * @param origin the tile the transport is entered from
     * @param destination the tile it leaves the player on
     * @param objectId the id of the object to click
     * @param action the menu action to use
     * @return a builder to add requirements or other options to before building
     */
    public static TransportDefinitionBuilder object(String id, WorldPoint origin, WorldPoint destination,
                                                    int objectId, String action) {
        return builder()
                .id(id)
                .origin(origin)
                .destination(destination)
                .shape(TransportShape.SINGLE_CLICK)
                .entityId(objectId)
                .menuOption(action);
    }

    /**
     * Starts a definition crossed by one click on an NPC, such as a boatman who travels straight
     * away.
     *
     * @param id the definition's stable key
     * @param origin the tile the transport is entered from
     * @param destination the tile it leaves the player on
     * @param npcName the name of the NPC to click
     * @param action the menu action to use
     * @return a builder to add requirements or other options to before building
     */
    public static TransportDefinitionBuilder npc(String id, WorldPoint origin, WorldPoint destination,
                                                 String npcName, String action) {
        return builder()
                .id(id)
                .origin(origin)
                .destination(destination)
                .shape(TransportShape.SINGLE_CLICK)
                .menuTarget(npcName)
                .menuOption(action);
    }

    /**
     * Starts a definition crossed by clicking an NPC and then working through a conversation.
     *
     * @param id the definition's stable key
     * @param origin the tile the transport is entered from
     * @param destination the tile it leaves the player on
     * @param npcName the name of the NPC to click
     * @param action the menu action to use
     * @param dialogueOption the chat option to choose when it is offered; confirmations such as
     *                       {@code Yes} are accepted without being named
     * @return a builder to add requirements or other options to before building
     */
    public static TransportDefinitionBuilder npcDialogue(String id, WorldPoint origin, WorldPoint destination,
                                                         String npcName, String action, String dialogueOption) {
        return npc(id, origin, destination, npcName, action)
                .shape(TransportShape.CLICK_THEN_DIALOGUE)
                .displayInfo(dialogueOption);
    }

    /**
     * Starts a teleport that an item gives from anywhere, such as a tablet, a scroll or a piece of
     * charged jewellery. Holding one of the items becomes a requirement of the definition.
     *
     * <p>Add {@code menuOption} for the action on the item when it is not one of the usual teleport
     * actions, and {@code displayInfo} when a stop has to be chosen after it, such as
     * {@code Edgeville} on an amulet of glory.</p>
     *
     * @param id the definition's stable key
     * @param destination the tile the teleport leaves the player on
     * @param itemIds the items that each carry the teleport, for example every charge level
     * @return a builder to add the stop, requirements or other options to before building
     */
    public static TransportDefinitionBuilder itemTeleport(String id, WorldPoint destination, int... itemIds) {
        TransportDefinitionBuilder builder = builder()
                .id(id)
                .destination(destination)
                .shape(TransportShape.ITEM_SUBOP);
        if (itemIds != null) {
            for (int itemId : itemIds) {
                builder.itemId(itemId);
            }
        }
        return builder;
    }

    /**
     * Starts a teleport cast from the spellbook, usable from anywhere. The planner uses it only
     * while the client reports the spell as castable, so its runes, level and spellbook do not have
     * to be repeated as requirements.
     *
     * @param id the definition's stable key
     * @param destination the tile the spell leaves the player on
     * @param spellName the name of the spell as the spellbook shows it, such as
     *                  {@code Varrock Teleport}
     * @return a builder to add requirements or other options to before building
     */
    public static TransportDefinitionBuilder spellTeleport(String id, WorldPoint destination, String spellName) {
        return builder()
                .id(id)
                .destination(destination)
                .shape(TransportShape.SPELL)
                .displayInfo(spellName);
    }

    /**
     * Reports whether this is a teleport that can be used from wherever the player stands.
     *
     * @return true when the definition has no origin tile
     */
    public boolean isUsableAnywhere() {
        return origin == null;
    }

    /**
     * Returns who registered this definition, taken from the part of the id before the first colon.
     *
     * @return the owner, or the whole id when it has no colon
     */
    public String getOwner() {
        int separator = id.indexOf(':');
        return separator > 0 ? id.substring(0, separator) : id;
    }

    /**
     * Returns what to click in structured form. For a definition that states its click as one
     * string, the split between menu option and target name is a first guess, which the walker
     * corrects once it has found the object and knows its name.
     *
     * @return the click target, or null when the definition names none
     */
    public ObjectInfo toObjectInfo() {
        return objectInfo != null ? ObjectInfo.parse(objectInfo) : ObjectInfo.of(menuOption, menuTarget, entityId);
    }

    /**
     * Reports whether the click is stated as one string of option, target and id, as the bundled
     * dataset states a click it could not check against the game cache.
     *
     * @return true when the definition was given object info and no separate option, target or id
     */
    public boolean statesObjectInfo() {
        return objectInfo != null;
    }

    /**
     * Returns what to click as one string, for logs and overlays.
     *
     * @return the menu option, target and id joined by spaces, or null when the definition names none
     */
    public String getObjectInfo() {
        if (objectInfo != null) {
            return objectInfo;
        }
        ObjectInfo info = toObjectInfo();
        if (info == null) {
            return null;
        }
        String remainder = info.getRemainder().trim();
        return info.hasId() ? (remainder + " " + info.getId()).trim() : remainder;
    }

    @Override
    public String toString() {
        return "TransportDefinition(" + id + ": " + (origin != null ? origin : "anywhere") + " -> " + destination + ")";
    }

    /**
     * Builds a {@link TransportDefinition}. Lombok generates its methods, one per field; the class
     * is declared here so the factory methods above can name it as their return type.
     */
    public static class TransportDefinitionBuilder {
    }
}
