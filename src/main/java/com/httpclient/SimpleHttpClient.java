package main.java.com.httpclient;

import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SimpleHttpClient {

    /**
     * Main method to send an HTTP request and get a response.
     *
     * @param method  - HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param url     - Complete URL (e.g., "http://example.com:8080/cats")
     * @param headers - Additional headers to send (excluding Host and Content-Length)
     * @param body    - Body content (for methods like POST, PUT), may be null or empty
     * @return        - Parsed HttpResponse with status, headers, body
     * @throws IOException - If there's an error with socket communication
     */
    public HttpResponse request(String method, String url,
                                Map<String, String> headers,
                                String body) throws IOException {
        // Parse URL (scheme, host, port, path)
        ParsedUrl parsedUrl = parseUrl(url);

        // Open a socket to host:port
        Socket socket = new Socket(parsedUrl.getHost(), parsedUrl.getPort());
        // Create the output and input streams once (do not close them separately)
        PrintWriter out = new PrintWriter(socket.getOutputStream(), false);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        try {
            // Build the HTTP request string
            String requestString = buildRequestString(method, parsedUrl, headers, body);

            // Send the request (write and flush without closing the stream)
            out.print(requestString);
            out.flush();

            // Read and parse the response from the already open reader
            return readResponse(reader);
        } finally {
            // Close resources in the reverse order of creation
            try { out.close(); } catch (Exception e) { /* Ignore */ }
            try { reader.close(); } catch (Exception e) { /* Ignore */ }
            try { socket.close(); } catch (Exception e) { /* Ignore */ }
        }
    }

    /**
     * Parse a URL into scheme, host, port, and path.
     */
    private ParsedUrl parseUrl(String urlString) {
        ParsedUrl result = new ParsedUrl();
        try {
            URI uri = new URI(urlString);
            result.setScheme(uri.getScheme() != null ? uri.getScheme() : "http");
            result.setHost(uri.getHost());
            // If no port is specified, default to 80 (for HTTP)
            int port = (uri.getPort() == -1) ? 80 : uri.getPort();
            result.setPort(port);
            // If path is empty, default to "/"
            String path = (uri.getPath() == null || uri.getPath().isEmpty()) ? "/" : uri.getPath();
            result.setPath(path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + urlString, e);
        }
        return result;
    }

    /**
     * Build the HTTP request string (start-line + headers + body).
     */
    private String buildRequestString(String method,
                                      ParsedUrl parsedUrl,
                                      Map<String, String> headers,
                                      String body) {
        StringBuilder sb = new StringBuilder();

        // Request line: e.g. "GET /cats HTTP/1.1"
        sb.append(method)
                .append(" ")
                .append(parsedUrl.getPath())
                .append(" HTTP/1.1\r\n");

        // Host header
        sb.append("Host: ")
                .append(parsedUrl.getHost())
                .append("\r\n");

        // Additional headers
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                sb.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("\r\n");
            }
        }

        // If there is a body, add Content-Length header
        if (body != null && !body.isEmpty()) {
            sb.append("Content-Length: ")
                    .append(body.getBytes().length)
                    .append("\r\n");
        }

        // End headers with a blank line
        sb.append("\r\n");

        // Append body if present
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }

        return sb.toString();
    }

    /**
     * Read and parse the HTTP response from the BufferedReader.
     */
    private HttpResponse readResponse(BufferedReader reader) throws IOException {
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setHeaders(new HashMap<>());

        // Read the status line, e.g. "HTTP/1.1 200 OK"
        String statusLine = reader.readLine();
        if (statusLine == null || !statusLine.startsWith("HTTP/")) {
            throw new IOException("Invalid response status line: " + statusLine);
        }
        String[] parts = statusLine.split(" ", 3);
        httpResponse.setStatusCode(Integer.parseInt(parts[1]));
        httpResponse.setStatusMessage(parts.length > 2 ? parts[2] : "");

        // Read headers until a blank line is encountered
        String headerLine;
        int contentLength = 0;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(":");
            if (colonIndex != -1) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                httpResponse.getHeaders().put(headerName, headerValue);
                if (headerName.equalsIgnoreCase("Content-Length")) {
                    contentLength = Integer.parseInt(headerValue);
                }
            }
        }

        // Read the body if a Content-Length is provided
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int read = reader.read(bodyChars, 0, contentLength);
            if (read > 0) {
                httpResponse.setBody(new String(bodyChars, 0, read));
            }
        }
        return httpResponse;
    }
}
