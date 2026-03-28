package com.kraken.api.core.packet.v2.model;

import com.kraken.api.core.packet.v2.ObfuscatedMapping;

public class MappingNotFoundException extends RuntimeException {
    public MappingNotFoundException(ObfuscatedMapping mapping) {
        super(String.format(
                "Could not resolve mapping [%s]: no %s named '%s' found in owner '%s'",
                mapping.name(), mapping.getScope(), mapping.getSearchName(),
                mapping.getOwnerName() != null ? mapping.getOwnerName() : "<any>"
        ));
    }
}