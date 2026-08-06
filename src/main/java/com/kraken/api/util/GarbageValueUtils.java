package com.kraken.api.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

/**
 * Coerces an obfuscated "garbage value" to the primitive width a reflected client method
 * actually declares for its trailing dummy parameter.
 * <p>
 * The obfuscator re-rolls this parameter every revision — it has been {@code int}, {@code short}
 * and {@code byte} in different releases — and reflection performs no widening or narrowing, so
 * passing an {@code Integer} to a {@code byte} parameter fails with "argument type mismatch".
 * The value itself is never read by the client; only its declared type matters. Inspecting the
 * resolved method's parameter type is therefore authoritative, unlike inferring the width from
 * the magnitude of the configured value.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GarbageValueUtils {

    /**
     * Boxes the given value at the width declared by {@code parameterType}.
     *
     * @param parameterType The declared type of the method's garbage parameter. Must be one of
     *                      the primitive types {@code byte}, {@code short}, {@code int} or {@code long}.
     * @param value         The configured garbage value from the hooks.
     * @return A {@code Byte}, {@code Short}, {@code Integer} or {@code Long} carrying the value at
     *         the declared width, or {@code null} if {@code parameterType} is not a supported
     *         primitive numeric type.
     */
    @Nullable
    public static Object coerceToParameterType(Class<?> parameterType, Number value) {
        if (parameterType == byte.class) {
            return value.byteValue();
        }
        if (parameterType == short.class) {
            return value.shortValue();
        }
        if (parameterType == int.class) {
            return value.intValue();
        }
        if (parameterType == long.class) {
            return value.longValue();
        }
        return null;
    }

    /**
     * Reports whether a declared parameter type is one this utility can coerce a garbage value to.
     *
     * @param parameterType The declared parameter type to check.
     * @return True if the type is the primitive {@code byte}, {@code short}, {@code int} or {@code long}.
     */
    public static boolean isSupportedParameterType(Class<?> parameterType) {
        return parameterType == byte.class
                || parameterType == short.class
                || parameterType == int.class
                || parameterType == long.class;
    }
}
