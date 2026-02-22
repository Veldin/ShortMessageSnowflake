package com.veldin.shortmessagesnowflake;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.Month.FEBRUARY;
import static org.junit.jupiter.api.Assertions.*;

class ReadmeTest {


    @Test
    void exampleOne() {
        Generator generator = new Generator();

        List<ShortMessageSnowflake> ids = new ArrayList<>();

        ids.add(generator.generate("hello"));
        ids.add(generator.generate("i created"));
        ids.add(generator.generate("some ids"));
        ids.add(generator.generate("that contain"));
        ids.add(generator.generate("some meaning"));

        assertEquals(5, ids.size());
    }

    @Test
    void exampleTwo() {
        List<String> strings = new ArrayList<>();

        strings.add("019c86a7-4f68-1398-5391-6b737527c6af");
        strings.add("019c86a7-4f90-a383-946c-51202641d079");
        strings.add("019c86a7-4f90-34b6-8939-84da07265c78");
        strings.add("019c86a7-4f90-f26e-c99c-13d89cd9810d");
        strings.add("019c86a7-4f90-13bf-c939-84db0806a1a6");

        assertEquals(5, strings.size());

        List<ShortMessageSnowflake> ids = new ArrayList<>();
        for (String str : strings){
            ids.add(Factory.fromUUIDString(str));
        }

        assertEquals(5, ids.size());
    }

    @Test
    void exampleThree() {
        ShortMessageSnowflake sms = Factory.fromUUIDString("019c86a7-4f68-1398-5391-6b737527c6af");

        // Get the message
        assertEquals(5, sms.getMessageLength());
        assertEquals("hello", sms.getMessage());

        // Get the timestamp
        LocalDateTime localDateTime = sms.getTimestampLocalDateTime();
        assertEquals( 2026, localDateTime.getYear());
        assertEquals( FEBRUARY, localDateTime.getMonth());
        assertEquals( 22, localDateTime.getDayOfMonth());
        assertEquals( 19, localDateTime.getHour());
        assertEquals( 40, localDateTime.getMinute());
        assertEquals( 37, localDateTime.getSecond());
    }
}
