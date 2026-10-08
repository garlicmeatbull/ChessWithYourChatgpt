package com.chesscoach.app;
import org.json.*;
/** Converts legal engine notation locally, retaining every evaluation and position. */
final class CoachEvidence {
    static JSONObject compact(JSONObject source)throws JSONException{
        JSONObject original=new JSONObject(source.toString()),out=new JSONObject(source.toString());
        try{
            Chess root=new Chess(out.getString("before"));out.put("played",san(root,out.getString("played")));
            JSONArray candidates=out.getJSONArray("candidates");
            for(int i=0;i<candidates.length();i++){
                JSONObject line=candidates.getJSONObject(i);line.put("move",san(root,line.getString("move")));Chess position=root.copy();JSONArray pv=line.getJSONArray("pv"),readable=new JSONArray();
                for(int j=0;j<pv.length();j++){String uci=pv.getString(j);readable.put(san(position,uci));position.play(Chess.Move.parse(uci));}line.put("pv",readable);
            }
            out.put("notation","SAN");return out.toString().length()<original.toString().length()?out:original;
        }catch(Exception unsupported){return original;}
    }
    private static String san(Chess position,String uci){Chess.Move move=Chess.Move.parse(uci);if(!position.legalMoves().contains(move))throw new IllegalArgumentException("Illegal engine notation");return position.san(move);}
}
