package com.kraken.api.core.interaction.resolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.MenuActionResolver;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import com.kraken.api.query.groundobject.GroundItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;

import java.util.Optional;

@Slf4j
@Singleton
public class GroundItemMenuActionResolver implements MenuActionResolver<GroundItem> {

    private static final MenuAction[] GROUND_ITEM_ACTIONS = {
            MenuAction.GROUND_ITEM_FIRST_OPTION,
            MenuAction.GROUND_ITEM_SECOND_OPTION,
            MenuAction.GROUND_ITEM_THIRD_OPTION,
            MenuAction.GROUND_ITEM_FOURTH_OPTION,
            MenuAction.GROUND_ITEM_FIFTH_OPTION
    };

    @Inject
    private Provider<Context> ctxProvider;

    @Override
    public Class<GroundItem> getEntityType() { return GroundItem.class; }

    @Override
    public Optional<ResolvedMenuAction> resolve(GroundItem item, String action) {
        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();

            LocalPoint point = resolveLocalPoint(item);
            if (point == null) {
                log.info("Failed to resolve local point for ground item: id={}", item.getId());
                return Optional.empty();
            }

            if (client.isWidgetSelected() && action.equalsIgnoreCase("Use")) {
                return Optional.of(new ResolvedMenuAction(
                        new MenuOption(MenuAction.WIDGET_TARGET_ON_GROUND_ITEM, item.getTileItem().getId(),
                                point.getSceneX(), point.getSceneY(), -1, worldView),
                        item.getName() == null ? "" : item.getName()
                ));
            }

            // The OSRS client hardcodes "Take" as the 3rd option (index 2).
            // It is rarely present in the actual ItemComposition ground actions array.
            // Bury is also supported since some newer objects (like Brutus's bones aren't "Takeable" and can be directly buried).
            if (action.equalsIgnoreCase("Take") || action.equalsIgnoreCase("Bury")) {
                return Optional.of(new ResolvedMenuAction(
                        new MenuOption(MenuAction.GROUND_ITEM_THIRD_OPTION, item.getTileItem().getId(),
                                point.getSceneX(), point.getSceneY(), -1, worldView),
                        item.getName() == null ? "" : item.getName()
                ));
            }

            return ActionResolver.findAction(action, GroundItem.getGroundItemActions(item.getItemComposition()), i ->
                    i < GROUND_ITEM_ACTIONS.length
                            ? new MenuOption(GROUND_ITEM_ACTIONS[i], item.getTileItem().getId(),
                            point.getSceneX(), point.getSceneY(), -1, worldView)
                            : null
            ).map(opt -> new ResolvedMenuAction(opt, item.getName() == null ? "" : item.getName()));
        });
    }

    private LocalPoint resolveLocalPoint(GroundItem item) {
        TileObject tileObject = item.getTileObject();
        if (tileObject != null) {
            return tileObject.getLocalLocation();
        }
        return LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), item.getLocation());
    }
}
