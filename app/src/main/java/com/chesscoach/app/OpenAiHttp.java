package com.chesscoach.app;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
/** Fixed HTTPS destinations, bounded reads, no credential-bearing redirects or logs. */
final class OpenAiHttp {
    static HttpURLConnection open(String url,String token,String type,String body)throws Exception {
        URI uri=new URI(url);if(!"https".equals(uri.getScheme())||!java.util.Arrays.asList("auth.openai.com","api.openai.com").contains(uri.getHost())||uri.getUserInfo()!=null||uri.getFragment()!=null)throw new IOException("OpenAI 주소를 확인할 수 없습니다.");
        HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setConnectTimeout(15000);c.setReadTimeout(120000);c.setInstanceFollowRedirects(false);c.setRequestProperty("Accept",body!=null&&url.endsWith("/responses")?"text/event-stream":"application/json");
        if(token!=null)c.setRequestProperty("Authorization","Bearer "+token);
        try{if(body!=null){c.setRequestMethod("POST");c.setRequestProperty("Content-Type",type);c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.getBytes(StandardCharsets.UTF_8));}}return c;}catch(Exception e){c.disconnect();throw e;}
    }
    static String read(InputStream in,int limit)throws Exception {if(in==null)return "";try(in){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>limit)throw new IOException("OpenAI 응답이 너무 큽니다.");out.write(buf,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}}
    static void check(HttpURLConnection c)throws Exception {int status=c.getResponseCode();if(status>=200&&status<300)return;String body=read(c.getErrorStream(),65536);JSONObject json=null;try{json=new JSONObject(body);}catch(JSONException ignored){}String id=c.getHeaderField("x-request-id");if(id==null)id=c.getHeaderField("openai-request-id");throw ChatGptProtocol.ApiError.from(status,json,id==null?"":id);}
    static JSONObject json(String url,String token,String body,String type)throws Exception {HttpURLConnection c=open(url,token,type,body);try{check(c);String s=read(c.getInputStream(),1048576);return s.isEmpty()?new JSONObject():new JSONObject(s);}finally{c.disconnect();}}
    static JSONObject form(String url,java.util.Map<String,String> values)throws Exception{return json(url,null,ChatGptProtocol.form(values),"application/x-www-form-urlencoded");}
    static JSONObject response(String token,JSONObject body)throws Exception {return response(token,body,null);}
    static JSONObject response(String token,JSONObject body,ChatGptProtocol.TextProgress progress)throws Exception {HttpURLConnection c=open(ChatGptProtocol.RESOURCE+"/responses",token,"application/json",body.toString());try{check(c);try(Reader reader=new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8)){return ChatGptProtocol.completed(reader,progress);}}finally{c.disconnect();}}
}
