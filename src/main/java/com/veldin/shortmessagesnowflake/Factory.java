package com.veldin.shortmessagesnowflake;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class Factory {

    private Factory() {}

    public static ShortMessageSnowflake fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Expected exactly 16 bytes.");
        }
        return ShortMessageSnowflake.Of(bytes);
    }

    public static ShortMessageSnowflake fromLongs(long mostSignificantBit, long leastSignificantBits) {
        return ShortMessageSnowflake.Of(
                mostSignificantBit,
                leastSignificantBits
        );
    }

    public static ShortMessageSnowflake fromUUID(UUID uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID cannot be null");
        }
        return ShortMessageSnowflake.Of(
                uuid.getMostSignificantBits(),
                uuid.getLeastSignificantBits()
        );
    }

    public static ShortMessageSnowflake fromUUIDString(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new IllegalArgumentException("UUID string cannot be null or empty");
        }
        try {
            UUID uuid = UUID.fromString(uuidString);
            return fromUUID(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string format", e);
        }
    }

    public static ShortMessageSnowflake fromHexString(String hex) {
        if (hex == null || hex.length() != 32) {
            throw new IllegalArgumentException("Expected exactly 32 hexadecimal characters (so yea, no dashes)");
        }

        ByteBuffer buf = ByteBuffer.allocate(16);

        for (int i = 0; i < 16; i++) {
            int pos = i * 2;

            int high = Character.digit(hex.charAt(pos), 16);
            int low  = Character.digit(hex.charAt(pos + 1), 16);

            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character at position " + pos);
            }

            buf.put((byte) ((high << 4) | low));
        }

        buf.flip();

        return ShortMessageSnowflake.Of(buf.getLong(), buf.getLong());
    }
}
