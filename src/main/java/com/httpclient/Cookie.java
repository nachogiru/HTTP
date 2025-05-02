package main.java.com.httpclient;

public record Cookie(
        String name,
        String value,
        String domain,   // e.g. example.com
        String path,     // e.g. /cats
        long   expiresAt // millis since epoch; Long.MAX_VALUE = session
) { }

