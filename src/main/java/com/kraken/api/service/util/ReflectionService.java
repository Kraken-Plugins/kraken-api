package com.kraken.api.service.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling reflection operations, including field access and method invocation.
 * <p>
 * This service maintains a cache of reflected fields and methods to improve performance
 * for repeated accesses. It supports accessing obfuscated members via the
 * class and member names mapped in {@code hooks.json}.
 */
@Slf4j
@Singleton
public class ReflectionService {

    private final ClassLoader classLoader;
    private final Map<FieldLookup, Field> fieldCache = new ConcurrentHashMap<>();
    private final Map<MethodLookup, Method> methodCache = new ConcurrentHashMap<>();

    /**
     * Constructs a new ReflectionService.
     *
     * @param client The RuneLite client instance, used to obtain the class loader.
     */
    @Inject
    public ReflectionService(Client client) {
        this.classLoader = client.getClass().getClassLoader();
    }

    /**
     * Retrieves the value of a field specified by the given class and field name.
     *
     * @param className The obfuscated class name containing the field.
     * @param fieldName The obfuscated field name.
     * @param instance The object instance to retrieve the field value from.
     * @param <T>      The expected type of the field value.
     * @return The value of the field, or null if an error occurs.
     */
    public <T> T getFieldValue(String className, String fieldName, Object instance) {
        try {
            Field field = getField(className, fieldName);
            return (T) field.get(instance);
        } catch (Exception e) {
            log.error("Failed to get field {}.{}", className, fieldName, e);
            return null;
        }
    }

    /**
     * Sets the value of a field specified by the given class and field name.
     *
     * @param className The obfuscated class name containing the field.
     * @param fieldName The obfuscated field name.
     * @param instance The object instance to set the field value on.
     * @param value    The new value to set.
     * @return {@code true} if the field was set, {@code false} if resolution or assignment failed.
     */
    public boolean setFieldValue(String className, String fieldName, Object instance, Object value) {
        try {
            Field field = getField(className, fieldName);
            field.set(instance, value);
            return true;
        } catch (Exception e) {
            log.error("Failed to set field {}.{}", className, fieldName, e);
            return false;
        }
    }

    /**
     * Invokes a method specified by the given class and method name.
     * <p>
     * This method automatically handles garbage values if the mapping specifies one,
     * appending it to the arguments list.
     *
     * @param className The obfuscated class name containing the method.
     * @param methodName The obfuscated method name.
     * @param garbageValue Optional trailing garbage value required by the client method.
     * @param instance The object instance to invoke the method on.
     * @param args     The arguments to pass to the method.
     * @return The result of the method invocation, or null if an error occurs.
     */
    public Object invoke(String className, String methodName, Integer garbageValue, Object instance, Object... args) {
        try {
            Method method = getMethod(className, methodName, garbageValue, args.length);
            Object[] finalArgs = prepareArgs(garbageValue, args);
            return method.invoke(instance, finalArgs);
        } catch (Exception e) {
            log.error("Failed to invoke {}.{}", className, methodName, e);
            return null;
        }
    }

    private Field getField(String className, String fieldName) {
        FieldLookup lookup = new FieldLookup(className, fieldName);
        return fieldCache.computeIfAbsent(lookup, key -> {
            try {
                Class<?> clazz = classLoader.loadClass(key.getClassName());
                Field field = clazz.getDeclaredField(key.getFieldName());
                field.setAccessible(true);
                return field;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load field: " + key, e);
            }
        });
    }

    private Method getMethod(String className, String methodName, Integer garbageValue, int argCount) throws Exception {
        int expectedParams = argCount + (garbageValue != null ? 1 : 0);
        MethodLookup lookup = new MethodLookup(className, methodName, expectedParams);
        return methodCache.computeIfAbsent(lookup, key -> {
            try {
                Class<?> clazz = classLoader.loadClass(key.getClassName());

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(key.getMethodName())
                            && method.getParameterCount() == key.getParameterCount()) {
                        method.setAccessible(true);
                        return method;
                    }
                }
                throw new NoSuchMethodException(
                        String.format("Method %s with %d params not found in %s",
                                key.getMethodName(), key.getParameterCount(), key.getClassName())
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to load method: " + key, e);
            }
        });
    }

    private Object[] prepareArgs(Integer garbageValue, Object[] args) {
        if (garbageValue == null) {
            return args;
        }

        Object[] newArgs = Arrays.copyOf(args, args.length + 1);
        int gVal = garbageValue;

        // Determine primitive type based on value range
        if (gVal >= Byte.MIN_VALUE && gVal <= Byte.MAX_VALUE) {
            newArgs[args.length] = (byte) gVal;
        } else if (gVal >= Short.MIN_VALUE && gVal <= Short.MAX_VALUE) {
            newArgs[args.length] = (short) gVal;
        } else {
            newArgs[args.length] = gVal;
        }

        return newArgs;
    }

    @Value
    private static class FieldLookup {
        String className;
        String fieldName;
    }

    @Value
    private static class MethodLookup {
        String className;
        String methodName;
        int parameterCount;
    }
}
