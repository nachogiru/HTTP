package main.java.com.httpserver;

import java.util.Map;

// Object representing an incoming HTTP request (method, path, headers, body).
// Mutability is fine here because the server will populate this object once per request.
public class HttpRequest {
    private String method;                        // HTTP verb (GET, POST, ...)
    private String path;                          // requested path, e.g. "/users/42"
    private Map<String,String> headers;           // request headers (case‑insensitive keys)
    private String body;                          // request body as raw string (may be null)

    // --- getters / setters -------------------------------------------------
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Map<String,String> getHeaders() { return headers; }
    public void setHeaders(Map<String,String> headers) { this.headers = headers; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}