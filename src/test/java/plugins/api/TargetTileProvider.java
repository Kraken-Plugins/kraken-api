package plugins.api;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.coords.WorldPoint;

/**
 * Holds the destination tile that the movement, camera and pathfinder tests act on.
 *
 * <p>Those tests were written around a tile the user picked by shift right clicking "Set", and they
 * block for up to thirty seconds waiting for one. That is fine interactively and fatal for an
 * unattended run. Routing both sources through here lets the runner publish a tile before a test
 * starts — so the poll returns on its first iteration — while the manual pathway keeps working
 * unchanged whenever no suite is running.</p>
 *
 * <p>Both fields are volatile: they are written from the runner's worker thread and from menu
 * callbacks on the client thread, and read from whichever thread the test happens to run on.</p>
 */
@Setter
@Singleton
public class TargetTileProvider {

    /**
     *  Records a tile the user picked in game.
     *  The tile the user last picked, ignoring anything a suite published.
     */
    @Getter
    private volatile WorldPoint manualTile;

    /**
     *  Publishes a tile on behalf of a running suite. Takes precedence over any manual selection.
     */
    private volatile WorldPoint suiteTile;

    /**
     * The tile tests should act on.
     *
     * @return the suite supplied tile when a run is in progress, otherwise the manually picked one,
     *         or null when neither has been set
     */
    public WorldPoint get() {
        WorldPoint fromSuite = suiteTile;
        return fromSuite != null ? fromSuite : manualTile;
    }

    /**
     * Clears the suite supplied tile, handing control back to manual selection.
     */
    public void clearSuiteTile() {
        this.suiteTile = null;
    }
}
