package com.kraken.api.service.walker.transport.registry;

import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.TransportShape;
import lombok.Builder;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * One transport the walker can plan through and operate: where it starts, where it leads, what the
 * player needs, and how it is crossed.
 *
 * <p>A definition is crossed in one of two ways. A declarative definition names a
 * {@link TransportShape} and what to click (menu option, target name, id), and the handler for that
 * shape operates it. A definition with its own {@link TransportHandler} runs that handler instead.
 * Only declarative definitions can be stored as data.</p>
 *
 * <p>Register a definition with {@link TransportRegistry#register(TransportDefinition)} and every
 * walk in the client plans through it from the next search on.</p>
 */
@Getter
@Builder
public final class TransportDefinition {

    /**
     * The shapes whose built in handlers work from a click target and display info alone, so a
     * definition can use them without bringing a handler.
     */
    public static final Set<TransportShape> DECLARATIVE_SHAPES = Collections.unmodifiableSet(EnumSet.of(
            TransportShape.SINGLE_CLICK,
            TransportShape.CLICK_THEN_DIALOGUE,
            TransportShape.HUB_DIALOGUE,
            TransportShape.HUB_RESUME_PAUSE,
            TransportShape.FAIRY_RING));

    /** World coordinates run from zero up to, but not including, this value on both axes. */
    private static final int WORLD_SIZE = 16384;

    /** The game has four planes, numbered from zero. */
    private static final int PLANES = 4;

    /**
     * Stable key written as {@code owner:name}, for example {@code my-plugin:temple-entrance}.
     * Registering the same id again replaces the earlier definition.
     */
    private final String id;

    /** The tile the transport is entered from. */
    private final WorldPoint origin;

    /** The tile the transport leaves the player on. */
    private final WorldPoint destination;

    /** The kind of transport, which decides the planning option that turns it on and off. */
    @Builder.Default
    private final TransportCategory category = TransportCategory.GENERAL;

    /**
     * How the transport is operated. Required unless a {@link #handler} is given.
     *
     * <p>The handlers for the {@link #DECLARATIVE_SHAPES} work from the click target and the display
     * info alone. The handlers for the other shapes read detail only the dataset carries, so a
     * definition with one of those shapes needs a handler of its own or one registered for the
     * shape. A fairy ring defined here does not get the staff or diary rule the dataset's rings
     * get; state it in {@link #requirements} or {@link #availableWhen}.</p>
     */
    private final TransportShape shape;

    /** The menu action to use, such as {@code Open}. Blank lets the entity's own actions decide. */
    private final String menuOption;

    /** The name of the object or NPC to click. */
    private final String menuTarget;

    /** The id of the object or NPC to click, or {@link ObjectInfo#NO_ID} to find it by name. */
    @Builder.Default
    private final int entityId = ObjectInfo.NO_ID;

    /**
     * Names the destination for transports that offer a choice: the chat option of a conversation,
     * a numbered hub entry such as {@code "4: Grand Exchange"}, or a fairy ring code.
     */
    private final String displayInfo;

    /** How long the crossing takes in game ticks, used as its planning cost and arrival timeout. */
    @Builder.Default
    private final int duration = 1;

    /** Whether the transport can also be taken from the destination back to the origin. */
    private final boolean bidirectional;

    /**
     * Whether this definition is the authority for its edge. When true, the dataset's own transports
     * between the same two tiles are removed from every search, including searches where this
     * definition's requirements are not met.
     */
    private final boolean replaces;

    /** What the player needs before the transport can be used. */
    @Builder.Default
    private final DefinitionRequirements requirements = DefinitionRequirements.NONE;

    /**
     * Optional extra condition for what {@link #requirements} cannot express. It runs on the client
     * thread each time a search captures state, so it must be quick and must not block.
     */
    private final BooleanSupplier availableWhen;

    /** Optional handler that operates this transport in place of the handler for its shape. */
    private final TransportHandler handler;

    private TransportDefinition(String id,
                                WorldPoint origin,
                                WorldPoint destination,
                                TransportCategory category,
                                TransportShape shape,
                                String menuOption,
                                String menuTarget,
                                int entityId,
                                String displayInfo,
                                int duration,
                                boolean bidirectional,
                                boolean replaces,
                                DefinitionRequirements requirements,
                                BooleanSupplier availableWhen,
                                TransportHandler handler) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("a transport definition needs an id");
        }
        if (origin == null) {
            throw new IllegalArgumentException(id + ": an origin tile is required; "
                    + "transports usable from anywhere are not supported yet");
        }
        if (destination == null) {
            throw new IllegalArgumentException(id + ": a destination tile is required");
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException(id + ": origin and destination are the same tile");
        }
        if (outsideWorld(origin) || outsideWorld(destination)) {
            throw new IllegalArgumentException(id + ": origin and destination must be world tiles on planes 0 to 3");
        }
        if (duration < 0) {
            throw new IllegalArgumentException(id + ": duration cannot be negative");
        }
        if (handler == null && shape == null) {
            throw new IllegalArgumentException(id + ": give a shape to use a built in handler, or a handler");
        }

        this.id = id.trim();
        this.origin = origin;
        this.destination = destination;
        this.category = category != null ? category : TransportCategory.GENERAL;
        this.shape = shape;
        this.menuOption = menuOption;
        this.menuTarget = menuTarget;
        this.entityId = entityId;
        this.displayInfo = displayInfo;
        this.duration = duration;
        this.bidirectional = bidirectional;
        this.replaces = replaces;
        this.requirements = requirements != null ? requirements : DefinitionRequirements.NONE;
        this.availableWhen = availableWhen;
        this.handler = handler;

        if (handler == null && toObjectInfo() == null) {
            throw new IllegalArgumentException(id + ": name what to click with a menu target or an entity id");
        }
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
     * Returns who registered this definition, taken from the part of the id before the first colon.
     *
     * @return the owner, or the whole id when it has no colon
     */
    public String getOwner() {
        int separator = id.indexOf(':');
        return separator > 0 ? id.substring(0, separator) : id;
    }

    /**
     * Returns what to click in structured form.
     *
     * @return the click target, or null when the definition names none
     */
    public ObjectInfo toObjectInfo() {
        return ObjectInfo.of(menuOption, menuTarget, entityId);
    }

    /**
     * Returns what to click as the dataset writes it, for logs and overlays.
     *
     * @return the menu option, target and id joined by spaces, or null when the definition names none
     */
    public String getObjectInfo() {
        ObjectInfo info = toObjectInfo();
        if (info == null) {
            return null;
        }
        String remainder = info.getRemainder().trim();
        return info.hasId() ? (remainder + " " + info.getId()).trim() : remainder;
    }

    @Override
    public String toString() {
        return "TransportDefinition(" + id + ": " + origin + " -> " + destination + ")";
    }

    /**
     * Builds a {@link TransportDefinition}. Lombok generates its methods, one per field; the class
     * is declared here so the factory methods above can name it as their return type.
     */
    public static class TransportDefinitionBuilder {
    }
}
