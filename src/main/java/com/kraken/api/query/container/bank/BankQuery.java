package com.kraken.api.query.container.bank;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.service.bank.BankService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
public class BankQuery extends AbstractQuery<BankEntity, BankQuery, BankItemWidget> {

    // Item compositions are global and immutable, so the cache is shared across all BankQuery
    // instances rather than being rebuilt and discarded on every ctx.bank() call.
    private static final Cache<Integer, ItemComposition> ITEM_DEFS = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build();

    private int lastUpdateTick = -1;
    private List<BankItemWidget> cachedItems = Collections.emptyList();

    public BankQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<BankEntity>> source() {
        return () -> {
            List<BankItemWidget> bankItems = ctx.runOnClientThread(() -> {
                // Rebuild at most once per tick; subsequent terminal ops within the same tick reuse the
                // cached snapshot instead of rebuilding (or, previously, silently returning an empty bank).
                if (lastUpdateTick >= ctx.getClient().getTickCount()) {
                    return cachedItems;
                }

                List<BankItemWidget> items = new ArrayList<>();
                int i = 0;
                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.BANK);
                if(container == null) {
                    return Collections.emptyList();
                }

                for (Item item : container.getItems()) {
                    try {
                        if (item == null) {
                            i++;
                            continue;
                        }

                        ItemComposition comp = ITEM_DEFS.get(item.getId(), () -> ctx.getItemManager().getItemComposition(item.getId()));
                        if (comp.getPlaceholderTemplateId() == 14401) {
                            i++;
                            continue;
                        }

                        if(comp.getName().equalsIgnoreCase("Bank filler")) {
                            i++;
                            continue;
                        }

                        items.add(new BankItemWidget(comp.getName(), item.getId(), item.getQuantity(), i, ctx));
                    } catch (NullPointerException | ExecutionException ex) {
                        log.error("exception thrown while attempting to get items from bank:", ex);
                    }
                    i++;
                }
                lastUpdateTick = ctx.getClient().getTickCount();
                cachedItems = items;
                return items;
            });

            return bankItems.stream().map(i -> new BankEntity(ctx, i));
        };
    }

    /**
     * Filters for items in the bank which have a specified item id.
     * @param id The item id to filter for
     * @return BankQuery
     */
    public BankQuery withId(int id) {
        return filter(item -> item.raw().getItemId() == id);
    }


    /**
     * Determines whether the bank interface is currently open.
     *
     * <p>This method interacts with the {@code BankService} to check the status of the bank interface.
     * The bank is considered open if the corresponding interface is visible and active in the client.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        return ctx.getService(BankService.class).isOpen();
    }
}