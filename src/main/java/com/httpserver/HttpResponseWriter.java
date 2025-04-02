package main.java.com.httpserver;

/**
 * An abstraction for writing an HTTP response.
 */
public interface HttpResponseWriter {
    void setStatus(int code, String message);
    void setHeader(String name, String value);
    void writeBody(String data);
    void send();
}
