package unit.com.kraken.api.service.walker.transport;

import com.kraken.api.service.walker.transport.ObjectInfo;
import com.kraken.api.service.walker.transport.TransportEntityResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers picking the menu action a transport's object info names.
 *
 * <p>The dataset and the client disagree on punctuation, and some rows name no option at all, so
 * this is the last chance to turn an unstructured string into a click.</p>
 */
class TransportEntityResolverTest {

    @Test
    void climbDownMatchesTheClientsSpelling() {
        ObjectInfo info = ObjectInfo.parse("Climb Down Ladder 16680");

        assertEquals("Climb-down", TransportEntityResolver.chooseAction(
                info, new String[]{"Climb-up", "Climb-down", "Examine"}));
    }

    @Test
    void aWebRowIsSlashed() {
        ObjectInfo info = ObjectInfo.parse("Slash Web 733").withEntityName("Web");

        assertEquals("Slash", TransportEntityResolver.chooseAction(
                info, new String[]{"Slash", null, null, null, "Examine"}));
    }

    @Test
    void anEmptyOptionFallsBackToConfigure() {
        ObjectInfo info = ObjectInfo.parse("Fairy ring 29560").withEntityName("Fairy ring");

        assertEquals("", info.getMenuOption());
        assertEquals("Configure", TransportEntityResolver.chooseAction(
                info, new String[]{"Examine", "Configure"}));
    }

    @Test
    void anEmptyOptionFallsBackToTravel() {
        ObjectInfo info = ObjectInfo.parse("Spirit tree 26261").withEntityName("Spirit tree");

        assertEquals("Travel", TransportEntityResolver.chooseAction(
                info, new String[]{"Travel", "Examine"}));
    }

    @Test
    void anEmptyOptionFallsBackToTheFirstRealAction() {
        ObjectInfo info = ObjectInfo.parse("Door 1535").withEntityName("Door");

        assertEquals("Open", TransportEntityResolver.chooseAction(
                info, new String[]{null, "Open", "Examine"}));
    }

    @Test
    void anEmptyOptionWithNoActionsYieldsNothing() {
        ObjectInfo info = ObjectInfo.parse("Fairy ring 29560").withEntityName("Fairy ring");

        assertNull(TransportEntityResolver.chooseAction(info, new String[]{}));
        assertNull(TransportEntityResolver.chooseAction(info, null));
    }

    @Test
    void aParsedOptionIsPreferredWhenItMatches() {
        ObjectInfo info = ObjectInfo.parse("Open Door 9398");

        assertEquals("Open", TransportEntityResolver.chooseAction(
                info, new String[]{"Open", "Examine"}));
    }

    @Test
    void musaPointIsUsedWhenTheSailorOffersIt() {
        ObjectInfo info = ObjectInfo.parse("Musa Point Captain Tobias 14979")
                .withEntityName("Captain Tobias");

        assertEquals("Musa Point", TransportEntityResolver.chooseAction(
                info, new String[]{"Talk-to", "Musa Point", "The Pandemonium"}));
    }

    @Test
    void travelIsUsedWhenTheSailorDoesNotOfferMusaPoint() {
        ObjectInfo info = ObjectInfo.parse("Musa Point Captain Tobias 14979")
                .withEntityName("Captain Tobias");

        assertEquals("Travel", TransportEntityResolver.chooseAction(
                info, new String[]{"Talk-to", "Travel"}));
    }

    @Test
    void aDefinitionThatListsMusaPointStillFallsBackWhenTheLiveMenuDoesNot() {
        ObjectInfo info = ObjectInfo.parse("Musa Point Captain Tobias 14979")
                .withEntityName("Captain Tobias");

        assertEquals("Musa Point", TransportEntityResolver.chooseAction(
                info, new String[]{"Talk-to", "Musa Point", "The Pandemonium", "Travel"}));
        assertEquals("Travel", TransportEntityResolver.chooseAction(
                info, new String[]{"Talk-to", "Travel"}));
    }

    @Test
    void payTollIsUsedWhenTheGateDoesNotOfferOpen() {
        ObjectInfo info = ObjectInfo.parse("Open Gate 44050").withEntityName("Gate");

        assertEquals("Pay-toll(10gp)", TransportEntityResolver.chooseAction(
                info, new String[]{"Pay-toll(10gp)", "Examine"}));
    }
}
