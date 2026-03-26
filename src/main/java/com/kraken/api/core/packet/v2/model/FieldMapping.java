package com.kraken.api.core.packet.v2.model;

import lombok.Data;

@Data
public class FieldMapping {
    private String name;
    private String obfuscatedName;
    private String owner;
    private String ownerObfuscatedName;
    private String descriptor;
    private Long getter;
    private Long setter;
    private boolean isStatic;
}
