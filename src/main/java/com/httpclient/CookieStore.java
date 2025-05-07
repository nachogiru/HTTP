package main.java.com.httpclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Simple persistent cookie store for single‑threaded HTTP clients.
 */
public class CookieStore {

    private static final String FILE = "cookies.db"; // file for persistence

    // in‑memory list of cookies (the "cookie jar")
    private final List<Cookie> jar = new ArrayList<>();

    public CookieStore() {
        load(); // load cookies from disk at startup
    }

    // Return cookies that match given host and path
    public List<Cookie> match(String host, String path) {
        long now = System.currentTimeMillis();
        List<Cookie> list = new ArrayList<>();

        for (Cookie c : jar) {
            if (now > c.expiresAt()) continue;           // expired → skip
            if (!domainMatch(host, c.domain())) continue;// domain mismatch
            if (!path.startsWith(c.path())) continue;    // path mismatch
            list.add(c);
        }
        return list;
    }

    // Parse a Set‑Cookie header and add or replace the cookie in the jar
    public void addFromHeader(String setCookie, String reqHost) {
        String[] tokens = setCookie.split(";", -1);     // name=value ; attr ; attr ...
        if (tokens.length == 0) return;

        String[] nv = tokens[0].trim().split("=", 2);
        if (nv.length < 2) return;                       // malformed header
        String name  = nv[0].trim();
        String value = nv[1].trim();

        // Defaults when attributes are missing
        String domain = reqHost;
        String path   = "/";
        long   exp    = Long.MAX_VALUE;                  // session cookie (no expiry)

        // Walk through optional attributes
        for (int i = 1; i < tokens.length; i++) {
            String t  = tokens[i].trim();
            int eq    = t.indexOf('=');
            String attr = eq > 0 ? t.substring(0, eq).trim().toLowerCase() : t.toLowerCase();
            String val  = eq > 0 ? t.substring(eq + 1).trim() : "";

            switch (attr) {
                case "domain"  -> domain = val.startsWith(".") ? val.substring(1) : val;
                case "path"    -> path   = val.isBlank() ? "/" : val;
                case "max-age" -> {
                    // relative seconds from now
                    try { exp = System.currentTimeMillis() + 1000L * Long.parseLong(val); }
                    catch (NumberFormatException ignored) {}
                }
                case "expires" -> {
                    // absolute RFC‑1123 timestamp
                    try { exp = Date.from(Instant.parse(Rfc1123.toIso(val))).getTime(); }
                    catch (Exception ignored) {}
                }
                // Secure, HttpOnly, SameSite … are ignored
            }
        }

        // Replace existing cookie with identical (name, domain, path)
        String finalDomain = domain;
        String finalPath   = path;
        jar.removeIf(c -> c.name().equals(name)
                && c.domain().equalsIgnoreCase(finalDomain)
                && c.path().equals(finalPath));

        jar.add(new Cookie(name, value, domain, path, exp)); // add new/updated cookie
    }

    // Persist all cookies to disk
    public void save() {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(FILE))) {
            for (Cookie c : jar) {
                w.write(String.join("\t",
                        c.name(), c.value(), c.domain(), c.path(), Long.toString(c.expiresAt())));
                w.newLine();
            }
        } catch (IOException ignored) {}
    }

    // Load cookies from disk at startup
    private void load() {
        Path p = Paths.get(FILE);
        if (!Files.exists(p)) return;

        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split("\t", 5);
                if (f.length < 5) continue;              // skip malformed lines
                jar.add(new Cookie(f[0], f[1], f[2], f[3], Long.parseLong(f[4])));
            }
        } catch (Exception ignored) {}
    }

    // RFC 6265 domain‑match predicate
    private static boolean domainMatch(String host, String cookieDomain) {
        if (host.equalsIgnoreCase(cookieDomain)) return true; // exact match
        return host.endsWith('.' + cookieDomain);            // sub‑domain match
    }

    // Helper: convert RFC‑1123 date to ISO‑8601 so Instant can parse it
    private static final class Rfc1123 {
        static String toIso(String rfc1123) {
            return DateTimeFormatter.RFC_1123_DATE_TIME
                    .parse(rfc1123, Instant::from)
                    .toString();
        }
    }
}
