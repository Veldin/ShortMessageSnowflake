package com.veldin.shortmessagesnowflake;

import java.security.SecureRandom;
import java.time.Instant;

public final class Generator {

    // Base epoch (default = Unix epoch). All timestamps are stored relative to this.
    public static final long DEFAULT_EPOCH_MILLIS = 0L;

    private final long epochMillis;

    // Used to generate the lower 16 bits (random component)
    private final SecureRandom random = new SecureRandom();

    public Generator() {
        this(DEFAULT_EPOCH_MILLIS);
    }

    private Generator(long epochMillis) {
        this.epochMillis = epochMillis;
    }

    /**
     * Returns the epoch as an Instant.
     */
    public Instant getEpoch() {
        return Instant.ofEpochMilli(epochMillis);
    }

    /**
     * Generate a ShortMessageSnowflake using the current system time.
     */
    public ShortMessageSnowflake generate(String message) {
        return generateWithTimestamp(message, System.currentTimeMillis());
    }

    /**
     * Generate a ShortMessageSnowflake with a custom timestamp.
     *
     * @param message The message to encode (must not be null)
     * @param timestampMillis The timestamp in milliseconds since the Unix epoch
     */
    public ShortMessageSnowflake generateWithTimestamp(String message, long timestampMillis) {
        if (message == null) {
            throw new IllegalArgumentException("Can't invoke generate with a null message.");
        }

        // Pack timestamp + randomness into the most significant bits
        return ShortMessageSnowflake.Of(packMostSignificantBits(timestampMillis), message);
    }

    /**
     * Packs the timestamp and random bits into a single 64-bit value:
     *
     * Layout:
     * - Upper 48 bits: timestamp (relative to epochMillis)
     * - Lower 16 bits: random value
     *
     * Note:
     * This does NOT guarantee uniqueness within the same millisecond,
     * since randomness can collide (though kinda unlikely enough).
     */
    private long packMostSignificantBits(long timestampMillis) {
        // Convert absolute timestamp into relative timestamp
        long relative = timestampMillis - epochMillis;

        // Generate 16 bits of randomness
        long randomBits = random.nextInt(1 << 16) & 0xFFFFL;

        // Combine timestamp and randomness into a single long
        return ((relative & 0xFFFFFFFFFFFFL) << 16) | randomBits;
    }
}