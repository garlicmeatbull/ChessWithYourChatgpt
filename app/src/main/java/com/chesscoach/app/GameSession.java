package com.chesscoach.app;
import android.content.Context;
import java.util.*;
/** Durable active pointer; the SQLite PGN is the authoritative position. */
final class GameSession {
    static long active(Context c){return c.getSharedPreferences("coach",0).getLong("activeRecord",0);}
    static boolean available(Context c){var saved=Records.get(c).find(active(c));return saved!=null&&saved.origin().equals("played")&&GameOutcome.savedResult(saved.pgn()).equals("*");}
    static Pgn.Game prefix(Pgn.Game source,int count){int n=Math.max(0,Math.min(count,source.plies().size()));List<Pgn.Ply> plies=new ArrayList<>(source.plies().subList(0,n));Chess position=new Chess(n==0?source.initial():plies.get(n-1).after());Map<String,Integer> repetitions=new HashMap<>();repetitions.put(new Chess(source.initial()).key(),1);for(var ply:plies)repetitions.merge(new Chess(ply.after()).key(),1,Integer::sum);if(position.terminal(repetitions.getOrDefault(position.key(),1))!=null)throw new IllegalArgumentException("끝난 국면에서는 이어 둘 수 없어요. 이전 수를 선택하세요.");return new Pgn.Game(source.initial(),Collections.unmodifiableList(plies),Map.of("CoachPlayer",position.white?"white":"black","CoachBranchStart",Integer.toString(n)),"*");}
    static boolean playerWhite(Pgn.Game g){return !"black".equals(g.tags().get("CoachPlayer"));}
}
