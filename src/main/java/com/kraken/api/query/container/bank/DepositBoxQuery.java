package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.ItemSource;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.bank.DepositBoxService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * Represents a query system for interacting with items stored in a deposit box interface.
 *
 * <p>This class provides functionality to filter and refine query results for items
 * in the deposit box, which is a dedicated inventory structure for depositing items.
 * It extends an abstract query mechanism, inheriting filtering and stream-processing
 * capabilities.
 *
 * <p>The {@code DepositBoxQuery} is context-aware and operates based on the current
 * client state. It retrieves items and their corresponding details (such as noted
 * state, stackability, and quantity) directly from the deposit box widget in the client UI.
 *
 * <p>Supported features include:
 * <ul>
 *   <li>Filtering items by their ID.</li>
 *   <li>Filtering items by noted or unnoted state.</li>
 *   <li>Filtering items by stackable properties.</li>
 *   <li>Filtering items by quantity thresholds.</li>
 * </ul>
 * <p>The query operations are chainable, allowing for the combination of multiple filters.
 * This enables flexible and precise selection of deposit box items to meet various needs.
 *
 * <p><b>Thread Safety:</b> This class interacts with client UI components and should be used in
 * the appropriate thread context (e.g., client-side thread) to ensure proper behavior.
 */
@Slf4j
public class DepositBoxQuery extends AbstractQuery<DepositBoxEntity, DepositBoxQuery, ContainerItem> {

    private ItemSource dataSource = ItemSource.BOTH;
    private Map<Integer, Integer> equipmentSlotWidgetMapping = new HashMap<>();

    public DepositBoxQuery(Context ctx) {
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
    }

    /**
     * Configures the query to only look for depositable items currently
     * inside the player's inventory.
     * @return DepositBoxQuery
     */
    public DepositBoxQuery inInventory() {
        this.dataSource = ItemSource.INVENTORY_ONLY;
        return this;
    }

    /**
     * Configures the query to only look for items to deposit into the deposit box currently equipped
     * on the player.
     * @return DepositBoxQuery
     */
    public DepositBoxQuery inEquipment() {
        this.dataSource = ItemSource.INTERFACE_ONLY;
        return this;
    }

    /**
     * Configures the query to look at both equipped items and wearable items
     * in the inventory.
     * @return EquipmentQuery
     */
    public DepositBoxQuery all() {
        this.dataSource = ItemSource.BOTH;
        return this;
    }

    @Override
    protected Supplier<Stream<DepositBoxEntity>> source() {
        return () -> {
            List<DepositBoxEntity> entities = new ArrayList<>();
            if (dataSource == ItemSource.INVENTORY_ONLY || dataSource == ItemSource.BOTH) {
                entities.addAll(collectInventoryItems());
            }

            if (dataSource == ItemSource.INTERFACE_ONLY || dataSource == ItemSource.BOTH) {
                entities.addAll(collectEquippedItems());
            }

            return entities.stream();
        };
    }

    private List<DepositBoxEntity> collectInventoryItems() {
        return ctx.runOnClientThread(() -> {
            WidgetEntity depositBox = ctx.widgets().fromClient(InterfaceID.BankDepositbox.INVENTORY);

            if(depositBox == null || !depositBox.isVisible()) {
                return Collections.emptyList();
            }

            Widget[] depositBoxWidgets = depositBox.raw().getDynamicChildren();
            List<DepositBoxEntity> entities = new ArrayList<>();
            for (int i = 0; i < depositBoxWidgets.length; i++) {
                final Widget widget = depositBoxWidgets[i];
                if (widget.getItemId() == -1 || widget.getItemId() == 6512) continue;
                final ItemComposition itemComposition = ctx.getClient().getItemDefinition(widget.getItemId());
                entities.add(new DepositBoxEntity(ctx, new ContainerItem(new Item(widget.getItemId(), widget.getItemQuantity()), itemComposition, i, ctx, widget, null)));
            }
            return entities;
        });
    }

    private List<DepositBoxEntity> collectEquippedItems() {
        return ctx.runOnClientThread(() -> {
            WidgetEntity container = ctx.widgets().fromClient(InterfaceID.BankDepositbox.WORN);
            if(container == null) {
                return Collections.emptyList();
            }

            List<DepositBoxEntity> entities = new ArrayList<>();
            for(int i = InterfaceID.BankDepositbox.SLOT0; i < InterfaceID.BankDepositbox.SLOT13; i++) {
                final Widget widget = ctx.getWidget(i);

                if(widget.getDynamicChildren().length < 2) {
                    log.error("Expected widget id: {} to have 2 dynamic children but got: {}", i, widget.getDynamicChildren().length);
                    continue;
                }

                // Each widget has 2 dynamic children. The second child contains the item id. However, actions take to deposit items
                // in the inventory require the parent widget not the dynamic child.
                int id = widget.getDynamicChildren()[1].getItemId();
                if (widget.getItemId() == -1) continue;
                final ItemComposition itemComposition = ctx.getClient().getItemDefinition(id);
                log.info("Found item: {} with id: {} in widget slot: {} for equipment", widget.getName(), id, i);
                entities.add(new DepositBoxEntity(ctx, new ContainerItem(new Item(id, 1), itemComposition, i, ctx, widget, null)));
            }
            return entities;
        });
    }

