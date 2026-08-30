package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.query.ItemSource;
import com.kraken.api.query.container.AbstractContainerQuery;
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
public class DepositBoxQuery extends AbstractContainerQuery<DepositBoxEntity, DepositBoxQuery, ContainerItem> {

    private ItemSource dataSource = ItemSource.BOTH;

    public DepositBoxQuery(Context ctx) {
        super(ctx);
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
                entities.add(new DepositBoxEntity(ctx, new ContainerItem(new Item(widget.getItemId(), widget.getItemQuantity()), itemComposition, i, ctx, widget, ContainerItem.ItemOrigin.INVENTORY)));
            }
            return entities;
        });
    }

    private List<DepositBoxEntity> collectEquippedItems() {
        Widget container = ctx.getWidget(InterfaceID.BankDepositbox.FRAME);
        if(container == null) {
            return Collections.emptyList();
        }

        return ctx.runOnClientThread(() -> {
            List<DepositBoxEntity> entities = new ArrayList<>();
            for(int i = InterfaceID.BankDepositbox.SLOT0; i <= InterfaceID.BankDepositbox.SLOT13; i++) {
                final Widget widget = ctx.getWidget(i);

                if(widget.getDynamicChildren().length < 2) {
                    log.error("Expected widget id: {} to have 2 dynamic children but got: {}", i, widget.getDynamicChildren().length);
                    continue;
                }

                // Each widget has 2 dynamic children. The second child contains the item id. However, actions taken to deposit items
                // in the inventory require the parent widget not the dynamic child. i = the parent widget id (i.e SLOT0, SLOT1, etc...)
                int id = widget.getDynamicChildren()[1].getItemId();
                if (id == -1) continue;
                final ItemComposition itemComposition = ctx.getClient().getItemDefinition(id);
                entities.add(new DepositBoxEntity(ctx, new ContainerItem(new Item(id, 1), itemComposition, i, ctx, widget, ContainerItem.ItemOrigin.EQUIPMENT)));
            }

            return entities;
        });
    }

    /**
     * Returns the interactable equipment entity for a given equipment slot.
     * @param slot The {@code EquipmentInventorySlot} to retrieve.
     * @return The entity in that slot, or {@link Optional#empty()} when the slot is empty.
     */
    public Optional<DepositBoxEntity> inSlot(EquipmentInventorySlot slot) {
        // The container item returned from the raw().getSlot() will be the BankDepositBox.SLOT0,SLOT1,SLOT2, etc...
        // so in order to lookup an equipment slot 1-13 -> Id 12582924-31 we need to map the deposit box widget
        // otherwise we are comparing apples (1-13) to oranges (12582924-31)
        DepositBoxService depositBoxService = ctx.getService(DepositBoxService.class);
        int mappedIndex = depositBoxService.getDepositBoxWidget(slot);

        return source().get()
                .filter(i -> i.raw().getSlot() == mappedIndex)
                .findFirst();
    }
}