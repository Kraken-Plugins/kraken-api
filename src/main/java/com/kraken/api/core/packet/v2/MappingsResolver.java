package com.kraken.api.core.packet.v2;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.kraken.api.core.packet.v2.model.ClassMapping;
import com.kraken.api.core.packet.v2.model.FieldMapping;
import com.kraken.api.core.packet.v2.model.MappingNotFoundException;
import com.kraken.api.core.packet.v2.model.MethodMapping;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Parses mappings.json and resolves all ObfuscatedMapping entries.
 * Zero obfuscated values are used during lookup — only non-obfuscated names.
 */
@Slf4j
public class MappingsResolver {
    private static final String MAPPINGS_URL = "https://minio.kraken-plugins.com/kraken-bootstrap-static/mappings.json";

    // Statically cache the mappings so they are only loaded once across all instances
    private static volatile List<ClassMapping> cachedClasses = null;

    private static final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
            .setPrettyPrinting()
            .create();

    // Instance variable points to the statically loaded cache
    private final List<ClassMapping> classes;

    public MappingsResolver() {
        // Ensure the cache is populated when an instance is created
        if (cachedClasses == null) {
            loadMappings();
        }
        this.classes = cachedClasses;
    }

    /**
     * Fetches and parses mappings.json from MinIO.
     * Synchronized to prevent multiple threads from fetching the 2MB file simultaneously on startup.
     */
    public static synchronized void loadMappings() {
        if (cachedClasses != null) {
            return; // Already loaded
        }

        try {
            log.info("Fetching mappings from MinIO: {}", MAPPINGS_URL);
            URL url = new URL(MAPPINGS_URL);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);

            try (InputStream inputStream = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                cachedClasses = gson.fromJson(reader, new TypeToken<List<ClassMapping>>() {}.getType());

                if (cachedClasses == null) {
                    cachedClasses = Collections.emptyList();
                    log.warn("Mappings JSON parsed to null.");
                } else {
                    log.info("Successfully loaded {} class mappings", cachedClasses.size());
                }
            }
        } catch (IOException e) {
            log.error("Failed to load class mappings json from MinIO: ", e);
            cachedClasses = Collections.emptyList();
        }
    }

    /**
     * Resolves all mappings at once. Logs a warning for any that can't be found
     * rather than blowing up, so you can see all failures at once on startup.
     */
    public Map<ObfuscatedMapping, Object> resolveAll() {
        Map<ObfuscatedMapping, Object> results = new EnumMap<>(ObfuscatedMapping.class);
        for (ObfuscatedMapping mapping : ObfuscatedMapping.values()) {
            try {
                results.put(mapping, resolve(mapping));
            } catch (MappingNotFoundException e) {
                log.warn("MISSING MAPPING: {}", e.getMessage());
            }
        }
        return results;
    }

    public Object resolve(ObfuscatedMapping mapping) {
        switch (mapping.getScope()) {
            case CLASS:  return resolveClass(mapping);
            case FIELD:  return resolveField(mapping);
            case METHOD: return resolveMethod(mapping);
            default: throw new IllegalArgumentException("Unknown scope: " + mapping.getScope());
        }
    }

    // -------------------------------------------------------------------------
    // Scope resolvers
    // -------------------------------------------------------------------------

    private Object resolveClass(ObfuscatedMapping mapping) {
        ClassMapping cls = classes.stream()
                .filter(c -> mapping.getSearchName().equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new MappingNotFoundException(mapping));

        if (mapping.getExtract() == ExtractTarget.OBFUSCATED_NAME) {
            return cls.getObfuscatedName();
        }
        throw new IllegalArgumentException(
                "Unsupported extract target " + mapping.getExtract() + " for CLASS scope"
        );
    }

    private Object resolveMethod(ObfuscatedMapping mapping) {
        for (ClassMapping cls : classes) {
            if (mapping.getOwnerName() != null && !mapping.getOwnerName().equals(cls.getName())) {
                continue;
            }
            for (MethodMapping method : cls.getMethods()) {
                if (mapping.getSearchName().equals(method.getName())) {
                    return extractFromMethod(method, cls, mapping);
                }
            }
        }
        throw new MappingNotFoundException(mapping);
    }

    private Object resolveField(ObfuscatedMapping mapping) {
        switch (mapping.getStrategy()) {
            case BY_NAME:
                return resolveFieldByName(mapping);
            case BY_DESCRIPTOR_TYPE:
                return resolveFieldByDescriptorType(mapping);
            case BY_DESCRIPTOR:
                return resolveFieldByDescriptor(mapping);
            default:
                throw new IllegalArgumentException("Unknown strategy: " + mapping.getStrategy());
        }
    }

    private Object resolveFieldByName(ObfuscatedMapping mapping) {
        for (ClassMapping cls : classes) {
            if (mapping.getOwnerName() != null && !mapping.getOwnerName().equals(cls.getName())) {
                continue;
            }
            for (FieldMapping field : cls.getFields()) {
                if (mapping.getSearchName().equals(field.getName())) {
                    return extractFromField(field, mapping);
                }
            }
        }
        throw new MappingNotFoundException(mapping);
    }

    /**
     * Finds a field whose descriptor references the class named by searchName.
     *
     * Steps:
     *   1. Resolve the obfuscated name of the target type (e.g. "IsaacCipher" → "xj")
     *   2. Build the expected descriptor string ("Lxj;")
     *   3. Scan all fields (filtered by ownerName if set) for a descriptor match
     *
     * This never touches any hardcoded obfuscated value.
     */
    private Object resolveFieldByDescriptorType(ObfuscatedMapping mapping) {
        // Step 1: find the obfuscated name of the type we're looking for
        String targetObfuscatedName = classes.stream()
                .filter(c -> mapping.getSearchName().equals(c.getName()))
                .map(ClassMapping::getObfuscatedName)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Cannot resolve BY_DESCRIPTOR_TYPE for [" + mapping.name() + "]: " +
                                "no class named '" + mapping.getSearchName() + "' found in mappings"
                ));

        // Step 2: build the JVM descriptor for that class
        String expectedDescriptor = "L" + targetObfuscatedName + ";";

        // Step 3: find the field with that descriptor
        for (ClassMapping cls : classes) {
            if (mapping.getOwnerName() != null && !mapping.getOwnerName().equals(cls.getName())) {
                continue;
            }
            for (FieldMapping field : cls.getFields()) {
                if (expectedDescriptor.equals(field.getDescriptor())) {
                    return extractFromField(field, mapping);
                }
            }
        }
        throw new MappingNotFoundException(mapping);
    }

    /**
     * Finds a field whose descriptor exactly matches searchName.
     * ownerName should always be set here — primitive descriptors like "J" or "I"
     * are far too common across the whole mappings file to search without it.
     */
    private Object resolveFieldByDescriptor(ObfuscatedMapping mapping) {
        for (ClassMapping cls : classes) {
            if (mapping.getOwnerName() != null && !mapping.getOwnerName().equals(cls.getName())) {
                continue;
            }
            for (FieldMapping field : cls.getFields()) {
                if (mapping.getSearchName().equals(field.getDescriptor())) {
                    return extractFromField(field, mapping);
                }
            }
        }
        throw new MappingNotFoundException(mapping);
    }

    // -------------------------------------------------------------------------
    // Extraction helpers
    // -------------------------------------------------------------------------

    private Object extractFromField(FieldMapping field, ObfuscatedMapping mapping) {
        switch (mapping.getExtract()) {
            case OBFUSCATED_NAME:  return field.getObfuscatedName();
            case GETTER:           return castNumber(field.getGetter(), mapping.getType());
            case SETTER:           return castNumber(field.getSetter(), mapping.getType());
            case DESCRIPTOR_CLASS: return parseDescriptorClass(field.getDescriptor());
            default: throw new IllegalArgumentException(
                    "Unsupported extract target " + mapping.getExtract() + " for FIELD scope"
            );
        }
    }

    private Object extractFromMethod(MethodMapping method, ClassMapping owner, ObfuscatedMapping mapping) {
        switch (mapping.getExtract()) {
            case OBFUSCATED_NAME:       return method.getObfuscatedName();
            case GARBAGE_VALUE:         return castNumber(method.getGarbageValue(), mapping.getType());
            case OWNER_OBFUSCATED_NAME: return owner.getObfuscatedName();
            default: throw new IllegalArgumentException(
                    "Unsupported extract target " + mapping.getExtract() + " for METHOD scope"
            );
        }
    }

    /**
     * Parses a JVM descriptor like "Ldh;" into "dh",
     * or "Lnet/runelite/api/Foo;" into "net/runelite/api/Foo".
     */
    private String parseDescriptorClass(String descriptor) {
        if (descriptor == null || !descriptor.startsWith("L") || !descriptor.contains(";")) {
            throw new IllegalArgumentException("Cannot parse class from descriptor: " + descriptor);
        }
        return descriptor.substring(1, descriptor.indexOf(';'));
    }

    private Object castNumber(Number value, Class<?> type) {
        if (value == null) return null;
        if (type == Long.class)    return value.longValue();
        if (type == Integer.class) return value.intValue();
        return value;
    }
}