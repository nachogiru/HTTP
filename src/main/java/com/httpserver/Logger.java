package main.java.com.httpserver;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Thread‑safe file logger with four levels (INFO, WARN, ERROR, DEBUG).
// Appends plain‑text lines to server.log - one line per call.
public class Logger {
    private static final String LOG_FILE = "server.log"; // output file
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // timestamp format

    // Log severity enum – order not important here
    public enum Level { INFO, WARN, ERROR, DEBUG }

    // Writes a single log entry (synchronized to avoid clobbering).
    public static synchronized void log(Level level, String message) {
        String timestamp   = LocalDateTime.now().format(formatter);         // current time
        String fullMessage = String.format("[%s] [%s] %s", timestamp, level, message);

        // try‑with‑resources to auto‑close writer; true = append mode
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println(fullMessage);
        } catch (IOException e) {
            // fallback to stderr if file write fails
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
}