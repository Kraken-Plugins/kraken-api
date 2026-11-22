package com.kraken.api.service.walker.transport.registry;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Builds transport registries for tests in other packages, which cannot reach the constructors
 * that leave the bundled data out.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestRegistries {

    /**
     * Creates a registry with nothing registered and no dataset, so a test decides every transport
     * a search can see.
     *
     * @return an empty registry
     */
    public static TransportRegistry empty() {
        return new TransportRegistry(List.of());
    }

    /**
     * Creates a registry with nothing registered over a dataset the test supplies.
     *
     * @param dataset the transports that stand in for the bundled dataset
     * @return the registry
     */
    public static TransportRegistry over(List<TransportDefinition> dataset) {
        return new TransportRegistry(List.of(), () -> dataset);
    }
}
