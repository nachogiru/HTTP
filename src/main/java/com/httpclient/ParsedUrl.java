package main.java.com.httpclient;

import java.net.URI;
import java.net.URISyntaxException;

/*
 * Helper that parses a URL into scheme, host, port, and path.
 */
public class ParsedUrl {
    private String scheme, host, path;
    private int port;

    // Factory method returns populated ParsedUrl or throws IllegalArgumentException
    public static ParsedUrl parse(String url) {
        try {
            URI u = new URI(url);
            ParsedUrl p = new ParsedUrl();
            p.scheme = (u.getScheme() == null) ? "http" : u.getScheme();
            p.host   = u.getHost();
            p.port   = (u.getPort() == -1) ? 80 : u.getPort();
            p.path   = (u.getPath() == null || u.getPath().isEmpty()) ? "/" : u.getPath();
            return p;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e); // wrap checked exception
        }
    }

    // simple accessors (package‑private style)
    public String host() { return host; }
    public String path() { return path; }
    public int port()    { return port; }
}
