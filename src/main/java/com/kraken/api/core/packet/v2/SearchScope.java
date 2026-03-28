package com.kraken.api.core.packet.v2;

public enum SearchScope {
    /** Search in a class's fields[] array */
    FIELD,
    /** Search in a class's methods[] array */
    METHOD,
    /** Search the top-level class list */
    CLASS
}
