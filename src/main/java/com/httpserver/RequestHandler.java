package main.java.com.httpserver;

/**
 * A functional interface for handling HTTP requests.
 */
@FunctionalInterface
public interface RequestHandler {
    /**
     * Handles the HTTP request and writes a response.
     *
     * @param request the HttpRequest object
     * @param responseWriter the HttpResponseWriter to write the response
     */
    void handle(HttpRequest request, HttpResponseWriter responseWriter);
}
