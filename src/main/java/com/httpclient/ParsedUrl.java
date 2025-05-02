package main.java.com.httpclient;

import java.net.URI;
import java.net.URISyntaxException;

public class ParsedUrl {
    private String scheme, host, path;
    private int port;

    public static ParsedUrl parse(String url) {
        try {
            URI u = new URI(url);
            ParsedUrl p = new ParsedUrl();
            p.scheme = u.getScheme()==null?"http":u.getScheme();
            p.host = u.getHost();
            p.port = (u.getPort()==-1) ? 80 : u.getPort();
            p.path = (u.getPath()==null||u.getPath().isEmpty())?"/":u.getPath();
            return p;
        } catch (URISyntaxException e) { throw new IllegalArgumentException(e); }
    }
    public String host(){
        return host;
    }
    public String path(){
        return path;
    }
    public int port(){
        return port;
    }
}
