package main.java.com.httpclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/*
 * Very small blocking HTTP/1.1 client that supports:
 *   • arbitrary method, URL, headers, and optional body
 *   • persistent cookie jar (via CookieStore)
 *   • optional X-API-Key header for auth
 */
public class SimpleHttpClient {

    private final String apiKey;            // static API key (may be null)
    private final CookieStore cookies = new CookieStore(); // persists between requests

    public SimpleHttpClient(String apiKey) {
        this.apiKey = apiKey;
    }

    /* ------------------------------------------------------------------ */
    /*                       ░░  M A I N   A P I  ░░                       */
    /* ------------------------------------------------------------------ */

    // Perform a single HTTP request and return parsed response.
    public HttpResponse request(String method, String url,
                                Map<String,String> extraHeaders,
                                String body) throws Exception {

        ParsedUrl u = ParsedUrl.parse(url); // split into host/port/path

        try (Socket sock = new Socket(u.host(), u.port());
             PrintWriter out = new PrintWriter(sock.getOutputStream(), false);
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {

            /* -------- build request text -------- */
            StringBuilder sb = new StringBuilder();
            sb.append(method).append(" ").append(u.path()).append(" HTTP/1.1\r\n");
            sb.append("Host: ").append(u.host()).append("\r\n");

            // optional API key header
            if (apiKey != null)
                sb.append("X-API-Key: ").append(apiKey).append("\r\n");

            // attach cookies valid for this host + path
            List<Cookie> sendable = cookies.match(u.host(), u.path());
            if (!sendable.isEmpty()) {
                String cookieLine = sendable.stream()
                        .map(c -> c.name() + "=" + c.value())
                        .reduce((a,b) -> a + "; " + b)
                        .orElse("");
                sb.append("Cookie: ").append(cookieLine).append("\r\n");
            }

            // user‑supplied headers (may overwrite defaults)
            if (extraHeaders != null)
                extraHeaders.forEach((k,v) -> sb.append(k).append(": ").append(v).append("\r\n"));

            // body length header (only if body present)
            if (body != null && !body.isEmpty())
                sb.append("Content-Length: ").append(body.getBytes().length).append("\r\n");

            sb.append("\r\n");        // blank line separates headers + body
            if (body != null && !body.isEmpty()) sb.append(body);

            // send request
            out.print(sb.toString());
            out.flush();

            /* -------- read response -------- */
            HttpResponse resp = new HttpResponse();

            // status line (e.g. HTTP/1.1 200 OK)
            String status = in.readLine();
            String[] sp = status.split(" ", 3);
            resp.setStatusCode(Integer.parseInt(sp[1]));
            resp.setStatusMessage(sp.length > 2 ? sp[2] : "");

            // headers
            String line;
            int len = 0; // Content-Length if present
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                String h = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                resp.getHeaders().put(h, v);

                if (h.equalsIgnoreCase("Content-Length")) len = Integer.parseInt(v);
                if (h.equalsIgnoreCase("Set-Cookie"))
                    cookies.addFromHeader(v, u.host());
            }

            // body (blocking read of declared length)
            if (len > 0) {
                char[] buf = new char[len];
                in.read(buf);
                resp.setBody(new String(buf));
            }

            cookies.save(); // persist any new cookies to disk
            return resp;
        }
    }
}
