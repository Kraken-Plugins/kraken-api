package com.kraken.api.simulation.colosseum.combat;

import com.kraken.api.simulation.colosseum.ScenarioFixture;
import com.kraken.api.simulation.player.Command;
import com.kraken.api.simulation.geom.Coords;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The core cover mechanic must emerge from the model, not be scripted: naive ranged NPCs
 * hug the far side of a pillar while the player has cover, and stepping out grants them
 * line of sight — and damage — within a tick or two.
 */
public class PillarStackTest {
    @Test
    public void nptStackBehindPillarThenPunishStepOut() {
        ScenarioFixture fx = new ScenarioFixture();
        // Player west of the SW pillar (x 8-10, y 8-10); shamans east of it.
        fx.player(6, 9);
        fx.npc(0, "serpent_shaman", 14, 9);
        fx.npc(1, "serpent_shaman", 14, 10);
        fx.finish();
        fx.ticks(12);
        assertEquals("pillar cover holds: no damage while the shamans hug the far side",
                99, fx.st.hpExpected);
        assertTrue("shamans converged on the pillar", Coords.x(fx.st.npcPos[0]) >= 10);

        // Step out south of the pillar: they gain line of sight and punish it.
        fx.tick(Command.move(Coords.pack(6, 5)));
        fx.ticks(8);
        assertTrue("stepping out costs hp", fx.st.hpExpected < 99);
    }
}
