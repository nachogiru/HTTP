package main.java.com.httpclient;

public class ParsedUrl {
    private String scheme;  // e.g. "http"
    private String host;    // e.g. "example.com"
    private int port;       // e.g. 80
    private String path;    // e.g. "/cats"

    // Constructors
    public ParsedUrl() {
    }

    public ParsedUrl(String scheme, String host, int port, String path) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
    }

    // Getters and setters
    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPath() {
        return path == null || path.isEmpty() ? "/" : path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
