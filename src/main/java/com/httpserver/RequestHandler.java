package main.java.com.httpserver;

@FunctionalInterface
public interface RequestHandler {
    void handle(HttpRequest request, HttpResponseWriter response);
}
