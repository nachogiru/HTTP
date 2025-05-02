package main.java.com.httpclient;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CookieStore {

    private static final String FILE = "cookies.db";
    private final List<Cookie> jar = new ArrayList<>();

    public CookieStore() { load(); }

    /* -------- public API -------- */

    /** Return cookies valid for host + path, as "name=value" pairs. */
    public List<Cookie> match(String host, String path) {
        long now = System.currentTimeMillis();
        List<Cookie> list = new ArrayList<>();
        for (Cookie c : jar) {
            if (now > c.expiresAt()) continue;                        // expired
            if (!domainMatch(host, c.domain())) continue;
            if (!path.startsWith(c.path())) continue;
            list.add(c);
        }
        return list;
    }

    /** Parse a Set‑Cookie header and add / replace cookie in the jar. */
    public void addFromHeader(String setCookie, String reqHost) {
        // Split tokens:  name=value ; attr ; attr ...
        String[] tokens = setCookie.split(";", -1);
        if (tokens.length == 0) return;
        String[] nv = tokens[0].trim().split("=", 2);
        if (nv.length < 2) return;
        String name = nv[0].trim(), value = nv[1].trim();

        String domain = reqHost;
        String path   = "/";
        long   exp    = Long.MAX_VALUE;     // session cookie default

        for (int i = 1; i < tokens.length; i++) {
            String t = tokens[i].trim();
            int eq = t.indexOf('=');
            String attr = eq > 0 ? t.substring(0, eq).trim().toLowerCase() : t.toLowerCase();
            String val  = eq > 0 ? t.substring(eq + 1).trim() : "";
            switch (attr) {
                case "domain"  -> domain = val.startsWith(".") ? val.substring(1) : val;
                case "path"    -> path = val.isBlank() ? "/" : val;
                case "max-age" -> {
                    try { exp = System.currentTimeMillis() + 1000L * Long.parseLong(val); }
                    catch (NumberFormatException ignored) { }
                }
                case "expires" -> {
                    try { exp = Date.from(Instant.parse(Rfc1123.toIso(val))).getTime(); }
                    catch (Exception ignored) { }
                }
            }
        }
        // Replace if same (domain, path, name)
        String finalPath = path;
        String finalDomain = domain;
        jar.removeIf(c -> c.name().equals(name) && c.domain().equalsIgnoreCase(finalDomain)
                && c.path().equals(finalPath));
        jar.add(new Cookie(name, value, domain, path, exp));
    }

    public void save() {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(FILE))) {
            for (Cookie c : jar) {
                w.write(String.join("\t",
                        c.name(), c.value(), c.domain(), c.path(), Long.toString(c.expiresAt())));
                w.newLine();
            }
        } catch (IOException ignored) { }
    }

    /* -------- helpers -------- */

    private void load() {
        Path p = Paths.get(FILE);
        if (!Files.exists(p)) return;
        try (BufferedReader r = Files.newBufferedReader(p)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split("\t", 5);
                if (f.length < 5) continue;
                jar.add(new Cookie(f[0], f[1], f[2], f[3], Long.parseLong(f[4])));
            }
        } catch (Exception ignored) { }
    }

    private static boolean domainMatch(String host, String cookieDomain) {
        if (host.equalsIgnoreCase(cookieDomain)) return true;
        return host.endsWith("." + cookieDomain);
    }

    /* tiny RFC‑1123 helper */
    private static final class Rfc1123 {
        static String toIso(String rfc1123) { // Tue, 15 Nov 2022 12:45:26 GMT → 2022‑11‑15T12:45:26Z
            return DateTimeFormatter.RFC_1123_DATE_TIME.parse(rfc1123, Instant::from).toString();
        }
    }
}
