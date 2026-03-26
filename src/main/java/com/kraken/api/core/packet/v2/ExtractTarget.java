package com.kraken.api.core.packet.v2;

public enum ExtractTarget {
    /** The obfuscated name of the field/method/class itself */
    OBFUSCATED_NAME,
    /** The getter multiplier on a field */
    GETTER,
    /** The setter multiplier on a field */
    SETTER,
    /** The garbage value on a method */
    GARBAGE_VALUE,
    /** The obfuscated name of the class that OWNS this method */
    OWNER_OBFUSCATED_NAME,
    /** Parse "Lsome/ClassName;" from a field's descriptor → returns "some/ClassName" */
    DESCRIPTOR_CLASS
}
