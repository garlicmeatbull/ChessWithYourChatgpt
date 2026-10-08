package com.chesscoach.app;
import org.json.*;
import java.util.*;
public final class AnalysisJson {
    public static JSONObject line(Stockfish.Line l)throws JSONException{return new JSONObject().put("rank",l.rank()).put("depth",l.depth()).put("cp",l.cp()).put("mate",l.mate()==null?JSONObject.NULL:l.mate()).put("pv",new JSONArray(l.pv()));}
    public static Stockfish.Line line(JSONObject p)throws JSONException {List<String> pv=new ArrayList<>();JSONArray a=p.getJSONArray("pv");for(int i=0;i<a.length();i++)pv.add(a.getString(i));return new Stockfish.Line(p.getInt("rank"),p.getInt("depth"),p.getInt("cp"),p.isNull("mate")?null:p.getInt("mate"),pv);}
    public static JSONObject analysis(Stockfish.Analysis a)throws JSONException{JSONArray lines=new JSONArray();for(var l:a.lines())lines.put(line(l));return new JSONObject().put("best",a.best()).put("lines",lines);}
    public static Stockfish.Analysis analysis(JSONObject p)throws JSONException{List<Stockfish.Line> lines=new ArrayList<>();JSONArray a=p.getJSONArray("lines");for(int i=0;i<a.length();i++)lines.add(line(a.getJSONObject(i)));return new Stockfish.Analysis(p.getString("best"),lines);}
    public static JSONObject details(Stockfish.Analysis a,Stockfish.Line actual,Stockfish.Analysis after)throws JSONException {return new JSONObject().put("analysis",analysis(a)).put("playedScore",actual==null?JSONObject.NULL:line(actual)).put("afterAnalysis",after==null?JSONObject.NULL:analysis(after));}
    public static String explanation(JSONObject response)throws JSONException{JSONObject e=response.getJSONObject("explanation");return "CHATGPT / "+response.getString("model")+(response.optBoolean("cached")?" · 캐시":"")+"\n"+e.getString("flow")+"\n\n추천수의 근거\n"+e.getString("bestMoveReason")+"\n\n다음 계획\n"+e.getString("plan");}
}
