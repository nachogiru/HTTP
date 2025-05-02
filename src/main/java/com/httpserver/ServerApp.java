package main.java.com.httpserver;

import main.java.com.common.ApiKeyConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerApp {

    private static final Map<Integer,Map<String,Object>> store = new HashMap<>();
    private static final AtomicInteger ids = new AtomicInteger(1);

    public static void main(String[] args) {
        int port = args.length>0?Integer.parseInt(args[0]):8080;
        String apiKey = ApiKeyConfig.load(args, 1);
        SimpleHttpServer srv = new SimpleHttpServer(port, apiKey);

        /* ---- static file ---- */
        srv.on("GET","/static",(req,res)->{
            try {
                byte[] b = Files.readAllBytes(Paths.get("index.html"));
                res.setStatus(200,"OK");
                res.setHeader("Content-Type","text/html");
                res.writeBody(new String(b));
            } catch (IOException e){
                res.setStatus(404,"Not Found");
                res.setHeader("Content-Type","text/plain");
                res.writeBody("Could not find the static file");
            }
        });

        /* ---- create ---- */
        srv.on("POST","/resources",(req,res)->{
            if (!isJson(req)){bad(res,"Expected JSON"); return;}
            Map<String,Object> data = parseJson(req.getBody());
            if (data==null){bad(res,"Invalid JSON"); return;}
            int id = ids.getAndIncrement();
            store.put(id,data);
            res.setStatus(201,"Created");
            res.setHeader("Content-Type","application/json");
            res.writeBody("{\"id\": "+id+"}");
        });

        /* ---- list ---- */
        srv.on("GET","/resources",(rq,rs)->{
            StringBuilder sb=new StringBuilder("[");
            boolean first=true;
            for (var e:store.entrySet()){
                if(!first) sb.append(",");
                sb.append(toJson(e.getKey(),e.getValue()));
                first=false;
            }
            sb.append("]");
            rs.setStatus(200,"OK");
            rs.setHeader("Content-Type","application/json");
            rs.writeBody(sb.toString());
        });

        /* ---- update & delete (dynamic) ---- */
        srv.on("PUT","/resources/",ServerApp::update);
        srv.on("DELETE","/resources/",ServerApp::delete);

        try { srv.start(); } catch (IOException e){
            e.printStackTrace();
        }
    }

    /* ---------- helpers ---------- */
    private static void bad(HttpResponseWriter r,String m){
        r.setStatus(400,"Bad Request");
        r.setHeader("Content-Type","text/plain");
        r.writeBody(m);
    }
    private static boolean isJson(HttpRequest r){
        String ct= r.getHeaders().getOrDefault("Content-Type","").toLowerCase();
        return ct.contains("application/json");
    }
    private static int parseId(String path){
        try {
            return Integer.parseInt(path.replaceFirst("^/resources/",""));
        }
        catch (Exception e){
            return -1;
        }
    }
    private static void update(HttpRequest rq,HttpResponseWriter rs){
        int id=parseId(rq.getPath());
        if (id<=0){ bad(rs,"Invalid ID"); return;}
        if (!store.containsKey(id)){
            rs.setStatus(404,"Not Found");
            rs.writeBody("Not found"); return;
        }
        if (!isJson(rq)){
            bad(rs,"Expected JSON"); return;
        }
        Map<String,Object> data=parseJson(rq.getBody());
        if (data==null){ bad(rs,"Invalid JSON"); return;}
        store.put(id,data);
        rs.setStatus(200,"OK"); rs.setHeader("Content-Type","application/json");
        rs.writeBody("{\"status\":\"updated\"}");
    }
    private static void delete(HttpRequest rq,HttpResponseWriter rs){
        int id=parseId(rq.getPath());
        if (id<=0){ bad(rs,"Invalid ID"); return;}
        if (store.remove(id)==null){ rs.setStatus(404,"Not Found"); rs.writeBody("Not found"); return;}
        rs.setStatus(200,"OK"); rs.setHeader("Content-Type","application/json");
        rs.writeBody("{\"status\":\"deleted\"}");
    }
    /* super-naive JSON (key:string or int) */
    private static Map<String,Object> parseJson(String j){
        try{
            Map<String,Object> m=new HashMap<>();
            String in=j.trim(); if(!in.startsWith("{")||!in.endsWith("}")) return null;
            in=in.substring(1,in.length()-1).trim();
            if(in.isEmpty()) return m;
            for (String p: in.split(",")){
                String[] kv=p.split(":",2); if(kv.length<2) return null;
                String k=kv[0].trim().replaceAll("^\"|\"$","");
                String v=kv[1].trim();
                if(v.startsWith("\"")&&v.endsWith("\"")) m.put(k, v.substring(1,v.length()-1));
                else m.put(k,Integer.parseInt(v));
            }
            return m;
        }catch(Exception e){ return null;}
    }
    private static String toJson(int id,Map<String,Object> d){
        StringBuilder sb=new StringBuilder("{\"id\":").append(id);
        d.forEach((k,v)->{
            sb.append(",\"").append(k).append("\":");
            sb.append((v instanceof Number)?v:("\""+v+"\""));
        });
        sb.append("}");
        return sb.toString();
    }
}
