package com.kraken.api.core.interaction.model;

import lombok.Value;

@Value
public class ResolvedMenuAction {
    MenuOption option;
    String target;
}
