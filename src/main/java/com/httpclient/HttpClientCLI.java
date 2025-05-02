package main.java.com.httpclient;

import main.java.com.common.ApiKeyConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class HttpClientCLI {
    public static void main(String[] argv) {
        String apiKey = ApiKeyConfig.load(argv, 0);
        SimpleHttpClient client = new SimpleHttpClient(apiKey);
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("HTTP method or 'quit': ");
            String m = sc.nextLine();
            if ("quit".equalsIgnoreCase(m)) break;

            System.out.print("URL: ");
            String url = sc.nextLine();

            System.out.print("Extra header (Key:Value) blank=none: ");
            String hline = sc.nextLine();
            Map<String,String> hdrs = new HashMap<>();
            if (!hline.isBlank()) {
                String[] kv = hline.split(":",2);
                hdrs.put(kv[0].trim(), kv[1].trim());
            }

            System.out.print("Body (blank=none): ");
            String body = sc.nextLine();
            try {
                HttpResponse r = client.request(m, url, hdrs, body.isBlank()?null:body);
                System.out.println("Status: "+r.getStatusCode()+" "+r.getStatusMessage());
                r.getHeaders().forEach((k,v)-> System.out.println(k+": "+v));
                System.out.println();
                System.out.println(r.getBody());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}
