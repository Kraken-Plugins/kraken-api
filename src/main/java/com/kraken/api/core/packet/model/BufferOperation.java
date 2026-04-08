package com.kraken.api.core.packet.model;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BufferOperation {
    private BufferOperationType type;
    private Integer operand;

    public static BufferOperation raw() {
        return new BufferOperation(BufferOperationType.RAW, null);
    }

    public static BufferOperation add(int operand) {
        return new BufferOperation(BufferOperationType.ADD, operand);
    }

    public static BufferOperation subtract(int operand) {
        return new BufferOperation(BufferOperationType.SUBTRACT, operand);
    }

    public static BufferOperation rightShift(int operand) {
        return new BufferOperation(BufferOperationType.RIGHT_SHIFT, operand);
    }

    public static BufferOperation stringCp1252NullTerminated() {
        return new BufferOperation(BufferOperationType.STRING_CP1252_NULL_TERMINATED, null);
    }

    public static BufferOperation stringCp1252NullCircumfixed() {
        return new BufferOperation(BufferOperationType.STRING_CP1252_NULL_CIRCUMFIXED, null);
    }

    public int requireOperand() {
        if (operand == null) {
            throw new IllegalStateException("Buffer operation " + type + " requires an operand");
        }
        return operand;
    }

    public enum BufferOperationType {
        RAW,
        ADD,
        SUBTRACT,
        RIGHT_SHIFT,
        STRING_CP1252_NULL_TERMINATED,
        STRING_CP1252_NULL_CIRCUMFIXED
    }
}
