package com.kraken.api.core.packet;

import com.kraken.api.core.hooks.HooksLoader;
import com.kraken.api.core.packet.model.BufferOperation;
import com.kraken.api.core.packet.model.PacketWrite;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reverses the obfuscated buffer encoding written by {@link BufferUtils}.
 *
 * <p>The write side encodes each field by:
 * <ol>
 *   <li>Advancing a logical {@code offset} by {@code offsetMultiplier} once per byte written.</li>
 *   <li>Computing the real array index as {@code offset * indexMultiplier - 1}.</li>
 *   <li>Writing one byte derived from the full field value via the {@link BufferOperation}:
 *       <ul>
 *         <li>RAW         → {@code (byte) value}</li>
 *         <li>ADD(x)      → {@code (byte)(x + value)}</li>
 *         <li>SUBTRACT(x) → {@code (byte)(x - value)}</li>
 *         <li>RIGHT_SHIFT(n) → {@code (byte)(value >> n)}</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Reading back is the mirror image:
 * <ul>
 *   <li>Replay the same offset arithmetic to find each byte in the payload.</li>
 *   <li>Invert the obfuscation to recover the byte's bit contribution.</li>
 *   <li>OR all bit contributions for the same field together to get the original value.</li>
 * </ul>
 *
 * <p>The {@code indexMultiplier} and {@code offsetMultiplier} are the same obfuscated
 * constants used by {@link BufferUtils} and are loaded from PacketFactory.getReflectionHooks.
 */
public class BufferReader {

    /**
     * Decodes an encoded packet payload into a map of {@code param → value} entries,
     * in the order the writes were originally performed.
     *
     * @param payload the raw bytes captured from the packet buffer (after Isaac cipher decoding)
     * @param writes  the ordered list of {@link PacketWrite} descriptors for this packet opcode
     * @return a {@link LinkedHashMap} preserving write order, values as {@link Integer} or {@link String}
     */
    public static Map<String, Object> decode(byte[] payload, List<PacketWrite> writes) {
        Map<String, Object> result = new LinkedHashMap<>();

        int indexMultiplier = HooksLoader.getReflectionHooks().getIndexMultiplier();
        int offsetMultiplier = HooksLoader.getReflectionHooks().getOffsetMultiplier();

        // Offset starts at 0, identical to the write side.
        int offset = 0;

        for (PacketWrite write : writes) {
            BufferOperation[] ops = write.getOperations();
            if (ops == null || ops.length == 0) continue;

            BufferOperation.BufferOperationType firstType = ops[0].getType();

            if (firstType == BufferOperation.BufferOperationType.STRING_CP1252_NULL_TERMINATED) {
                ReadResult<String> r = readStringNullTerminated(payload, offset, indexMultiplier, offsetMultiplier);
                result.put(write.getParam(), r.value);
                offset = r.nextOffset;

            } else if (firstType == BufferOperation.BufferOperationType.STRING_CP1252_NULL_CIRCUMFIXED) {
                ReadResult<String> r = readStringNullCircumfixed(payload, offset, indexMultiplier, offsetMultiplier);
                result.put(write.getParam(), r.value);
                offset = r.nextOffset;

            } else {
                // Numeric field — each operation in the array is one byte of the same value.
                int value = 0;
                for (BufferOperation op : ops) {
                    // Mirror of BufferUtils.nextIndex() + the index formula.
                    offset += offsetMultiplier;
                    int arrayIndex = offset * indexMultiplier - 1;

                    byte rawByte = payload[arrayIndex];
                    value |= invertOperation(op, rawByte);
                }
                result.put(write.getParam(), value);
            }
        }

        return result;
    }

