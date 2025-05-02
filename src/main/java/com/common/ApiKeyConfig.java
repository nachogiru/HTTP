package main.java.com.common;

import java.io.FileInputStream;
import java.util.Properties;

/** Loads an API key at runtime from
 *    • command-line arg   (highest priority)
 *    • environment var    API_KEY
 *    • api.properties     file in working dir
 *  If nothing is found, returns null (auth disabled). */
public final class ApiKeyConfig {

    private static final String PROP_FILE = "api.properties";

    public static String load(String[] argv, int argIndex) {
        if (argv.length > argIndex) return argv[argIndex];
        String env = System.getenv("API_KEY");
        if (env != null && !env.isBlank()) return env.trim();

        try (FileInputStream in = new FileInputStream(PROP_FILE)) {
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("apiKey");
            if (v != null && !v.isBlank()) return v.trim();
        } catch (Exception ignored) { }
        return null;
    }

    private ApiKeyConfig() { }
}
