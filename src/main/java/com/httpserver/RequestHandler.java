package main.java.com.httpserver;

// Functional interface implemented by endpoint lambdas in SimpleHttpServer.
// Each handler receives the parsed HttpRequest and a response writer they
// must populate (status, headers, body) before returning.
@FunctionalInterface
public interface RequestHandler {
    void handle(HttpRequest request, HttpResponseWriter response);
}