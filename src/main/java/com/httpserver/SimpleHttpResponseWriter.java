package main.java.com.httpserver;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class SimpleHttpResponseWriter implements HttpResponseWriter {

    private final PrintWriter out;
    private int statusCode = 200;
    private String statusMessage = "OK";
    private final Map<String, String> headers = new HashMap<>();
    private final StringBuilder body = new StringBuilder();

    public SimpleHttpResponseWriter(PrintWriter out) {
        this.out = out;
    }

    @Override public void setStatus(int c, String m) {
        statusCode = c; statusMessage = m;
        }
    @Override public void setHeader(String n, String v) {
        headers.put(n, v);
    }
    @Override public void writeBody(String data) {
        body.append(data);
    }

    @Override public void send() {
        String b = body.toString();
        headers.putIfAbsent("Content-Length", String.valueOf(b.getBytes().length));
        out.print("HTTP/1.1 " + statusCode + " " + statusMessage + "\r\n");
        headers.forEach((k,v) -> out.print(k + ": " + v + "\r\n"));
        out.print("\r\n");
        out.print(b);
        out.flush();
    }
}
