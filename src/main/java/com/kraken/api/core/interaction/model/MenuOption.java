package com.kraken.api.core.interaction.model;

import lombok.Value;
import net.runelite.api.MenuAction;

@Value
public class MenuOption {
    MenuAction type;
    int identifier;
    int param0;
    int param1;
    int itemId;
    int worldView;
}
