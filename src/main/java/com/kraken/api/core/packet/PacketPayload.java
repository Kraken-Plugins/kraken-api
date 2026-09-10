package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketDefinition;
import com.kraken.api.core.packet.model.PacketWrite;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Validates and serializes payloads without touching the client writer, node pool or cipher. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PacketPayload {

    static byte[] encode(PacketDefinition definition, Object[] values, int packetLength) {
        if (definition == null || values == null || definition.getWrites() == null) {
            throw new IllegalArgumentException("Packet definition, writes and values are required");
        }

        List<String> parameters = definition.getType().getParams();
        if (parameters == null || values.length != parameters.size()) {
            throw new IllegalArgumentException("Packet argument count does not match its type");
        }

        // Pinned factory capacity includes the one-byte encrypted opcode.
        int capacity;
        if (packetLength == -1) {
            capacity = 260;
        } else if (packetLength == -2) {
            capacity = 10000;
        } else if (packetLength >= 0 && packetLength <= 259) {
            capacity = packetLength <= 18 ? 20 : packetLength <= 98 ? 100 : 260;
        } else {
            throw new IllegalArgumentException("Unsupported packet length: " + packetLength);
        }
        BufferUtils.ScratchBuffer scratch = new BufferUtils.ScratchBuffer(capacity - 1);
        Set<String> written = new HashSet<>();
        for (PacketWrite write : definition.getWrites()) {
            if (write == null || !written.add(write.getParam()) || write.getOperations() == null
                    || write.getOperations().length == 0) {
                throw new IllegalArgumentException("Missing or duplicate packet write");
            }
            int index = parameters.indexOf(write.getParam());
            if (index < 0 || values[index] == null) {
                throw new IllegalArgumentException("Missing packet parameter: " + write.getParam());
            }
            for (BufferOperation operation : write.getOperations()) {
                if (operation == null || operation.getType() == null) {
                    throw new IllegalArgumentException("Missing buffer operation");
                }
                switch (operation.getType()) {
                    case STRING_CP1252_NULL_TERMINATED:
                    case STRING_CP1252_NULL_CIRCUMFIXED:
                        if (!(values[index] instanceof String) || ((String) values[index]).indexOf('\0') >= 0) {
                            throw new IllegalArgumentException("Packet strings must be non-null and contain no NUL characters");
                        }
                        break;
                    default:
                        if (!(values[index] instanceof Number)) {
                            throw new IllegalArgumentException("Numeric packet value required: " + write.getParam());
                        }
                        if (operation.getType() != BufferOperation.BufferOperationType.RAW) {
                            int operand = operation.requireOperand();
                            if (operation.getType() == BufferOperation.BufferOperationType.RIGHT_SHIFT && (operand < 0 || operand > 31)) {
                                throw new IllegalArgumentException("Invalid byte shift: " + operand);
                            }
                        }
                }
                BufferUtils.writeOperation(operation, values[index], scratch);
            }
        }
        if (written.size() != parameters.size()) {
            throw new IllegalArgumentException("Packet write plan omits parameters");
        }
        int size = BufferUtils.getOffset(scratch)
                * HooksLoader.getReflectionHooks().getIndexMultiplier();
        byte[] bytes = BufferUtils.getArray(scratch);
        if (packetLength >= 0 && size != packetLength) {
            throw new IllegalArgumentException("Encoded payload does not match the live packet length");
        }
        if (packetLength == -1 && (size < 1 || size - 1 > 255 || (bytes[0] & 255) != size - 1)) {
            throw new IllegalArgumentException("Invalid one-byte packet length prefix");
        }
        if (packetLength == -2 && (size < 2 || ((bytes[0] & 255) << 8 | bytes[1] & 255) != size - 2)) {
            throw new IllegalArgumentException("Invalid two-byte packet length prefix");
        }
        return Arrays.copyOf(bytes, size);
    }
}
