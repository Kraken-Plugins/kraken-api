package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.ItemSource;
import com.kraken.api.query.container.ContainerItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Optional;


/**
 * Queries the items the player is wearing and the wearable items in the inventory. Both sources are included
 * by default; {@link #inInventory()}, {@link #inInterface()} and {@link #all()} narrow or reset the source.
 */
@Slf4j
public class EquipmentQuery extends AbstractQuery<EquipmentEntity, EquipmentQuery, ContainerItem> {

    private final Map<Integer, Integer> equipmentSlotWidgetMapping = new HashMap<>();
    private final Map<Integer, Integer> intMapping = new HashMap<>();

    private ItemSource dataSource = ItemSource.BOTH;

    // collectEquippedItems runs 11 client scripts to populate the equipment widgets; source(),
    // isWearing() and inSlot() all call it. Equipment cannot change within a tick, so the result is
    // reused across those calls on this instance for the current tick.
    private int equippedItemsTick = -1;
    private List<EquipmentEntity> cachedEquippedItems;

    /**
     * Creates a query over worn and wearable items, including both sources by default.
     *
     * @param ctx The context used to read the equipment and inventory
     */
    public EquipmentQuery(Context ctx) {
        super(ctx);
        equipmentSlotWidgetMapping.put(0, 15);
        equipmentSlotWidgetMapping.put(1, 16);
        equipmentSlotWidgetMapping.put(2, 17);
        equipmentSlotWidgetMapping.put(3, 18);
        equipmentSlotWidgetMapping.put(4, 19);
        equipmentSlotWidgetMapping.put(5, 20);
        equipmentSlotWidgetMapping.put(7, 21);
        equipmentSlotWidgetMapping.put(9, 22);
        equipmentSlotWidgetMapping.put(10, 23);
        equipmentSlotWidgetMapping.put(12, 24);
        equipmentSlotWidgetMapping.put(13, 25);
        intMapping.put(0, 0);
        intMapping.put(1, 1);
        intMapping.put(2, 2);
        intMapping.put(3, 3);
        intMapping.put(4, 4);
        intMapping.put(5, 5);
        intMapping.put(6, 7);
        intMapping.put(7, 9);
        intMapping.put(8, 10);
        intMapping.put(9, 12);
        intMapping.put(10, 13);
    }


    /**
     * Configures the query to only look for wearable/wieldable items currently
     * inside the player's inventory.
     * @return EquipmentQuery
     */
    public EquipmentQuery inInventory() {
        this.dataSource = ItemSource.INVENTORY_ONLY;
        return this;
    }

    /**
     * Configures the query to only look for items currently equipped on the player. Items come from the worn item
     * container, so every worn item is found even when the equipment interface is not loaded; such items cannot be
     * interacted with (for example removed) until it is.
     * @return EquipmentQuery
     */
    public EquipmentQuery inInterface() {
        this.dataSource = ItemSource.INTERFACE_ONLY;
        return this;
    }

    /**
     * Configures the query to look at both equipped items and wearable items
     * in the inventory.
     * @return EquipmentQuery
     */
    public EquipmentQuery all() {
        this.dataSource = ItemSource.BOTH;
        return this;
    }

    @Override
    protected Supplier<Stream<EquipmentEntity>> source() {
        return () -> {
            List<EquipmentEntity> entities = new ArrayList<>();
            if (dataSource == ItemSource.INVENTORY_ONLY || dataSource == ItemSource.BOTH) {
                entities.addAll(collectInventoryItems());
            }

            if (dataSource == ItemSource.INTERFACE_ONLY || dataSource == ItemSource.BOTH) {
                entities.addAll(collectEquippedItems());
            }

            return entities.stream();
        };
    }

    /**
     * Sources inventory items which are wieldable or wearable from the players inventory
     * @return List of equipment entities
     */
    private List<EquipmentEntity> collectInventoryItems() {
        ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

        ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
        if (container == null) return Collections.emptyList();

        Widget inventory = ctx.getClient().getWidget(149, 0);
        if (inventory == null) return Collections.emptyList();

        Widget[] inventoryWidgets = inventory.getDynamicChildren();
        List<EquipmentEntity> entities = new ArrayList<>();

        Item[] items = container.getItems();
        for (int i = 0; i < items.length; i++) {
            final Item item = items[i];
            if (item.getId() == -1 || item.getId() == 6512) continue;

            final ItemComposition def = ctx.getClient().getItemDefinition(item.getId());
            if (def == null || def.getInventoryActions() == null) continue;

            List<String> actions = Arrays.stream(def.getInventoryActions())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if(actions.contains("wield") || actions.contains("wear")) {
                Widget w = (i < inventoryWidgets.length) ? inventoryWidgets[i] : null;
                entities.add(new EquipmentEntity(ctx, new ContainerItem(item, def, i, ctx, w, ContainerItem.ItemOrigin.INVENTORY)));
            }
        }
        return entities;
    }