    /**
     * Inverts a single {@link BufferOperation} to recover the bits it contributed to
     * the original field value, ready to be OR-ed together with other bytes.
     *
     * <pre>
     * Operation         Written as              Inverted to
     * ─────────────     ──────────────────────  ─────────────────────────────────
     * RAW               (byte) value            rawByte & 0xFF              → bits 0-7
     * ADD(x)            (byte)(x + value)       (rawByte - x) & 0xFF        → bits 0-7
     * SUBTRACT(x)       (byte)(x - value)       (x - rawByte) & 0xFF        → bits 0-7
     * RIGHT_SHIFT(n)    (byte)(value >> n)       (rawByte & 0xFF) << n       → bits n-(n+7)
     * </pre>
     *
     * @param op      the operation that was used when writing this byte
     * @param rawByte the byte read back from the payload at the computed array index
     * @return the bit contribution of this byte toward the reconstructed integer field value
     */
    private static int invertOperation(BufferOperation op, byte rawByte) {
        switch (op.getType()) {
            case RAW:
                // Written as: arr[i] = (byte) value
                // So: rawByte = (byte) value  →  low 8 bits of value
                return rawByte & 0xFF;

            case ADD:
                // Written as: arr[i] = (byte)(operand + value)
                // So: rawByte = (byte)(operand + value)
                //    value_byte = rawByte - operand  (mod 256, low 8 bits)
                return (rawByte - op.requireOperand()) & 0xFF;

            case SUBTRACT:
                // Written as: arr[i] = (byte)(operand - value)
                // So: rawByte = (byte)(operand - value)
                //    value_byte = operand - rawByte  (mod 256, low 8 bits)
                return (op.requireOperand() - rawByte) & 0xFF;

            case RIGHT_SHIFT:
                // Written as: arr[i] = (byte)(value >> n)
                // So: rawByte carries bits n..(n+7) of value, already shifted down.
                // Shift them back up to their correct position.
                return (rawByte & 0xFF) << op.requireOperand();

            default:
                throw new IllegalArgumentException(
                        "BufferReader: cannot invert operation type " + op.getType());
        }
    }

    /**
     * Reads a CP1252, null-terminated string from the payload, mirroring
     * {@link BufferUtils#writeStringCp1252NullTerminated}.
     *
     * <p>Write layout (one byte per logical offset advance):
     * <pre>
     *   [ char0 ][ char1 ] ... [ charN ][ 0x00 ]
     * </pre>
     *
     * @param payload          raw payload bytes
     * @param offset           current logical offset <em>before</em> any advance for this field
     * @param indexMultiplier  obfuscated index multiplier
     * @param offsetMultiplier obfuscated offset increment (may be negative as a 32-bit int)
     * @return a {@link ReadResult} containing the decoded string and the updated offset
     */
    private static ReadResult<String> readStringNullTerminated(
            byte[] payload, int offset, int indexMultiplier, int offsetMultiplier) {

        StringBuilder sb = new StringBuilder();

        while (true) {
            offset += offsetMultiplier;
            int arrayIndex = offset * indexMultiplier - 1;
            byte b = payload[arrayIndex];

            if (b == 0) break;  // null terminator
            sb.append(decodeCp1252Byte(b));
        }

        return new ReadResult<>(sb.toString(), offset);
    }


