package com.kraken.api.core.interaction.resolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.MenuActionResolver;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import com.kraken.api.query.container.bank.BankItemWidget;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Optional;

@Slf4j
@Singleton
public class BankItemMenuActionResolver implements MenuActionResolver<BankItemWidget> {

    @Inject
    private Provider<Context> ctxProvider;

    @Override
    public Class<BankItemWidget> getEntityType() { return BankItemWidget.class; }

    @Override
    public Optional<ResolvedMenuAction> resolve(BankItemWidget item, String action) {
        if (action == null) return Optional.empty();

        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();

            int identifier = resolveIdentifier(item, action);
            MenuAction menuAction = identifier >= 6 ? MenuAction.CC_OP_LOW_PRIORITY : MenuAction.CC_OP;

            // Construct the target string with colors, exactly as the game engine logs it
            String target = "";
            if (item.getItemId() != -1) {
                ItemComposition comp = client.getItemDefinition(item.getItemId());
                target = "<col=ff9040>" + comp.getName() + "</col>";
            }

            return Optional.of(new ResolvedMenuAction(
                    new MenuOption(menuAction, identifier, item.getIndex(), item.getId(),
                            item.getItemId(), worldView),
                    target
            ));
        });
    }

    private int resolveIdentifier(BankItemWidget item, String action) {
        // First attempt: match against the action array on the item or its parent
        String[] actions = item.getActions();
        if (actions == null || actions.length == 0) {
            Widget parent = item.getParent();
            if (parent != null) {
                actions = parent.getActions();
            }
        }

        if (actions != null) {
            for (int i = 0; i < actions.length; i++) {
                if (ActionResolver.matches(action, actions[i])) {
                    return i + 1; // CC_OP indices are 1-based
                }
            }
        }

        // Fallback: hardcoded standard OSRS bank indices
        switch (Text.sanitize(action).toLowerCase()) {
            case "withdraw-1":
                return 2;
            case "withdraw-5":
                return 3;
            case "withdraw-10":
                return 4;
            case "withdraw-all":
                return 5;
            case "withdraw-x":
                return 6;
            case "withdraw-all-but-1":
                return 7;
            case "examine":
                return 8;
            // Dynamic left-click quantity (e.g. "Withdraw-50") not in the standard list
            // falls back to the default left-click option
            default:
                return 1;
        }
    }
}