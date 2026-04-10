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
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.LocalPoint;

import java.util.Optional;

@Slf4j
@Singleton
public class NpcMenuActionResolver implements MenuActionResolver<NPC> {

    private static final MenuAction[] NPC_ACTIONS = {
            MenuAction.NPC_FIRST_OPTION,  MenuAction.NPC_SECOND_OPTION,
            MenuAction.NPC_THIRD_OPTION,  MenuAction.NPC_FOURTH_OPTION,
            MenuAction.NPC_FIFTH_OPTION
    };

    @Inject
    private Provider<Context> ctxProvider;

    @Override
    public Class<NPC> getEntityType() { return NPC.class; }

    @Override
    public Optional<ResolvedMenuAction> resolve(NPC npc, String action) {
        return ctxProvider.get().runOnClientThread(() -> {
            Client client = ctxProvider.get().getClient();
            int worldView = client.getTopLevelWorldView().getId();
            LocalPoint point = npc.getLocalLocation();

            if (client.isWidgetSelected() && action.equalsIgnoreCase("Use")) {
                return Optional.of(new ResolvedMenuAction(
                        new MenuOption(MenuAction.WIDGET_TARGET_ON_NPC, npc.getIndex(),
                                point.getSceneX(), point.getSceneY(), -1, worldView),
                        npc.getName() == null ? "" : npc.getName()
                ));
            }

            NPCComposition composition = Optional.ofNullable(npc.getTransformedComposition())
                    .orElse(npc.getComposition());

            if (composition == null) return Optional.empty();

            return ActionResolver.findAction(action, composition.getActions(), i ->
                    i < NPC_ACTIONS.length
                            ? new MenuOption(NPC_ACTIONS[i], npc.getIndex(),
                            point.getSceneX(), point.getSceneY(), -1, worldView)
                            : null
            ).map(opt -> new ResolvedMenuAction(opt, npc.getName() == null ? "" : npc.getName()));
        });
    }
}
