package com.kraken.api.core.interaction.resolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.MenuActionResolver;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Optional;

@Slf4j
@Singleton
public class WidgetMenuActionResolver implements MenuActionResolver<Widget> {

    @Inject
    private Provider<Context> ctxProvider;

    @Override
    public Class<Widget> getEntityType() { return Widget.class; }

    @Override
    public Optional<ResolvedMenuAction> resolve(Widget widget, String action) {
        if (widget == null || !widget.isIf3()) {
            return Optional.empty();
        }

        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();

            Optional<MenuOption> option = resolveOption(client, widget, action, worldView);
            if (option.isEmpty()) {
                log.info("Failed to resolve widget interaction: id={}, action={}", widget.getId(), action);
                return Optional.empty();
            }

            String target = widget.getName();
            if (target == null || target.isBlank()) {
                target = widget.getText();
            }

            return Optional.of(new ResolvedMenuAction(option.get(), target == null ? "" : Text.removeTags(target)));
        });
    }

    private Optional<MenuOption> resolveOption(Client client, Widget widget, String action, int worldView) {
        // Widget-target-on-widget takes priority when a widget is already selected
        if (client.isWidgetSelected() && (action.equalsIgnoreCase("Use") || action.equalsIgnoreCase("Cast"))) {
            return Optional.of(new MenuOption(MenuAction.WIDGET_TARGET_ON_WIDGET, 0,
                    widget.getIndex(), widget.getId(), widget.getItemId(), worldView));
        }

        // Match against the widget's target verb (e.g. "Use", "Cast")
        if (ActionResolver.matches(action, widget.getTargetVerb()) || action.equalsIgnoreCase("Use")) {
            return Optional.of(new MenuOption(MenuAction.WIDGET_TARGET, 0,
                    widget.getIndex(), widget.getId(), widget.getItemId(), worldView));
        }

        // Match against the widget's action array
        Optional<MenuOption> widgetAction = ActionResolver.findAction(action, widget.getActions(), i ->
                new MenuOption(
                        i > widget.getTargetPriority() ? MenuAction.CC_OP_LOW_PRIORITY : MenuAction.CC_OP,
                        i + 1,
                        widget.getIndex(),
                        widget.getId(),
                        widget.getItemId(),
                        worldView
                )
        );

        if (widgetAction.isPresent()) {
            return widgetAction;
        }

        // Fall back to WIDGET_CONTINUE if the click mask permits it
        if ((widget.getClickMask() & 0x1) != 0) {
            return Optional.of(new MenuOption(MenuAction.WIDGET_CONTINUE, 0,
                    widget.getIndex(), widget.getId(), widget.getItemId(), worldView));
        }

        return Optional.empty();
    }
}