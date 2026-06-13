package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketWrite;
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

/**
 * {@code PacketClient} is an instance-based RuneLite client packet sending utility which uses reflection to
 * construct and send low-level packets directly to the game servers. Generally, you should not need to use this class directly
 * within your plugins as it functions at a lower level to construct and sending packets.
 * <p>
 * Instead, it's recommended to use the higher level API's like {@code MousePackets}, {@code WidgetPackets}, or {@code NpcPackets} for
 * sending game packets to the server based on your specific entity interaction needs (clicking interfaces, NPC's, GameObjects, etc...
 */
@Slf4j
@Singleton
public class PacketClient {

    @Getter
    private final Client client;

    private final boolean isUsingClientAddNode;

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
        // 1. Get all necessary reflection components to build and send the packet.
        Object packetBufferNode = null;
        Method getPacketBufferNode = getGetPacketBufferNode();
        Class<?> clientPacket = getClientPacketClass();
        Object isaac = getIsaacObject();

        if (getPacketBufferNode == null || clientPacket == null || isaac == null) {
            log.error("Failed to get critical reflection components for sending packet: {}", def.getObfuscatedName());
            return;
        }

        // Invoke the getPacketBufferNode method to create a new packet node instance.
        // This method is obfuscated and may require a "garbage value" of a specific type.
        getPacketBufferNode.setAccessible(true);
        long garbageValue = Math.abs(HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue());

