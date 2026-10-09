package com.chesscoach.app;
import java.util.*;
import java.util.regex.Pattern;
import org.json.*;

/** Hidden annotations reference engine-validated positions, never model-provided FEN. */
final class CoachDiagrams {
    record Part(String text,String diagramId) {}
    private static final Pattern ID=Pattern.compile("[acn][0-9]{1,2}");
    static List<Part> parts(String raw){
        List<Part> out=new ArrayList<>();StringBuilder text=new StringBuilder();
        for(int i=0;i<raw.length();){
            char c=raw.charAt(i++);if(c=='}')continue;if(c!='{'){text.append(c);continue;}
            int start=i,depth=1;while(i<raw.length()&&depth>0){char next=raw.charAt(i++);if(next=='{')depth++;else if(next=='}')depth--;}
            if(depth>0)break;String hidden=raw.substring(start,i-1);String id=hidden.startsWith("viz:")?hidden.substring(4).trim():"";
            if(ID.matcher(id).matches()){out.add(new Part(text.toString(),id));text.setLength(0);}
        }
        if(text.length()>0||out.isEmpty())out.add(new Part(text.toString(),""));return out;
    }
    static String visible(String raw){StringBuilder out=new StringBuilder();for(Part p:parts(raw))out.append(p.text());return out.toString();}
    static Map<String,CoachVisual.Scene> catalog(Chess before,JSONObject details){
        Map<String,CoachVisual.Scene> out=new LinkedHashMap<>();if(details==null)return out;
        try{JSONObject played=details.optJSONObject("playedScore");if(played!=null)trace(before,AnalysisJson.line(played).pv(),"a",0,"실전 수 이후 · 가능한 진행",out);
            JSONObject analysis=details.optJSONObject("analysis");if(analysis!=null){int branch=0;for(var line:AnalysisJson.analysis(analysis).lines()){if(branch==3)break;trace(before,line.pv(),"c",branch++*12,"대안 · 후보의 가능한 진행",out);}}
            JSONObject after=details.optJSONObject("afterAnalysis");if(after!=null&&played!=null){List<String> actual=AnalysisJson.line(played).pv();if(!actual.isEmpty()){Chess position=before.copy();Chess.Move move=Chess.Move.parse(actual.get(0));if(position.legalMoves().contains(move)){position.play(move);var top=AnalysisJson.analysis(after).top();if(top!=null)trace(position,top.pv(),"n",0,"현재 위치 이후 · 가능한 진행",out);}}}
        }catch(Exception invalid){/* Retain only the individually validated prefixes. */}return Collections.unmodifiableMap(out);
    }
    private static void trace(Chess root,List<String> pv,String prefix,int offset,String caption,Map<String,CoachVisual.Scene> out){
        Chess board=root.copy();StringBuilder path=new StringBuilder();String previous=null;
        for(int i=0;i<Math.min(12,pv.size());i++){Chess.Move move;try{move=Chess.Move.parse(pv.get(i));}catch(Exception e){break;}if(!board.legalMoves().contains(move))break;String san=board.san(move);if(path.length()>0)path.append(' ');if(board.white||i==0)path.append(board.fullmove).append(board.white?". ":"... ");path.append(san);out.put(prefix+(offset+i),new CoachVisual.Scene(board.fen(),move.uci(),san,caption,path.toString(),previous));board.play(move);previous=move.uci();}
    }
    static List<CoachVisual.Scene> selected(Map<String,CoachVisual.Scene> catalog,String raw){List<CoachVisual.Scene> out=new ArrayList<>();Set<String> seen=new HashSet<>();for(Part part:parts(raw)){var scene=catalog.get(part.diagramId());if(scene!=null&&seen.add(scene.fen()+scene.move()))out.add(scene);if(out.size()==6)break;}return out;}
    static JSONArray evidence(Map<String,CoachVisual.Scene> catalog)throws JSONException{JSONArray out=new JSONArray();for(var entry:catalog.entrySet()){var scene=entry.getValue();out.put(new JSONObject().put("id",entry.getKey()).put("moveSAN",scene.san()).put("pathSAN",scene.path()).put("kind",scene.caption()));}return out;}
}
