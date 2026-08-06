package com.kraken.api.query.world;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.util.SleepService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.gameval.InterfaceID;

@Slf4j
public class WorldEntity extends AbstractEntity<World> {

    @Getter
    private final net.runelite.http.api.worlds.World httpPackageWorld;

    public WorldEntity(Context ctx, World raw, net.runelite.http.api.worlds.World httpPackageWorld) {
        super(ctx, raw);
        this.httpPackageWorld = httpPackageWorld;
    }

    @Override
    public String getName() {
        World w = raw();
        return w != null ? "w" + w.getId() : null;
    }

    @Override
    public int getId() {
        World w = raw();
        return w != null ? w.getId() : -1;
    }

    @Override
    public boolean interact(String action) {
        // runOnClientThread returns null when it times out, so every result is compared rather than
        // unboxed — an unboxing NPE here would surface as a failure in the caller's world hop.
        boolean isLoginScreen = Boolean.TRUE.equals(
                ctx.runOnClientThread(() -> ctx.getClient().getGameState() == GameState.LOGIN_SCREEN));
        if(!isLoginScreen) {
            boolean worldHopperNotOpen = Boolean.TRUE.equals(
                    ctx.runOnClientThread(() -> ctx.widgets().get(InterfaceID.Worldswitcher.BUTTONS) == null));
            if(worldHopperNotOpen) {
                ctx.runOnClientThread(() -> ctx.getClient().openWorldHopper());
                boolean opened = SleepService.sleepUntilTicks(() -> Boolean.TRUE.equals(
                        ctx.runOnClientThread(() -> ctx.widgets().get(InterfaceID.Worldswitcher.BUTTONS) != null)),
                        4
                );

                if (!opened) {
                    log.error("Timed out waiting for World Hopper to open.");
                    return false;
                }

            }

            return ctx.runOnClientThread(() -> {
                WidgetEntity widget = ctx.widgets()
                        .withId(InterfaceID.Worldswitcher.BUTTONS)
                        .nameContains(String.valueOf(getId())).first();

                if(widget == null) {
                    log.error("world widget: {} is null", getId());
                    return false;
                }
                return widget.interact("Switch");
            });
        } else {
            return ctx.runOnClientThread(() -> {
                Client client = ctx.getClient();
                client.changeWorld(raw());
                client.hopToWorld(raw());
                return true;
            });
        }
    }

    /**
     * Returns true if the account is a member and false otherwise
     * @return True if the account is a member and false otherwise
     */
    public boolean isMember() {
        return membershipDaysRemaining() > 0;
    }

    /**
     * Returns the number of days of membership the account has left.
     * @return int membership duration in days.
     */
    public int membershipDaysRemaining() {
        return ctx.getVarpValue(1780); // Membership varp value is 1780
    }

    /**
     * Attempts to perform a world hop for the current {@code WorldEntity}.
     * <p>
     * This method interacts with the RuneLite client to hop to the target world associated
     * with this {@code WorldEntity}. Depending on the client's state, it may handle login
     * screen transitions or directly use the world hopper interface to complete the action.
     * This operation may require the world hopper plugin to be enabled.
     * </p>
     *
     * @return {@code true} if the world hop was successfully performed; {@code false} otherwise.
     */
    public boolean hop() {
        return this.interact("");
    }
}
