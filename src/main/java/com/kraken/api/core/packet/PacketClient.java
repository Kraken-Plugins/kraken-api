package com.kraken.api.core.packet;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.core.ClientThreadException;
import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.util.GarbageValueUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code PacketClient} is an instance-based RuneLite client packet sending utility which uses reflection to
 * construct and send low-level packets directly to the game servers. Generally, you should not need to use this class directly
 * within your plugins as it functions at a lower level to construct and sending packets.
 * <p>
 * Instead, it's recommended to use the higher level API's like {@code MousePackets}, {@code WidgetPackets}, or {@code NpcPackets} for
 * sending game packets to the server based on your specific entity interaction needs (clicking interfaces, NPC's, GameObjects, etc...
 * <p>
 * Factory, writer, cipher, node-buffer and enqueue handles are cached with the same
 * double-checked-locking shape {@code DoActionInvoker} uses. Only the handles are cached — the
 * {@code PacketWriter} instance, ISAAC cipher and packet definition constants are re-read from the
 * live client on every send. Each instance-bound handle is guarded by the declaring class of the
 * object it is applied to, so if the client ever supplies an object of a different class the handle
 * is re-resolved instead of being applied stale. Failed resolutions are never cached and are
 * retried on the next send. The live packet length is read reflectively during each preflight.
 * <p>
 * Every send runs on the client thread. The packet writer, node pool and ISAAC cipher this class
 * reaches through are the same objects the game thread uses for its own traffic, and nothing about
 * them is thread safe, so {@link #sendPacket} hands the whole build-write-enqueue sequence to the
 * client thread and {@link #sendPacketOnClientThread} refuses to run anywhere else.
 */
@Slf4j
@Singleton
public class PacketClient {

    @Getter
    private final Client client;

    private final Provider<Context> ctxProvider;

    private final boolean isUsingClientAddNode;

    private final Object resolutionLock = new Object();
    private volatile Class<?> clientPacketClass;
    private volatile Method getPacketBufferNodeMethod;
    private volatile Field packetWriterField;
    private volatile Field isaacField;
    private volatile Field packetBufferField;
    private volatile Method addNodeMethod;
    private boolean transportCompromised;
    private final Map<String, Field> packetDefinitionFields = new ConcurrentHashMap<>();

    /**
     * Creates a new PacketSender. This constructor initializes packet queueing functionality by either loading the client packet
     * sending method from the cached JSON file or running an analysis on the RuneLite injected client
     * to determine the packet sending method.
     *
     * @param client The RuneLite Client instance.
     * @param ctxProvider Supplies the {@link Context} used to hand each send to the client thread.
     */
    @Inject
    public PacketClient(Client client, Provider<Context> ctxProvider) {
        this.client = client;
        this.ctxProvider = ctxProvider;
        // Some revs the packet add node method will be like client.aq.az() client.packetWriter.addNode() but other times
        // it may be on a static helper class like ap.aq.az() helper.packetWriter.addNode()
        this.isUsingClientAddNode = HooksLoader.getReflectionHooks().getAddNodeClassName().equalsIgnoreCase("client");
    }

    /**
     * Constructs and sends a packet to the game server.
     * This is the primary public method of this class, and the only entry point that may be called
     * from a worker thread: it marshals the whole operation onto the client thread and blocks until
     * the packet has been queued, so packets sent one after another reach the writer in that order.
     *
     * @param def     The {@link PacketDefinition} enumeration defining the packet structure.
     * @param objects The data (payload) for the packet, in the order defined by the PacketDefinition.
     * @throws ClientThreadException if execution has an unknown outcome or transport recovery is required
     */
    public void sendPacket(PacketDefinition def, Object... objects) {
        Objects.requireNonNull(def, "Packet definition is required");
        boolean sent = ctxProvider.get().runOnClientThreadOptional(() -> {
            sendPacketOnClientThread(def, objects);
            return Boolean.TRUE;
        }).isPresent();

        if (!sent) {
            log.error("Packet {} was not sent: the client thread did not accept it", def.getObfuscatedName());
        }
    }

    /**
     * Builds, writes and queues a packet. Runs on the client thread only.
     *
     * @param def     The {@link PacketDefinition} enumeration defining the packet structure.
     * @param objects The data (payload) for the packet, in the order defined by the PacketDefinition.
     * @throws IllegalStateException if called from any thread other than the client thread
     */
    private void sendPacketOnClientThread(PacketDefinition def, Object... objects) {
        if (!client.isClientThread()) {
            throw new IllegalStateException("Packets must be built and queued on the client thread; "
                    + "reached from '" + Thread.currentThread().getName() + "' for packet " + def.getObfuscatedName()
                    + ". Call sendPacket(), which marshals for you.");
        }

        if (transportCompromised) {
            throw new ClientThreadException("Packet transport is compromised; restart the client before sending again", null, true);
        }

        boolean factoryInvoked = false;
        try {
            Method factory = getGetPacketBufferNode();
            Field writerField = getPacketWriterField();
            if (factory == null || writerField == null) {
                log.error("Missing packet factory or writer hooks; packet was not sent");
                return;
            }
            Object writer = writerField.get(null);
            if (writer == null) {
                throw new IllegalStateException("Packet writer is unavailable");
            }
            Object isaac = getIsaacField(writer.getClass()).get(writer);
            Field packetField = fetchPacketField(def.getObfuscatedName());
            if (packetField == null || !Modifier.isStatic(packetField.getModifiers())) {
                throw new IllegalStateException("Missing static packet definition field");
            }
            Object packet = packetField.get(null);
            Class<?>[] factoryTypes = factory.getParameterTypes();
            if (!Modifier.isStatic(factory.getModifiers()) || factoryTypes.length != 3
                    || !factoryTypes[0].isInstance(packet) || !factoryTypes[1].isInstance(isaac)) {
                throw new IllegalStateException("Invalid packet factory signature or live arguments");
            }
            Object factoryGarbage = requireGarbage(factoryTypes[2],
                    HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue());
            Class<?> nodeClass = factory.getReturnType();
            Field bufferField = getPacketBufferField(nodeClass);
            if (Modifier.isStatic(bufferField.getModifiers())) {
                throw new IllegalStateException("Packet buffer must be an instance field");
            }
            Method enqueue = getAddNodeMethod(writer, nodeClass);
            if (enqueue == null) {
                throw new IllegalStateException("Missing packet enqueue method");
            }
            Class<?>[] enqueueTypes = enqueue.getParameterTypes();
            Object[] enqueueArguments;
            if (isUsingClientAddNode) {
                if (Modifier.isStatic(enqueue.getModifiers()) || enqueueTypes.length != 2
                        || !enqueueTypes[0].isAssignableFrom(nodeClass)) {
                    throw new IllegalStateException("Invalid instance enqueue signature");
                }
                enqueueArguments = new Object[]{null, requireGarbage(enqueueTypes[1],
                        HooksLoader.getReflectionHooks().getAddNodeGarbageValue())};
            } else {
                if (!Modifier.isStatic(enqueue.getModifiers())
                        || (enqueueTypes.length != 2 && enqueueTypes.length != 3)
                        || !enqueueTypes[0].isInstance(writer) || !enqueueTypes[1].isAssignableFrom(nodeClass)) {
                    throw new IllegalStateException("Invalid static enqueue signature");
                }
                enqueueArguments = enqueueTypes.length == 2 ? new Object[]{writer, null}
                        : new Object[]{writer, null, requireGarbage(enqueueTypes[2],
                        HooksLoader.getReflectionHooks().getAddNodeGarbageValue())};
            }

            // Read the live packet length before the factory can advance ISAAC. These two hooks
            // must be re-vetted along with the factory capacity rules on every client revision.
            Field lengthField = packet.getClass().getDeclaredField(
                    HooksLoader.getReflectionHooks().getClientPacketLengthField());
            lengthField.setAccessible(true);
            if (lengthField.getType() != int.class || Modifier.isStatic(lengthField.getModifiers())) {
                throw new IllegalStateException("Invalid packet length field");
            }
            int length = lengthField.getInt(packet) * HooksLoader.getReflectionHooks().getClientPacketLengthMultiplier();
            BufferUtils.BufferAccess bufferAccess = BufferUtils.validateFields(bufferField.getType());
            byte[] payload = PacketPayload.encode(def, objects, length);

            // No payload conversion or reflection resolution is allowed beyond this boundary.
            factoryInvoked = true;
            Object node = factory.invoke(null, packet, isaac, factoryGarbage);
            Object buffer = bufferField.get(node);
            byte[] array = (byte[]) bufferAccess.array.get(buffer);
            int offset = bufferAccess.offset.getInt(buffer) * HooksLoader.getReflectionHooks().getIndexMultiplier();
            if (offset != 1 || array == null || payload.length > array.length - offset) {
                throw new IllegalStateException("Factory buffer violates the vetted opcode/capacity contract");
            }
            System.arraycopy(payload, 0, array, offset, payload.length);
            bufferAccess.offset.setInt(buffer, (offset + payload.length) * HooksLoader.getReflectionHooks().getOffsetMultiplier());
            enqueueArguments[isUsingClientAddNode ? 0 : 1] = node;
            enqueue.invoke(isUsingClientAddNode ? writer : null, enqueueArguments);
        } catch (Exception | LinkageError e) {
            if (factoryInvoked) {
                transportCompromised = true;
                log.error("Packet construction failed after possible ISAAC consumption; stopping game traffic. Restart the client.", e);
                try {
                    client.setGameState(net.runelite.api.GameState.LOGIN_SCREEN);
                } catch (Exception recoveryFailure) {
                    e.addSuppressed(recoveryFailure);
                }
                throw new ClientThreadException("Packet transport is compromised; restart the client", e, true);
            }
            throw new IllegalArgumentException("Packet rejected before ISAAC consumption", e);
        }
    }

    private static Object requireGarbage(Class<?> type, Integer value) {
        Object argument = GarbageValueUtils.coerceToParameterType(type, value);
        if (argument == null) {
            throw new IllegalArgumentException("Unsupported garbage parameter: " + type.getName());
        }
        return argument;
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
     * @param packetBufferNodeClass The factory return type, resolved before allocation.
     * @return The resolved {@code addNode} method with its accessible flag set, or {@code null} if it cannot be found.
     */
    private Method getAddNodeMethod(Object packetWriter, Class<?> packetBufferNodeClass) {
        if (isUsingClientAddNode) {
            Class<?> packetWriterClass = packetWriter.getClass();
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
     * argument coercion in the preflight phase.
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

                // Resolve the mapped factory name and signature, not an arbitrary node-returning method.
                Method resolved = Arrays.stream(packetBufferNodeAccessorClass.getDeclaredMethods())
                        .filter(m -> m.getName().equals(HooksLoader.getReflectionHooks().getPacketBufferNodeFactoryMethodName()))
                        .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getReturnType().equals(packetBufferNodeClass))
                        .filter(m -> m.getParameterCount() == 3 && m.getParameterTypes()[0] == getClientPacketClass()
                                && GarbageValueUtils.isSupportedParameterType(m.getParameterTypes()[2]))
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
