package main.java.com.httpserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SimpleHttpServer {

    private final int port;
    private final String expectedApiKey;                     // null ⇒ auth off
    private final Map<String, RequestHandler> routes = new HashMap<>();

    public SimpleHttpServer(int port, String expectedApiKey) {
        this.port = port;
        this.expectedApiKey = expectedApiKey;
    }

    public void on(String method, String path, RequestHandler handler) {
        routes.put(method.toUpperCase() + " " + path, handler);
    }

    public void start() throws IOException {
        try (ServerSocket ss = new ServerSocket(port)) {
            System.out.println("Server listening on " + port);
            Logger.log(Logger.Level.INFO, "Server started on port " + port);
            while (true) {
                Socket s = ss.accept();
                new Thread(() -> handleClient(s)).start();
                Logger.log(Logger.Level.INFO, "Accepted connection from " + ss.getInetAddress());
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            HttpRequest request = parseRequest(in);
            if (request == null) return;             // bad HTTP, just drop

            /* ----------  API-KEY CHECK  ---------- */
            if (expectedApiKey != null) {
                String provided = request.getHeaders().getOrDefault("X-API-Key", "");
                if (!expectedApiKey.equals(provided)) {
                    out.print("HTTP/1.1 401 Unauthorized\r\n");
                    out.print("WWW-Authenticate: ApiKey realm=\"SimpleServer\"\r\n");
                    out.print("Content-Length: 0\r\n\r\n");
                    out.flush();
                    return;
                }
            }
            /* ------------------------------------- */

            String key = request.getMethod().toUpperCase() + " " + request.getPath();
            RequestHandler h = routes.get(key);
            SimpleHttpResponseWriter resp = new SimpleHttpResponseWriter(out);
            if (h != null) h.handle(request, resp);
            else {
                resp.setStatus(404, "Not Found");
                resp.setHeader("Content-Type", "text/plain");
                resp.writeBody("404 Not Found");
            }
            resp.send();
            Logger.log(Logger.Level.DEBUG, "Received request: " + request.getMethod() + " " + request.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HttpRequest parseRequest(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null || line.isEmpty()) return null;
        String[] p = line.split(" ");
        if (p.length < 3) return null;

        HttpRequest r = new HttpRequest();
        r.setMethod(p[0]);
        r.setPath(p[1]);

        Map<String,String> headers = new HashMap<>();
        String h;
        while ((h = in.readLine()) != null && !h.isEmpty()) {
            int idx = h.indexOf(':');
            if (idx>0) headers.put(h.substring(0,idx).trim(), h.substring(idx+1).trim());
        }
        r.setHeaders(headers);

        int len = headers.getOrDefault("Content-Length", "0").isEmpty() ? 0 :
                Integer.parseInt(headers.getOrDefault("Content-Length","0"));
        if (len>0) {
            char[] buf = new char[len];
            int read = in.read(buf);
            r.setBody(new String(buf));
        }
        return r;
    }
}
