package com.kraken.api.sim.colosim;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UrlTest {

    @Test
    public void testEmptySpawnUrl() {
        assertEquals("http://localhost/?", LineOfSight.getSpawnUrl(new ArrayList<>()));
    }

    @Test
    public void testSingleMobSpawnUrl() {
        List<Types.MobSpec> specs = new ArrayList<>();
        specs.add(TestUtils.createMob(0, 0, 0, null));
        assertEquals("http://localhost/?00000.", LineOfSight.getSpawnUrl(specs));
    }

    @Test
    public void testMultipleMobSpawnUrl() {
        List<Types.MobSpec> specs = new ArrayList<>();
        specs.add(TestUtils.createMob(0, 0, 0, null));
        specs.add(TestUtils.createMob(1, 1, 1, null));
        assertEquals("http://localhost/?00000.01011.", LineOfSight.getSpawnUrl(specs));
    }

    @Test
    public void testNonManticoreSpawnUrlWithMobExtraValue() {
        List<Types.MobSpec> specs = new ArrayList<>();
        specs.add(TestUtils.createMob(0, 0, 0, "r"));
        assertEquals("http://localhost/?00000.", LineOfSight.getSpawnUrl(specs));
    }

    @Test
    public void testManticoreSpawnUrlWithMobExtraValue() {
        List<Types.MobSpec> specs = new ArrayList<>();
        specs.add(TestUtils.createMob(0, 0, LineOfSight.MANTICORE, "r"));
        assertEquals("http://localhost/?00004r.", LineOfSight.getSpawnUrl(specs));
    }

    // ... Add other test cases similarly ...
}
