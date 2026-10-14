package unit.com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.query.equipment.EquipmentEntity;
import com.kraken.api.query.equipment.EquipmentQuery;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

class EquipmentQueryTest {
    private static final int HELM = 1000;
    private static final int WHIP = 2000;
    private static final int HEAD_WIDGET_CHILD = 15;

    private final Context ctx = mock(Context.class);
    private final Client client = mock(Client.class);
    private final Item[] worn = new Item[14];

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        when(ctx.getClient()).thenReturn(client);
        when(ctx.runOnClientThread(any(Callable.class)))
                .thenAnswer(invocation -> ((Callable<Object>) invocation.getArgument(0)).call());
        when(ctx.runOnClientThread(any(Callable.class), any()))
                .thenAnswer(invocation -> ((Callable<Object>) invocation.getArgument(0)).call());

        for (int slot = 0; slot < worn.length; slot++) {
            worn[slot] = new Item(-1, 0);
        }
        worn[EquipmentInventorySlot.HEAD.getSlotIdx()] = new Item(HELM, 1);
        worn[EquipmentInventorySlot.WEAPON.getSlotIdx()] = new Item(WHIP, 1);
        worn[EquipmentInventorySlot.AMMO.getSlotIdx()] = new Item(ItemID.BLANKOBJECT, 1);

        ItemContainer container = mock(ItemContainer.class);
        when(container.getItems()).thenReturn(worn);
        when(container.getItem(anyInt())).thenAnswer(invocation -> worn[(int) invocation.getArgument(0)]);
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(container);

        stubItem(HELM, "Helm of neitiznot");
        stubItem(WHIP, "Abyssal whip");

        // Only the head slot's equipment widget is loaded.
        when(client.getWidget(387, HEAD_WIDGET_CHILD)).thenReturn(mock(Widget.class));
    }

    @Test
    void findsWornItemsWhoseEquipmentWidgetIsNotLoaded() {
        Set<Integer> ids = new EquipmentQuery(ctx).inInterface().list().stream()
                .map(EquipmentEntity::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(HELM, WHIP), ids);
        assertTrue(new EquipmentQuery(ctx).inSlot(EquipmentInventorySlot.WEAPON).isPresent());
    }

    @Test
    void readsWornIdsWithoutClientScripts() {
        EquipmentQuery query = new EquipmentQuery(ctx);

        assertEquals(Set.of(HELM, WHIP), query.wornIds());
        assertEquals(WHIP, query.wornWeaponId());
        assertEquals(HELM, query.wornId(EquipmentInventorySlot.HEAD));
        assertEquals(-1, query.wornId(EquipmentInventorySlot.AMMO));
        assertEquals(-1, query.wornId(EquipmentInventorySlot.SHIELD));
        assertEquals(-1, query.wornId(null));
        assertTrue(query.isWearing(WHIP));
        assertTrue(query.isWearing("abyssal whip"));
        assertFalse(query.isWearing(ItemID.BLANKOBJECT));

        assertTrue(mockingDetails(client).getInvocations().stream()
                .noneMatch(invocation -> invocation.getMethod().getName().equals("runScript")));
    }

    @Test
    void readsNothingBeforeTheWornContainerLoads() {
        when(client.getItemContainer(InventoryID.WORN)).thenReturn(null);
        EquipmentQuery query = new EquipmentQuery(ctx);

        assertTrue(query.wornIds().isEmpty());
        assertEquals(-1, query.wornWeaponId());
        assertFalse(query.isWearing(WHIP));
    }

    private void stubItem(int id, String name) {
        ItemComposition composition = mock(ItemComposition.class);
        when(composition.getName()).thenReturn(name);
        when(composition.getInventoryActions()).thenReturn(new String[]{"Wield", null, null, null, "Drop"});
        when(client.getItemDefinition(id)).thenReturn(composition);
    }
}
