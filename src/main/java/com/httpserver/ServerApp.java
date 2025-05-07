package main.java.com.httpserver;

import main.java.com.common.ApiKeyConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Scanner;

/*
 * Supports GET, HEAD, POST, PUT, DELETE on /resources and /resources/{id}, plus a static file.
 */
public class ServerApp {

    // In-memory store: id → JSON-like map
    private static final Map<Integer, Map<String, Object>> store = new HashMap<>();
    private static final AtomicInteger ids = new AtomicInteger(1);

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Prompt for port
        System.out.print("Enter port for server (blank for default 8080): ");
        String portLine = sc.nextLine().trim();
        int port;
        if (portLine.isEmpty()) {
            port = 8080;
        } else {
            try {
                port = Integer.parseInt(portLine);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port; using default 8080");
                port = 8080;
            }
        }

        // Prompt for API key
        System.out.print("Enter API key (blank to load from env or properties): ");
        String inputKey = sc.nextLine().trim();
        String apiKey;
        if (inputKey.isEmpty()) {
            apiKey = ApiKeyConfig.load(args, 1);
        } else {
            apiKey = inputKey;
        }
        System.out.println("API Key: " + apiKey);

        // Create and configure server
        SimpleHttpServer srv = new SimpleHttpServer(port, apiKey);

        // Static file endpoints
        srv.on("GET",  "/static", serveStatic());
        srv.on("HEAD", "/static", headStatic());

        // Create resource
        srv.on("POST", "/resources", ServerApp::createResource);

        // List resources
        srv.on("GET",  "/resources", ServerApp::listResources);
        srv.on("HEAD", "/resources", ServerApp::headList);

        // Single resource endpoints
        srv.on("GET",    "/resources/", ServerApp::readResource);
        srv.on("HEAD",   "/resources/", ServerApp::headResource);
        srv.on("PUT",    "/resources/", ServerApp::updateResource);
        srv.on("DELETE", "/resources/", ServerApp::deleteResource);

        // Start server
        try {
            srv.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ----- Handlers -----

    // GET /static
    private static RequestHandler serveStatic() {
        return (req, res) -> {
            try {
                byte[] b = Files.readAllBytes(Paths.get("index.html"));
                res.setStatus(200, "OK");
                res.setHeader("Content-Type", "text/html");
                res.writeBody(new String(b));
            } catch (IOException e) {
                res.setStatus(404, "Not Found");
                res.setHeader("Content-Type", "text/plain");
                res.writeBody("Static file not found");
            }
        };
    }

    // HEAD /static
    private static RequestHandler headStatic() {
        return (req, res) -> {
            res.setStatus(200, "OK");
            res.setHeader("Content-Type", "text/html");
        };
    }

    // POST /resources
    private static void createResource(HttpRequest req, HttpResponseWriter res) {
        if (!isJson(req)) { bad(res, "Expected JSON"); return; }
        Map<String, Object> data = parseJson(req.getBody());
        if (data == null) { bad(res, "Invalid JSON"); return; }

        int id = ids.getAndIncrement();
        store.put(id, data);

        res.setStatus(201, "Created");
        res.setHeader("Content-Type", "application/json");
        res.writeBody("{\"id\":" + id + "}");
    }

    // GET /resources
    private static void listResources(HttpRequest req, HttpResponseWriter res) {
        res.setStatus(200, "OK");
        res.setHeader("Content-Type", "application/json");

        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (var e : store.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(toJson(e.getKey(), e.getValue()));
        }
        sb.append("]");
        res.writeBody(sb.toString());
    }

    // HEAD /resources
    private static void headList(HttpRequest req, HttpResponseWriter res) {
        res.setStatus(200, "OK");
        res.setHeader("Content-Type", "application/json");
    }

    // GET /resources/{id}
    private static void readResource(HttpRequest req, HttpResponseWriter res) {
        int id = parseId(req.getPath());
        if (id <= 0) { bad(res, "Invalid ID"); return; }

        Map<String, Object> data = store.get(id);
        if (data == null) {
            res.setStatus(404, "Not Found");
            return;
        }
        res.setStatus(200, "OK");
        res.setHeader("Content-Type", "application/json");
        res.writeBody(toJson(id, data));
    }

    // HEAD /resources/{id}
    private static void headResource(HttpRequest req, HttpResponseWriter res) {
        int id = parseId(req.getPath());
        if (id <= 0 || !store.containsKey(id)) {
            res.setStatus(404, "Not Found");
            return;
        }
        res.setStatus(200, "OK");
        res.setHeader("Content-Type", "application/json");
    }

    // PUT /resources/{id}
    private static void updateResource(HttpRequest req, HttpResponseWriter res) {
        int id = parseId(req.getPath());
        if (id <= 0)               { bad(res, "Invalid ID"); return; }
        if (!store.containsKey(id)) { res.setStatus(404, "Not Found"); return; }
        if (!isJson(req))           { bad(res, "Expected JSON"); return; }

        Map<String, Object> data = parseJson(req.getBody());
        if (data == null) { bad(res, "Invalid JSON"); return; }

        store.put(id, data);
        res.setStatus(200, "OK");
        res.setHeader("Content-Type", "application/json");
        res.writeBody("{\"status\":\"updated\"}");
    }

    // DELETE /resources/{id}
    private static void deleteResource(HttpRequest req, HttpResponseWriter res) {
        int id = parseId(req.getPath());
        if (id <= 0)               { bad(res, "Invalid ID"); return; }
        if (store.remove(id) == null) {
            res.setStatus(404, "Not Found");
            return;
        }
        res.setStatus(200, "OK");
        res.setHeader("Content-Type", "application/json");
        res.writeBody("{\"status\":\"deleted\"}");
    }

    // ----- Helpers -----

    private static void bad(HttpResponseWriter r, String m) {
        r.setStatus(400, "Bad Request");
        r.setHeader("Content-Type", "text/plain");
        r.writeBody(m);
    }

    private static boolean isJson(HttpRequest r) {
        String ct = r.getHeaders().getOrDefault("Content-Type", "").toLowerCase();
        return ct.contains("application/json");
    }

    private static int parseId(String path) {
        try {
            return Integer.parseInt(path.replaceFirst("^/resources/", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    private static Map<String, Object> parseJson(String j) {
        try {
            Map<String, Object> m = new HashMap<>();
            String in = j.trim();
            if (!in.startsWith("{") || !in.endsWith("}")) return null;
            in = in.substring(1, in.length() - 1).trim();
            if (in.isEmpty()) return m;
            for (String part : in.split(",")) {
                String[] kv = part.split(":", 2);
                if (kv.length < 2) return null;
                String k = kv[0].trim().replaceAll("^\"|\"$", "");
                String v = kv[1].trim();
                if (v.startsWith("\"") && v.endsWith("\"")) {
                    m.put(k, v.substring(1, v.length() - 1));
                } else {
                    m.put(k, Integer.parseInt(v));
                }
            }
            return m;
        } catch (Exception e) {
            return null;
        }
    }

    private static String toJson(int id, Map<String, Object> d) {
        StringBuilder sb = new StringBuilder("{\"id\":").append(id);
        d.forEach((k, v) -> {
            sb.append(",\"").append(k).append("\":");
            sb.append(v instanceof Number ? v : ("\"" + v + "\""));
        });
        sb.append("}");
        return sb.toString();
    }
}
