package com.kraken.api.input.mouse.strategy;

import net.runelite.api.Point;

public interface MoveableMouse {
    void move(Point start, Point target);
}