    /**
     * Reads a CP1252, null-circumfixed string from the payload, mirroring
     * {@link BufferUtils#writeStringCp1252NullCircumfixed}.
     *
     * <p>Write layout:
     * <pre>
     *   [ 0x00 ][ char0 ][ char1 ] ... [ charN ][ 0x00 ]
     * </pre>
     *
     * @param payload          raw payload bytes
     * @param offset           current logical offset before advancing for this field
     * @param indexMultiplier  obfuscated index multiplier
     * @param offsetMultiplier obfuscated offset increment
     * @return a {@link ReadResult} containing the decoded string and the updated offset
     */
    private static ReadResult<String> readStringNullCircumfixed(
            byte[] payload, int offset, int indexMultiplier, int offsetMultiplier) {

        // Skip the leading null byte.
        offset += offsetMultiplier;
        // (We don't assert payload[offset * indexMultiplier - 1] == 0, but it should be.)

        StringBuilder sb = new StringBuilder();

        while (true) {
            offset += offsetMultiplier;
            int arrayIndex = offset * indexMultiplier - 1;
            byte b = payload[arrayIndex];

            if (b == 0) break;  // trailing null terminator
            sb.append(decodeCp1252Byte(b));
        }

        return new ReadResult<>(sb.toString(), offset);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CP1252 byte → char  (inverse of BufferUtils.encodeStringCp1252)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maps a single CP1252-encoded byte back to the original Java {@code char},
     * the inverse of {@link BufferUtils#encodeStringCp1252}.
     *
     * <p>Standard ASCII (0x01-0x7F) and Latin-1 supplement (0xA0-0xFF) pass
     * straight through.  The 0x80-0x9F block contains the Windows-1252 extras
     * that differ from ISO-8859-1 and must be remapped manually.
     *
     * @param b the raw byte read from the payload
     * @return the decoded character
     */
    private static char decodeCp1252Byte(byte b) {
        int v = b & 0xFF;   // treat as unsigned

        // Standard ASCII and Latin-1 supplement pass through directly.
        if ((v > 0 && v < 128) || (v >= 160 && v <= 255)) {
            return (char) v;
        }

        // Windows-1252 extras (0x80-0x9F) — manual map, inverse of encodeStringCp1252.
        switch (v) {
            case 0x80: return '\u20AC'; // €
            case 0x82: return '\u201A'; // ‚
            case 0x83: return '\u0192'; // ƒ
            case 0x84: return '\u201E'; // „
            case 0x85: return '\u2026'; // …
            case 0x86: return '\u2020'; // †
            case 0x87: return '\u2021'; // ‡
            case 0x88: return '\u02C6'; // ˆ
            case 0x89: return '\u2030'; // ‰
            case 0x8A: return '\u0160'; // Š
            case 0x8B: return '\u2039'; // ‹
            case 0x8C: return '\u0152'; // Œ
            case 0x8E: return '\u017D'; // Ž
            case 0x91: return '\u2018'; // '
            case 0x92: return '\u2019'; // '
            case 0x93: return '\u201C'; // "
            case 0x94: return '\u201D'; // "
            case 0x95: return '\u2022'; // •
            case 0x96: return '\u2013'; // –
            case 0x97: return '\u2014'; // —
            case 0x98: return '\u02DC'; // ˜
            case 0x99: return '\u2122'; // ™
            case 0x9A: return '\u0161'; // š
            case 0x9B: return '\u203A'; // ›
            case 0x9C: return '\u0153'; // œ
            case 0x9E: return '\u017E'; // ž
            case 0x9F: return '\u0178'; // Ÿ
            default:   return '?';       // unmappable (was written as 0x3F = '?')
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Typed tuple returned by the string read methods, carrying the decoded value and the
     *  updated logical offset so subsequent reads start at the correct position. */
    private static final class ReadResult<T> {
        final T value;
        final int nextOffset;
        ReadResult(T value, int nextOffset) {
            this.value = value;
            this.nextOffset = nextOffset;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience — pretty print a decoded packet for logging
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Decodes the payload and returns a human-readable string, e.g.:
     * <pre>
     *   OPOBJ1 { objectId=41096, worldPointX=3221, worldPointY=3219, ctrlDown=0, subop=0 }
     * </pre>
     *
     * @param packetName the display name of the packet opcode (e.g. "OPOBJ1")
     * @param payload    the encoded payload bytes
     * @param writes     the ordered {@link PacketWrite} descriptors for this opcode
     * @return a debug string
     */
    public static String debugDecode(String packetName, byte[] payload, List<PacketWrite> writes) {
        Map<String, Object> fields = decode(payload, writes);
        StringBuilder sb = new StringBuilder(packetName).append(" { ");
        fields.forEach((k, v) -> sb.append(k).append('=').append(v).append(", "));
        if (!fields.isEmpty()) sb.setLength(sb.length() - 2);
        return sb.append(" }").toString();
    }
}