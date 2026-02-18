package com.kraken.api.sim.colosim;

import java.util.List;

public class ManticoreOverlay {
    public enum ManticoreOrb {
        RANGE, MAGE, MELEE
    }

    private List<ManticoreOrb> order;
    private boolean transparent;

    public ManticoreOverlay(List<ManticoreOrb> order, boolean transparent) {
        this.order = order;
        this.transparent = transparent;
    }

    public void render() {
        // In a real Java UI (like Swing or JavaFX), this would draw the overlay.
        // For now, we can just print the representation.
        System.out.println("ManticoreOverlay: " + order + (transparent ? " (transparent)" : ""));
    }
}
