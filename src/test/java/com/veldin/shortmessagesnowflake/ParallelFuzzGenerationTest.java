package com.veldin.shortmessagesnowflake;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

import static com.veldin.shortmessagesnowflake.Generator.DEFAULT_EPOCH_MILLIS;

public class ParallelFuzzGenerationTest {

    private static final Generator generator = new Generator();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz" + "- _./@";

    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static final int LOG_INTERVAL_SECONDS = 1;

    public static void main(String[] args) {

        System.out.println("Starting ParallelFuzzTest with " + THREADS + " threads...");

        AtomicLong counter = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);

        // Logger
        ScheduledExecutorService logger = Executors.newSingleThreadScheduledExecutor();
        logger.scheduleAtFixedRate(() -> {
            long count = counter.getAndSet(0);
            System.out.println("Generated " + count + " IDs in last " + LOG_INTERVAL_SECONDS + " second(s).");
        }, LOG_INTERVAL_SECONDS, LOG_INTERVAL_SECONDS, TimeUnit.SECONDS);

        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                while (true) {
                    String message = randomMessage();

                    // Generate Snowflake
                    ShortMessageSnowflake id = generator.generate(message);

                    // Message & length
                    assert id.getMessage().equals(message) : "Message mismatch";
                    assert id.getMessageLength() == message.length() : "Length mismatch";

                    // Byte round-trip using Factory.fromBytes
                    byte[] bytes = id.toBytes();
                    ShortMessageSnowflake fromBytes = Factory.fromBytes(bytes);

                    assert fromBytes.equals(id) : "Byte round-trip equals failed";
                    assert fromBytes.getMessage().equals(message) : "Byte round-trip message mismatch";
                    assert fromBytes.getMessageLength() == message.length() : "Byte round-trip length mismatch";
                    assert fromBytes.hashCode() == id.hashCode() : "Byte round-trip hash mismatch";

                    // CompareTo consistency
                    assert id.compareTo(fromBytes) == 0 : "CompareTo mismatch";

                    // UUID consistency
                    UUID uuid1 = id.toUUID();
                    UUID uuid2 = fromBytes.toUUID();
                    assert uuid1.equals(uuid2) : "UUID mismatch";
                    assert uuid1.getMostSignificantBits() == uuid2.getMostSignificantBits();
                    assert uuid1.getLeastSignificantBits() == uuid2.getLeastSignificantBits();

                    // Timestamp sanity
                    Instant ts = id.getTimestamp(Instant.ofEpochMilli(DEFAULT_EPOCH_MILLIS));
                    assert ts != null : "Timestamp null";

                    counter.incrementAndGet();
                }
            });
        }

        // Keep main thread alive
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.err.println("Interrupted: " + e.getMessage());
        } finally {
            executor.shutdownNow();
            logger.shutdownNow();
        }
    }

    private static String randomMessage() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(0, ShortMessageSnowflake.MAX_CHARS + 1);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
