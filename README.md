
# ShortMessageSnowflake

**I made a 128-bit, time-ordered 'unique' identifier that embeds a very short message.
Inspired by Twitter Snowflake, but trading sequence/machine-id for a small human-readable message.**

(Note, that I made this for fun.)

128-bit ShortMessageSnowflake

| Most significant 64 bits            | Least significant 64 bits                                                                   |
|-------------------------------------|---------------------------------------------------------------------------------------------|
| └─	48-bit - signed timestamp<br/>└─	16-bit - random bits | └─	4-bit message length indicator  <br/>└─	60-bit payload (Actual message + random padding) |



## Features

- Naturally sortable by time (timestamp are the most significant bits)
- Stored as standard UUID (Works great in databases)
- Can represent dates before the epoch (signed timestamp)
- Store up to 12 characters 'inside' the UUID along with the timestamp

_____
## Part 1 - Signed timestamp (Most Significant 64 bits)

```java
ShortMessageSnowflake shortMessageSnowflake;
```

We can generate some ID's using the Generator.

```java
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
```
And when we have them, we can store them like any UUD. (There is even an ToUUID() method.)
So store em, pass em around. And handle them just as you like.

```java
0 = {ShortMessageSnowflake@2227} "019c86a7-4f68-1398-5391-6b737527c6af"
1 = {ShortMessageSnowflake@2228} "019c86a7-4f90-a383-946c-51202641d079"
2 = {ShortMessageSnowflake@2229} "019c86a7-4f90-34b6-8939-84da07265c78"
3 = {ShortMessageSnowflake@2230} "019c86a7-4f90-f26e-c99c-13d89cd9810d"
4 = {ShortMessageSnowflake@2231} "019c86a7-4f90-13bf-c939-84db0806a1a6"
```

And if you want to re-hidrate them at a later date you can use the Factory methods. 

```java
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
```

The message and the timestamp are encoded within. 
The ShortMessageSnowflake is designed so that newer IDs are always numerically larger than older ones, 
making sorting easy (and efficient) in databases and such.


```java
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
```

<3

_____
Next up are the 2 parts that make up the ShortMessageSnowflake
## Part 1 - Signed timestamp (Most Significant 64 bits)
The Signed Timestamp lives in the most significant 64 bits of the 128-bit ShortMessageSnowflake.
 

When you compare two ShortMessageSnowflakes it;  
└─ First compare the most significant 64 bits (unsigned comparison);  
└─ And only if the entire MSB is identical compare the least significant 64 bits

The epoch in ShortMessageSnowflake is the Unix epoch (by default). 
Because the timestamp occupies 48 signed bits in the most significant 64 bits of the ID, the snowflake can represent roughly 4,500 years around the epoch with millisecond precision. (We have way more bits to work with then the Twitter snowflake.)

This leaves us with 16 bit for random bits that prevents two IDs generated in the exact same millisecond from instantly colliding.

This means that even if two different parts of your ecosystem generate an ID in the exact same millisecond, 
there are is a 1 in 65,536 possibility for the same value to be generated. 
(Ignoring the payload, which van also contain randomness, further reducing collision probability.)

## Part 2 - Payload  (Least significant 64 bits)
The **payload** lives in the **least significant 64 bits** of the 128-bit ShortMessageSnowflake.

It consists of two main parts: a 4-bit length field (bits 63–60) that tells you exactly how many characters (0 to 12) are actually meaningful, and a fixed 60-bit payload area (bits 59–0) that holds the message itself.

When the message is shorter than 12 characters, the unused portion of the 60-bit payload is filled with random bits (using `ThreadLocalRandom` for speed).  
└─	shorter message - 'more randomness' in the ID  
└─	 full 12-char message - zero randomness in payload

This means:  
└─	very short messages get extra collision resistance "for free" (more random bits overall)  
└─	long messages trade that extra randomness for actual data content

So why 12 characters? Well, there is a fundamental trade-off between how many characters you can store in the message and how rich / large the allowed character set (alphabet) can be. 
This trade-off comes directly from the fixed number of bits available for the payload and the way bits are assigned per character.

The payload is fixed at 60 bits, so I made the choice to use 5 bits per character. 
This gives us 2⁵ (= 32) different symbols. This makes the message lenght a maximum of 60 ÷ 5 (= 12) characters.

My main contenters where:
5 bits per character, with 32 character alphabet size, 12 characters max per string;
6 bits per characters, gives a 'huge' 64 character alphabet size, 10 characters max per string;

```java
// 32 possible symbols = 2⁶
private static final String ALPHABET =
        "abcdefghijklmnopqrstuvwxyz" +              // 26 lowercase
        "- _./@";                                   // 6 others
                                                    // _______ +
                                                    // 32
```

## When to use it

One important thing to understand about ShortMessageSnowflake is that the message doesn’t have a fixed meaning. 
It’s just a short string (up to 12 characters) encoded into the ID. 

There are no rules about what it has to represent, that part is completely up to the consumer.

You can use it for anything. 
A status like "ok" or "error", a tag like "login" or "signup", a region like "eu-west", or even just a random label. 
The class doesn’t care, it just makes sure the message is stored efficiently and can be decoded reliably.

So maybe;
- tokens
- activity markers
- short system messages or diagnostic tags
- shortcodes / application codes
- audit trail entries with minimal description
- written worker-id / machine ID
- hiding nasty messages about your colleagues

This might feel a bit “loose” at first, but it’s very intentional. 
By not enforcing meaning, the system stays flexible and reusable. 

Different teams or services can use the same structure in totally different ways without stepping on each other’s toes or needing changes to the core code.

## Twitter Snowflake vs ShortMessageSnowflake

Lastly; here is some table with info about our snowflake and twitter's snowflake.

|                         | Twitter Snowflake (2010–today)          | ShortMessageSnowflake                                                                |
|-------------------------------|---------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| Total size                    | 64 bits                                     | 128 bits (stored as UUID)                                                                                   |
| Timestamp bits                | 41 bits                                     | 48 bits (signed)                                                                                            |
| Time range                    | ~69 years from custom epoch (≈2010)         | ≈ ±4,500 years around default (Unix) epoch                                                                          |
| Collision avoidance           | datacenter (5) + worker (5) + sequence (12) | 16 random bits per millisecond + more randomness in payload when message is shorter (faster random fill) |
| Monotonicity within 1 ms      | Yes (Tiwtter Snowflake uses an sequence counter)                      | No (pure random)                                                                                            |         |
| Output format                 | 64-bit integer                              | UUID string / 16-byte binary / 2 longs                                                                              |
| Purpose               | Scalable tweets / events        | Tiny self-describing time-ordered tokens                                                                    |


