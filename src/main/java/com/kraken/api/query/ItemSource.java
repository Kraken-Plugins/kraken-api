package com.kraken.api.query;

/**
 * <p>Represents the source from which an item can be obtained or accessed.</p>
 *
 * <p>This enumeration is typically used to define or restrict the context
 * in which an item exists or is available for interaction.</p>
 *
 * <ul>
 *   <li>{@literal INVENTORY_ONLY} - Indicates that the item is available exclusively within the inventory.</li>
 *   <li>{@literal INTERFACE_ONLY} - Indicates that the item is accessible only through the user interface i.e. equipment interface or bank deposit boxes equipment interface.</li>
 *   <li>{@literal BOTH} - Indicates that the item is available in both the inventory and the user interface.</li>
 * </ul>
 */
public enum ItemSource {
    INVENTORY_ONLY,
    INTERFACE_ONLY,
    BOTH
}

