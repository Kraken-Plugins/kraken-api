package com.kraken.api.core.hooks;

import com.kraken.api.core.packet.model.PacketDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * The obfuscation hooks the client needs for one RuneLite revision: the RuneLite version they were
 * mapped against, the client revision, and the reflection, login and security hooks plus the packet
 * definitions.
 */
@Data
@AllArgsConstructor
public class GameHooks {
    /** The RuneLite version the hooks were mapped for. */
    private String runeliteVersion;
    /** The client revision the hooks were mapped for. */
    private Integer revision;
    /** Hooks used to reach client fields and methods by reflection. */
    private ReflectionHooks reflectionHooks;
    /** Hooks for the login flow. */
    private LoginHooks loginHooks;
    /** Hooks for the mouse hook DLL, the call stack and the PlatformInfo fields sent at login. */
    private SecurityHooks securityHooks;
    /** The packet definitions available to the packet layer. */
    private List<PacketDefinition> packets;
}
