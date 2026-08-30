package com.kraken.api.service.walker.transport.handler;

import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.util.SleepService;
import com.kraken.api.service.walker.transport.DisplayInfo;
import com.kraken.api.service.walker.transport.HubWidgets;
import com.kraken.api.service.walker.transport.TransportContext;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import com.kraken.api.service.walker.transport.TransportHandler;
import com.kraken.api.service.walker.transport.WidgetClicks;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import shortestpath.transport.TransportType;

/**
 * Crosses a hub transport whose destinations are laid out on a dedicated interface.
 *
 * <p>Two layouts appear. Gliders, balloons and mushtrees give each destination its own component, so
 * the right one is named directly. Quetzals build their list at runtime, so the entry is found by
 * the text it shows. Either way the destination comes from the transport's display info, which the
 * dataset fills in per journey.</p>
 */
@Slf4j
public class WidgetSelectHandler implements TransportHandler {

    /** How long to wait for the destination interface to open. */
    private static final long INTERFACE_TIMEOUT_MS = 6_000;

    /** How long to wait for the journey to finish. */
    private static final long TRAVEL_TIMEOUT_MS = 20_000;

    /** How far the player must move for the journey to count as having happened. */
    private static final int TRAVEL_DISTANCE = 16;

    @Override
    public boolean execute(TransportContext context) {
        DisplayInfo displayInfo = DisplayInfo.parse(context.getDisplayInfo());
        if (displayInfo == null) {
            log.debug("Hub transport carries no destination: {}", context.getDisplayInfo());
            return false;
        }

        TransportType type = context.getType();
        WorldPoint before = context.playerLocation();

        if (!TransportEntityResolver.interact(context)) {
            return false;
        }

        Context ctx = context.getCtx();
        if (!SleepService.sleepUntil(() -> isOpen(ctx, type, displayInfo), INTERFACE_TIMEOUT_MS)) {
            log.debug("Destination interface for {} did not open", type);
            return false;
        }

        if (!select(ctx, type, displayInfo)) {
            log.debug("Could not select '{}' on the {} interface", displayInfo.getLabel(), type);
            return false;
        }

        return awaitTravel(context, before);
    }

    private boolean isOpen(Context ctx, TransportType type, DisplayInfo displayInfo) {
        if (HubWidgets.hasFixedComponents(type)) {
            int component = HubWidgets.component(type, displayInfo);
            if (component == HubWidgets.NOT_FOUND) {
                return false;
            }
            WidgetEntity widget = ctx.widgets().get(component);
            return widget != null && widget.raw() != null;
        }

        return findByText(ctx, type, displayInfo) != null;
    }

    private boolean select(Context ctx, TransportType type, DisplayInfo displayInfo) {
        if (HubWidgets.hasFixedComponents(type)) {
            int component = HubWidgets.component(type, displayInfo);
            if (component == HubWidgets.NOT_FOUND) {
                return false;
            }
            return click(ctx, ctx.widgets().get(component));
        }

        return click(ctx, findByText(ctx, type, displayInfo));
    }

    /**
     * Finds a runtime built list entry by the destination name it shows.
     */
    private WidgetEntity findByText(Context ctx, TransportType type, DisplayInfo displayInfo) {
        int group = HubWidgets.textInterfaceGroup(type);
        if (group == HubWidgets.NOT_FOUND) {
            return null;
        }

        String label = displayInfo.getLabel();
        if (label == null || label.isEmpty()) {
            return null;
        }

        return ctx.widgets().inGroup(group).withText(label).first().orElse(null);
    }

    private boolean click(Context ctx, WidgetEntity widget) {
        return WidgetClicks.click(ctx, widget);
    }

    private boolean awaitTravel(TransportContext context, WorldPoint before) {
        if (before == null) {
            SleepService.sleepFor(3);
            return true;
        }

        return SleepService.sleepUntil(() -> {
            WorldPoint now = context.playerLocation();
            return now != null && now.distanceTo(before) > TRAVEL_DISTANCE;
        }, TRAVEL_TIMEOUT_MS);
    }
}
