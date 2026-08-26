package unit.plugins.api;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import plugins.api.WalkerDestination;
import plugins.api.world.NamedLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the named tiles WalkerTest walks to from the plugin config.
 */
class WalkerDestinationTest {

    @Test
    void manualHasNoTileSoTheTestStillWaitsForAClick() {
        assertNull(WalkerDestination.MANUAL.getTile());
    }

    @Test
    void grandExchangeAndVarrockEastBankReuseTheHubAnchors() {
        assertEquals(NamedLocation.GRAND_EXCHANGE.getAnchor(),
                WalkerDestination.GRAND_EXCHANGE.getTile());
        assertEquals(NamedLocation.VARROCK_EAST_BANK.getAnchor(),
                WalkerDestination.VARROCK_EAST_BANK.getTile());
    }

    @Test
    void lumbridgeCastleBankIsUpstairs() {
        WorldPoint bank = WalkerDestination.LUMBRIDGE_CASTLE_BANK.getTile();
        assertNotNull(bank);
        assertEquals(2, bank.getPlane());
    }

    @Test
    void gnomeStrongholdIsTheSpiritTreeTile() {
        assertEquals(new WorldPoint(2461, 3444, 0),
                WalkerDestination.TREE_GNOME_STRONGHOLD.getTile());
    }

    @Test
    void everyNamedPlaceHasATile() {
        for (WalkerDestination destination : WalkerDestination.values()) {
            if (destination == WalkerDestination.MANUAL) {
                continue;
            }
            assertNotNull(destination.getTile(), destination.name());
        }
    }

    @Test
    void aNamedPlaceIgnoresALeftoverShiftClick() {
        WorldPoint clicked = new WorldPoint(3200, 3200, 0);

        assertEquals(WalkerDestination.KARAMJA.getTile(),
                WalkerDestination.KARAMJA.resolve(clicked));
        assertEquals(clicked, WalkerDestination.MANUAL.resolve(clicked));
        assertNull(WalkerDestination.MANUAL.resolve(null));
    }
}
