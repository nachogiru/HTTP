package main.java.com.httpserver;

public class ServerApp {
    public static void main(String[] args) {
        SimpleHttpServer server = new SimpleHttpServer(8080);

        // Register a route for GET /cats
        server.on("GET", "/cats", (request, response) -> {
            response.setStatus(200, "OK");
            response.setHeader("Content-Type", "text/plain");
            response.writeBody("Welcome to the Cat API!");
        });

        // Register a route for POST /cats
        server.on("POST", "/cats", (request, response) -> {
            response.setStatus(201, "Created");
            response.setHeader("Content-Type", "application/json");
            response.writeBody("{ \"message\": \"New cat created.\" }");
        });

        try {
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
