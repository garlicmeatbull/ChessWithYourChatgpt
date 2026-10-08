package com.chesscoach.app;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class CoachEvidenceTest {
    private JSONObject source()throws Exception{
        return new JSONObject().put("before",Chess.START).put("after",new Chess().fen()).put("played","e2e4").put("moveAssessment",new JSONObject().put("kind","BEST"))
            .put("playedScore",new JSONObject().put("cp",42).put("mate",JSONObject.NULL).put("depth",21)).put("candidates",new JSONArray()
            .put(line("e2e4",new String[]{"e2e4","e7e5","g1f3","b8c6","f1b5","a7a6"}))
            .put(line("d2d4",new String[]{"d2d4","d7d5","c2c4","e7e6","b1c3","g8f6"}))
            .put(line("g1f3",new String[]{"g1f3","g8f6","d2d4","d7d5","c2c4","e7e6"})));
    }
    private JSONObject line(String move,String[] pv)throws Exception{return new JSONObject().put("move",move).put("pv",new JSONArray(pv)).put("cp",31).put("mate",JSONObject.NULL).put("depth",20);}
    @Test public void legalLinesBecomeHumanNotationWithoutLosingScores()throws Exception{JSONObject old=source(),result=CoachEvidence.compact(old);assertEquals("SAN",result.getString("notation"));assertEquals("e4",result.getString("played"));assertEquals("Nf3",result.getJSONArray("candidates").getJSONObject(0).getJSONArray("pv").getString(2));assertEquals(old.getJSONObject("playedScore").toString(),result.getJSONObject("playedScore").toString());assertEquals(old.getString("before"),result.getString("before"));assertEquals(old.getString("after"),result.getString("after"));assertEquals(20,result.getJSONArray("candidates").getJSONObject(0).getInt("depth"));}
    @Test public void sourceAndStoredEngineNotationStayUnchanged()throws Exception{JSONObject old=source();String saved=old.toString();CoachEvidence.compact(old);assertEquals(saved,old.toString());}
    @Test public void compactEvidenceNeverHasMoreCharacters()throws Exception{JSONObject old=source();assertTrue(CoachEvidence.compact(old).toString().length()<old.toString().length());JSONObject tiny=new JSONObject().put("before",Chess.START).put("played","e2e4").put("candidates",new JSONArray());assertEquals(tiny.toString(),CoachEvidence.compact(tiny).toString());}
    @Test public void invalidVariationRetainsOriginalEvidence()throws Exception{JSONObject old=source();old.getJSONArray("candidates").getJSONObject(0).getJSONArray("pv").put(1,"e7e4");assertEquals(old.toString(),CoachEvidence.compact(old).toString());}
    @Test public void unrelatedPayloadIsNotGuessed()throws Exception{JSONObject old=new JSONObject().put("learningFocus","기물 안전");assertEquals(old.toString(),CoachEvidence.compact(old).toString());}
}
