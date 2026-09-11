package com.kraken.api.input.mouse.strategy.instant;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.input.InputDispatch;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.Canvas;
import java.awt.event.MouseEvent;

@Singleton
public class InstantStrategy implements MoveableMouse {

    @Inject
    private Client client;

    @Override
    public void move(Point start, Point target) {
        Canvas canvas = client.getCanvas();
        if (canvas == null) return;
        InputDispatch.dispatch(canvas, new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, target.getX(), target.getY(), 0, false));
    }
}