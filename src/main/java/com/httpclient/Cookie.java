package main.java.com.httpclient;

// Immutable value object that represents a single HTTP cookie.
// Using Java 17 record for brevity – provides compact data carrier
// with auto‑generated constructor, accessors, equals/hashCode, toString.
public record Cookie(
        String name,        // cookie name (e.g. "sessionId")
        String value,       // cookie value
        String domain,      // domain attribute – e.g. example.com
        String path,        // path attribute – e.g. /cats
        long   expiresAt    // expiry time (epoch millis); Long.MAX_VALUE = session cookie
) { }
