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
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;

import java.util.Optional;

@Slf4j
@Singleton
public class PlayerMenuActionResolver implements MenuActionResolver<Player> {

    private static final MenuAction[] PLAYER_ACTIONS = {
            MenuAction.PLAYER_FIRST_OPTION,
            MenuAction.PLAYER_SECOND_OPTION,
            MenuAction.PLAYER_THIRD_OPTION,
            MenuAction.PLAYER_FOURTH_OPTION,
            MenuAction.PLAYER_FIFTH_OPTION,
            MenuAction.PLAYER_SIXTH_OPTION,
            MenuAction.PLAYER_SEVENTH_OPTION,
            MenuAction.PLAYER_EIGHTH_OPTION
    };

    @Inject
    private Provider<Context> ctxProvider;

    @Override
    public Class<Player> getEntityType() { return Player.class; }

    @Override
    public Optional<ResolvedMenuAction> resolve(Player player, String action) {
        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();
            LocalPoint point = player.getLocalLocation();

            if (client.isWidgetSelected() && action.equalsIgnoreCase("Use")) {
                return Optional.of(new ResolvedMenuAction(
                        new MenuOption(MenuAction.WIDGET_TARGET_ON_PLAYER, player.getId(),
                                point.getSceneX(), point.getSceneY(), -1, worldView),
                        player.getName() == null ? "" : player.getName()
                ));
            }

            return ActionResolver.findAction(action, client.getPlayerOptions(), i ->
                    i < PLAYER_ACTIONS.length
                            ? new MenuOption(PLAYER_ACTIONS[i], player.getId(),
                            point.getSceneX(), point.getSceneY(), -1, worldView)
                            : null
            ).map(opt -> new ResolvedMenuAction(opt, player.getName() == null ? "" : player.getName()));
        });
    }
}