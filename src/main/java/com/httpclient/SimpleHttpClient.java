package main.java.com.httpclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class SimpleHttpClient {

    private final String apiKey;   // may be null

    public SimpleHttpClient(String apiKey) { this.apiKey = apiKey; }

    public HttpResponse request(String method, String url,
                                Map<String,String> extraHeaders,
                                String body) throws Exception {

        ParsedUrl u = ParsedUrl.parse(url);
        try (Socket sock = new Socket(u.host(), u.port());
             PrintWriter out = new PrintWriter(sock.getOutputStream(), false);
             BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()))) {

            /* ---- build & send request ---- */
            StringBuilder sb = new StringBuilder();
            sb.append(method).append(" ").append(u.path()).append(" HTTP/1.1\r\n");
            sb.append("Host: ").append(u.host()).append("\r\n");
            if (apiKey!=null) sb.append("X-API-Key: ").append(apiKey).append("\r\n");
            if (extraHeaders!=null)
                extraHeaders.forEach((k,v)-> sb.append(k).append(": ").append(v).append("\r\n"));
            if (body!=null && !body.isEmpty())
                sb.append("Content-Length: ").append(body.getBytes().length).append("\r\n");
            sb.append("\r\n");
            if (body!=null && !body.isEmpty()) sb.append(body);
            out.print(sb.toString());
            out.flush();

            /* ---- read response ---- */
            HttpResponse resp = new HttpResponse();
            String status = in.readLine();
            String[] sp = status.split(" ",3);
            resp.setStatusCode(Integer.parseInt(sp[1]));
            resp.setStatusMessage(sp.length>2?sp[2]:"");

            String line;
            int len=0;
            while ((line = in.readLine())!=null && !line.isEmpty()){
                int idx=line.indexOf(':');
                String h=line.substring(0,idx).trim(), v=line.substring(idx+1).trim();
                resp.getHeaders().put(h,v);
                if (h.equalsIgnoreCase("Content-Length")) len=Integer.parseInt(v);
            }
            if (len>0){
                char[] buf=new char[len];
                in.read(buf);
                resp.setBody(new String(buf));
            }
            return resp;
        }
    }
}
