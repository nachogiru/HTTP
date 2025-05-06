package main.java.com.httpclient;

import main.java.com.common.ApiKeyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
 * Command‑line REPL that lets a user craft raw HTTP requests interactively.
 * Inline comments only – no Javadoc, to match the requested style.
 */
public class HttpClientCLI {
    public static void main(String[] argv) {
        // Load API key: from argv[0] if supplied, otherwise fall back to config
        String apiKey = ApiKeyConfig.load(argv, 0);

        // Create minimal HTTP client that attaches the key to each request
        SimpleHttpClient client = new SimpleHttpClient(apiKey);

        // Console reader for user input (blocking)
        Scanner sc = new Scanner(System.in);

        while (true) {
            // === Build request – step 1: method (or quit) ===
            System.out.print("HTTP method or 'quit': ");
            String m = sc.nextLine();
            if ("quit".equalsIgnoreCase(m)) break;   // exit REPL loop

            // === step 2: full URL ===
            System.out.print("URL: ");
            String url = sc.nextLine();

            // === step 3: optional single header in Key:Value form ===
            System.out.print("Extra header (Key:Value) blank=none: ");
            String hline = sc.nextLine();
            Map<String,String> hdrs = new HashMap<>();
            if (!hline.isBlank()) {
                String[] kv = hline.split(":", 2);  // only first ':' splits
                hdrs.put(kv[0].trim(), kv[1].trim()); // trim spaces around key/value
            }

            // === step 4: optional body ===
            System.out.print("Body (blank=none): ");
            String body = sc.nextLine();

            // === step 5: issue request and dump response ===
            try {
                HttpResponse r = client.request(
                        m, url, hdrs,
                        body.isBlank() ? null : body // null → no body
                );

                // Status line
                System.out.println("Status: " + r.getStatusCode() + " " + r.getStatusMessage());

                // Headers
                r.getHeaders().forEach((k, v) -> System.out.println(k + ": " + v));
                System.out.println();

                // Body
                System.out.println(r.getBody());

            } catch (Exception e) {
                e.printStackTrace(); // print stack trace for debugging
            }
        }
    }
}
