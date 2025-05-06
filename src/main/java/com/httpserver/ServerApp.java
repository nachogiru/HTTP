package main.java.com.httpserver;

import main.java.com.common.ApiKeyConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Demo HTTP server exposing a very small RESTish API backed by an in‑memory map.
 * Routes:
 *   GET  /static          – serve index.html from working dir
 *   POST /resources       – create JSON object, returns {"id": n}
 *   GET  /resources       – list all stored objects
 *   PUT  /resources/{id}  – replace object with given id
 *   DELETE /resources/{id}– delete object
 * API key (optional) loaded via ApiKeyConfig and enforced by SimpleHttpServer.
 */
public class ServerApp {

    // in‑memory data store: id → JSON‑like map
    private static final Map<Integer,Map<String,Object>> store = new HashMap<>();

    // auto‑incrementing id generator
    private static final AtomicInteger ids = new AtomicInteger(1);

    public static void main(String[] args) {
        int port   = args.length > 0 ? Integer.parseInt(args[0]) : 8080;   // CLI port arg
        String apiKey = ApiKeyConfig.load(args, 1);                        // CLI/env/file
        System.out.println("Clave API cargada: " + apiKey);

        SimpleHttpServer srv = new SimpleHttpServer(port, apiKey);

        /* -------------------------- static file -------------------------- */
        srv.on("GET", "/static", (req, res) -> {
            try {
                byte[] b = Files.readAllBytes(Paths.get("index.html"));
                res.setStatus(200, "OK");
                res.setHeader("Content-Type", "text/html");
                res.writeBody(new String(b));
            } catch (IOException e) {
                res.setStatus(404, "Not Found");
                res.setHeader("Content-Type", "text/plain");
                res.writeBody("Could not find the static file");
            }
        });

        /* -------------------------- create ------------------------------- */
        srv.on("POST", "/resources", (req, res) -> {
            if (!isJson(req)) { bad(res, "Expected JSON"); return; }
            Map<String,Object> data = parseJson(req.getBody());
            if (data == null) { bad(res, "Invalid JSON"); return; }

            int id = ids.getAndIncrement(); // assign new id
            store.put(id, data);

            res.setStatus(201, "Created");
            res.setHeader("Content-Type", "application/json");
            res.writeBody("{\"id\": " + id + "}");
        });

        /* -------------------------- list --------------------------------- */
        srv.on("GET", "/resources", (rq, rs) -> {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (var e : store.entrySet()) {
                if (!first) sb.append(",");
                sb.append(toJson(e.getKey(), e.getValue()));
                first = false;
            }
            sb.append("]");

            // set a demo cookie to show cookie handling
            rs.setHeader("Set-Cookie", "visited=true; Path=/; Max-Age=3600");
            rs.setStatus(200, "OK");
            rs.setHeader("Content-Type", "application/json");
            rs.writeBody(sb.toString());
        });

        /* ------------------- dynamic update & delete --------------------- */
        srv.on("PUT",    "/resources/", ServerApp::update);
        srv.on("DELETE", "/resources/", ServerApp::delete);

        /* -------------------------- start ------------------------------- */
        try { srv.start(); } catch (IOException e) { e.printStackTrace(); }
    }

    /* ============================ helpers =============================== */

    // send 400 with plain‑text message
    private static void bad(HttpResponseWriter r, String m) {
        r.setStatus(400, "Bad Request");
        r.setHeader("Content-Type", "text/plain");
        r.writeBody(m);
    }

    // crude Content-Type check
    private static boolean isJson(HttpRequest r) {
        String ct = r.getHeaders().getOrDefault("Content-Type", "").toLowerCase();
        return ct.contains("application/json");
    }

    // grab numeric id from /resources/{id}
    private static int parseId(String path) {
        try { return Integer.parseInt(path.replaceFirst("^/resources/", "")); }
        catch (Exception e) { return -1; }
    }

    // -------- PUT handler --------
    private static void update(HttpRequest rq, HttpResponseWriter rs) {
        int id = parseId(rq.getPath());
        if (id <= 0) { bad(rs, "Invalid ID"); return; }
        if (!store.containsKey(id)) { rs.setStatus(404, "Not Found"); rs.writeBody("Not found"); return; }
        if (!isJson(rq)) { bad(rs, "Expected JSON"); return; }

        Map<String,Object> data = parseJson(rq.getBody());
        if (data == null) { bad(rs, "Invalid JSON"); return; }

        store.put(id, data); // overwrite
        rs.setStatus(200, "OK");
        rs.setHeader("Content-Type", "application/json");
        rs.writeBody("{\"status\":\"updated\"}");
    }

    // -------- DELETE handler --------
    private static void delete(HttpRequest rq, HttpResponseWriter rs) {
        int id = parseId(rq.getPath());
        if (id <= 0) { bad(rs, "Invalid ID"); return; }
        if (store.remove(id) == null) { rs.setStatus(404, "Not Found"); rs.writeBody("Not found"); return; }

        rs.setStatus(200, "OK");
        rs.setHeader("Content-Type", "application/json");
        rs.writeBody("{\"status\":\"deleted\"}");
    }

    /* ---------- super‑naïve JSON parsing (numbers or quoted strings) ---- */
    private static Map<String,Object> parseJson(String j) {
        try {
            Map<String,Object> m = new HashMap<>();
            String in = j.trim();
            if (!in.startsWith("{") || !in.endsWith("}")) return null;
            in = in.substring(1, in.length() - 1).trim();
            if (in.isEmpty()) return m;
            for (String p : in.split(",")) {
                String[] kv = p.split(":", 2);
                if (kv.length < 2) return null;
                String k = kv[0].trim().replaceAll("^\"|\"$", "");
                String v = kv[1].trim();
                if (v.startsWith("\"") && v.endsWith("\"")) m.put(k, v.substring(1, v.length() - 1));
                else m.put(k, Integer.parseInt(v));
            }
            return m;
        } catch (Exception e) { return null; }
    }

    // build JSON string {"id":<id>,...}
    private static String toJson(int id, Map<String,Object> d) {
        StringBuilder sb = new StringBuilder("{\"id\":").append(id);
        d.forEach((k, v) -> {
            sb.append(",\"").append(k).append("\":");
            sb.append((v instanceof Number) ? v : ("\"" + v + "\""));
        });
        sb.append("}");
        return sb.toString();
    }
}