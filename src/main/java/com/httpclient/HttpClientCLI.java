package main.java.com.httpclient;

import main.java.com.common.ApiKeyConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/*
 * Interactive CLI for sending arbitrary HTTP requests in a single session.
 */
public class HttpClientCLI {
    public static void main(String[] argv) {
        // load API key from args/env/file
        String apiKey = ApiKeyConfig.load(argv, 0);

        // HTTP client instance (will reuse connection settings)
        SimpleHttpClient client = new SimpleHttpClient(apiKey);
        Scanner sc = new Scanner(System.in);

        while (true) {
            // get HTTP verb or exit
            System.out.print("HTTP method or 'quit': ");
            String method = sc.nextLine().trim();
            if ("quit".equalsIgnoreCase(method)) break;

            // get target URL
            System.out.print("URL: ");
            String url = sc.nextLine().trim();

            // read arbitrary headers from user
            System.out.println("Enter headers (Key:Value), blank line to finish:");
            Map<String, String> headers = new HashMap<>();
            while (true) {
                String line = sc.nextLine().trim();
                if (line.isBlank()) break;
                String[] kv = line.split(":", 2);
                if (kv.length == 2) {
                    headers.put(kv[0].trim(), kv[1].trim());
                } else {
                    System.out.println("Invalid header format, expected Key:Value");
                }
            }

            // read request body (multi-line) until blank line
            System.out.println("Enter body (blank line to finish):");
            StringBuilder bodyBuilder = new StringBuilder();
            while (true) {
                String line = sc.nextLine();
                if (line.isBlank()) break;
                bodyBuilder.append(line).append("\n");
            }
            String body = bodyBuilder.isEmpty() ? null : bodyBuilder.toString();

            // perform the HTTP request
            try {
                HttpResponse response = client.request(method, url, headers, body);

                // display status line
                System.out.println("Response status: " + response.getStatusCode() + " "
                        + response.getStatusMessage());

                // show response headers
                System.out.println("Response headers:");
                response.getHeaders().forEach((k, v) -> System.out.println(k + ": " + v));

                // show response body
                System.out.println("\nResponse body:");
                System.out.println(response.getBody());

            } catch (Exception e) {
                System.err.println("Request failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
        sc.close();
    }
}
