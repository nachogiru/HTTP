package main.java.com.httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Very small blocking HTTP/1.1 server with route table, optional API-key auth,
// and proper 405 Method Not Allowed handling. One thread per connection.
public class SimpleHttpServer {

    private final int port;                               // TCP port to listen on
    private final String expectedApiKey;                   // null ⇒ auth disabled
    private final Map<String,RequestHandler> routes = new HashMap<>(); // "METHOD path" → handler

    public SimpleHttpServer(int port, String expectedApiKey) {
        this.port = port;
        this.expectedApiKey = expectedApiKey;
    }

    // register handler for exact METHOD + path prefix
    public void on(String method, String path, RequestHandler handler) {
        routes.put(method.toUpperCase() + " " + path, handler);
    }

    // blocking accept loop (spawns new thread per connection)
    public void start() throws IOException {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server listening on " + port);
            Logger.log(Logger.Level.INFO, "Server started on port " + port);
            while (true) {
                Socket s = ss.accept();
                Logger.log(Logger.Level.INFO, "Accepted connection from " + s.getInetAddress());
                new Thread(() -> handleClient(s)).start();
            }
        }
    }

    // per-client handling: parse, auth, routing, method checks, response
    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            HttpRequest request = parseRequest(in);
            if (request == null) return; // malformed → drop

            String method = request.getMethod().toUpperCase();
            String path   = request.getPath();
            String incoming = method + " " + path;

            // API key check
            if (expectedApiKey != null) {
                String provided = request.getHeaders().entrySet().stream()
                        .filter(e -> e.getKey().equalsIgnoreCase("X-API-Key"))
                        .map(Map.Entry::getValue)
                        .findFirst().orElse("");
                if (!expectedApiKey.equals(provided)) {
                    out.print("HTTP/1.1 401 Unauthorized\r\n");
                    out.print("WWW-Authenticate: ApiKey realm=\"SimpleServer\"\r\n");
                    out.print("Content-Length: 0\r\n\r\n");
                    out.flush();
                    return;
                }
            }

            // find matching handler by longest prefix
            RequestHandler handler = null;
            int bestLen = -1;
            for (var e : routes.entrySet()) {
                String key = e.getKey();
                if (incoming.startsWith(key) && key.length() > bestLen) {
                    bestLen = key.length();
                    handler = e.getValue();
                }
            }

            SimpleHttpResponseWriter resp = new SimpleHttpResponseWriter(out);
            if (handler != null) {
                handler.handle(request, resp);
                resp.send();
            } else {
                // method not allowed?
                Set<String> allowed = new HashSet<>();
                for (String key : routes.keySet()) {
                    String routePath = key.substring(key.indexOf(' ') + 1);
                    if (path.startsWith(routePath)) {
                        allowed.add(key.split(" ")[0]);
                    }
                }
                if (!allowed.isEmpty()) {
                    // 405 Method Not Allowed
                    out.print("HTTP/1.1 405 Method Not Allowed\r\n");
                    out.print("Allow: " + String.join(", ", allowed) + "\r\n");
                    out.print("Content-Length: 0\r\n\r\n");
                    out.flush();
                } else {
                    // 404 Not Found
                    resp.setStatus(404, "Not Found");
                    resp.setHeader("Content-Type", "text/plain");
                    resp.writeBody("404 Not Found");
                    resp.send();
                }
            }

            Logger.log(Logger.Level.DEBUG, "Handled " + method + " " + path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // parse start-line, headers, and optional body into HttpRequest
    private HttpRequest parseRequest(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null || line.isEmpty()) return null;
        String[] p = line.split(" ");
        if (p.length < 3) return null;

        HttpRequest req = new HttpRequest();
        req.setMethod(p[0]);
        req.setPath(p[1]);

        Map<String,String> headers = new HashMap<>();
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String val  = line.substring(idx+1).trim();
                headers.put(name, val);
            }
        }
        req.setHeaders(headers);

        int len = headers.getOrDefault("Content-Length", "0").isEmpty() ? 0 :
                Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        if (len > 0) {
            char[] buf = new char[len];
            in.read(buf);
            req.setBody(new String(buf));
        }
        return req;
    }
}
