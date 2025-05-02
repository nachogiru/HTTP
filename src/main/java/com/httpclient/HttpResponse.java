package main.java.com.httpclient;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private int statusCode;
    private String statusMessage;
    private Map<String,String> headers = new HashMap<>();
    private String body;

    public int getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(int c){
        statusCode=c;
    }
    public String getStatusMessage(){
        return statusMessage;
    }
    public void setStatusMessage(String m){
        statusMessage=m;
    }
    public Map<String,String> getHeaders(){
        return headers;
    }
    public String getBody(){
        return body;
    }
    public void setBody(String b){
        body=b;
    }
}
