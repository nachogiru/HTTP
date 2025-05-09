package main.java.com.httpserver;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

// HTTP/1.1 response assembler that writes directly to a socket PrintWriter.
// No chunked encoding, sends Content-Length and flushes once in send().
public class SimpleHttpResponseWriter implements HttpResponseWriter {

    private final PrintWriter out;                 // underlying socket writer
    private int statusCode = 200;                  // default status
    private String statusMessage = "OK";           // default reason phrase
    private final Map<String,String> headers = new HashMap<>(); // response headers
    private final StringBuilder body = new StringBuilder();     // body buffer

    public SimpleHttpResponseWriter(PrintWriter out) {
        this.out = out;
    }

    // --- HttpResponseWriter impl -----------------------------------------

    @Override public void setStatus(int c, String m) {
        statusCode = c;
        statusMessage = m;
    }

    @Override public void setHeader(String n, String v) {
        headers.put(n, v);
    }

    @Override public void writeBody(String data) {
        body.append(data);
    }

    @Override public void send() {
        String b = body.toString();

        // ensure Content-Length is present so clients know when body ends
        headers.putIfAbsent("Content-Length", String.valueOf(b.getBytes().length));

        // status line
        out.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");

        // headers (one per line)
        headers.forEach((k, v) -> out.print(k + ": " + v + "\r\n"));

        out.print("\r\n"); // blank line separates headers from body
        out.print(b);        // actual body
        out.flush();         // push everything to client
    }
}
