package com.chesscoach.app;
import android.content.*;
import org.json.*;
import java.util.*;
/** Per-registration credentials; refreshes and profile switches share one lock. */
public final class ChatGptAccounts {
    private static final Object REFRESH_LOCK=new Object();
    private final Context context;private final ChatGptStore store;
    private static Map<String,String> params(String... pairs){Map<String,String> map=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)map.put(pairs[i],pairs[i+1]);return map;}
    public ChatGptAccounts(Context c){context=c;store=new ChatGptStore(c);}
    public static boolean connected(Context c){try{return ChatGptProtocol.sharing(new ChatGptAccounts(c).active());}catch(Exception ignored){return false;}}
    public String host(){synchronized(ChatGptStore.LOCK){var p=context.getSharedPreferences("chatgpt-install",0);String id=p.getString("host","");if(id.isEmpty()){id="urn:uuid:"+UUID.randomUUID();if(!p.edit().putString("host",id).commit())throw new IllegalStateException("설치 식별자를 저장할 수 없습니다.");}return id;}}
    private static JSONObject find(JSONObject all,String id)throws Exception {JSONArray array=all.getJSONArray("profiles");for(int i=0;i<array.length();i++)if(array.getJSONObject(i).getString("client_id").equals(id))return array.getJSONObject(i);return new JSONObject();}
    public JSONObject active()throws Exception {synchronized(ChatGptStore.LOCK){JSONObject all=store.read();return find(all,all.optString("active"));}}
    public JSONObject profile(String id)throws Exception {synchronized(ChatGptStore.LOCK){return find(store.read(),id);}}
    public JSONArray profiles()throws Exception {synchronized(ChatGptStore.LOCK){JSONArray summaries=new JSONArray(),all=store.read().getJSONArray("profiles");for(int i=0;i<all.length();i++){JSONObject p=all.getJSONObject(i);summaries.put(new JSONObject().put("client_id",p.getString("client_id")).put("email",p.optString("email","등록 진행 중")).put("connected",ChatGptProtocol.sharing(p)));}return summaries;}}
    public void registration(String id)throws Exception {synchronized(ChatGptStore.LOCK){JSONObject all=store.read();if(find(all,id).length()==0){all.getJSONArray("profiles").put(new JSONObject().put("client_id",id));store.write(all);}}}
    public void accept(ChatGptProtocol.Attempt attempt,ChatGptProtocol.Callback callback)throws Exception {
        registration(callback.client());JSONObject tokens=OpenAiHttp.form(ChatGptProtocol.TOKEN,params("grant_type","authorization_code","client_id",callback.client(),"code",callback.code(),"code_verifier",attempt.verifier(),"redirect_uri",attempt.redirect(),"resource",ChatGptProtocol.RESOURCE));
        JSONObject discovery=OpenAiHttp.json(ChatGptProtocol.ISSUER+"/.well-known/openid-configuration",null,null,null);
        if(!ChatGptProtocol.ISSUER.equals(discovery.getString("issuer")))throw new Exception("OpenAI 발급자를 확인할 수 없습니다.");
        String jwks=OpenAiHttp.json(discovery.getString("jwks_uri"),null,null,null).toString();JSONObject identity=ChatGptProtocol.identity(tokens.getString("id_token"),jwks,callback.client(),attempt.nonce(),attempt.subject());
        synchronized(ChatGptStore.LOCK){if(Thread.currentThread().isInterrupted())throw new java.io.IOException("로그인을 취소했습니다.");JSONObject all=store.read(),p=find(all,callback.client());for(String k:new String[]{"issuer","subject","email"})p.put(k,identity.get(k));replaceTokens(p,tokens,false);all.put("active",callback.client());store.write(all);clearModels();}
    }
    static void replaceTokens(JSONObject profile,JSONObject tokens,boolean refresh)throws Exception {
        if(!"Bearer".equalsIgnoreCase(tokens.getString("token_type"))||tokens.getString("access_token").isEmpty()||tokens.getLong("expires_in")<=0)throw new Exception("로그인 토큰 응답을 확인할 수 없습니다.");
        String scope=tokens.has("scope")?tokens.getString("scope"):refresh?profile.optString("scope",""):"";
        profile.put("access_token",tokens.getString("access_token")).put("scope",scope).put("expires_at",System.currentTimeMillis()+tokens.getLong("expires_in")*1000L).put("earliest_refresh_at",tokens.optLong("earliest_refresh_at",0)*1000L);
        if(tokens.has("refresh_token"))profile.put("refresh_token",tokens.getString("refresh_token"));else if(refresh)throw new Exception("교체된 갱신 토큰이 없습니다.");else profile.remove("refresh_token");
        if(!refresh){profile.put("id_token",tokens.getString("id_token"));profile.remove("paused");}
    }
    private void clearModels(){context.getSharedPreferences("coach",0).edit().remove("models").remove("modelCatalog").remove("model").remove("endpoint").remove("token").remove("iv").apply();}
    public void select(String id)throws Exception {synchronized(ChatGptStore.LOCK){JSONObject all=store.read();if(find(all,id).length()==0)throw new Exception("등록된 계정이 없습니다.");all.put("active",id);store.write(all);clearModels();}}
    public record Credential(String client,String access) {}
    public Credential credential()throws Exception {synchronized(REFRESH_LOCK){
        JSONObject snapshot=active();String client=snapshot.optString("client_id"),refresh=snapshot.optString("refresh_token");
        if(!ChatGptProtocol.sharing(snapshot))throw new Exception("ChatGPT로 로그인하고 구독 사용 권한을 허용하세요.");
        if(snapshot.optBoolean("paused",false))throw ChatGptProtocol.ApiError.from(429,new JSONObject().put("error",new JSONObject().put("code","subscription_sharing_usage_limit_exceeded")),"");
        long now=System.currentTimeMillis();if(snapshot.optLong("expires_at")<now+60000){
            if(refresh.isEmpty())throw new Exception("ChatGPT에 다시 로그인하세요.");if(snapshot.optLong("earliest_refresh_at")>now)throw new Exception("아직 계정을 갱신할 수 없습니다. 잠시 후 다시 시도하세요.");
            JSONObject fresh;
            try{fresh=OpenAiHttp.form(ChatGptProtocol.TOKEN,params("grant_type","refresh_token","client_id",client,"refresh_token",refresh,"resource",ChatGptProtocol.RESOURCE));}
            catch(ChatGptProtocol.ApiError e){if(e.terminalRefresh())synchronized(ChatGptStore.LOCK){JSONObject all=store.read(),p=find(all,client);if(refresh.equals(p.optString("refresh_token"))){clearTokens(p);store.write(all);}}throw e;}
            synchronized(ChatGptStore.LOCK){JSONObject all=store.read(),p=find(all,client);if(!refresh.equals(p.optString("refresh_token")))throw new Exception("계정 세션이 바뀌었습니다. 다시 시도하세요.");replaceTokens(p,fresh,true);store.write(all);snapshot=p;}
        }
        if(!ChatGptProtocol.sharing(snapshot))throw new Exception("ChatGPT 구독 사용 권한이 없습니다.");Credential result=new Credential(client,snapshot.getString("access_token"));assertActive(result);return result;
    }}
    public void assertActive(Credential credential)throws Exception {if(!credential.client().equals(active().optString("client_id"))||!ChatGptProtocol.sharing(active()))throw new Exception("계정이 바뀌거나 로그아웃됐습니다. 요청을 다시 시작하세요.");}
    public void loadModels()throws Exception {Credential credential=credential();JSONObject reply=OpenAiHttp.json(ChatGptProtocol.RESOURCE+"/models",credential.access(),null,null);JSONArray models=new JSONArray(),array=reply.getJSONArray("models");List<String> slugs=new ArrayList<>();for(int i=0;i<array.length();i++){JSONObject m=array.getJSONObject(i);if(!"list".equals(m.optString("visibility")))continue;String slug=m.getString("slug");slugs.add(slug);models.put(new JSONObject().put("slug",slug).put("display_name",m.optString("display_name",slug)));}if(models.length()==0)throw new Exception("이 계정에서 선택 가능한 모델이 없습니다.");synchronized(ChatGptStore.LOCK){assertActive(credential);var prefs=context.getSharedPreferences("coach",0);var edit=prefs.edit().putString("modelCatalog",models.toString()).putString("models",String.join(",",slugs));if(!slugs.contains(prefs.getString("model","")))edit.putString("model",slugs.get(0));edit.apply();}}
    private static void clearTokens(JSONObject p){for(String k:new String[]{"access_token","refresh_token","id_token","scope","expires_at","earliest_refresh_at","paused"})p.remove(k);}
    public void pause(Credential credential,ChatGptProtocol.ApiError error)throws Exception {if(!error.limit())return;synchronized(ChatGptStore.LOCK){JSONObject all=store.read(),p=find(all,credential.client());if(credential.access().equals(p.optString("access_token"))){p.put("paused",true);store.write(all);}}}
    public void resumeRequests()throws Exception {synchronized(ChatGptStore.LOCK){JSONObject all=store.read();find(all,all.optString("active")).remove("paused");store.write(all);}}
    public boolean logout()throws Exception {
        JSONObject snapshot=active();String client=snapshot.optString("client_id"),refresh=snapshot.optString("refresh_token");
        // Stop local use immediately. Network revocation must never block the UI's profile reads.
        synchronized(ChatGptStore.LOCK){JSONObject all=store.read();clearTokens(find(all,client));store.write(all);clearModels();}
        if(refresh.isEmpty())return true;
        try{JSONObject discovery=OpenAiHttp.json(ChatGptProtocol.ISSUER+"/.well-known/openid-configuration",null,null,null);OpenAiHttp.form(discovery.getString("revocation_endpoint"),params("token",refresh,"token_type_hint","refresh_token","client_id",client));return true;}catch(Exception ignored){return false;}
    }
}
