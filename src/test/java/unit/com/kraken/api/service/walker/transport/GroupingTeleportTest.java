package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.GroupingTeleport;
import com.kraken.api.service.walker.transport.TransportShape;
import com.kraken.api.service.walker.transport.registry.TransportDataset;
import com.kraken.api.service.walker.transport.registry.TransportDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers reading a minigame teleport's display info and finding the activity it names.
 *
 * <p>The sweep matters most: every minigame teleport of the dataset has to name an activity the
 * game lists, because that name is all the walker has to select it by. A regenerated dataset that
 * names one differently fails here instead of on the grouping tab.</p>
 */
class GroupingTeleportTest {

    /** The game's table of minigame teleports (revision 242): the id of each activity and its name. */
    private static final Map<Integer, String> ACTIVITIES = activities();

    private static Map<Integer, String> activities() {
        Map<Integer, String> activities = new LinkedHashMap<>();
        activities.put(1, "Barbarian Assault");
        activities.put(2, "Blast Furnace");
        activities.put(4, "Burthorpe Games Room");
        activities.put(3, "Bounty Hunter");
        activities.put(5, "Castle Wars");
        activities.put(6, "Clan Wars");
        activities.put(8, "Fishing Trawler");
        activities.put(9, "Giants' Foundry");
        activities.put(11, "Guardians of the Rift");
        activities.put(12, "Last Man Standing");
        activities.put(13, "Mage Training Arena");
        activities.put(14, "Mastering Mixology");
        activities.put(15, "Nightmare Zone");
        activities.put(16, "Pest Control");
        activities.put(18, "Rat Pits");
        activities.put(20, "Shades of Mort'ton");
        activities.put(23, "Sorceress's Garden");
        activities.put(24, "Soul Wars");
        activities.put(26, "Tithe Farm");
        activities.put(28, "Trouble Brewing");
        activities.put(29, "TzHaar Fight Pit");
        return activities;
    }

    @Test
    void everyMinigameTeleportOfTheDatasetNamesAnActivityTheGameLists() {
        List<String> problems = new ArrayList<>();
        Set<Integer> reached = new HashSet<>();
        for (TransportDefinition definition : TransportDataset.bundled()) {
            if (definition.getShape() != TransportShape.GROUPING_TELEPORT) {
                continue;
            }

            GroupingTeleport teleport = GroupingTeleport.parse(definition.getDisplayInfo());
            int id = teleport != null
                    ? GroupingTeleport.activityId(ACTIVITIES, teleport.getActivity())
                    : GroupingTeleport.NO_ACTIVITY;
            if (id == GroupingTeleport.NO_ACTIVITY) {
                problems.add(definition.getId() + " '" + definition.getDisplayInfo() + "'");
            }
            reached.add(id);
        }

        assertTrue(problems.isEmpty(), problems.toString());
        assertEquals(ACTIVITIES.keySet(), reached, "the dataset has a teleport for every activity the game lists");
    }

    @Test
    void theDatasetsWayOfWritingAnActivityIsRead() {
        GroupingTeleport castleWars = GroupingTeleport.parse("Castle Wars Minigame Teleport");

        assertEquals("Castle Wars", castleWars.getActivity());
        assertNull(castleWars.getChoice(), "an activity with one landing asks nothing afterwards");
    }

    @Test
    void anActivityWithSeveralLandingsCarriesTheOneToChoose() {
        GroupingTeleport ratPits = GroupingTeleport.parse("Rat Pits Minigame Teleport: 2. Varrock");

        assertEquals("Rat Pits", ratPits.getActivity());
        assertNotNull(ratPits.getChoice());
        assertEquals("Varrock", ratPits.getChoice().getLabel());
        assertEquals(2, ratPits.getChoice().getPosition());
    }

    @Test
    void aBareActivityNameIsEnough() {
        assertEquals("Soul Wars", GroupingTeleport.parse(" Soul Wars ").getActivity());
        assertEquals("Rat Pits", GroupingTeleport.parse("Rat Pits: Port Sarim").getActivity());
        assertEquals("Port Sarim", GroupingTeleport.parse("Rat Pits: Port Sarim").getChoice().getLabel());
        assertNull(GroupingTeleport.parse("Rat Pits:").getChoice());
    }

    @Test
    void displayInfoThatNamesNoActivityIsNotATeleport() {
        assertNull(GroupingTeleport.parse(null));
        assertNull(GroupingTeleport.parse("  "));
        assertNull(GroupingTeleport.parse("Minigame Teleport"));
        assertNull(GroupingTeleport.parse(": 2. Varrock"));
    }

    @Test
    void anActivityIsFoundWhateverItsPunctuation() {
        assertEquals(9, GroupingTeleport.activityId(ACTIVITIES, "Giant's Foundry"));
        assertEquals(23, GroupingTeleport.activityId(ACTIVITIES, "Sorceress's Garden"));
        assertEquals(20, GroupingTeleport.activityId(ACTIVITIES, "shades of mortton"));
        assertEquals(29, GroupingTeleport.activityId(ACTIVITIES, "TzHaar Fight Pit"));
    }

    @Test
    void anActivityTheGameDoesNotListHasNoId() {
        assertEquals(GroupingTeleport.NO_ACTIVITY, GroupingTeleport.activityId(ACTIVITIES, "Fight Caves"));
        assertEquals(GroupingTeleport.NO_ACTIVITY, GroupingTeleport.activityId(ACTIVITIES, "Castle"));
        assertEquals(GroupingTeleport.NO_ACTIVITY, GroupingTeleport.activityId(ACTIVITIES, ""));
        assertEquals(GroupingTeleport.NO_ACTIVITY, GroupingTeleport.activityId(ACTIVITIES, null));
        assertEquals(GroupingTeleport.NO_ACTIVITY, GroupingTeleport.activityId(null, "Castle Wars"));
    }

    @Test
    void theTeleportButtonOnlyCountsOnceItNamesAnActivity() {
        String toCastleWars = "Teleport to <col=ff8040>Castle Wars</col>";

        assertEquals(toCastleWars, GroupingTeleport.teleportAction(new String[]{toCastleWars}));
        assertEquals(toCastleWars, GroupingTeleport.teleportAction(new String[]{null, toCastleWars}));
        assertNull(GroupingTeleport.teleportAction(new String[]{"Teleport"}),
                "with nothing selected the button goes nowhere");
        assertNull(GroupingTeleport.teleportAction(new String[]{"Switch <col=ff8040>330</col>"}));
        assertNull(GroupingTeleport.teleportAction(null));
    }
}
