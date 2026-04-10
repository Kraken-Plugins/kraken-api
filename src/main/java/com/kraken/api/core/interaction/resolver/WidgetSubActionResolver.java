package com.kraken.api.core.interaction.resolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;

import java.util.Optional;

/**
 * Resolves sub-action menu interactions — i.e. the nested option menu shown when
 * hovering over a right-click option (e.g. Ring of Dueling location selection).
 */
@Slf4j
@Singleton
public class WidgetSubActionResolver {

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Resolves a widget sub-action menu interaction, i.e. the nested option menu like Max cape teleports, games necklace teleports, or fairy ring sub options.
     * @param widget        The widget to interact with (e.g. Ring of Dueling in inventory)
     * @param primaryMenu   The primary action label (e.g. "Rub")
     * @param subActionName The sub-action label (e.g. "Fortis Colosseum")
     * @return The resolved sub-action menu as an optional
     */
    public Optional<ResolvedMenuAction> resolve(Widget widget, String primaryMenu, String subActionName) {
        if (widget == null || widget.getItemId() == -1) {
            return Optional.empty();
        }

        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();

            int primaryActionIndex = resolvePrimaryActionIndex(widget, primaryMenu);
            if (primaryActionIndex == -1) {
                log.error("Failed to resolve primary action '{}' on widget: {}", primaryMenu, widget.getId());
                return Optional.empty();
            }

            int subActionIndex = resolveSubActionIndex(client, widget.getItemId(), subActionName);
            if (subActionIndex == -1) {
                log.error("Failed to resolve sub-action '{}' for item: {}", subActionName, widget.getItemId());
                return Optional.empty();
            }

            // Pack the identifier: (primaryIndex << 16) | subActionIndex
            int identifier = (primaryActionIndex << 16) | subActionIndex;

            return Optional.of(new ResolvedMenuAction(
                    new MenuOption(MenuAction.CC_OP_LOW_PRIORITY, identifier,
                            widget.getIndex(), widget.getId(), widget.getItemId(), worldView),
                    subActionName
            ));
        });
    }

    private int resolvePrimaryActionIndex(Widget widget, String primaryMenu) {
        String[] actions = widget.getActions();
        if (actions == null) return -1;

        for (int i = 0; i < actions.length; i++) {
            if (ActionResolver.matches(primaryMenu, actions[i])) {
                return i + 1; // CC_OP primary indices are 1-based
            }
        }
        return -1;
    }

    private int resolveSubActionIndex(Client client, int itemId, String subActionName) {
        ItemComposition composition = client.getItemDefinition(itemId);
        String[][] subOps = composition.getSubops();
        if (subOps == null) return -1;

        for (String[] subOpArray : subOps) {
            if (subOpArray == null) continue;
            for (int i = 0; i < subOpArray.length; i++) {
                if (ActionResolver.matches(subActionName, subOpArray[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
}
