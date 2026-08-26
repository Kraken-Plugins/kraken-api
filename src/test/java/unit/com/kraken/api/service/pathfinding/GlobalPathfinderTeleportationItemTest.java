package unit.com.kraken.api.service.pathfinding;

import com.kraken.api.service.pathfinding.GlobalPathfinder;
import org.junit.jupiter.api.Test;
import shortestpath.TeleportationItem;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers mapping the on/off teleport-item flag to shortest-path's jewellery setting.
 */
class GlobalPathfinderTeleportationItemTest {

    @Test
    void chargedJewelleryInInventoryIsUsedWhenItemTeleportsAreOn() {
        assertEquals(TeleportationItem.INVENTORY, GlobalPathfinder.teleportationItemSetting(true));
    }

    @Test
    void itemTeleportsAreOffWhenTheCallerDisabledThem() {
        assertEquals(TeleportationItem.NONE, GlobalPathfinder.teleportationItemSetting(false));
    }
}
