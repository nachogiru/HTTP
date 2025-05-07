package main.java.com.common;

import java.io.FileInputStream;
import java.util.Properties;

/*
 * Utility for loading an API key at runtime.
 * Priority: argv → env var → properties file → null.
 */
public final class ApiKeyConfig {

    private static final String PROP_FILE = "api.properties"; // default properties file name

    // Load API key according to priority order
    public static String load(String[] argv, int argIndex) {
        // 1) command‑line argument (highest priority)
        if (argv.length > argIndex) return argv[argIndex];

        // 2) environment variable
        String env = System.getenv("API_KEY");
        if (env != null && !env.isBlank()) return env.trim();

        // 3) properties file (best‑effort)
        try (FileInputStream in = new FileInputStream(PROP_FILE)) {
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("apiKey");          // key expected as "apiKey=<value>"
            if (v != null && !v.isBlank()) return v.trim();
        } catch (Exception ignored) { /* file missing or unreadable → fall through */ }

        // 4) nothing found ⇒ return null (API auth disabled)
        return null;
    }

    private ApiKeyConfig() { } // prevent instantiation
}
