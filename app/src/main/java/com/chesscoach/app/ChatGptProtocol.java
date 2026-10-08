package com.chesscoach.app;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Public Sign in with ChatGPT protocol. No Codex endpoints, API key or client secret. */
public final class ChatGptProtocol {
    public static final String ISSUER="https://auth.openai.com",RESOURCE="https://api.openai.com/v1";
    public static final String AUTHORIZE=ISSUER+"/api/accounts/authorize",TOKEN=ISSUER+"/api/accounts/oauth/token";
    public static final String SCOPE="openid profile email offline_access resource.invoke chatgpt.tokens.use.direct";
    public static final String USAGE="https://chatgpt.com/settings/usage";
    public record Attempt(String state,String nonce,String verifier,String redirect,String client,String subject,long deadline) {}
    public record Callback(String code,String client) {}
    public static String random(int bytes){byte[] b=new byte[bytes];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    public static Attempt attempt(int port,String client,String subject){return new Attempt(random(32),random(32),random(64),"http://127.0.0.1:"+port+"/auth/callback",client,subject,System.currentTimeMillis()+600000);}
    private static String encode(String value){try{return URLEncoder.encode(value,"UTF-8");}catch(UnsupportedEncodingException impossible){throw new AssertionError(impossible);}}
    public static String form(Map<String,String> values){StringJoiner s=new StringJoiner("&");for(var e:values.entrySet())s.add(encode(e.getKey())+"="+encode(e.getValue()));return s.toString();}
    public static String authorize(Attempt a,String host,String hint,boolean consent)throws Exception {
        Map<String,String> q=new LinkedHashMap<>();q.put("client_id",a.client());q.put("ext_agent_host_id",host);
        if(a.client().equals("dynamic_agent_client"))q.put("agent_name_hint","ChessWithYourChatgpt");
        else if(hint!=null&&!hint.isEmpty())q.put("id_token_hint",hint);
        q.put("response_type","code");q.put("redirect_uri",a.redirect());q.put("scope",SCOPE);q.put("resource",RESOURCE);q.put("state",a.state());q.put("nonce",a.nonce());q.put("code_challenge_method","S256");
        q.put("code_challenge",Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(a.verifier().getBytes(StandardCharsets.US_ASCII))));if(consent)q.put("prompt","consent");return AUTHORIZE+"?"+form(q);
    }
    public static Callback callback(Attempt a,String target)throws Exception {
        URI u=new URI(target);if(u.isAbsolute()||!"/auth/callback".equals(u.getPath())||u.getFragment()!=null||u.getRawQuery()==null)throw new IOException("잘못된 로그인 콜백입니다.");
        Map<String,String> q=new HashMap<>();for(String part:u.getRawQuery().split("&")){String[] pair=part.split("=",2);String key=URLDecoder.decode(pair[0],"UTF-8"),value=pair.length==2?URLDecoder.decode(pair[1],"UTF-8"):"";if(q.put(key,value)!=null)throw new IOException("중복된 로그인 응답입니다.");}
        if(System.currentTimeMillis()>=a.deadline()||!MessageDigest.isEqual(a.state().getBytes(StandardCharsets.UTF_8),q.getOrDefault("state","").getBytes(StandardCharsets.UTF_8)))throw new IOException("로그인 요청을 확인할 수 없습니다. 다시 시작하세요.");
        if(q.containsKey("error"))throw new IOException("로그인 또는 사용 권한 동의가 취소됐습니다.");
        String cid=q.getOrDefault("client_id",a.client());if(cid.isEmpty()||cid.equals("dynamic_agent_client")||(!a.client().equals("dynamic_agent_client")&&!cid.equals(a.client())))throw new IOException("계정 등록 정보를 확인할 수 없습니다.");
        String code=q.getOrDefault("code","");if(code.isEmpty())throw new IOException("인증 코드가 없습니다.");return new Callback(code,cid);
    }
    public static JSONObject identity(String token,String jwks,String client,String nonce,String expectedSubject)throws Exception {
        SignedJWT jwt=SignedJWT.parse(token);if(!JWSAlgorithm.RS256.equals(jwt.getHeader().getAlgorithm()))throw new IOException("지원하지 않는 ID 토큰 서명입니다.");
        JWK key=JWKSet.parse(jwks).getKeyByKeyId(jwt.getHeader().getKeyID());if(!(key instanceof RSAKey rsa)||rsa.size()<2048||!jwt.verify(new RSASSAVerifier(rsa.toRSAPublicKey())))throw new IOException("계정 서명을 확인할 수 없습니다.");
        JWTClaimsSet c=jwt.getJWTClaimsSet();long now=System.currentTimeMillis();
        if(!ISSUER.equals(c.getIssuer())||!c.getAudience().contains(client)||c.getExpirationTime()==null||c.getExpirationTime().getTime()<=now-5000||c.getIssueTime()==null||c.getIssueTime().getTime()>now+5000||(c.getNotBeforeTime()!=null&&c.getNotBeforeTime().getTime()>now+5000)||!nonce.equals(c.getStringClaim("nonce"))||c.getSubject()==null||c.getSubject().isEmpty()||(expectedSubject!=null&&!expectedSubject.isEmpty()&&!expectedSubject.equals(c.getSubject()))||(c.getAudience().size()>1&&!client.equals(c.getStringClaim("azp"))))throw new IOException("계정의 발급자·대상·유효기간·로그인 요청을 확인할 수 없습니다.");
        return new JSONObject().put("issuer",c.getIssuer()).put("subject",c.getSubject()).put("email",c.getStringClaim("email")==null?"":c.getStringClaim("email"));
    }
    public static boolean sharing(JSONObject profile){List<String> scopes=Arrays.asList(profile.optString("scope","").split("\\s+"));return !profile.optString("access_token","").isEmpty()&&scopes.contains("chatgpt.tokens.use.direct")&&scopes.contains("resource.invoke");}
    public static JSONObject responseRequest(String model,String input,String instructions)throws Exception {return new JSONObject().put("model",model).put("input",new JSONArray().put(new JSONObject().put("role","user").put("content",input))).put("instructions",instructions).put("store",false).put("stream",true);}
    public static JSONObject completed(Reader source)throws Exception {
        BufferedReader reader=new BufferedReader(source);StringBuilder data=new StringBuilder(),text=new StringBuilder();long bytes=0;String line;
        while((line=reader.readLine())!=null){bytes+=line.length();if(bytes>4194304)throw new IOException("AI 응답이 너무 큽니다.");if(line.startsWith("data:")){if(data.length()>0)data.append('\n');data.append(line.substring(5).trim());}else if(line.isEmpty()&&data.length()>0){
                String event=data.toString();data.setLength(0);if(event.equals("[DONE]"))break;JSONObject e=new JSONObject(event);String type=e.optString("type");
                if(type.equals("response.output_text.delta")){text.append(e.optString("delta"));if(text.length()>65536)throw new IOException("AI 해설이 너무 깁니다.");}
                if(type.equals("response.failed"))throw ApiError.from(200,e.optJSONObject("response"),"");
                if(type.equals("error"))throw ApiError.from(200,e,"");
                if(type.equals("response.incomplete"))throw new IOException("AI 응답이 미완료입니다. 저장하지 않았습니다.");
                if(type.equals("response.completed")){JSONObject r=e.getJSONObject("response");if(!"completed".equals(r.optString("status")))throw new IOException("AI 응답 완료를 확인할 수 없습니다.");String full=outputText(r);if(!full.isEmpty())text=new StringBuilder(full);if(text.toString().trim().isEmpty()||text.length()>65536)throw new IOException("완료된 AI 응답이 비어 있거나 너무 깁니다.");return new JSONObject().put("text",text.toString()).put("usage",r.optJSONObject("usage")==null?JSONObject.NULL:r.getJSONObject("usage"));}
            }}throw new IOException("AI 연결이 완료 전에 끊겼습니다. 부분 응답은 저장하지 않았습니다.");
    }
    private static String outputText(JSONObject r){StringBuilder s=new StringBuilder();JSONArray out=r.optJSONArray("output");if(out!=null)for(int i=0;i<out.length();i++){JSONObject item=out.optJSONObject(i);JSONArray content=item==null?null:item.optJSONArray("content");if(content!=null)for(int j=0;j<content.length();j++){JSONObject c=content.optJSONObject(j);if(c!=null&&"output_text".equals(c.optString("type")))s.append(c.optString("text"));}}return s.toString();}
    public static final class ApiError extends IOException {
        public final int status;public final String code,requestId,shape;
        private ApiError(int status,String code,String requestId,String shape){super(message(status,code)+(requestId.isEmpty()?"":" · 요청 "+requestId));this.status=status;this.code=code;this.requestId=requestId;this.shape=shape;}
        public static ApiError from(int status,JSONObject body,String requestId){String code="",shape="empty";if(body!=null){Object error=body.opt("error");if(error instanceof JSONObject e){code=e.optString("code");shape="error-object";}else if(error instanceof String s){code=s;shape="error-string";}else if(body.has("detail"))shape="detail";else if(body.has("code")){code=body.optString("code");shape="event-error";}}return new ApiError(status,code,requestId,shape);}
        public boolean limit(){return code.equals("subscription_sharing_usage_limit_exceeded");}
        public boolean terminalRefresh(){return Arrays.asList("invalid_grant","invalid_refresh_token","token_expired","refresh_token_expired","refresh_token_invalidated","refresh_token_reused").contains(code);}
        private static String message(int status,String code){if(code.equals("subscription_sharing_usage_limit_exceeded"))return "ChatGPT 사용 한도에 도달했습니다. 사용량 관리에서 확인하세요.";if(code.equals("subscription_sharing_user_not_eligible"))return "이 계정 또는 워크스페이스는 ChatGPT 구독 사용 대상이 아닙니다.";if(status==401||code.contains("invalidated")||code.contains("revoked"))return "ChatGPT 계정과 사용 권한을 다시 확인하세요.";if(status==403)return "계정 권한·정책 또는 제공 지역에서 요청이 허용되지 않았습니다.";if(status==503)return "ChatGPT 연결이 일시적으로 불가능합니다. 잠시 후 다시 시도하세요.";return "ChatGPT 요청 실패 ("+status+(code.isEmpty()?"":" · "+code)+")";}
    }
}
