package main.java.com.httpserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates a SimpleHttpServer with:
 * 1) Static file return
 * 2) CRUD endpoints for an in-memory resource
 * 3) Concurrency (threads)
 * 4) Configurable port
 */
public class ServerApp {

    // In-memory data store for our "resources".
    // Each resource is stored as a Map<String, Object> with an auto-generated ID.
    private static Map<Integer, Map<String, Object>> resources = new HashMap<>();
    private static AtomicInteger idGenerator = new AtomicInteger(1);

    public static void main(String[] args) {
        // Get port from program arguments or default to 8080.
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port, falling back to 8080");
            }
        }

        // Create a new instance of our SimpleHttpServer.
        SimpleHttpServer server = new SimpleHttpServer(port);

        // 1) Return static content (HTML)
        // GET /static => returns the contents of "index.html" from the current working directory.
        server.on("GET", "/static", (request, response) -> {
            try {
                byte[] fileBytes = Files.readAllBytes(Paths.get("index.html"));
                response.setStatus(200, "OK");
                response.setHeader("Content-Type", "text/html");
                response.writeBody(new String(fileBytes));
            } catch (IOException e) {
                response.setStatus(404, "Not Found");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Could not find the static file.\n");
            }
        });

        // 2) Add a new resource (POST /resources)
        server.on("POST", "/resources", (request, response) -> {
            if (!hasJsonContentType(request)) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Expected Content-Type: application/json\n");
                return;
            }

            String body = request.getBody();
            if (body == null || body.isEmpty()) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Request body is empty.\n");
                return;
            }

            Map<String, Object> data = parseSimpleJson(body);
            if (data == null) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Invalid JSON.\n");
                return;
            }

            int newId = idGenerator.getAndIncrement();
            resources.put(newId, data);

            response.setStatus(201, "Created");
            response.setHeader("Content-Type", "application/json");
            response.writeBody("{\"id\": " + newId + ", \"status\": \"created\"}");
        });

        // 3) View the list of resources (GET /resources)
        server.on("GET", "/resources", (request, response) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            boolean first = true;
            for (Map.Entry<Integer, Map<String, Object>> entry : resources.entrySet()) {
                if (!first) sb.append(",");
                sb.append(mapToJson(entry.getKey(), entry.getValue()));
                first = false;
            }
            sb.append("]");

            response.setStatus(200, "OK");
            response.setHeader("Content-Type", "application/json");
            response.writeBody(sb.toString());
        });

        // 4) Modify a resource (PUT /resources/{id})
        // Register a dynamic route using a trailing slash to catch IDs, e.g. /resources/123
        server.on("PUT", "/resources/", (request, response) -> {
            String fullPath = request.getPath();
            // Remove the prefix "/resources/" to extract the resource ID.
            String tail = fullPath.replaceFirst("^/resources/", "");
            if (tail.isEmpty()) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Missing resource ID in path.\n");
                return;
            }

            int resourceId;
            try {
                resourceId = Integer.parseInt(tail);
            } catch (NumberFormatException e) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Invalid resource ID.\n");
                return;
            }

            if (!resources.containsKey(resourceId)) {
                response.setStatus(404, "Not Found");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Resource not found.\n");
                return;
            }

            if (!hasJsonContentType(request)) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Expected Content-Type: application/json\n");
                return;
            }

            String body = request.getBody();
            if (body == null || body.isEmpty()) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Request body is empty.\n");
                return;
            }

            Map<String, Object> updatedData = parseSimpleJson(body);
            if (updatedData == null) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Invalid JSON.\n");
                return;
            }

            // Update the resource in the in-memory store.
            resources.put(resourceId, updatedData);

            response.setStatus(200, "OK");
            response.setHeader("Content-Type", "application/json");
            response.writeBody("{\"id\": " + resourceId + ", \"status\": \"updated\"}");
        });

        // 5) Delete a resource (DELETE /resources/{id})
        server.on("DELETE", "/resources/", (request, response) -> {
            String fullPath = request.getPath();
            String tail = fullPath.replaceFirst("^/resources/", "");
            if (tail.isEmpty()) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Missing resource ID in path.\n");
                return;
            }

            int resourceId;
            try {
                resourceId = Integer.parseInt(tail);
            } catch (NumberFormatException e) {
                response.setStatus(400, "Bad Request");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Invalid resource ID.\n");
                return;
            }

            if (!resources.containsKey(resourceId)) {
                response.setStatus(404, "Not Found");
                response.setHeader("Content-Type", "text/plain");
                response.writeBody("Resource not found.\n");
                return;
            }

            resources.remove(resourceId);
            response.setStatus(200, "OK");
            response.setHeader("Content-Type", "application/json");
            response.writeBody("{\"status\": \"deleted\"}");
        });

        // Start the server.
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Minimal check: Does the "Content-Type" header contain "application/json"?
     */
    private static boolean hasJsonContentType(HttpRequest request) {
        String ct = request.getHeaders().get("Content-Type");
        if (ct == null) return false;
        // Check substring in case of additional parameters (e.g., charset)
        return ct.toLowerCase().contains("application/json");
    }

    /**
     * A basic JSON parser for demonstration purposes.
     * This naive implementation expects a flat JSON object like:
     * {"key":"value", "num":123}
     * For real projects, use a robust JSON library such as Jackson or Gson.
     */
    private static Map<String, Object> parseSimpleJson(String json) {
        Map<String, Object> map = new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            return null;
        }
        // Remove the outer braces.
        String inner = json.substring(1, json.length() - 1).trim();
        if (inner.isEmpty()) {
            return map; // Empty JSON object.
        }
        // Naively split key-value pairs.
        String[] pairs = inner.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) return null;
            String k = kv[0].trim().replaceAll("^\"|\"$", "");
            String v = kv[1].trim();
            if (v.startsWith("\"") && v.endsWith("\"")) {
                v = v.substring(1, v.length() - 1);
                map.put(k, v);
            } else {
                try {
                    Integer intVal = Integer.valueOf(v);
                    map.put(k, intVal);
                } catch (NumberFormatException e) {
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    /**
     * Convert a resource entry (ID and data) to a JSON object string.
     */
    private static String mapToJson(Integer id, Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\": ").append(id);
        for (Map.Entry<String, Object> e : data.entrySet()) {
            sb.append(", \"")
                    .append(e.getKey())
                    .append("\": ");
            if (e.getValue() instanceof Number) {
                sb.append(e.getValue());
            } else {
                sb.append("\"").append(e.getValue()).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