    /**
     * Returns the interactable equipment entity for a given equipment slot.
     * @param slot The {@code EquipmentInventorySlot} to retrieve.
     * @return DepositBoxEntity
     */
    public DepositBoxEntity inSlot(EquipmentInventorySlot slot) {
        // The container item returned from the raw().getSlot() will be the BankDepositBox.SLOT0,1,2, etc...
        // so in order to lookup an equipment slot 1-13 -> Id 12582924-31 we need to map the deposit box widget
        // otherwise we are comparing apples (1-13) to oranges (12582924-31)
        DepositBoxService depositBoxService = ctx.getService(DepositBoxService.class);
        int mappedIndex = depositBoxService.getDepositBoxWidget(slot);

        return ctx.runOnClientThread(() -> collectEquippedItems()
                .stream()
                .filter(i -> i.raw().getSlot() == mappedIndex)
                .findFirst()
                .orElse(new DepositBoxEntity(ctx, null)));
    }

    /**
     * Filters for items in the deposit box that have the specified item id.
     *
     * <p>This method is used to refine the {@code DepositBoxQuery} by including only items
     * within the deposit box that match the provided item id. It applies a filtering condition
     * to the query and returns the updated query object.
     *
     * @param id The item id to filter for.
     *           <ul>
     *             <li>The id must represent a valid item within the deposit box.</li>
     *             <li>If the id does not correspond to any item, the result will be an empty query.</li>
     *           </ul>
     * @return {@literal DepositBoxQuery} The updated {@code DepositBoxQuery} instance with the specified filter applied.
     */
    public DepositBoxQuery withId(int id) {
        return filter(item -> item.getId() == id);
    }

    /**
     * Filters the {@code DepositBoxQuery} to include only items that are noted.
     *
     * <p>An item is considered "noted" if it is a placeholder or tradeable voucher
     * referring to a stackable quantity of the item rather than the physical item itself.
     * This method relies on the {@code isNoted()} method of the underlying item's composition
     * to determine its noted state.
     *
     * @return {@literal DepositBoxQuery} A refined query instance including only the items
     *         from the deposit box that are in a noted state.
     */
    public DepositBoxQuery noted() {
        return filter(item -> item.raw().isNoted());
    }

    /**
     * Filters the {@code DepositBoxQuery} to include only items that are unnoted.
     *
     * <p>An item is considered "unnoted" if it is the actual physical item
     * rather than a placeholder or tradeable voucher for a stackable quantity.
     * This method relies on the {@code isNoted()} method of the raw item to
     * determine its state, and excludes items that are noted.
     *
     * @return {@literal DepositBoxQuery} A refined query instance including only the
     *         items from the deposit box that are in an unnoted state.
     */
    public DepositBoxQuery unnoted() {
        return filter(item -> !item.raw().isNoted());
    }

    /**
     * Filters the {@code DepositBoxQuery} to include only items that are stackable.
     *
     * <p>This method refines the {@code DepositBoxQuery} by applying a filtering condition
     * that selects items from the deposit box which are deemed stackable. Stackable items
     * allow multiple units to occupy a single inventory slot.
     *
     * <ul>
     *   <li>An item is considered stackable if its {@code isStackable()} method returns {@code true}.</li>
     *   <li>If no stackable items are found, the result of this query will be empty.</li>
     * </ul>
     *
     * @return {@literal DepositBoxQuery} A refined {@code DepositBoxQuery} instance that
     *         includes only the stackable items in the deposit box.
     */
    public DepositBoxQuery stackable() {
        return filter(item -> item.raw().isStackable());
    }

    /**
     * Filters the {@code DepositBoxQuery} to include only items with a quantity greater than the specified amount.
     *
     * <p>This method refines the {@code DepositBoxQuery} by applying a filtering condition that selects
     * items from the deposit box where their quantity exceeds the given value. The resulting query will
     * only include items meeting this criteria.
     *
     * <ul>
     *   <li>If no items in the deposit box have a quantity greater than the provided amount, the query result will be empty.</li>
     * </ul>
     *
     * @param amount The minimum quantity threshold for filtering items.
     *               <ul>
     *                 <li>Must be a non-negative integer.</li>
     *                 <li>If {@code amount} is zero, all items with a positive quantity will be included in the result.</li>
     *               </ul>
     * @return {@literal DepositBoxQuery} An updated query instance including only items whose quantity
     *                                    exceeds the specified amount.
     */
    public DepositBoxQuery quantityGreaterThan(int amount) {
        return filter(item -> item.raw().getQuantity() > amount);
    }
}