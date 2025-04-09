package main.java.com.httpclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HttpClientCLI {

    public static void main(String[] args) {
        SimpleHttpClient client = new SimpleHttpClient();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter HTTP method (GET, POST, PUT, DELETE) or 'quit' to exit:");
            String method = scanner.nextLine();
            if (method.equalsIgnoreCase("quit")) {
                break;
            }

            System.out.println("Enter URL:");
            String url = scanner.nextLine();

            System.out.println("Any custom header? (format: Key=Value) or blank to skip:");
            String headerInput = scanner.nextLine();
            Map<String, String> headers = new HashMap<>();
            if (!headerInput.isEmpty()) {
                String[] kv = headerInput.split("=", 2);
                if (kv.length == 2) {
                    headers.put(kv[0].trim(), kv[1].trim());
                }
            }

            System.out.println("Enter request body (or leave empty if none):");
            String body = scanner.nextLine();

            try {
                HttpResponse response = client.request(method, url, headers, body);
                printResponse(response);
            } catch (IOException e) {
                System.err.println("Error performing request: " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void printResponse(HttpResponse response) {
        System.out.println("\n--- HTTP Response ---");
        System.out.println("Status: " + response.getStatusCode() + " " + response.getStatusMessage());
        System.out.println("Headers:");
        for (Map.Entry<String, String> h : response.getHeaders().entrySet()) {
            System.out.println("  " + h.getKey() + ": " + h.getValue());
        }
        System.out.println("Body:");
        System.out.println(response.getBody());
        System.out.println("---------------------\n");
    }
}
