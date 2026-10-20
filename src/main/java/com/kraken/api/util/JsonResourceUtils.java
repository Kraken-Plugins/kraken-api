package com.kraken.api.util;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Utility helpers for loading and deserializing JSON files from the classpath.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonResourceUtils {

    /**
     * Reads a UTF-8 JSON resource from the classpath and deserializes it into the given type.
     * @param resourceOwner The class whose class loader and package resolve the resource path.
     * @param resourcePath The resource path, resolved as {@link Class#getResourceAsStream(String)} resolves it.
     * @param gson The Gson instance used to deserialize the content.
     * @param type The type to deserialize the content into.
     * @param <T> The type of the deserialized value.
     * @return The deserialized value.
     * @throws IllegalStateException If the resource does not exist, cannot be read, or deserializes to null.
     */
    public static <T> T loadJsonResource(Class<?> resourceOwner, String resourcePath, Gson gson, Class<T> type) {
        try (InputStream inputStream = resourceOwner.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                T value = gson.fromJson(reader, type);
                if (value == null) {
                    throw new IllegalStateException("Failed to deserialize resource: " + resourcePath);
                }
                return value;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception while loading resource: " + resourcePath, e);
        }
    }
}
