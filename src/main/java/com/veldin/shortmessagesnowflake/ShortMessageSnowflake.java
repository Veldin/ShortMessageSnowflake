package com.veldin.shortmessagesnowflake;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.veldin.shortmessagesnowflake.Generator.DEFAULT_EPOCH_MILLIS;

public final class ShortMessageSnowflake implements Comparable<ShortMessageSnowflake> {

    // ───────────── Config ─────────────
    public static final int CHAR_BITS = 5;
    public static final int MAX_CHARS = 12;

    // exactly CHAR_BITS × MAX_CHARS = 60 bits
    // 60 bits / 6 CHAR_BITS = 10 char max message.

    // 64 possible symbols = 2⁶
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyz" +              // 26 lowercase
            "- _./@";                                   // 6 others
                                                        // _______ +
                                                        // 32

    private final long mostSigBits; // timestamp + random
    private final long leastSigBits; // payload + length (padded with random)

    static ShortMessageSnowflake Of(long msb, String message){
        return ShortMessageSnowflake.Of(msb, ShortMessageSnowflake.packMessage(message));
    }

    static ShortMessageSnowflake Of(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long msb = buffer.getLong();
        long lsb = buffer.getLong();

        return ShortMessageSnowflake.Of(msb, lsb);
    }

    static ShortMessageSnowflake Of(long msb, long lsb){
        return new ShortMessageSnowflake(msb, lsb);
    }

    private ShortMessageSnowflake(long msb, long lsb) {
        this.mostSigBits = msb;
        this.leastSigBits = lsb;
    }

    // ───────────── Signed 48-bit timestamp decode ─────────────
    private static long signExtend48(long value) {
        if ((value & 0x8000_0000_0000L) != 0) { // top bit of 48 = negative
            return value | 0xFFFF_0000_0000_0000L;
        }
        return value;
    }

    public Instant getTimestamp(Instant epoch) {
        long ts48 = mostSigBits >>> 16;
        long signedMs = signExtend48(ts48);
        return Instant.ofEpochMilli(signedMs + epoch.toEpochMilli());
    }

    public Instant getTimestamp() {
        return getTimestamp(Instant.ofEpochMilli(DEFAULT_EPOCH_MILLIS));
    }

    public LocalDateTime getTimestampLocalDateTime(Instant epoch, ZoneId zone) {
        return LocalDateTime.ofInstant(getTimestamp(epoch), zone);
    }

    public LocalDateTime getTimestampLocalDateTime(ZoneId zone) {
        return getTimestampLocalDateTime(Instant.ofEpochMilli(DEFAULT_EPOCH_MILLIS), zone);
    }

    public LocalDateTime getTimestampLocalDateTime() {
        return getTimestampLocalDateTime(ZoneId.systemDefault());
    }

    // ───────────── Message decode ─────────────
    public String getMessage() {
        int msgLen = getMessageLength();
        long payload = leastSigBits & 0x0FFFFFFFFFFFFFFFL; // bottom 60 bits
        char[] chars = new char[msgLen];
        for (int i = 0; i < msgLen; i++) {
            int shift = (MAX_CHARS - i - 1) * CHAR_BITS;
            int idx = (int)((payload >>> shift) & 0x1F);
            if (idx >= ALPHABET.length()) throw new IllegalStateException("Corrupted payload");
            chars[i] = ALPHABET.charAt(idx);
        }
        return new String(chars);
    }

    public int getMessageLength() {
        return (int)((leastSigBits >>> 60) & 0xF);
    }

    // ───────────── UUID ─────────────
    public UUID toUUID() {
        return new UUID(mostSigBits, leastSigBits);
    }

    @Override
    public String toString() {
        return toUUID().toString();
    }

    public String toJsonString(Instant epoch) {
        return String.format("{\"timestamp\":\"%s\",\"message\":\"%s\"}",
                getTimestamp(epoch),
                getMessage());
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(mostSigBits);
        buffer.putLong(leastSigBits);
        return buffer.array();
    }

    // ───────────── Value semantics ─────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShortMessageSnowflake that)) return false;
        return mostSigBits == that.mostSigBits && leastSigBits == that.leastSigBits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mostSigBits, leastSigBits);
    }

    @Override
    public int compareTo(ShortMessageSnowflake other) {
        int cmp = Long.compareUnsigned(this.mostSigBits, other.mostSigBits);
        if (cmp != 0) return cmp;
        return Long.compareUnsigned(this.leastSigBits, other.leastSigBits);
    }

    // ───────────── Packing ─────────────
    static long packMessage(String message) {
        int msgLen = message.length();
        if (msgLen > MAX_CHARS)
            throw new IllegalArgumentException("Message too long");

        long bits = 0L;

        // pack actual message (top-aligned)
        for (int i = 0; i < msgLen; i++) {
            int idx = ALPHABET.indexOf(message.charAt(i));
            if (idx < 0)
                throw new IllegalArgumentException("Invalid character: " + message.charAt(i));

            bits |= ((long) idx) << ((MAX_CHARS - i - 1) * CHAR_BITS);
        }

        // fill unused bits with randomness
        int unusedChars = MAX_CHARS - msgLen;
        int unusedBits = unusedChars * CHAR_BITS;

        if (unusedBits > 0) {
            // ThreadLocalRandom used here due to performance (and it's non-blocking)
            long random = ThreadLocalRandom.current().nextLong();
            long mask = (1L << unusedBits) - 1;
            bits |= (random & mask);
        }

        // store length in top 4 bits
        bits |= ((long) msgLen & 0xF) << 60;

        return bits;
    }


    /*
     * On application startup (when the class is first loaded), the static block runs automatically.
     * If anything is wrong (wrong bit count, alphabet too big/small, length overflow), we throw an
     * ExceptionInInitializerError.
     */
    static {
        final int maxSymbols = 1 << CHAR_BITS;           // e.g. 64 when CHAR_BITS=6
        final int requiredBits = MAX_CHARS * CHAR_BITS;   // e.g. 10 × 6 = 60

        // Alphabet must not exceed what the bits can represent
        if (ALPHABET.length() > maxSymbols) {
            throw new ExceptionInInitializerError(
                    String.format(
                            "Alphabet too large: %d symbols provided, but only %d possible with %d bits per character",
                            ALPHABET.length(), maxSymbols, CHAR_BITS
                    ));
        }

        // Alphabet should ideally use most/all available slots (strict version)
        if (ALPHABET.length() < maxSymbols) {
            throw new ExceptionInInitializerError(
                    String.format(
                            "Alphabet under-utilized: only %d / %d possible symbols defined. " +
                                    "Add more characters (or reduce CHAR_BITS.)",
                            ALPHABET.length(), maxSymbols
                    ));
        }

        // Total payload bits must exactly match the hardcoded 60-bit field
        if (requiredBits != 60) {
            throw new ExceptionInInitializerError(
                    String.format(
                            "Payload size mismatch: %d chars × %d bits = %d bits, " +
                                    "but the payload field is fixed at 60 bits",
                            MAX_CHARS, CHAR_BITS, requiredBits
                    ));
        }

        // Length field is 4 bits → max 15 characters
        if (MAX_CHARS > 15) {
            throw new ExceptionInInitializerError(
                    "MAX_CHARS cannot exceed 15 (length is stored in 4 bits: 0–15)"
            );
        }
    }
}
