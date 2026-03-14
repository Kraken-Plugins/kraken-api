package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.widget.WidgetEntity;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class DepositBoxQuery extends AbstractQuery<DepositBoxEntity, DepositBoxQuery, ContainerItem> {

    public DepositBoxQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<DepositBoxEntity>> source() {
        return () -> {
            List<DepositBoxEntity> depositBoxItems = ctx.runOnClientThread(() -> {
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

            return depositBoxItems.stream();
        };
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