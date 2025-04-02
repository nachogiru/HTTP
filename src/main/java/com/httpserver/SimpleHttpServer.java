package main.java.com.httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple HTTP server that lets you register routes and dispatch requests.
 */
public class SimpleHttpServer {

    private int port;
    // Map with keys as "METHOD path" (e.g., "GET /cats") and values as handlers.
    private Map<String, RequestHandler> routes = new HashMap<>();

    public SimpleHttpServer(int port) {
        this.port = port;
    }

    /**
     * Register an endpoint with a given HTTP method and path.
     *
     * @param method  HTTP method (GET, POST, etc.)
     * @param path    URL path (e.g., "/cats")
     * @param handler The request handler for this endpoint
     */
    public void on(String method, String path, RequestHandler handler) {
        String key = method.toUpperCase() + " " + path;
        routes.put(key, handler);
    }

    /**
     * Start the HTTP server. This method will block.
     *
     * @throws IOException if an I/O error occurs when waiting for a connection.
     */
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Server listening on port " + port);
        while (true) {
            Socket clientSocket = serverSocket.accept();
            // Handle each connection in a new thread (for concurrency)
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    /**
     * Handles a single client connection.
     */
    private void handleClient(Socket clientSocket) {
        try (Socket socket = clientSocket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Parse the request from the client
            HttpRequest request = parseRequest(in);
            // Create a response writer
            SimpleHttpResponseWriter responseWriter = new SimpleHttpResponseWriter(out);
            // Dispatch the request to a handler based on method and path
            String key = request.getMethod().toUpperCase() + " " + request.getPath();
            RequestHandler handler = routes.get(key);
            if (handler != null) {
                handler.handle(request, responseWriter);
            } else {
                // No matching route; return 404 Not Found.
                responseWriter.setStatus(404, "Not Found");
                responseWriter.setHeader("Content-Type", "text/plain");
                responseWriter.writeBody("404 Not Found");
            }
            // Send the response
            responseWriter.send();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * A simple parser for an HTTP request.
     */
    private HttpRequest parseRequest(BufferedReader in) throws IOException {
        HttpRequest request = new HttpRequest();
        // Read request line (e.g., "GET /cats HTTP/1.1")
        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            throw new IOException("Empty request line");
        }
        String[] parts = requestLine.split(" ");
        if (parts.length < 3) {
            throw new IOException("Invalid request line: " + requestLine);
        }
        request.setMethod(parts[0]);
        request.setPath(parts[1]);

        // Read headers until an empty line is encountered.
        Map<String, String> headers = new HashMap<>();
        String headerLine;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(":");
            if (colonIndex != -1) {
                String name = headerLine.substring(0, colonIndex).trim();
                String value = headerLine.substring(colonIndex + 1).trim();
                headers.put(name, value);
            }
        }
        request.setHeaders(headers);

        // If Content-Length is specified, read that many characters for the body.
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            char[] bodyChars = new char[contentLength];
            int read = in.read(bodyChars, 0, contentLength);
            request.setBody(new String(bodyChars, 0, read));
        }
        return request;
    }
}
