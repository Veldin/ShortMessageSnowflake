package com.veldin.shortmessagesnowflake;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.veldin.shortmessagesnowflake.Generator.DEFAULT_EPOCH_MILLIS;
import static org.junit.jupiter.api.Assertions.*;

class ShortMessageSnowflakeTest {

    private final Generator generator = new Generator();

    @Test
    void shouldEncodeAndDecodeMessage() {
        String message = "hello world";

        ShortMessageSnowflake id = generator.generate(message);
        assertNotNull(id);

        String decoded = id.getMessage();
        assertEquals(message, decoded);
    }

    @Test
    void shouldEncodeLongMessageAndDecodeLongMessage() {
        String message = "aaaaaaaaaaaa";
        ShortMessageSnowflake id = generator.generate(message);

        assertNotNull(id);
        assertEquals(message, id.getMessage());
        assertEquals(message.length(), id.getMessageLength());
    }

    @Test
    void shouldEncodeShortMessageAndDecodeLongMessage() {
        String message = "a";
        ShortMessageSnowflake id = generator.generate(message);

        assertNotNull(id);
        assertEquals(message, id.getMessage());
        assertEquals(message.length(), id.getMessageLength());
    }

    @Test
    void shouldEncodeEmptyMessageAndDecodeEmptyMessage() {
        String message = "";
        ShortMessageSnowflake id = generator.generate(message);

        assertNotNull(id);
        assertEquals(message, id.getMessage());
        assertEquals(message.length(), id.getMessageLength());
    }

    @Test
    void shouldThrowWhenEncodeNullMessage() {
        assertThrows(
                IllegalArgumentException.class,
                () ->  generator.generate(null) // uppercase not allowed
        );
    }

    @Test
    void shouldProduceValidUUID() {
        ShortMessageSnowflake id = generator.generate("testvalue");

        UUID uuid = id.toUUID();

        assertNotNull(uuid);
        assertEquals(uuid.toString(), id.toString());
    }

    @Test
    void shouldContainRecentTimestamp() {
        Instant before = Instant.now();

        ShortMessageSnowflake id = generator.generate("timestamp");

        Instant after = Instant.now();

        Instant extracted =
                id.getTimestamp(Instant.ofEpochMilli(DEFAULT_EPOCH_MILLIS));

        assertFalse(extracted.isBefore(before.minusSeconds(1)));
        assertFalse(extracted.isAfter(after.plusSeconds(1)));
    }

    @Test
    void shouldRejectTooLongMessage() {
        String tooLong = "abcdefghijklmnop"; // > 12 chars

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate(tooLong)
        );
    }

    @Test
    void shouldRejectInvalidCharacters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate("HELLO") // uppercase not allowed
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> generator.generate("hello@!") // ! not allowed
        );
    }

    @Test
    void shouldEncodeDifferenceSizeMessage() {
        StringBuilder sb = new StringBuilder();

        while(sb.length() < 13){
            ShortMessageSnowflake id = generator.generate(sb.toString());
            assertNotNull(id);
            assertEquals(sb.toString(),  id.getMessage());
            assertEquals(sb.length(), id.getMessageLength());
            assertEquals(id.getMessageLength(), sb.toString().length());

            sb.append('a');
        }

    }

    @Test
    void shouldEncodeAllAlphabetMessage() {
        StringBuilder sb = new StringBuilder();

        while(sb.length() < 13){
            ShortMessageSnowflake id = generator.generate(sb.toString());
            assertNotNull(id);
            assertEquals(sb.toString(),  id.getMessage());
            assertEquals(sb.length(), id.getMessageLength());
            assertEquals(id.getMessageLength(), sb.toString().length());

            sb.append('a');
        }
    }

    @Test
    @Timeout(4) // hard safety cap
    void fuzzForThreeSeconds() {

        final String alphabet = "abcdefghijklmnopqrstuvwxyz-. ";
        final long durationNanos = Duration.ofSeconds(3).toNanos();

        long start = System.nanoTime();
        long count = 0;

        while (System.nanoTime() - start < durationNanos) {

            ThreadLocalRandom random = ThreadLocalRandom.current();

            int length = random.nextInt(0, ShortMessageSnowflake.MAX_CHARS + 1);
            StringBuilder sb = new StringBuilder(length);

            for (int i = 0; i < length; i++) {
                sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }

            String message = sb.toString();

            ShortMessageSnowflake id = generator.generate(message);

            // message round-trip
            assertEquals(message, id.getMessage());
            assertEquals(length, id.getMessageLength());

            // byte round-trip
            ShortMessageSnowflake fromBytes =
                    ShortMessageSnowflake.Of(id.toBytes());

            assertEquals(id, fromBytes);
            assertEquals(message, fromBytes.getMessage());

            // compareTo consistency
            assertEquals(0, id.compareTo(fromBytes));

            count++;
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("Generated and verified " + count + " IDs in " + elapsedMs + " ms");
    }

    @Test
    void fuzzCompareToConsistency() {
        Generator gen = new Generator();
        List<ShortMessageSnowflake> list = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            list.add(gen.generate("msg"));
        }

        // Sort using compareTo
        List<ShortMessageSnowflake> sorted = new ArrayList<>(list);
        Collections.sort(sorted);

        // Validate that compareTo ordering is consistent with equals
        for (int i = 1; i < sorted.size(); i++) {
            ShortMessageSnowflake prev = sorted.get(i - 1);
            ShortMessageSnowflake curr = sorted.get(i);

            assertTrue(prev.compareTo(curr) <= 0, "List not sorted correctly");
            assertEquals(prev.equals(curr), prev.compareTo(curr) == 0, "compareTo inconsistent with equals");
        }
    }

    @Test
    void readmeExampleOne() {
        // ------ SERVICE 1

        Generator generator = new Generator();
        ShortMessageSnowflake id = generator.generate("hello world");

        assertNotNull(id);
        assertEquals("hello world", id.getMessage());
        assertEquals(11, id.getMessageLength());

        // We can use the ShortMessageSnowflake as any ol UUID
        UUID uuid = id.toUUID();
        assertNotNull(uuid); // Something like: 019c75be-e132-cdae-b391-6b76ece8ac7d

        // Imagine we send, or store the ID somewhere
        byte[] sendBytes = id.toBytes();
        assertEquals(16, sendBytes.length); // They are just 16 bytes.

        // ------ SERVICE 2

        // Now say we are an entire different service that wants to read the SMS.
        // So for this example we only have the bytes of the first id.
        ShortMessageSnowflake gottenId = Factory.fromBytes(sendBytes);
        assertNotNull(gottenId);

        // The message is part of the SMS and thus also saved.
        assertEquals("hello world", gottenId.getMessage());
        assertEquals(11, gottenId.getMessageLength());

        // And you can also get the timestamp.
        Instant instant = gottenId.getTimestamp(Instant.ofEpochMilli(DEFAULT_EPOCH_MILLIS));
        assertNotNull(instant);

        // The sender send an ID, and the receiver got the exact same.
        assertEquals(id, gottenId);
    }
}
