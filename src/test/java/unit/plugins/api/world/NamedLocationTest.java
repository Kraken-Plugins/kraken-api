package unit.plugins.api.world;

import net.runelite.api.coords.WorldPoint;
import org.junit.jupiter.api.Test;
import plugins.api.world.Facility;
import plugins.api.world.NamedLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers location resolution. The runner decides whether to walk based on these answers, so a wrong
 * one either sends the player on a pointless trip or starts a test somewhere it cannot work.
 */
class NamedLocationTest {

    @Test
    void theHubContainsItsOwnAnchor() {
        NamedLocation hub = NamedLocation.VARROCK_EAST_BANK;
        assertTrue(hub.contains(hub.getAnchor()));
    }

    @Test
    void aTileInsideTheDeclaredBoundsCounts() {
        // Bounds are WorldArea(3250, 3416, 8, 8, 0).
        assertTrue(NamedLocation.VARROCK_EAST_BANK.contains(new WorldPoint(3252, 3418, 0)));
    }

    @Test
    void aTileJustOutsideTheBoundsStillCountsViaTheRadius() {
        // Standing a couple of tiles outside the doorway should still read as being at the bank,
        // otherwise the runner walks the player back and forth over nothing.
        NamedLocation hub = NamedLocation.VARROCK_EAST_BANK;
        WorldPoint justOutside = hub.getAnchor().dx(hub.getDefaultRadius() - 1);

        assertFalse(hub.getBounds().contains(justOutside), "tile should be outside the strict bounds");
        assertTrue(hub.contains(justOutside), "but still within the location's radius");
    }

    @Test
    void aDistantTileDoesNotCount() {
        assertFalse(NamedLocation.VARROCK_EAST_BANK.contains(new WorldPoint(3500, 3500, 0)));
    }

    @Test
    void aTileOnAnotherPlaneDoesNotCount() {
        NamedLocation hub = NamedLocation.VARROCK_EAST_BANK;
        WorldPoint upstairs = new WorldPoint(hub.getAnchor().getX(), hub.getAnchor().getY(), 1);

        assertFalse(hub.contains(upstairs));
    }

    @Test
    void nullTilesAreHandled() {
        assertFalse(NamedLocation.VARROCK_EAST_BANK.contains(null));
        assertTrue(NamedLocation.at(null).isPresent() == false);
    }

    @Test
    void anywhereNeverContainsAnything() {
        // ANYWHERE is a sentinel meaning "no travel required", not a place you can be.
        assertFalse(NamedLocation.ANYWHERE.contains(new WorldPoint(3253, 3421, 0)));
    }

    @Test
    void anywhereIsNeverReturnedAsACandidate() {
        List<NamedLocation> banks =
                NamedLocation.providing(EnumSet.of(Facility.BANK_BOOTH), null);

        assertFalse(banks.isEmpty(), "at least one location must offer a bank booth");
        assertFalse(banks.contains(NamedLocation.ANYWHERE));
    }

    @Test
    void everyCandidateActuallyOffersWhatWasAskedFor() {
        // Asserted as a property rather than against a fixed list of locations: the location table is
        // hand-tuned against the live game, and a test that names its rows would break every time a
        // coordinate or facility is adjusted.
        for (Facility facility : Facility.values()) {
            for (NamedLocation candidate : NamedLocation.providing(EnumSet.of(facility), null)) {
                assertTrue(candidate.getFacilities().contains(facility),
                        candidate + " was offered for " + facility + " but does not provide it");
            }
        }
    }

    @Test
    void everyLocationOfferingAFacilityIsReturned() {
        for (Facility facility : Facility.values()) {
            List<NamedLocation> offered = NamedLocation.providing(EnumSet.of(facility), null);

            for (NamedLocation location : NamedLocation.values()) {
                if (location != NamedLocation.ANYWHERE && location.getFacilities().contains(facility)) {
                    assertTrue(offered.contains(location),
                            location + " provides " + facility + " but was not offered");
                }
            }
        }
    }

    @Test
    void allRequiredFacilitiesMustBePresentTogether() {
        // The GE has bankers but no bank booth, so a test needing a booth must not be sent there.
        assertFalse(NamedLocation.GRAND_EXCHANGE.provides(EnumSet.of(Facility.BANK_BOOTH)));
        assertTrue(NamedLocation.GRAND_EXCHANGE.provides(
                EnumSet.of(Facility.BANKER_NPC, Facility.DEPOSIT_BOX)));
    }

    @Test
    void anEmptyFacilitySetIsSatisfiedByAnyLocation() {
        assertTrue(NamedLocation.VARROCK_EAST_BANK.provides(EnumSet.noneOf(Facility.class)));
        assertTrue(NamedLocation.VARROCK_EAST_BANK.provides(null));
    }

    @Test
    void candidatesAreOrderedNearestFirst() {
        // Again a property, not a named ordering: what matters is that distance never decreases as you
        // walk the list, whatever the location table currently contains.
        WorldPoint origin = NamedLocation.VARROCK_EAST_BANK.getAnchor();
        List<NamedLocation> populated =
                NamedLocation.providing(EnumSet.of(Facility.OTHER_PLAYERS), origin);

        int previous = -1;
        for (NamedLocation location : populated) {
            int distance = location.getAnchor().distanceTo(origin);
            assertTrue(distance >= previous,
                    location + " at " + distance + " tiles came after something further away");
            previous = distance;
        }
    }

    @Test
    void aLocationIsItsOwnNearestCandidate() {
        WorldPoint origin = NamedLocation.VARROCK_EAST_BANK.getAnchor();

        List<NamedLocation> banks = NamedLocation.providing(EnumSet.of(Facility.BANK_BOOTH), origin);

        assertEquals(NamedLocation.VARROCK_EAST_BANK, banks.get(0),
                "measuring from a location's own anchor must put it first");
    }

    @Test
    void aTileResolvesBackToItsLocation() {
        Optional<NamedLocation> resolved =
                NamedLocation.at(NamedLocation.VARROCK_EAST_BANK.getAnchor());

        assertTrue(resolved.isPresent());
        assertEquals(NamedLocation.VARROCK_EAST_BANK, resolved.get());
    }

    @Test
    void anUnknownTileResolvesToNothing() {
        assertFalse(NamedLocation.at(new WorldPoint(2000, 2000, 0)).isPresent());
    }

    @Test
    void distanceToIsSymmetricAndSafeForTheSentinel() {
        int there = NamedLocation.VARROCK_EAST_BANK.distanceTo(NamedLocation.GRAND_EXCHANGE);
        int back = NamedLocation.GRAND_EXCHANGE.distanceTo(NamedLocation.VARROCK_EAST_BANK);

        assertEquals(there, back);
        assertTrue(there > 0);
        assertEquals(Integer.MAX_VALUE, NamedLocation.ANYWHERE.distanceTo(NamedLocation.GRAND_EXCHANGE));
        assertEquals(Integer.MAX_VALUE, NamedLocation.GRAND_EXCHANGE.distanceTo(NamedLocation.ANYWHERE));
    }

    @Test
    void facilitySetsAreImmutable() {
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> NamedLocation.VARROCK_EAST_BANK.getFacilities().clear());
    }
}
