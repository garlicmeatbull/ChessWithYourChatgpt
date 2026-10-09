package com.chesscoach.app;

import org.json.*;

/** Presentation of both saved legacy explanations and incomplete streamed JSON strings. */
public final class CoachText {
    private static final String[] KEYS={"strategy","continuation","principle","opening","headline","summary","flow","bestMoveReason","plan"};
    private static final String[] TITLES={"지금까지의 계획","앞으로의 진행","다른 대국에 적용하기","오프닝 연습","핵심","요약","이번 수의 판단","추천수의 근거","다음 계획"};
    public static String partial(String json){
        StringBuilder out=new StringBuilder();
        for(int i=0;i<KEYS.length;i++){
            String value=field(json,KEYS[i]);if(value.isEmpty())continue;
            if(out.length()>0)out.append("\n\n");
            if(!KEYS[i].equals("summary"))out.append("## ").append(TITLES[i]).append("\n");
            out.append(value);
        }
        return out.toString();
    }
    private static String field(String json,String key){
        // Walk complete JSON key tokens; never match a key inside another field's text.
        for(int p=0;p<json.length();p++){
            if(json.charAt(p)!='"')continue;int end=endQuote(json,p+1);if(end<0)return "";
            String token=json.substring(p,end+1);int next=end+1;
            while(next<json.length()&&Character.isWhitespace(json.charAt(next)))next++;
            if(next<json.length()&&json.charAt(next)==':'&&token.equals("\""+key+"\"")){
                next++;while(next<json.length()&&Character.isWhitespace(json.charAt(next)))next++;
                if(next>=json.length()||json.charAt(next)!='"')return "";
                StringBuilder value=new StringBuilder();
                for(int j=next+1;j<json.length();j++){
                    char c=json.charAt(j);if(c=='"')break;
                    if(c=='\\'){
                        if(++j>=json.length())break;char escaped=json.charAt(j);
                        if(escaped=='u'){
                            if(j+4>=json.length())break;
                            try{value.append((char)Integer.parseInt(json.substring(j+1,j+5),16));}catch(NumberFormatException e){break;}j+=4;
                        }else switch(escaped){case 'n'->value.append('\n');case 'r'->value.append('\r');case 't'->value.append('\t');case 'b'->value.append('\b');case 'f'->value.append('\f');case '"','\\','/'->value.append(escaped);default->{return value.toString();}}
                    }else value.append(c);
                }
                if(value.length()>0&&Character.isHighSurrogate(value.charAt(value.length()-1)))value.setLength(value.length()-1);
                return value.toString();
            }
            p=end;
        }
        return "";
    }
    private static int endQuote(String s,int p){for(;p<s.length();p++){if(s.charAt(p)=='\\')p++;else if(s.charAt(p)=='"')return p;}return -1;}
    public static JSONObject explanation(JSONObject cache){JSONObject response=cache==null?null:cache.optJSONObject("response");return response==null?null:response.optJSONObject("explanation");}
    public static String headline(JSONObject cache){JSONObject e=explanation(cache);return e==null?"":e.optString("headline").replace('\n',' ').trim();}
    public static String previewHeadline(String preview){String prefix="## 핵심\n";if(!preview.startsWith(prefix))return "";int end=preview.indexOf("\n\n",prefix.length());return preview.substring(prefix.length(),end<0?preview.length():end).replace('\n',' ').trim();}
    public static String previewSummary(String preview){String prefix="## 핵심\n";if(!preview.startsWith(prefix))return preview;int end=preview.indexOf("\n\n",prefix.length());return end<0?"":preview.substring(end+2);}
    public static JSONObject merge(JSONObject previous,JSONObject incoming)throws JSONException{
        JSONObject result=new JSONObject(incoming.toString()),fresh=incoming.optJSONObject("explanation");
        if(fresh==null)return result;
        if(fresh.has("strategy")){result.put("coachingVersion",2);return result;}
        JSONObject combined=new JSONObject();JSONObject prior=previous==null?null:previous.optJSONObject("explanation");
        if(prior!=null)for(String key:KEYS)if(prior.has(key))combined.put(key,prior.get(key));
        for(String key:KEYS)if(fresh.has(key))combined.put(key,fresh.get(key));
        result.put("explanation",combined);
        for(String key:new String[]{"summaryUsage","detailUsage"})if(previous!=null&&previous.has(key))result.put(key,previous.get(key));
        result.put(fresh.has("summary")?"summaryUsage":"detailUsage",incoming.opt("usage")==null?JSONObject.NULL:incoming.opt("usage"));
        return result;
    }
    public static boolean lesson(JSONObject cache){JSONObject e=explanation(cache);return e!=null&&e.has("strategy")&&e.has("continuation")&&e.has("principle");}
    public static boolean detailed(JSONObject cache){JSONObject e=explanation(cache);return lesson(cache)||e!=null&&e.has("flow")&&e.has("bestMoveReason")&&e.has("plan")||cache!=null&&e==null&&!cache.optString("text").isEmpty();}
    public static String summary(JSONObject cache){JSONObject e=explanation(cache);if(e!=null)return e.has("summary")?e.optString("summary"):shorten(e.optString("flow"));return shorten(legacy(cache==null?"":cache.optString("text")));}
    private static String shorten(String value){int end=value.indexOf('\n');if(end>=0)value=value.substring(0,end);if(value.length()<=160)return value;int n=value.offsetByCodePoints(0,Math.min(150,value.codePointCount(0,value.length())));return value.substring(0,n)+"…";}
    public static String markdown(JSONObject cache){JSONObject e=explanation(cache);if(e==null)return legacy(cache==null?"":cache.optString("text"));StringBuilder out=new StringBuilder();for(int i=0;i<KEYS.length;i++){String value=e.optString(KEYS[i]);if(value.isEmpty())continue;if(out.length()>0)out.append("\n\n");out.append("## ").append(TITLES[i]).append('\n').append(value);}return out.toString();}
    private static String legacy(String text){if(text.startsWith("CHATGPT /")||text.startsWith("CODEX /")){int line=text.indexOf('\n');text=line<0?"":text.substring(line+1);}return text.replace("\n\n추천수의 근거\n","\n\n## 추천수의 근거\n").replace("\n\n다음 계획\n","\n\n## 다음 계획\n");}
}
