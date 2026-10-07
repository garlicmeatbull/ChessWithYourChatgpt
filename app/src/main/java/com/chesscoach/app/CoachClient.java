package com.chesscoach.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;

/** No ChatGPT OAuth tokens on the phone; only the companion pairing credential. */
public final class CoachClient {
    private final Context context;
    public CoachClient(Context c){context=c;}
    private javax.crypto.SecretKey key()throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias("coach-pairing")) {
            KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            g.init(new KeyGenParameterSpec.Builder("coach-pairing",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
        }
        return (javax.crypto.SecretKey)store.getKey("coach-pairing",null);
    }
    public void saveToken(String token)throws Exception {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        context.getSharedPreferences("coach",0).edit()
            .putString("token",Base64.encodeToString(c.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP))
            .putString("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)).apply();
    }
    private String token()throws Exception {
        var prefs=context.getSharedPreferences("coach",0);String data=prefs.getString("token","");if(data.isEmpty())return "";
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(data,Base64.NO_WRAP)),StandardCharsets.UTF_8);
    }
    public static void validateEndpoint(String endpoint)throws Exception {
        URI uri=new URI(endpoint);
        boolean debug=(com.chesscoach.app.BuildConfig.DEBUG);
        boolean local=Arrays.asList("10.0.2.2","localhost","127.0.0.1").contains(uri.getHost());
        if(uri.getUserInfo()!=null||uri.getHost()==null||uri.getQuery()!=null||uri.getFragment()!=null||
            !("https".equals(uri.getScheme())||debug&&local&&"http".equals(uri.getScheme())))
            throw new IllegalArgumentException("HTTPS 주소를 입력하세요. 개발 빌드는 에뮬레이터 로컬 HTTP도 지원합니다.");
    }
    public JSONObject request(String path,JSONObject body)throws Exception {
        String base=context.getSharedPreferences("coach",0).getString("endpoint","");validateEndpoint(base);
        HttpURLConnection c=(HttpURLConnection)new URL(base.replaceAll("/+$","")+path).openConnection();
        c.setConnectTimeout(10000);c.setReadTimeout(90000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Authorization","Bearer "+token());c.setRequestProperty("Content-Type","application/json");
        try {
            if(body!=null) { c.setRequestMethod("POST");c.setDoOutput(true);try(var out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));} }
            int status=c.getResponseCode();
            if(status!=200)throw new IOException(status==401?"페어링 토큰을 확인하세요":status==429?"설명 서버 사용 중 · 잠시 후 다시 시도":"설명 서버 오류 ("+status+")");
            try(var in=c.getInputStream()) {
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
                while((n=in.read(buf))!=-1){if(out.size()+n>65536)throw new IOException("Response too large");out.write(buf,0,n);}
                return new JSONObject(new String(out.toByteArray(),StandardCharsets.UTF_8));
            }
        }finally{c.disconnect();}
    }
    public static JSONObject payload(Chess before,Chess after,String played,Stockfish.Analysis analysis,Stockfish.Line playedLine,String model,List<Integer> trend)throws Exception {
        JSONObject p=new JSONObject().put("before",before.fen()).put("after",after.fen()).put("played",played).put("model",model);
        JSONArray candidates=new JSONArray();
        for(var line:analysis.lines()) {
            JSONObject l=new JSONObject().put("move",line.pv().get(0)).put("depth",line.depth()).put("cp",line.cp())
                .put("mate",line.mate()==null?JSONObject.NULL:line.mate());
            l.put("pv",new JSONArray(line.pv().subList(0,Math.min(6,line.pv().size()))));candidates.put(l);
        }
        p.put("candidates",candidates);
        if(playedLine!=null)p.put("playedScore",new JSONObject().put("cp",playedLine.cp()).put("mate",playedLine.mate()==null?JSONObject.NULL:playedLine.mate()).put("depth",playedLine.depth()));
        p.put("trend",new JSONArray(trend));return p;
    }
}
