package com.chesscoach.app;
import java.util.*;
import org.json.*;

/** Position-bounded history and factual opening observations; no future moves or inference. */
final class CoachContext {
    static JSONObject attach(JSONObject source,Pgn.Game game,int position,boolean playerWhite,JSONObject analyses)throws Exception{
        if(position<1||position>game.plies().size()||!game.plies().get(position-1).after().equals(source.getString("after")))throw new IllegalArgumentException("코칭할 위치와 기록이 달라졌습니다.");
        JSONObject out=new JSONObject(source.toString());Chess board=new Chess(source.getString("after"));StringBuilder history=new StringBuilder();List<String> sans=new ArrayList<>();
        for(int i=0;i<position;i++){var p=game.plies().get(i);if(p.white()||i==0)history.append(p.number()).append(p.white()?". ":"... ");history.append(p.san()).append(' ');sans.add(p.san());}
        int pieces=0,queens=0;for(char p:board.squares){if(p!='.'&&Character.toLowerCase(p)!='p'&&Character.toLowerCase(p)!='k')pieces++;if(Character.toLowerCase(p)=='q')queens++;}
        boolean opening=game.initial().equals(Chess.START)&&position<=30;String phase=queens==0&&pieces<=4?"endgame":opening?"opening":"middlegame";
        JSONObject context=new JSONObject().put("initial",game.initial()).put("historySAN",history.toString().trim()).put("pliesSeen",position).put("phase",phase).put("playerSide",playerWhite?"white":"black").put("sideToMove",board.white?"white":"black").put("openingName",opening?openingName(sans):"");
        JSONArray home=new JSONArray(),central=new JSONArray();Chess start=new Chess();for(String square:new String[]{"b1","c1","f1","g1","b8","c8","f8","g8"}){int at=Chess.index(square);if(board.squares[at]==start.squares[at])home.put(square);}
        for(String square:new String[]{"d4","e4","d5","e5"}){char p=board.squares[Chess.index(square)];if(Character.toLowerCase(p)=='p')central.put((Character.isUpperCase(p)?"white ":"black ")+square);}
        context.put("observations",new JSONObject().put("minorPiecesOnHomeSquares",home).put("centralPawns",central).put("whiteKing",king(board,'K')).put("blackKing",king(board,'k')));
        JSONArray recent=new JSONArray();for(int i=Math.max(0,position-8);i<position;i++){JSONObject e=analyses==null?null:analyses.optJSONObject(Integer.toString(i));if(e==null)continue;JSONObject row=new JSONObject().put("ply",i+1).put("san",game.plies().get(i).san()).put("assessment",e.optString("local"));JSONObject d=e.optJSONObject("details"),played=d==null?null:d.optJSONObject("playedScore");if(played!=null)row.put("cp",played.optInt("cp")).put("mate",played.opt("mate"));recent.put(row);}context.put("recentAssessments",recent);out.put("gameContext",context);
        JSONObject selected=analyses==null?null:analyses.optJSONObject(Integer.toString(position-1)),details=selected==null?null:selected.optJSONObject("details"),after=details==null?null:details.optJSONObject("afterAnalysis");if(details!=null){JSONObject played=details.optJSONObject("playedScore");if(played!=null){Stockfish.Line actual=AnalysisJson.line(played);JSONObject score=out.optJSONObject("playedScore");if(score==null)score=new JSONObject().put("cp",actual.cp()).put("mate",actual.mate()==null?JSONObject.NULL:actual.mate()).put("depth",actual.depth());score.put("continuationSAN",sanLine(new Chess(game.plies().get(position-1).before()),actual.pv(),12));out.put("playedScore",score);}JSONObject root=details.optJSONObject("analysis");if(root!=null){JSONArray candidates=new JSONArray();for(var line:AnalysisJson.analysis(root).lines()){if(line.pv().isEmpty())continue;candidates.put(new JSONObject().put("move",line.pv().get(0)).put("depth",line.depth()).put("cp",line.cp()).put("mate",line.mate()==null?JSONObject.NULL:line.mate()).put("pv",new JSONArray(line.pv().subList(0,Math.min(12,line.pv().size())))));}out.put("candidates",candidates);}}
        if(after!=null){JSONArray lines=after.optJSONArray("lines"),current=new JSONArray();if(lines!=null)for(int i=0;i<lines.length();i++){Stockfish.Line line=AnalysisJson.line(lines.getJSONObject(i));current.put(new JSONObject().put("cp",line.cp()).put("mate",line.mate()==null?JSONObject.NULL:line.mate()).put("depth",line.depth()).put("pvSAN",sanLine(board,line.pv(),12)));}out.put("currentPositionCandidates",current);}
        out.put("visualCatalog",CoachDiagrams.evidence(CoachDiagrams.catalog(new Chess(game.plies().get(position-1).before()),details)));
        return out;
    }
    static JSONArray sanLine(Chess initial,List<String> moves,int limit){JSONArray out=new JSONArray();Chess board=initial.copy();for(String uci:moves.subList(0,Math.min(limit,moves.size()))){try{Chess.Move move=Chess.Move.parse(uci);if(!board.legalMoves().contains(move))break;out.put(board.san(move));board.play(move);}catch(Exception invalid){break;}}return out;}
    private static String king(Chess board,char king){for(int i=0;i<64;i++)if(board.squares[i]==king)return Chess.name(i);return "";}
    private static String openingName(List<String> moves){
        String history=String.join(" ",moves)+" ";String[][] patterns={{"e4 e5 Nf3 Nc6 Bb5 ","루이 로페즈"},{"e4 e5 Nf3 Nc6 Bc4 ","이탈리안 게임"},{"d4 d5 c4 ","퀸즈 갬빗"},{"e4 c5 ","시실리안 디펜스"},{"e4 e6 ","프렌치 디펜스"},{"e4 c6 ","카로칸 디펜스"},{"e4 e5 ","오픈 게임"},{"d4 Nf6 ","인디언 디펜스 계열"}};for(String[] pattern:patterns)if(history.startsWith(pattern[0]))return pattern[1];return "이 기보만으로 특정 오프닝 이름을 확정하지 않음";
    }
}
