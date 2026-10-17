package com.kraken.api.core.packet.model;

import lombok.*;

/**
 * One step that encodes a packet value into the buffer. Numeric operations write one byte; string operations
 * write CP1252 text with null bytes around it.
 */
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BufferOperation {
    /** The kind of encoding this operation applies. */
    private BufferOperationType type;
    /** The constant the operation uses, or {@code null} for operations that take none. */
    private Integer operand;

    /**
     * Writes the value's low byte unchanged.
     * @return A raw operation.
     */
    public static BufferOperation raw() {
        return new BufferOperation(BufferOperationType.RAW, null);
    }

    /**
     * Writes the value plus the operand, truncated to one byte.
     * @param operand The constant added to the value.
     * @return An add operation.
     */
    public static BufferOperation add(int operand) {
        return new BufferOperation(BufferOperationType.ADD, operand);
    }

    /**
     * Writes the operand minus the value, truncated to one byte.
     * @param operand The constant the value is subtracted from.
     * @return A subtract operation.
     */
    public static BufferOperation subtract(int operand) {
        return new BufferOperation(BufferOperationType.SUBTRACT, operand);
    }

    /**
     * Writes the value shifted right by the operand bit count, truncated to one byte.
     * @param operand The number of bits to shift right, from 0 to 31.
     * @return A right shift operation.
     */
    public static BufferOperation rightShift(int operand) {
        return new BufferOperation(BufferOperationType.RIGHT_SHIFT, operand);
    }

    /**
     * Writes a CP1252 string followed by a single null byte.
     * @return A null-terminated string operation.
     */
    public static BufferOperation stringCp1252NullTerminated() {
        return new BufferOperation(BufferOperationType.STRING_CP1252_NULL_TERMINATED, null);
    }

    /**
     * Writes a null byte, a CP1252 string, then a null byte.
     * @return A null-circumfixed string operation.
     */
    public static BufferOperation stringCp1252NullCircumfixed() {
        return new BufferOperation(BufferOperationType.STRING_CP1252_NULL_CIRCUMFIXED, null);
    }

    /**
     * Returns the operand, failing when this operation takes none.
     * @return The operand value.
     * @throws IllegalStateException If this operation has no operand.
     */
    public int requireOperand() {
        if (operand == null) {
            throw new IllegalStateException("Buffer operation " + type + " requires an operand");
        }
        return operand;
    }

    /** The kinds of buffer operation, matching the type names in {@code hooks.json}. */
    public enum BufferOperationType {
        /** Writes the value's low byte unchanged. */
        RAW,
        /** Writes the value plus the operand. */
        ADD,
        /** Writes the operand minus the value. */
        SUBTRACT,
        /** Writes the value shifted right by the operand. */
        RIGHT_SHIFT,
        /** Writes a CP1252 string followed by a null byte. */
        STRING_CP1252_NULL_TERMINATED,
        /** Writes a null byte, a CP1252 string, then a null byte. */
        STRING_CP1252_NULL_CIRCUMFIXED
    }
}
