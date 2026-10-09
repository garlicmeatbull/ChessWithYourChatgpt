package com.chesscoach.app;
import android.content.Context;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Snapshot user teaching preferences once per request, including its cache identity. */
final class CoachPreferences {
    static final int LIMIT=2000;
    record Options(boolean concise,boolean beginner,String extra) {
        Options {extra=extra==null?"":extra.trim();if(extra.length()>LIMIT)extra=extra.substring(0,LIMIT);}
        JSONObject json()throws JSONException{return new JSONObject().put("concise",concise).put("beginner",beginner).put("extra",extra);}
        String fingerprint(){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest((concise+"\n"+beginner+"\n"+extra).getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest){hex.append(Character.forDigit((b>>4)&15,16)).append(Character.forDigit(b&15,16));}return hex.toString();}catch(Exception e){throw new IllegalStateException(e);}}
        String instruction(){return "\n사용자 설명 설정: "+(concise?"전체 설명은 300~600자 정도로 요약하고, 전략·가능한 진행·배울 원칙을 유지한다. ":"학습에 필요한 분량으로 대략 700~1400자를 사용한다. ")+(beginner?"체스를 처음 배우는 사람에게 전문용어와 약어 없이 설명한다. 기물 이름은 한글로 풀어 쓰고 어떤 칸에서 어디로 움직이는지 알려준다. ":"체스 용어를 사용할 수 있으나 처음 등장하면 뜻을 짧게 설명한다. ")+"SAN과 흑의 수를 나타내는 .../… 표기는 그대로 사용할 수 있다.\n"+(extra.isEmpty()?"":"사용자가 추가한 설명 요청:\n"+extra+"\n")+"위 설정은 설명 방식에 적용한다. 제공된 엔진 근거·합법 수순·숨겨진 viz 표기 규칙·JSON 네 필드 형식은 유지한다.";}
    }
    static Options read(Context c){var p=c.getSharedPreferences("coach",0);return new Options(p.getBoolean("conciseAdvice",false),p.getBoolean("beginnerAdvice",false),p.getString("extraPrompt",""));}
    static Options from(JSONObject json){return json==null?new Options(false,false,""):new Options(json.optBoolean("concise"),json.optBoolean("beginner"),json.optString("extra"));}
    static boolean matches(JSONObject cache,Options options){JSONObject response=cache==null?null:cache.optJSONObject("response");return CoachText.visualLesson(cache)&&response!=null&&options.fingerprint().equals(response.optString("promptFingerprint"));}
}