        try {
            Field packetField = fetchPacketField(def.getObfuscatedName());
            if (packetField == null) {
                log.error("Could not find packet field for: {}", def.getObfuscatedName());
                getPacketBufferNode.setAccessible(false);
                return;
            }
            Object packetDefInstance = packetField.get(clientPacket);

            // The method signature for getPacketBufferNode changes based on the obfuscated garbage value.
            if (garbageValue < 256) {
                packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue().byteValue());
            } else if (garbageValue < 32768) {
                packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue().shortValue());
            } else if (garbageValue < Integer.MAX_VALUE) {
                packetBufferNode = getPacketBufferNode.invoke(null, packetDefInstance, isaac, HooksLoader.getReflectionHooks().getPacketBufferNodeGarbageValue());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Failed to invoke getPacketBufferNode: ", e);
            e.printStackTrace();
        } finally {
            getPacketBufferNode.setAccessible(false);
        }

        if (packetBufferNode == null) {
            log.error("PacketBufferNode was null after creation attempt for packet: {}", def.getObfuscatedName());
            return;
        }

        // Get the raw 'buffer' object from the 'packetBufferNode' to write data into.
        Object buffer;
        try {
            Field bufferField = packetBufferNode.getClass().getDeclaredField(HooksLoader.getReflectionHooks().getPacketBufferFieldName());
            bufferField.setAccessible(true);
            buffer = bufferField.get(packetBufferNode);
            bufferField.setAccessible(false);
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

            // 6. Get the PacketWriter field and queue the fully constructed packet node.
            Field packetWriterField = getPacketWriterField();
            if (packetWriterField == null) {
                log.error("Could not get PacketWriter field to queue packet.");
                return;
            }

            packetWriterField.setAccessible(true);
            try {
                Object packetWriter = packetWriterField.get(null);
                if (packetWriter != null) {
                    addNode(packetWriter, packetBufferNode);
                } else {
                    log.error("PacketWriter object was null.");
                }
            } catch (Exception e) {
                log.error("Failed to add packet node to queue: ", e);
                e.printStackTrace();
            } finally {
                packetWriterField.setAccessible(false);
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
     * the correct reflection call—including the dynamic resolution of anti-reversing dummy "garbage values"
     * (byte, short, or int)—based on the current revision's hooks.
     *
     * @param packetWriter     The client's {@code PacketWriter} instance responsible for handling network I/O.
     * @param packetBufferNode The fully constructed packet node containing the payload to be sent.
     */
    private void addNode(Object packetWriter, Object packetBufferNode) {
        try {
            // Path 1: The 'addNode' method is a member of the PacketWriter class itself and is accessed
            // via a static field from the client class like: client.aq.az() client.packetWriter.addNode()
            if (isUsingClientAddNode) {
                Method addNode = null;
                long garbageValue = Math.abs(HooksLoader.getReflectionHooks().getAddNodeGarbageValue());

                // Find the correct obfuscated method signature based on the garbage value type.
                if (garbageValue < 256) {
                    addNode = packetWriter.getClass().getDeclaredMethod(HooksLoader.getReflectionHooks().getAddNodeMethodName(), packetBufferNode.getClass(), byte.class);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, HooksLoader.getReflectionHooks().getAddNodeGarbageValue().byteValue());
                } else if (garbageValue < 32768) {
                    addNode = packetWriter.getClass().getDeclaredMethod(HooksLoader.getReflectionHooks().getAddNodeMethodName(), packetBufferNode.getClass(), short.class);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, HooksLoader.getReflectionHooks().getAddNodeGarbageValue().shortValue());
                } else if (garbageValue < Integer.MAX_VALUE) {
                    addNode = packetWriter.getClass().getDeclaredMethod(HooksLoader.getReflectionHooks().getAddNodeMethodName(), packetBufferNode.getClass(), int.class);
                    addNode.setAccessible(true);
                    addNode.invoke(packetWriter, packetBufferNode, HooksLoader.getReflectionHooks().getAddNodeGarbageValue());
                }

                if (addNode != null) {
                    addNode.setAccessible(false);
                }
            } else {
                // Path 2: The 'addNode' method is a static utility method found elsewhere. This means we must find the method
                // specifically because it won't exist on the packetWriter class. It will also accept packetwriter AND packetBuffer as arguments.
                Method addNode = getAddNodeMethod();
                if(addNode == null) {
                    log.error("Failed to locate addNode method: {} in class {}: ", HooksLoader.getReflectionHooks().getAddNodeMethodName(), HooksLoader.getReflectionHooks().getAddNodeClassName());
                    return;
                }

                addNode.setAccessible(true);

                if (addNode.getParameterCount() == 2) {
                    // Method signature is: addNode(packetWriter, packetBufferNode)
                    addNode.invoke(null, packetWriter, packetBufferNode);
                } else {
                    // Method signature is: addNode(packetWriter, packetBufferNode, garbageValue)
                    long garbageValue = Math.abs(HooksLoader.getReflectionHooks().getAddNodeGarbageValue().longValue());
                    if (garbageValue < 256) {
                        addNode.invoke(null, packetWriter, packetBufferNode, HooksLoader.getReflectionHooks().getAddNodeGarbageValue().byteValue());
                    } else if (garbageValue < 32768) {
                        addNode.invoke(null, packetWriter, packetBufferNode, HooksLoader.getReflectionHooks().getAddNodeGarbageValue().shortValue());
                    } else if (garbageValue < Integer.MAX_VALUE) {
                        addNode.invoke(null, packetWriter, packetBufferNode, HooksLoader.getReflectionHooks().getAddNodeGarbageValue());
                    }
                }
                addNode.setAccessible(false);
            }
        } catch (Exception e) {
            log.error("Failed during addNode packet queueing: ", e);
        }
    }

    /**
     * Resolves the reflection {@code Method} for the static utility variant of {@code addNode}.
     * <p>
     * This lookup is exclusively used when the packet-queueing logic is detached from the
     * {@code PacketWriter} class (Path 2). To locate the correct obfuscated method, this scans
     * the target utility class for a method that matches the injected hook name and explicitly
     * declares the {@code PacketWriter} class as its first parameter.
     * * @return The static {@code addNode} {@link Method}, or {@code null} if the method cannot be found
     * or the target class fails to load.
     */
    private Method getAddNodeMethod() {
        Method addNodeMethod = null;
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
                        && method.getParameterTypes().length != 0
                        && method.getParameterTypes()[0].getSimpleName().equals(HooksLoader.getReflectionHooks().getPacketWriterClassName())) {
                    addNodeMethod = method;
                    break;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("Failed to locate addNode method: {} in class {}: ", HooksLoader.getReflectionHooks().getAddNodeMethodName(), HooksLoader.getReflectionHooks().getAddNodeClassName(), e);
        }

        return addNodeMethod;
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
     * Finds the static method responsible for creating a {@code PacketBufferNode}.
     *
     * @return The reflected {@code Method} object, or null if not found.
     */
    private Method getGetPacketBufferNode() {
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
            return Arrays.stream(packetBufferNodeAccessorClass.getDeclaredMethods())
                    .filter(m -> m.getReturnType().equals(packetBufferNodeClass))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.error("Failed to get packet buffer node method: ", e);
        }
        return null;
    }

    /**
     * Loads the {@code ClientPacket} class, which contains static definitions for packets.
     *
     * @return The {@code ClientPacket} class, or null if not found.
     */
    private Class<?> getClientPacketClass() {
        return loadGameClientClass(HooksLoader.getReflectionHooks().getClientPacketClassName());
    }

    /**
     * Finds the client's static {@code PacketWriter} field.
     *
     * @return The reflected {@code Field} object, or null if not found.
     */
    private Field getPacketWriterField() {
        try {
            return client.getClass().getDeclaredField(HooksLoader.getReflectionHooks().getPacketWriterFieldName());
        } catch (NoSuchFieldException e) {
            log.error("Failed to get field: {}", HooksLoader.getReflectionHooks().getPacketWriterFieldName(), e);
        }
        return null;
    }

    /**
     * Retrieves the {@code IsaacCipher} object from the {@code PacketWriter}.
     * The cipher is needed to correctly construct the packet header.
     *
     * @return The {@code IsaacCipher} instance, or null if failed.
     */
    private Object getIsaacObject() {
        try {
            Field packetWriterField = getPacketWriterField();
            if (packetWriterField == null) return null;

            packetWriterField.setAccessible(true);
            Object packetWriter = packetWriterField.get(null); // Get static field
            packetWriterField.setAccessible(false);

            if (packetWriter == null) {
                 log.error("PacketWriter object is null, cannot get ISAAC cipher.");
                 return null;
            }

            Class<?> packetWriterClass = packetWriter.getClass();
            Field isaacField = packetWriterClass.getDeclaredField(HooksLoader.getReflectionHooks().getIsaacCipherFieldName());
            isaacField.setAccessible(true);
            Object isaacObject = isaacField.get(packetWriter); // Get instance field
            isaacField.setAccessible(false);

            return isaacObject;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed to get ISAAC object: ", e);
        }
        return null;
    }

    /**
     * Finds a specific static packet definition field within the {@code ClientPacket} class.
     *
     * @param name The name of the packet field (e.g., "IF_BUTTON1").
     * @return The reflected {@code Field} object, or null if not found.
     */
    private Field fetchPacketField(String name) {
        try {
            Class<?> clientPacket = getClientPacketClass();
            if (clientPacket == null) return null;
            return clientPacket.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            log.error("Failed to get packet field: {}", name, e);
        }
        return null;
    }
}