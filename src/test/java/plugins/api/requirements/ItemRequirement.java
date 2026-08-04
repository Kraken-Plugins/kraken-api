package plugins.api.requirements;

import lombok.Builder;
import lombok.Value;

/**
 * An item a test needs, expressed by name or by item id.
 *
 * <p>Static factories are provided because these are written by hand in every test's requirement
 * declaration, and the builder is too noisy for what is usually "one lobster".</p>
 */
@Value
@Builder
public class ItemRequirement {

    /** Whether the item must be in noted form. */
    public enum Noted {
        /** Must be unnoted. */
        UNNOTED,
        /** Must be noted. */
        NOTED,
        /** Either form satisfies the requirement. */
        EITHER
    }

    /** Item name, matched case insensitively. Null when {@link #id} is set instead. */
    String name;

    /** Item id. Negative when {@link #name} is used instead. */
    @Builder.Default
    int id = -1;

    /** How many are needed. Always at least one in practice. */
    @Builder.Default
    int quantity = 1;

    /** Whether the item is needed noted, unnoted, or either. */
    @Builder.Default
    Noted noted = Noted.UNNOTED;

    /**
     * A single unnoted item, matched by name.
     *
     * @param name the item name
     * @return the requirement
     */
    public static ItemRequirement of(String name) {
        return builder().name(name).build();
    }

    /**
     * Several unnoted items, matched by name.
     *
     * @param name the item name
     * @param quantity how many are needed
     * @return the requirement
     */
    public static ItemRequirement of(String name, int quantity) {
        return builder().name(name).quantity(quantity).build();
    }

    /**
     * Several unnoted items, matched by item id.
     *
     * @param id the item id
     * @param quantity how many are needed
     * @return the requirement
     */
    public static ItemRequirement of(int id, int quantity) {
        return builder().id(id).quantity(quantity).build();
    }

    /**
     * Several noted items, matched by name.
     *
     * @param name the item name
     * @param quantity how many are needed
     * @return the requirement
     */
    public static ItemRequirement noted(String name, int quantity) {
        return builder().name(name).quantity(quantity).noted(Noted.NOTED).build();
    }

    /**
     * Reports whether this requirement identifies its item by id rather than by name.
     *
     * @return true when the item id should be used to match
     */
    public boolean byId() {
        return id > 0;
    }

    /**
     * Renders the requirement for a human, used verbatim in skip reasons.
     *
     * @return a short description such as {@code "5x Lobster"} or {@code "item 1623"}
     */
    public String describe() {
        String what = byId() ? "item " + id : name;
        String suffix = noted == Noted.NOTED ? " (noted)" : "";
        return quantity > 1 ? quantity + "x " + what + suffix : what + suffix;
    }
}
