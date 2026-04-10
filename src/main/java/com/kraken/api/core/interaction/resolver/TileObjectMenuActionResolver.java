package com.kraken.api.core.interaction.resolver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.interaction.InteractionManager;
import com.kraken.api.core.interaction.MenuActionResolver;
import com.kraken.api.core.interaction.model.MenuOption;
import com.kraken.api.core.interaction.model.ResolvedMenuAction;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;

import java.util.Optional;

@Slf4j
@Singleton
public class TileObjectMenuActionResolver implements MenuActionResolver<TileObject> {

    private static final MenuAction[] GAME_OBJECT_ACTIONS = {
            MenuAction.GAME_OBJECT_FIRST_OPTION,  MenuAction.GAME_OBJECT_SECOND_OPTION,
            MenuAction.GAME_OBJECT_THIRD_OPTION,  MenuAction.GAME_OBJECT_FOURTH_OPTION,
            MenuAction.GAME_OBJECT_FIFTH_OPTION
    };

    @Inject
    private Provider<Context> ctxProvider;

    @Override
    public Class<TileObject> getEntityType() { return TileObject.class; }

    @Override
    public Optional<ResolvedMenuAction> resolve(TileObject object, String action) {
        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();

            // Multi-tile objects must use south-west tile
            int sceneX, sceneY;

            if (object instanceof GameObject) {
                GameObject go = (GameObject) object;
                sceneX = go.getSceneMinLocation().getX();
                sceneY = go.getSceneMinLocation().getY();
            } else {
                LocalPoint lp = object.getLocalLocation();
                sceneX = lp.getSceneX();
                sceneY = lp.getSceneY();
            }

            if (client.isWidgetSelected() && action.equalsIgnoreCase("Use")) {
                return Optional.of(new ResolvedMenuAction(
                        new MenuOption(MenuAction.WIDGET_TARGET_ON_GAME_OBJECT,
                                object.getId(), sceneX, sceneY, -1, worldView), ""
                ));
            }

            ObjectComposition composition = InteractionManager.getObjectComposition(client, object);
            if (composition == null) {
                log.warn("No composition for object id={}", object.getId());
                return Optional.empty();
            }

            String target = composition.getName() == null ? "" : composition.getName();
            int finalSceneX = sceneX, finalSceneY = sceneY;

            return ActionResolver.findAction(action, composition.getActions(), i ->
                    i < GAME_OBJECT_ACTIONS.length
                            ? new MenuOption(GAME_OBJECT_ACTIONS[i], object.getId(),
                            finalSceneX, finalSceneY, -1, worldView)
                            : null
            ).map(opt -> new ResolvedMenuAction(opt, target));
        });
    }
}
