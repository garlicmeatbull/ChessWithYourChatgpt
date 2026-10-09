package com.chesscoach.app;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
public class CoachPreferencesTest {
    @Test public void switchingEitherPresetChangesCacheAndKeepsCustomInstruction()throws Exception{
        var normal=new CoachPreferences.Options(false,false,"오프닝을 집중해서 설명해 주세요.");var concise=new CoachPreferences.Options(true,false,normal.extra());var beginner=new CoachPreferences.Options(false,true,normal.extra());
        assertNotEquals(normal.fingerprint(),concise.fingerprint());assertNotEquals(normal.fingerprint(),beginner.fingerprint());assertTrue(concise.instruction().contains("300~600"));assertTrue(beginner.instruction().contains("전문용어와 약어 없이"));assertTrue(beginner.instruction().contains(normal.extra()));assertTrue(beginner.instruction().contains("JSON 네 필드"));assertTrue(normal.instruction().contains(".../…"));
        assertEquals(beginner.fingerprint(),CoachPreferences.from(beginner.json()).fingerprint());
    }
    @Test public void customPromptIsBoundedAndCanonical(){var a=new CoachPreferences.Options(false,false,"  계획을 알려줘  ");assertEquals("계획을 알려줘",a.extra());assertEquals(a.fingerprint(),new CoachPreferences.Options(false,false,"계획을 알려줘").fingerprint());assertEquals(2000,new CoachPreferences.Options(false,false,"가".repeat(2100)).extra().length());assertNotEquals(a.fingerprint(),new CoachPreferences.Options(false,false,"다른 요청").fingerprint());}
    @Test public void oldLessonsStayReadableButOnlyMatchingPreferencesReuseCache()throws Exception{var options=new CoachPreferences.Options(false,true,"질문을 해주세요");var response=new JSONObject().put("visualVersion",1).put("explanation",new JSONObject().put("strategy","전략").put("continuation","진행").put("principle","원칙"));var cache=new JSONObject().put("response",response);assertTrue(CoachText.lesson(cache));assertFalse(CoachPreferences.matches(cache,options));response.put("promptFingerprint",options.fingerprint());assertTrue(CoachPreferences.matches(cache,options));assertFalse(CoachPreferences.matches(cache,new CoachPreferences.Options(true,true,options.extra())));}
    @Test public void cachedLegalMovesKeepCheckmateDrawAndLivePositionCorrect(){for(String pgn:new String[]{"1. e4 e5 *","1. f3 e5 2. g4 Qh4# 0-1","[FEN \"8/8/8/8/8/5k2/8/7K w - - 0 1\"] *"}){var g=Pgn.parse(pgn);var board=new Chess(g.plies().isEmpty()?g.initial():g.plies().get(g.plies().size()-1).after());var legal=board.legalMoves();assertEquals(board.terminal(1),board.terminal(1,legal));assertEquals(board.terminal(3),board.terminal(3,legal));}}
}
