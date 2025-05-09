package main.java.com.httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/*
 * HTTP/1.1 client that supports:
 *   • arbitrary method, URL, headers, and optional body
 *   • persistent cookie jar (via CookieStore)
 *   • optional X-API-Key header for auth
 *   • default headers: Host, Accept, User-Agent, Connection: close
 */
public class SimpleHttpClient {

    private final String apiKey;                         // static API key (may be null)
    private final CookieStore cookies = new CookieStore(); // persists cookies across requests

    public SimpleHttpClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /*
     * Perform a single HTTP request and return parsed response.
     * @param method HTTP verb (GET, POST, etc.)
     * @param url    full URL including scheme (http://host:port/path)
     * @param extraHeaders user-specified headers (may override defaults)
     * @param body   request body (e.g. JSON) or null
     */
    public HttpResponse request(String method, String url,
                                Map<String, String> extraHeaders,
                                String body) throws Exception {

        ParsedUrl u = ParsedUrl.parse(url); // split URL into host, port, path

        try (Socket sock = new Socket(u.host(), u.port());
             PrintWriter out = new PrintWriter(sock.getOutputStream(), false);
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {

            // Build request line + headers
            StringBuilder sb = new StringBuilder();
            sb.append(method).append(" ").append(u.path()).append(" HTTP/1.1\r\n");
            sb.append("Host: ").append(u.host()).append("\r\n");

            // Default headers (unless overridden)
            if (extraHeaders == null || !extraHeaders.containsKey("Accept"))
                sb.append("Accept: */*\r\n");
            if (extraHeaders == null || !extraHeaders.containsKey("User-Agent"))
                sb.append("User-Agent: SimpleHttpClient/1.0\r\n");
            if (extraHeaders == null || !extraHeaders.containsKey("Connection"))
                sb.append("Connection: close\r\n");

            // API key header
            if (apiKey != null) {
                sb.append("X-API-Key: ").append(apiKey).append("\r\n");
            }

            // Cookies
            List<Cookie> sendable = cookies.match(u.host(), u.path());
            if (!sendable.isEmpty()) {
                String cookieLine = sendable.stream()
                        .map(c -> c.name() + "=" + c.value())
                        .reduce((a, b) -> a + "; " + b)
                        .orElse("");
                sb.append("Cookie: ").append(cookieLine).append("\r\n");
            }

            // User-specified headers (may overwrite any above)
            if (extraHeaders != null) {
                for (Map.Entry<String, String> e : extraHeaders.entrySet()) {
                    sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
                }
            }

            // Body header
            if (body != null && !body.isEmpty()) {
                sb.append("Content-Length: ")
                        .append(body.getBytes().length).append("\r\n");
            }

            // End of headers
            sb.append("\r\n");

            // Body payload
            if (body != null && !body.isEmpty()) {
                sb.append(body);
            }

            // Send request
            out.print(sb.toString());
            out.flush();

            // Read response
            HttpResponse resp = new HttpResponse();
            String statusLine = in.readLine();
            if (statusLine == null) throw new IOException("No response from server");
            String[] statusParts = statusLine.split(" ", 3);
            resp.setStatusCode(Integer.parseInt(statusParts[1]));
            resp.setStatusMessage(statusParts.length > 2 ? statusParts[2] : "");

            // Read headers
            String line;
            int contentLength = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx > 0) {
                    String headerName = line.substring(0, idx).trim();
                    String headerValue = line.substring(idx + 1).trim();
                    resp.getHeaders().put(headerName, headerValue);
                    if (headerName.equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(headerValue);
                    }
                    if (headerName.equalsIgnoreCase("Set-Cookie")) {
                        cookies.addFromHeader(headerValue, u.host());
                    }
                }
            }

            // Read body if present
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = in.read(buf);
                resp.setBody(new String(buf, 0, read));
            }

            // Persist cookies
            cookies.save();
            return resp;
        }
    }
}