package main.java.com.httpclient;

import java.util.HashMap;
import java.util.Map;

/*
 * Mutable object representing an HTTP response – status line, headers, and body.
 */
public class HttpResponse {
    private int statusCode;                 // numeric status (e.g. 200)
    private String statusMessage;           // reason phrase (e.g. "OK")
    private final Map<String,String> headers = new HashMap<>(); // response headers
    private String body;                    // response body as raw string

    // getters / setters
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int c) { statusCode = c; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String m) { statusMessage = m; }

    public Map<String,String> getHeaders() { return headers; }

    public String getBody() { return body; }
    public void setBody(String b) { body = b; }
}
