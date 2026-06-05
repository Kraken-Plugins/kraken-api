package com.kraken.api.core.hooks;

import com.kraken.api.core.packet.model.PacketDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GameHooks {
    private ReflectionHooks reflectionHooks;
    private LoginHooks loginHooks;
    private SecurityHooks securityHooks;
    private Map<String, PacketDefinition> packets;
}
