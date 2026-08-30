package com.kraken.api.query.container.bank;

import com.kraken.api.Context;
import com.kraken.api.query.container.AbstractContainerQuery;
import com.kraken.api.query.container.ContainerItem;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class BankInventoryQuery extends AbstractContainerQuery<BankInventoryEntity, BankInventoryQuery, ContainerItem> {

    public BankInventoryQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<BankInventoryEntity>> source() {
        // Each empty result builds its own stream. A shared one is consumed by the first terminal
        // operation and throws IllegalStateException on the next evaluation of the same query.
        ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

        ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
        if(container == null) return Stream::empty;

        // WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER
        Widget bankInventory = ctx.getClient().getWidget(0x000f_0003);
        if(bankInventory == null) return Stream::empty;

        Widget[] inventoryWidgets = bankInventory.getDynamicChildren();

        List<BankInventoryEntity> bankInventoryEntities = new ArrayList<>();
        for (int i = 0; i < container.getItems().length; i++) {
            final Item item = container.getItems()[i];
            if (item.getId() == -1 || item.getId() == 6512) continue;

            final ItemComposition itemComposition = ctx.runOnClientThreadOptional(() -> ctx.getClient().getItemDefinition(item.getId()))
                    .orElse(null);

            if (itemComposition == null) continue;

            Widget widget = null;
            if (i < inventoryWidgets.length) {
                widget = inventoryWidgets[i];
            }

            bankInventoryEntities.add(new BankInventoryEntity(ctx, new ContainerItem(item, itemComposition, i, ctx, widget, ContainerItem.ItemOrigin.BANK_INVENTORY)));
        }

        return bankInventoryEntities::stream;
    }
}