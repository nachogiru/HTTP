package main.java.com.httpserver;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple implementation of HttpResponseWriter.
 */
public class SimpleHttpResponseWriter implements HttpResponseWriter {
    private PrintWriter out;
    private int statusCode = 200;
    private String statusMessage = "OK";
    private Map<String, String> headers = new HashMap<>();
    private StringBuilder body = new StringBuilder();

    public SimpleHttpResponseWriter(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void setStatus(int code, String message) {
        this.statusCode = code;
        this.statusMessage = message;
    }

    @Override
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    @Override
    public void writeBody(String data) {
        body.append(data);
    }

    @Override
    public void send() {
        // Write the status line
        out.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");

        // Ensure Content-Length is set
        if (!headers.containsKey("Content-Length")) {
            headers.put("Content-Length", String.valueOf(body.toString().getBytes().length));
        }

        // Write headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            out.print(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        // End headers with a blank line
        out.print("\r\n");
        // Write body
        out.print(body.toString());
        out.flush();
    }
}
