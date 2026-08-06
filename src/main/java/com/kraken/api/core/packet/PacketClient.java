package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketWrite;
import com.kraken.api.util.GarbageValueUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code PacketClient} is an instance-based RuneLite client packet sending utility which uses reflection to
 * construct and send low-level packets directly to the game servers. Generally, you should not need to use this class directly
 * within your plugins as it functions at a lower level to construct and sending packets.
 * <p>
 * Instead, it's recommended to use the higher level API's like {@code MousePackets}, {@code WidgetPackets}, or {@code NpcPackets} for
 * sending game packets to the server based on your specific entity interaction needs (clicking interfaces, NPC's, GameObjects, etc...
 * <p>
 * All reflective handles (classes, methods and fields) are resolved once and cached with the same
 * double-checked-locking shape {@code DoActionInvoker} uses. Only the handles are cached — the
 * {@code PacketWriter} instance, ISAAC cipher and packet definition constants are re-read from the
 * live client on every send. Each instance-bound handle is guarded by the declaring class of the
 * object it is applied to, so if the client ever supplies an object of a different class the handle
 * is re-resolved instead of being applied stale. Failed resolutions are never cached and are
 * retried on the next send.
 */
@Slf4j
@Singleton
public class PacketClient {

    @Getter
    private final Client client;

    private final boolean isUsingClientAddNode;

    private final Object resolutionLock = new Object();
    private volatile Class<?> clientPacketClass;
    private volatile Method getPacketBufferNodeMethod;
    private volatile Field packetWriterField;
    private volatile Field isaacField;
    private volatile Field packetBufferField;
    private volatile Method addNodeMethod;
    private final Map<String, Field> packetDefinitionFields = new ConcurrentHashMap<>();

    /**
     * Creates a new PacketSender. This constructor initializes packet queueing functionality by either loading the client packet
     * sending method from the cached JSON file or running an analysis on the RuneLite injected client
     * to determine the packet sending method.
     *
     * @param client The RuneLite Client instance.
     */
    @Inject
    public PacketClient(Client client) {
        this.client = client;
        // Some revs the packet add node method will be like client.aq.az() client.packetWriter.addNode() but other times
        // it may be on a static helper class like ap.aq.az() helper.packetWriter.addNode()
        this.isUsingClientAddNode = HooksLoader.getReflectionHooks().getAddNodeClassName().equalsIgnoreCase("client");
    }

    /**
     * Constructs and sends a packet to the game server.
     * This is the primary public method of this class.
     *
     * @param def     The {@link PacketDefinition} enumeration defining the packet structure.
     * @param objects The data (payload) for the packet, in the order defined by the PacketDefinition.
     */
    public void sendPacket(PacketDefinition def, Object... objects) {
        Object packetBufferNode = null;
        Method getPacketBufferNode = getGetPacketBufferNode();
        Class<?> clientPacket = getClientPacketClass();
        Object isaac = getIsaacObject();

        if (getPacketBufferNode == null || clientPacket == null || isaac == null) {
            log.error("Failed to get critical reflection components for sending packet: {}", def.getObfuscatedName());
            return;
        }

        try {
            Field packetField = fetchPacketField(def.getObfuscatedName());
            if (packetField == null) {
                log.error("Could not find packet field for: {}", def.getObfuscatedName());
                return;
            }
            Object packetDefInstance = packetField.get(clientPacket);

            // The factory method takes (packetDefinition, isaac, garbage). The obfuscator re-rolls the
            // trailing garbage parameter's primitive width every revision, so the value is boxed at
            // whatever width the resolved method actually declares.
            Class<?>[] parameterTypes = getPacketBufferNode.getParameterTypes();
            if (parameterTypes.length != 3) {
                log.error("getPacketBufferNode has an unexpected parameter count ({}) for packet: {}", parameterTypes.length, def.getObfuscatedName());
                return;
            }

            Object garbageArgument = GarbageValueUtils.coerceToParameterType(parameterTypes[2], HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue());
            if (garbageArgument == null) {
                log.error("Unsupported getPacketBufferNode garbage value type '{}' for packet: {}", parameterTypes[2].getName(), def.getObfuscatedName());
                return;
            }

            packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, garbageArgument);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to invoke getPacketBufferNode: ", e);
        }

        if (packetBufferNode == null) {
            log.error("PacketBufferNode was null after creation attempt for packet: {}", def.getObfuscatedName());
            return;
        }

        // Get the raw 'buffer' object from the 'packetBufferNode' to write data into.
        Object buffer;
        try {
            buffer = getPacketBufferField(packetBufferNode.getClass()).get(packetBufferNode);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.error("Failed to get packet buffer from node: ", e);
            return;
        }

        // Map the PacketType to the expected parameter order.
        // This is necessary because the varargs 'objects' must be written in a specific
        // sequence defined by the packet structure, not just the order they are passed in.
        List<String> params = def.getType().getParams();

        // If the packet type is recognized, write the data into the buffer.
        if (params != null) {
            Map<String, Integer> paramIndices = new HashMap<>();
            for (int i = 0; i < params.size(); i++) {
                paramIndices.put(params.get(i), i);
            }

            for (PacketWrite write : def.getWrites()) {
                Integer index = paramIndices.get(write.getParam());
                if (index == null || index >= objects.length) {
                    log.error("Missing packet value for {}.{} param {}", def.getPacketName(), def.getObfuscatedName(), write.getParam());
                    return;
                }

                Object writeValue = objects[index];
                for (BufferOperation operation : write.getOperations()) {
                    BufferUtils.writeOperation(operation, writeValue, buffer);
                }
            }

            // Get the PacketWriter field and queue the fully constructed packet node.
            Field writerField = getPacketWriterField();
            if (writerField == null) {
                log.error("Could not get PacketWriter field to queue packet.");
                return;
            }

            try {
                Object packetWriter = writerField.get(null);
                if (packetWriter != null) {
                    addNode(packetWriter, packetBufferNode);
                } else {
                    log.error("PacketWriter object was null.");
                }
            } catch (Exception e) {
                log.error("Failed to add packet node to queue: ", e);
            }
        } else {
            log.warn("Unrecognized packet type, packet not sent: {}", def.getType());
        }
    }

    /**
     * Queues a fully constructed {@code PacketBufferNode} to the client's {@code PacketWriter} for network dispatch.
     * <p>
     * Due to the client's dynamic obfuscation patterns across different revisions, the underlying
     * packet-queueing method manifests in one of two distinct structural paths:
     * <ul>
     * <li><b>Path 1 (Instance Method):</b> The method exists directly on the {@code PacketWriter} class.
     * It takes the buffer as an argument (e.g., {@code client.packetWriter.addNode(buffer)}).</li>
     * <li><b>Path 2 (Static Utility):</b> The method is detached into an unrelated static utility class.
     * Because it lacks instance context, it strictly requires the {@code PacketWriter} to be passed
     * in as its first argument (e.g., {@code RandomClass.addNode(packetWriter, buffer)}).</li>
     * </ul>
     * <p>
     * This method acts as a unified wrapper, abstracting away this instability. It seamlessly executes
     * the correct reflection call — the anti-reversing dummy "garbage value" is boxed at whatever
     * primitive width the resolved method declares for it.
     *
     * @param packetWriter     The client's {@code PacketWriter} instance responsible for handling network I/O.
     * @param packetBufferNode The fully constructed packet node containing the payload to be sent.
     */
    private void addNode(Object packetWriter, Object packetBufferNode) {
        try {
            Method addNode = getAddNodeMethod(packetWriter, packetBufferNode);
            if (addNode == null) {
                log.error("Failed to locate addNode method: {} in class {}: ", HooksLoader.getReflectionHooks().getAddNodeMethodName(), HooksLoader.getReflectionHooks().getAddNodeClassName());
                return;
            }

            Class<?>[] parameterTypes = addNode.getParameterTypes();
            if (isUsingClientAddNode) {
                // Path 1: instance method on the PacketWriter — addNode(packetBufferNode, garbage).
                Object garbageArgument = GarbageValueUtils.coerceToParameterType(parameterTypes[1], HooksLoader.getReflectionHooks().getAddNodeGarbageValue());
                if (garbageArgument == null) {
                    log.error("Unsupported addNode garbage value type '{}'", parameterTypes[1].getName());
                    return;
                }
                addNode.invoke(packetWriter, packetBufferNode, garbageArgument);
            } else {
                // Path 2: static utility method — addNode(packetWriter, packetBufferNode[, garbage]).
                if (parameterTypes.length == 2) {
                    addNode.invoke(null, packetWriter, packetBufferNode);
                } else if (parameterTypes.length == 3) {
                    Object garbageArgument = GarbageValueUtils.coerceToParameterType(parameterTypes[2], HooksLoader.getReflectionHooks().getAddNodeGarbageValue());
                    if (garbageArgument == null) {
                        log.error("Unsupported addNode garbage value type '{}'", parameterTypes[2].getName());
                        return;
                    }
                    addNode.invoke(null, packetWriter, packetBufferNode, garbageArgument);
                } else {
                    log.error("addNode method has an unexpected parameter count: {}", parameterTypes.length);
                }
            }
        } catch (Exception e) {
            log.error("Failed during addNode packet queueing: ", e);
        }
    }

    /**
     * Returns the cached {@code addNode} {@link Method}, resolving it on first use.
     * <p>
     * For Path 1 the cached handle is only reused while it still belongs to the live
     * {@code PacketWriter}'s class and accepts the live node's class as its first parameter;
     * otherwise it is re-resolved. The Path 2 handle is a static utility method and is stable
     * for the life of the client.
     *
     * @param packetWriter     The live {@code PacketWriter} instance.
     * @param packetBufferNode The packet node about to be queued.
     * @return The resolved {@code addNode} method with its accessible flag set, or {@code null} if it cannot be found.
     */
    private Method getAddNodeMethod(Object packetWriter, Object packetBufferNode) {
        if (isUsingClientAddNode) {
            Class<?> packetWriterClass = packetWriter.getClass();
            Class<?> packetBufferNodeClass = packetBufferNode.getClass();
            Method cached = addNodeMethod;
            if (isAddNodeCacheValid(cached, packetWriterClass, packetBufferNodeClass)) {
                return cached;
            }
            synchronized (resolutionLock) {
                cached = addNodeMethod;
                if (isAddNodeCacheValid(cached, packetWriterClass, packetBufferNodeClass)) {
                    return cached;
                }
                Method resolved = resolveAddNodeOnPacketWriter(packetWriterClass, packetBufferNodeClass);
                if (resolved != null) {
                    resolved.setAccessible(true);
                    addNodeMethod = resolved;
                }
                return resolved;
            }
        }

        Method cached = addNodeMethod;
        if (cached != null) {
            return cached;
        }
        synchronized (resolutionLock) {
            if (addNodeMethod != null) {
                return addNodeMethod;
            }
            Method resolved = findStaticAddNodeMethod();
            if (resolved != null) {
                resolved.setAccessible(true);
                addNodeMethod = resolved;
            }
            return resolved;
        }
    }

    /**
     * Checks whether a cached Path 1 {@code addNode} handle still matches the live client objects.
     *
     * @param cached                The currently cached method, possibly {@code null}.
     * @param packetWriterClass     The live {@code PacketWriter}'s class.
     * @param packetBufferNodeClass The live packet node's class.
     * @return True if the cached handle can be applied to the live objects.
     */
    private static boolean isAddNodeCacheValid(Method cached, Class<?> packetWriterClass, Class<?> packetBufferNodeClass) {
        return cached != null
                && cached.getDeclaringClass() == packetWriterClass
                && cached.getParameterCount() == 2
                && cached.getParameterTypes()[0] == packetBufferNodeClass;
    }

    /**
     * Resolves the Path 1 {@code addNode} method on the {@code PacketWriter} class.
     * <p>
     * The exact signature implied by the garbage value's magnitude is looked up first, preserving
     * the historically verified resolution for the current hooks. Only if that signature does not
     * exist does this fall back to scanning for a same-named two-parameter method taking the packet
     * node and a primitive numeric garbage parameter, whose declared width then drives the
     * argument coercion in {@link #addNode(Object, Object)}.
     *
     * @param packetWriterClass     The live {@code PacketWriter}'s class.
     * @param packetBufferNodeClass The packet node class the method must accept.
     * @return The resolved method, or {@code null} if no candidate exists.
     */
    private Method resolveAddNodeOnPacketWriter(Class<?> packetWriterClass, Class<?> packetBufferNodeClass) {
        String methodName = HooksLoader.getReflectionHooks().getAddNodeMethodName();
        long garbageMagnitude = Math.abs(HooksLoader.getReflectionHooks().getAddNodeGarbageValue().longValue());
        Class<?> preferredGarbageType = garbageMagnitude < 256 ? byte.class : garbageMagnitude < 32768 ? short.class : int.class;

        try {
            return packetWriterClass.getDeclaredMethod(methodName, packetBufferNodeClass, preferredGarbageType);
        } catch (NoSuchMethodException e) {
            log.debug("addNode signature ({}, {}) not found, falling back to declared-type scan", packetBufferNodeClass.getName(), preferredGarbageType.getName());
        }

        for (Method method : packetWriterClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)
                    && method.getParameterCount() == 2
                    && method.getParameterTypes()[0] == packetBufferNodeClass
                    && GarbageValueUtils.isSupportedParameterType(method.getParameterTypes()[1])) {
                return method;
            }
        }
        return null;
    }

    /**
     * Resolves the reflection {@code Method} for the static utility variant of {@code addNode}.
     * <p>
     * This lookup is exclusively used when the packet-queueing logic is detached from the
     * {@code PacketWriter} class (Path 2). To locate the correct obfuscated method, this scans
     * the target utility class for a method that matches the injected hook name and explicitly
     * declares the {@code PacketWriter} class as its first parameter.
     *
     * @return The static {@code addNode} {@link Method}, or {@code null} if the method cannot be found
     * or the target class fails to load.
     */
    private Method findStaticAddNodeMethod() {
        try {
            Class<?> addNodeClass = client.getClass().getClassLoader().loadClass(HooksLoader.getReflectionHooks().getAddNodeClassName());

            for (Method method : addNodeClass.getDeclaredMethods()) {
                // Identify the static utility variant of addNode (Path 2).
                // Because this method is detached from the PacketWriter class, it cannot access the
                // writer implicitly. Therefore, its signature MUST explicitly accept the PacketWriter
                // as its first argument (e.g., `ab.az(packetWriter, buffer)` instead of
                // `packetWriter.az(buffer)`). We filter the class methods based on this requirement.
                if (method.getName().equals(HooksLoader.getReflectionHooks().getAddNodeMethodName())
                        && method.getParameterCount() > 0
                        && method.getParameterTypes()[0].getSimpleName().equals(HooksLoader.getReflectionHooks().getPacketWriterClassName())) {
                    return method;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("Failed to locate addNode method: {} in class {}: ", HooksLoader.getReflectionHooks().getAddNodeMethodName(), HooksLoader.getReflectionHooks().getAddNodeClassName(), e);
        }

        return null;
    }

    /**
     * Loads a class from the game client via RuneLite's class loader.
     *
     * @param name The obfuscated or non-obfuscated name of the class to load.
     * @return The loaded {@code Class} object, or null if not found.
     */
    private Class<?> loadGameClientClass(String name) {
        try {
            ClassLoader clientLoader = client.getClass().getClassLoader();
            return clientLoader.loadClass(name);
        } catch (ClassNotFoundException e) {
            log.error("Failed to load game client class: {}", name, e);
        }
        return null;
    }

    /**
     * Returns the cached static method responsible for creating a {@code PacketBufferNode},
     * resolving it on first use by scanning the accessor class for the method whose return
     * type is the {@code PacketBufferNode} class.
     *
     * @return The reflected {@code Method} object with its accessible flag set, or null if not found.
     */
    private Method getGetPacketBufferNode() {
        Method cached = getPacketBufferNodeMethod;
        if (cached != null) {
            return cached;
        }
        synchronized (resolutionLock) {
            if (getPacketBufferNodeMethod != null) {
                return getPacketBufferNodeMethod;
            }
            try {
                Class<?> packetBufferNodeAccessorClass = loadGameClientClass(HooksLoader.getReflectionHooks().getClassContainingPacketBufferNodeName());
                if (packetBufferNodeAccessorClass == null) {
                    return null;
                }

                Class<?> packetBufferNodeClass = loadGameClientClass(HooksLoader.getReflectionHooks().getPacketBufferNodeClassName());
                if (packetBufferNodeClass == null) {
                    return null;
                }

                // Find the method within the accessor class that returns a PacketBufferNode.
                // This is fragile and assumes only one such method exists.
                Method resolved = Arrays.stream(packetBufferNodeAccessorClass.getDeclaredMethods())
                        .filter(m -> m.getReturnType().equals(packetBufferNodeClass))
                        .findFirst()
                        .orElse(null);
                if (resolved != null) {
                    resolved.setAccessible(true);
                    getPacketBufferNodeMethod = resolved;
                }
                return resolved;
            } catch (Exception e) {
                log.error("Failed to get packet buffer node method: ", e);
            }
            return null;
        }
    }

    /**
     * Returns the cached {@code ClientPacket} class, which contains static definitions for packets,
     * loading it on first use.
     *
     * @return The {@code ClientPacket} class, or null if not found.
     */
    private Class<?> getClientPacketClass() {
        Class<?> cached = clientPacketClass;
        if (cached != null) {
            return cached;
        }
        synchronized (resolutionLock) {
            if (clientPacketClass == null) {
                clientPacketClass = loadGameClientClass(HooksLoader.getReflectionHooks().getClientPacketClassName());
            }
            return clientPacketClass;
        }
    }

    /**
     * Returns the cached {@code Field} holding the raw buffer on the packet node class,
     * resolving it on first use. The handle is only reused while it belongs to the live
     * node's class.
     *
     * @param packetBufferNodeClass The live packet node's class.
     * @return The reflected {@code Field} with its accessible flag set.
     * @throws NoSuchFieldException If the hooked field name does not exist on the node class.
     */
    private Field getPacketBufferField(Class<?> packetBufferNodeClass) throws NoSuchFieldException {
        Field cached = packetBufferField;
        if (cached != null && cached.getDeclaringClass() == packetBufferNodeClass) {
            return cached;
        }
        synchronized (resolutionLock) {
            cached = packetBufferField;
            if (cached != null && cached.getDeclaringClass() == packetBufferNodeClass) {
                return cached;
            }
            Field resolved = packetBufferNodeClass.getDeclaredField(HooksLoader.getReflectionHooks().getPacketBufferFieldName());
            resolved.setAccessible(true);
            packetBufferField = resolved;
            return resolved;
        }
    }

    /**
     * Returns the cached static {@code PacketWriter} field on the client class, resolving it
     * on first use. Only the field handle is cached — the {@code PacketWriter} instance is
     * read through it on every use.
     *
     * @return The reflected {@code Field} object with its accessible flag set, or null if not found.
     */
    private Field getPacketWriterField() {
        Field cached = packetWriterField;
        if (cached != null) {
            return cached;
        }
        synchronized (resolutionLock) {
            if (packetWriterField != null) {
                return packetWriterField;
            }
            try {
                Field resolved = client.getClass().getDeclaredField(HooksLoader.getReflectionHooks().getPacketWriterFieldName());
                resolved.setAccessible(true);
                packetWriterField = resolved;
                return resolved;
            } catch (NoSuchFieldException e) {
                log.error("Failed to get field: {}", HooksLoader.getReflectionHooks().getPacketWriterFieldName(), e);
            }
            return null;
        }
    }

    /**
     * Retrieves the {@code IsaacCipher} object from the {@code PacketWriter}.
     * The cipher is needed to correctly construct the packet header. The cipher instance is
     * read from the live {@code PacketWriter} on every call; only the field handle is cached.
     *
     * @return The {@code IsaacCipher} instance, or null if failed.
     */
    private Object getIsaacObject() {
        try {
            Field writerField = getPacketWriterField();
            if (writerField == null) return null;

            Object packetWriter = writerField.get(null); // Get static field
            if (packetWriter == null) {
                 log.error("PacketWriter object is null, cannot get ISAAC cipher.");
                 return null;
            }

            return getIsaacField(packetWriter.getClass()).get(packetWriter); // Get instance field
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to get ISAAC object: ", e);
        }
        return null;
    }

    /**
     * Returns the cached ISAAC cipher {@code Field} on the {@code PacketWriter} class, resolving
     * it on first use. The handle is only reused while it belongs to the live writer's class.
     *
     * @param packetWriterClass The live {@code PacketWriter}'s class.
     * @return The reflected {@code Field} with its accessible flag set.
     * @throws NoSuchFieldException If the hooked field name does not exist on the writer class.
     */
    private Field getIsaacField(Class<?> packetWriterClass) throws NoSuchFieldException {
        Field cached = isaacField;
        if (cached != null && cached.getDeclaringClass() == packetWriterClass) {
            return cached;
        }
        synchronized (resolutionLock) {
            cached = isaacField;
            if (cached != null && cached.getDeclaringClass() == packetWriterClass) {
                return cached;
            }
            Field resolved = packetWriterClass.getDeclaredField(HooksLoader.getReflectionHooks().getIsaacCipherFieldName());
            resolved.setAccessible(true);
            isaacField = resolved;
            return resolved;
        }
    }

    /**
     * Finds a specific static packet definition field within the {@code ClientPacket} class,
     * cached per packet name. Only the field handle is cached — the packet definition instance
     * is read through it on every send.
     *
     * @param name The name of the packet field (e.g., "IF_BUTTON1").
     * @return The reflected {@code Field} object with its accessible flag set, or null if not found.
     */
    private Field fetchPacketField(String name) {
        Field cached = packetDefinitionFields.get(name);
        if (cached != null) {
            return cached;
        }

        Class<?> clientPacket = getClientPacketClass();
        if (clientPacket == null) return null;
        try {
            Field resolved = clientPacket.getDeclaredField(name);
            resolved.setAccessible(true);
            packetDefinitionFields.put(name, resolved);
            return resolved;
        } catch (NoSuchFieldException e) {
            log.error("Failed to get packet field: {}", name, e);
        }
        return null;
    }
}
