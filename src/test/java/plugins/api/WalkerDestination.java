package plugins.api;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import plugins.api.world.NamedLocation;

/**
 * Named destinations {@code WalkerTest} can walk to without a shift-click.
 *
 * <p>These are walker-test tiles, not suite hubs. {@link NamedLocation} stays the catalogue of
 * places the rest of the harness banks and shops at. Where a hub already has an anchor, that tile
 * is reused rather than invented twice.</p>
 */
@Getter
public enum WalkerDestination {

    /** Wait for a shift-click, then the nearby tile the test declares so Run All stays local. */
    MANUAL("Manual (shift-click)", null),

    GRAND_EXCHANGE("Grand Exchange", NamedLocation.GRAND_EXCHANGE.getAnchor()),

    VARROCK_EAST_BANK("Varrock East Bank", NamedLocation.VARROCK_EAST_BANK.getAnchor()),

    /** Top floor of the castle. The walk has to climb stairs, which a courtyard tile does not. */
    LUMBRIDGE_CASTLE_BANK("Lumbridge Castle bank", new WorldPoint(3208, 3218, 2)),

    /** Just inside the gate, so a walk from Lumbridge has to go through it. */
    AL_KHARID("Al Kharid", new WorldPoint(3273, 3228, 0)),

    /** Musa Point docks. From the mainland this is a boat. */
    KARAMJA("Karamja (Musa Point)", new WorldPoint(2956, 3146, 0)),

    /** Outside the east bank. A F2P walk from Varrock that needs no transport. */
    FALADOR_EAST_BANK("Falador East Bank", new WorldPoint(3013, 3355, 0)),

    /** Village bank. A glory landing, and a short F2P walk from Lumbridge. */
    DRAYNOR_VILLAGE("Draynor Village", new WorldPoint(3092, 3245, 0)),

    /** Bank. A glory landing on the wilderness edge. */
    EDGEVILLE("Edgeville", new WorldPoint(3094, 3491, 0)),

    /** Next to the spirit tree. From the Grand Exchange this is a tree hop. */
    TREE_GNOME_STRONGHOLD("Tree Gnome Stronghold", new WorldPoint(2461, 3444, 0)),

    /** South bank. Members, and far enough that a tablet or glider can beat walking. */
    EAST_ARDOUGNE("East Ardougne", new WorldPoint(2655, 3283, 0)),

    /** Castle Wars lobby. A ring of dueling destination. */
    CASTLE_WARS("Castle Wars", new WorldPoint(2440, 3089, 0));

    private final String displayName;
    private final WorldPoint tile;

    WalkerDestination(String displayName, WorldPoint tile) {
        this.displayName = displayName;
        this.tile = tile;
    }

    /**
     * Tile this option walks to.
     *
     * <p>A named place always uses its own tile, even if a shift-click is still stored. Manual uses
     * the shift-clicked tile, or {@code null} when none has been picked yet.</p>
     *
     * @param manualTile the last tile shift-clicked Set, or null
     * @return the destination to walk to, or null when Manual has no click yet
     */
    public WorldPoint resolve(WorldPoint manualTile) {
        return tile != null ? tile : manualTile;
    }

    /**
     * RuneLite uses {@code toString} as the dropdown label.
     *
     * @return the name shown in the config panel
     */
    @Override
    public String toString() {
        return displayName;
    }
}