    /**
     * Sources equipped items from the worn item container, attaching each slot's equipment interface widget when it
     * is loaded so the item can be interacted with.
     * @return List of equipped item objects
     */
    private List<EquipmentEntity> collectEquippedItems() {
        int tick = ctx.getClient().getTickCount();
        if (tick == equippedItemsTick && cachedEquippedItems != null) {
            return cachedEquippedItems;
        }

        for (int i = 0; i < 11; i++) {
            ctx.getClient().runScript(545, (25362447 + i), intMapping.get(i), 1, 1, 2);
        }

        List<EquipmentEntity> entities = new ArrayList<>();
        ItemContainer equipment = ctx.getClient().getItemContainer(InventoryID.WORN);
        if (equipment == null) {
            cachedEquippedItems = entities;
            equippedItemsTick = tick;
            return entities;
        }

        Item[] items = equipment.getItems();
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            if (!isWornItem(item)) continue;

            // 387 = WidgetInfo.EQUIPMENT.getGroupId(). The widget is null while the equipment interface is not
            // loaded; the item is still worn, so it is kept and only its interactions fail.
            Integer widgetChild = equipmentSlotWidgetMapping.get(i);
            Widget w = widgetChild == null ? null : ctx.getClient().getWidget(387, widgetChild);

            final ItemComposition def = ctx.getClient().getItemDefinition(item.getId());

            // Create ContainerItem with EQUIPMENT origin
            // Pass slotIndex so we know which equipment slot this is
            entities.add(new EquipmentEntity(ctx, new ContainerItem(item, def, i, ctx, w, ContainerItem.ItemOrigin.EQUIPMENT)));
        }

        cachedEquippedItems = entities;
        equippedItemsTick = tick;
        return entities;
    }


    /**
     * Filters for equipment with a specific item id.
     * @param id Item id to filter for
     * @return EquipmentQuery
     */
    public EquipmentQuery withId(int id) {
        return filter(i -> i.raw().getId() == id);
    }

    /**
     * Checks if the player is wearing an item by id, reading the worn item container.
     * Note: This strictly checks the equipment slots, ignoring the "inInventory" setting.
     * @param id The item id for the equipment to check
     * @return True if the player is wearing the equipment and false otherwise
     */
    public boolean isWearing(int id) {
        return ctx.runOnClientThread(() -> wornItems().stream().anyMatch(item -> item.getId() == id));
    }

    /**
     * Checks if the player is wearing an item by name, reading the worn item container.
     * Note: This strictly checks the equipment slots, ignoring the "inInventory" setting.
     * @param name The name of the equipment to check
     * @return True if the player is wearing the equipment and false otherwise
     */
    public boolean isWearing(String name) {
        return ctx.runOnClientThread(() -> wornItems().stream()
                .anyMatch(item -> name.equalsIgnoreCase(ctx.getClient().getItemDefinition(item.getId()).getName())));
    }

    /**
     * Reads the ids of every worn item from the worn item container. Unlike the query this runs no client scripts
     * and does not need the equipment interface, so it is cheap enough to call every game tick.
     * @return A new set of the worn item ids, empty when nothing is worn or the container is not loaded.
     */
    public Set<Integer> wornIds() {
        return ctx.runOnClientThread(() -> {
            Set<Integer> ids = new HashSet<>();
            for (Item item : wornItems()) {
                ids.add(item.getId());
            }
            return ids;
        }, new HashSet<>());
    }

    /**
     * Reads the id of the item worn in a slot from the worn item container, without running client scripts or
     * needing the equipment interface.
     * @param slot The equipment slot to read.
     * @return The id of the item worn in the slot, or -1 when the slot is empty or the container is not loaded.
     */
    public int wornId(EquipmentInventorySlot slot) {
        if (slot == null) return -1;
        return ctx.runOnClientThread(() -> {
            ItemContainer worn = ctx.getClient().getItemContainer(InventoryID.WORN);
            Item item = worn == null ? null : worn.getItem(slot.getSlotIdx());
            return isWornItem(item) ? item.getId() : -1;
        }, -1);
    }

    /**
     * Reads the id of the wielded weapon from the worn item container. See {@link #wornId(EquipmentInventorySlot)}.
     * @return The id of the wielded weapon, or -1 when no weapon is wielded or the container is not loaded.
     */
    public int wornWeaponId() {
        return wornId(EquipmentInventorySlot.WEAPON);
    }

    /**
     * Lists the worn items in the worn item container. Must be called on the client thread.
     * @return The worn items, empty when the container is not loaded.
     */
    private List<Item> wornItems() {
        ItemContainer worn = ctx.getClient().getItemContainer(InventoryID.WORN);
        if (worn == null) return Collections.emptyList();

        List<Item> items = new ArrayList<>();
        for (Item item : worn.getItems()) {
            if (isWornItem(item)) items.add(item);
        }
        return items;
    }

    /**
     * @param item A slot of the worn item container.
     * @return True when the slot holds an item rather than nothing or the blank placeholder.
     */
    private static boolean isWornItem(Item item) {
        return item != null && item.getId() != -1 && item.getId() != ItemID.BLANKOBJECT;
    }

    /**
     * Returns the equipment entity worn in a given equipment slot. It can only be interacted with while the
     * equipment interface is loaded; see {@link #inInterface()}.
     * @param slot The {@code EquipmentInventorySlot} to retrieve.
     * @return The entity in that slot, or {@link Optional#empty()} when the slot is empty.
     */
    public Optional<EquipmentEntity> inSlot(EquipmentInventorySlot slot) {
        if (slot == null) return Optional.empty();
        return Optional.ofNullable(ctx.runOnClientThread(() -> collectEquippedItems()
                .stream()
                .filter(i -> i.raw() != null && i.raw().getSlot() == slot.getSlotIdx())
                .findFirst()
                .orElse(null)));
    }
}
