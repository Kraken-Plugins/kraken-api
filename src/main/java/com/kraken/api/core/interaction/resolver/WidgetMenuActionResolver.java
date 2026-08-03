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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class WidgetMenuActionResolver implements MenuActionResolver<Widget> {

    private static final int EQUIPMENT_GROUP_ID = 387;
    private static final int EQUIPMENT_HEAD_CHILD_ID = 15;
    private static final int EQUIPMENT_CAPE_CHILD_ID = 16;
    private static final int EQUIPMENT_AMULET_CHILD_ID = 17;
    private static final int EQUIPMENT_WEAPON_CHILD_ID = 18;
    private static final int EQUIPMENT_BODY_CHILD_ID = 19;
    private static final int EQUIPMENT_SHIELD_CHILD_ID = 20;
    private static final int EQUIPMENT_LEGS_CHILD_ID = 21;
    private static final int EQUIPMENT_GLOVES_CHILD_ID = 22;
    private static final int EQUIPMENT_BOOTS_CHILD_ID = 23;
    private static final int EQUIPMENT_RING_CHILD_ID = 24;
    private static final int EQUIPMENT_AMMO_CHILD_ID = 25;
    private static final int EQUIPMENT_REMOVE_OP = 1;

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

                // Could be controversial but wield and wear are very similar and users may easily get them mixed up
                // but their intentions are clear: equip some gear. So if we fail to resolve an action but
                // we can reasonably infer that the user want's to wield gear then this will auto-resolve to the right
                // action if its present on the item.
                if(action.equalsIgnoreCase("wield") || action.equalsIgnoreCase("wear")) {
                    List<String> sanitizedActions = Arrays.stream(Objects.requireNonNull(widget.getActions()))
                            .filter(Objects::nonNull)
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());

                    if(action.equalsIgnoreCase("wear") && sanitizedActions.contains("wield")) {
                        option = resolveOption(client, widget, "wield", worldView);
                    } else if(action.equalsIgnoreCase("wield") && sanitizedActions.contains("wear")) {
                        option = resolveOption(client, widget, "wear", worldView);
                    }

                    // Still can't resolve, give up.
                    if(option.isEmpty()) {
                        log.info("Failed to resolve widget interaction (wield/wear inference): id={}, action={}, available actions={}", widget.getId(), action, widget.getActions());
                        return Optional.empty();
                    }

                    String target = widget.getName();
                    if (target == null || target.isBlank()) {
                        target = widget.getText();
                    }

                    return Optional.of(new ResolvedMenuAction(option.get(), target == null ? "" : Text.removeTags(target)));
                }

                log.info("Failed to resolve widget interaction: id={}, action={}, available actions={}", widget.getId(), action, widget.getActions());
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
        boolean targetable = widget.getTargetVerb() != null && !Text.sanitize(widget.getTargetVerb()).isBlank();
        if (targetable && (ActionResolver.matches(action, widget.getTargetVerb()) || action.equalsIgnoreCase("Use"))) {
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

        if (action.equalsIgnoreCase("remove") && isEquipmentSlotWidget(widget)) {
            return Optional.of(new MenuOption(MenuAction.CC_OP, EQUIPMENT_REMOVE_OP,
                    widget.getIndex(), widget.getId(), widget.getItemId(), worldView));
        }

        // Fall back to WIDGET_CONTINUE if the click mask permits it
        if ((widget.getClickMask() & 0x1) != 0) {
            return Optional.of(new MenuOption(MenuAction.WIDGET_CONTINUE, 0,
                    widget.getIndex(), widget.getId(), widget.getItemId(), worldView));
        }

        return Optional.empty();
    }

    private boolean isEquipmentSlotWidget(Widget widget) {
        if ((widget.getId() >>> 16) != EQUIPMENT_GROUP_ID) {
            return false;
        }

        int childId = widget.getId() & 0xFFFF;
        switch (childId) {
            case EQUIPMENT_HEAD_CHILD_ID:
            case EQUIPMENT_CAPE_CHILD_ID:
            case EQUIPMENT_AMULET_CHILD_ID:
            case EQUIPMENT_WEAPON_CHILD_ID:
            case EQUIPMENT_BODY_CHILD_ID:
            case EQUIPMENT_SHIELD_CHILD_ID:
            case EQUIPMENT_LEGS_CHILD_ID:
            case EQUIPMENT_GLOVES_CHILD_ID:
            case EQUIPMENT_BOOTS_CHILD_ID:
            case EQUIPMENT_RING_CHILD_ID:
            case EQUIPMENT_AMMO_CHILD_ID:
                return true;
            default:
                return false;
        }
    }
}
