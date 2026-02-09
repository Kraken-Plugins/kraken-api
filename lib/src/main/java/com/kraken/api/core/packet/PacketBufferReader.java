package com.kraken.api.core.packet;

import com.kraken.api.core.packet.model.PacketData;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;

@Slf4j
public class PacketBufferReader {

    /**
     * Reads the raw byte data from a packet buffer node
     */
    public static PacketData readPacketBuffer(Object packetBufferNode) {
        PacketData empty = new PacketData(new byte[0], 0, System.currentTimeMillis());
        if (packetBufferNode == null) return empty;

        try {
            // 1. Get the PacketBuffer instance
            Field bufferField = packetBufferNode.getClass().getDeclaredField(ObfuscatedNames.packetBufferFieldName);
            bufferField.setAccessible(true);
            Object packetBuffer = bufferField.get(packetBufferNode);

            if (packetBuffer == null) return empty;

            // 2. Get the payload array
            Field arrayField = findField(packetBuffer.getClass(), ObfuscatedNames.bufferArrayField);
            arrayField.setAccessible(true);
            byte[] payload = (byte[]) arrayField.get(packetBuffer);

            // 3. Get the obfuscated offset (current cursor position)
            Field offsetField = findField(packetBuffer.getClass(), ObfuscatedNames.bufferOffsetField);
            offsetField.setAccessible(true);
            int obfuscatedOffset = offsetField.getInt(packetBuffer);

            // 4. Calculate real length
            // The offset field tracks the NEXT write position, so its value is the count of bytes written.
            int length = obfuscatedOffset * Integer.parseInt(ObfuscatedNames.indexMultiplier);

            // 5. Return only the written portion of the buffer
            // Safety check to ensure we don't exceed array bounds if obfuscation data is stale
            if (length > payload.length) {
                log.warn("Calculated packet length {} exceeds payload size {}", length, payload.length);
                length = payload.length;
            }

            byte[] arr = Arrays.copyOfRange(payload, 0, length);
            return new PacketData(arr, length, System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Failed to extract packet payload", e);
            return empty;
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in hierarchy of " + clazz.getName());
    }

    /**
     * Converts byte array to hex string for display
     */
    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i] & 0xFF));
            if ((i + 1) % 16 == 0) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Reads an integer from the byte array at the given position
     */
    public static int readInt(byte[] data, int pos) {
        if (pos + 3 >= data.length) return 0;
        return ((data[pos] & 0xFF) << 24) |
                ((data[pos + 1] & 0xFF) << 16) |
                ((data[pos + 2] & 0xFF) << 8) |
                (data[pos + 3] & 0xFF);
    }

    /**
     * Reads a short from the byte array at the given position
     */
    public static int readShort(byte[] data, int pos) {
        if (pos + 1 >= data.length) return 0;
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    /**
     * Reads a byte from the array at the given position
     */
    public static int readByte(byte[] data, int pos) {
        if (pos >= data.length) return 0;
        return data[pos] & 0xFF;
    }

    /**
     * Reads a CP1252 null-terminated string from the byte array.
     * Format: [string bytes][0x00]
     *
     * @param data The byte array
     * @param startPos The starting position in the array
     * @return The decoded string
     */
    public static String readStringCp1252NullTerminated(byte[] data, int startPos) {
        // Find the null terminator
        int endPos = startPos;
        while (endPos < data.length && data[endPos] != 0) {
            endPos++;
        }

        // Decode the bytes between startPos and endPos (exclusive)
        return decodeStringCp1252(data, startPos, endPos);
    }

    /**
     * Reads a CP1252 null-circumfixed string from the byte array.
     * Format: [0x00][string bytes][0x00]
     *
     * @param data The byte array
     * @param startPos The starting position (should be at the leading null byte)
     * @return The decoded string
     */
    public static String readStringCp1252NullCircumfixed(byte[] data, int startPos) {
        // Skip the leading null byte
        int stringStart = startPos + 1;

        // Find the trailing null terminator
        int endPos = stringStart;
        while (endPos < data.length && data[endPos] != 0) {
            endPos++;
        }

        // Decode the bytes between the two null bytes
        return decodeStringCp1252(data, stringStart, endPos);
    }

    /**
     * Decodes a byte array range from CP1252 encoding back to a Java String.
     * This reverses the encodeStringCp1252 method.
     *
     * @param data The byte array containing CP1252 encoded data
     * @param startIndex The starting index in the array
     * @param endIndex The ending index (exclusive)
     * @return The decoded string
     */
    public static String decodeStringCp1252(byte[] data, int startIndex, int endIndex) {
        StringBuilder sb = new StringBuilder(endIndex - startIndex);

        for (int i = startIndex; i < endIndex; i++) {
            int byteVal = data[i] & 0xFF; // Convert to unsigned
            char character;

            // Standard ASCII range (0-127) and extended range (160-255)
            if ((byteVal > 0 && byteVal < 128) || (byteVal >= 160 && byteVal <= 255)) {
                character = (char) byteVal;
            }
            // Reverse the manual mappings for non-standard characters
            else {
                switch (byteVal) {
                    case 128: character = '\u20AC'; break; // € (8364)
                    case 130: character = '\u201A'; break; // ‚ (8218)
                    case 131: character = '\u0192'; break; // ƒ (402)
                    case 132: character = '\u201E'; break; // „ (8222)
                    case 133: character = '\u2026'; break; // … (8230)
                    case 134: character = '\u2020'; break; // † (8224)
                    case 135: character = '\u2021'; break; // ‡ (8225)
                    case 136: character = '\u02C6'; break; // ˆ (710)
                    case 137: character = '\u2030'; break; // ‰ (8240)
                    case 138: character = '\u0160'; break; // Š (352)
                    case 139: character = '\u2039'; break; // ‹ (8249)
                    case 140: character = '\u0152'; break; // Œ (338)
                    case 142: character = '\u017D'; break; // Ž (381)
                    case 145: character = '\u2018'; break; // ' (8216)
                    case 146: character = '\u2019'; break; // ' (8217)
                    case 147: character = '\u201C'; break; // " (8220)
                    case 148: character = '\u201D'; break; // " (8221)
                    case 149: character = '\u2022'; break; // • (8226)
                    case 150: character = '\u2013'; break; // – (8211)
                    case 151: character = '\u2014'; break; // — (8212)
                    case 152: character = '\u02DC'; break; // ˜ (732)
                    case 153: character = '\u2122'; break; // ™ (8482)
                    case 154: character = '\u0161'; break; // š (353)
                    case 155: character = '\u203A'; break; // › (8250)
                    case 156: character = '\u0153'; break; // œ (339)
                    case 158: character = '\u017E'; break; // ž (382)
                    case 159: character = '\u0178'; break; // Ÿ (376)
                    default:  character = '?'; break; // Unknown/unmapped
                }
            }

            sb.append(character);
        }

        return sb.toString();
    }
}
