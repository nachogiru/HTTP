package main.java.com.httpserver;

// Minimal abstraction for sending an HTTP response back to the client.
// Implementations (e.g. built on java.net.ServerSocket) will buffer data
// until send() is called.
public interface HttpResponseWriter {
    void setStatus(int code, String message);   // e.g. 200 "OK"
    void setHeader(String name, String value);  // add / replace a header field
    void writeBody(String data);                // append to response body
    void send();                                // flush headers + body to socket
}