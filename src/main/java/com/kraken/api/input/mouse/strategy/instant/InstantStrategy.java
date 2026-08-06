package com.kraken.api.input.mouse.strategy.instant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;
import java.awt.event.MouseEvent;

@Singleton
public class InstantStrategy implements MoveableMouse {

    @Inject
    private Client client;

    @Override
    public void move(Point start, Point target) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, target.getX(), target.getY(), 0, false);
        client.getCanvas().dispatchEvent(event);
    }
}