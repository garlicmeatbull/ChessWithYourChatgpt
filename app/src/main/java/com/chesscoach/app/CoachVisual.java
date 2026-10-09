package com.chesscoach.app;

import java.util.*;
import java.util.regex.*;

/** Match human-readable SAN/UCI in existing advice to legal, saved engine variations. No inference. */
final class CoachVisual {
    record Scene(String fen,String move,String san,String caption,String path,String lastMove) {}
    private static final Pattern NOTATION=Pattern.compile("(?<![A-Za-z0-9])(?:[a-h][1-8][a-h][1-8][qrbn]?|[O0]-[O0](?:-[O0])?|[KQRBN][a-h]?[1-8]?x?[a-h][1-8]|[a-h]x?[a-h]?[1-8](?:=[QRBN])?)[+#]?(?![A-Za-z0-9])");
    static List<Scene> find(Chess before,String played,List<Stockfish.Line> candidates,Stockfish.Line actual,String advice){
        Set<String> tokens=new HashSet<>();Matcher matcher=NOTATION.matcher(advice==null?"":advice);
        while(matcher.find())tokens.add(normalize(matcher.group()));
        if(tokens.isEmpty())return Collections.emptyList();
        LinkedHashMap<String,Scene> scenes=new LinkedHashMap<>();
        if(actual!=null&&!actual.pv().isEmpty()&&actual.pv().get(0).equals(played))trace(before,actual.pv(),true,tokens,scenes);
        if(candidates!=null)for(var candidate:candidates){if(scenes.size()>=8)break;trace(before,candidate.pv(),false,tokens,scenes);}
        List<Scene> ordered=new ArrayList<>(scenes.values());ordered.sort(Comparator.comparingInt(scene->scene.caption().equals("실전 이동")?1:0));return Collections.unmodifiableList(ordered);
    }
    private static void trace(Chess initial,List<String> pv,boolean actual,Set<String> tokens,Map<String,Scene> scenes){
        Chess position=initial.copy();StringBuilder path=new StringBuilder();String previous=null;
        for(int i=0;i<Math.min(12,pv.size());i++){
            Chess.Move move;try{move=Chess.Move.parse(pv.get(i));}catch(IllegalArgumentException e){break;}
            if(!position.legalMoves().contains(move))break;
            String san=position.san(move);if(path.length()>0)path.append(' ');
            if(position.white||i==0)path.append(position.fullmove).append(position.white?". ":"… ");
            path.append(san);
            if(tokens.contains(normalize(san))||tokens.contains(move.uci())){
                String caption=actual?(i==0?"실전 이동":i==1?"실전 수 이후 · 상대의 대응":"실전 수 이후 · 가능한 진행"):"대안 · Stockfish 후보의 가능한 진행";
                scenes.putIfAbsent(position.key()+":"+move.uci(),new Scene(position.fen(),move.uci(),san,caption,path.toString(),previous));
            }
            position.play(move);previous=move.uci();
            if(scenes.size()>=8)return;
        }
    }
    static Scene onBoard(List<Scene> scenes,Chess board){for(var scene:scenes)if(new Chess(scene.fen()).key().equals(board.key()))return scene;return null;}
    private static String normalize(String s){return s.replace('0','O').replace("+","").replace("#","");}
}
