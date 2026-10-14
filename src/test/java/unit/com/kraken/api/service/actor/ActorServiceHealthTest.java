package unit.com.kraken.api.service.actor;

import com.kraken.api.service.actor.ActorService;
import net.runelite.api.NPC;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActorServiceHealthTest {

    @Test
    void unknownHealthBarsAreMinusOne() {
        assertEquals(-1, ActorService.calculateHealth(-1, 30, 100));
        assertEquals(-1, ActorService.calculateHealth(10, 0, 100));
        assertEquals(-1, ActorService.calculateHealth(10, 30, 0));
        assertEquals(-1, ActorService.calculateHealth(null, 100));
    }

    @Test
    void anEmptyHealthBarIsDead() {
        assertEquals(0, ActorService.calculateHealth(0, 30, 100));
    }

    @Test
    void recoversExactHealthWhenTheScaleIsFineEnough() {
        int healthScale = 30;
        for (int maxHealth = 1; maxHealth < healthScale; maxHealth++) {
            for (int health = 1; health <= maxHealth; health++) {
                int ratio = serverHealthRatio(health, maxHealth, healthScale);
                assertEquals(health, ActorService.calculateHealth(ratio, healthScale, maxHealth),
                        "health " + health + " of " + maxHealth);
            }
        }
    }

    @Test
    void estimatesTheMiddleOfACoarseHealthBar() {
        assertEquals(550, ActorService.calculateHealth(15, 30, 1100));
        assertEquals(1100, ActorService.calculateHealth(30, 30, 1100));
        assertEquals(19, ActorService.calculateHealth(1, 30, 1100));
        assertEquals(51, ActorService.calculateHealth(1, 1, 100));
    }

    @Test
    void readsTheActorsHealthBar() {
        NPC npc = mock(NPC.class);
        when(npc.getHealthRatio()).thenReturn(15);
        when(npc.getHealthScale()).thenReturn(30);

        assertEquals(550, ActorService.calculateHealth(npc, 1100));
    }

    /**
     * The ratio the server sends for {@code health} out of {@code maxHealth} on a bar of {@code healthScale}.
     */
    private static int serverHealthRatio(int health, int maxHealth, int healthScale) {
        return health > 0 ? 1 + (healthScale - 1) * health / maxHealth : 0;
    }
}
