package com.chesscoach.app;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CoachVisualTest {
    private Stockfish.Line line(String... moves){return new Stockfish.Line(1,20,0,null,List.of(moves));}
    @Test public void explainsBlunderAtTheCorrectFuturePosition(){
        Chess before=new Chess();before.play(Chess.Move.parse("f2f3"));before.play(Chess.Move.parse("e7e5"));
        var actual=line("g2g4","d8h4");var scenes=CoachVisual.find(before,"g2g4",List.of(line("e2e4")),actual,"**킹이 노출돼요.** 상대의 `Qh4#`로 체크메이트가 됩니다.");
        assertEquals(1,scenes.size());var scene=scenes.get(0);assertEquals("d8h4",scene.move());assertEquals("Qh4#",scene.san());assertEquals("실전 수 이후 · 상대의 대응",scene.caption());
        assertNull(CoachVisual.onBoard(scenes,before));before.play(Chess.Move.parse("g2g4"));assertNotNull(CoachVisual.onBoard(scenes,before));assertTrue(scene.path().endsWith("g4 Qh4#"));assertEquals("g2g4",scene.lastMove());
    }
    @Test public void unsupportedOrIllegalAiMovesNeverBecomeArrows(){
        assertTrue(CoachVisual.find(new Chess(),"e2e4",List.of(line("e2e4","e7e5")),null,"Qh4# 혹은 e2e5, Nf3를 고려하세요.").isEmpty());
    }
    @Test public void validatesEveryMoveAndStopsAtBrokenEngineVariation(){
        assertTrue(CoachVisual.find(new Chess(),"e2e4",List.of(line("e2e4","e7e4","g1f3")),null,"Nf3").isEmpty());
    }
    @Test public void supportsReadableSanAndUciWithoutChangingTheSourceBoard(){
        Chess board=new Chess();String fen=board.fen();var scenes=CoachVisual.find(board,"d2d4",List.of(line("g1f3","d7d5")),null,"나이트를 `Nf3`로 전개하면 d7d5에 대응해요.");
        assertEquals(2,scenes.size());assertEquals("g1f3",scenes.get(0).move());assertEquals("d7d5",scenes.get(1).move());assertEquals(fen,board.fen());
    }
    @Test public void partialNotationAndOrdinaryWordsDoNotCreateMoves(){assertTrue(CoachVisual.find(new Chess(),"e2e4",List.of(line("g1f3")),null,"다음 계획 flow: Nf").isEmpty());}
    @Test public void removesDuplicateScenesAndPrefersActualConsequence(){var actual=line("e2e4","e7e5");var scenes=CoachVisual.find(new Chess(),"e2e4",List.of(actual,actual),actual,"1. e4 e5");assertEquals(2,scenes.size());assertEquals("실전 수 이후 · 상대의 대응",scenes.get(0).caption());}
    @Test public void castlingAndPromotionRemainReadableAndLegal(){
        Chess castle=new Chess("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");assertEquals("e1g1",CoachVisual.find(castle,"e1g1",List.of(line("e1g1")),null,"`0-0`으로 안전하게 캐슬링하세요.").get(0).move());
        Chess promotion=new Chess("7k/P7/8/8/8/8/8/7K w - - 0 1");assertEquals("a7a8q",CoachVisual.find(promotion,"a7a8q",List.of(line("a7a8q")),null,"a8=Q+로 승격해요.").get(0).move());
    }
    @Test public void aMoveMentionedInOneBranchNeverOverlaysAnotherPosition(){var scenes=CoachVisual.find(new Chess(),"d2d4",List.of(line("e2e4","e7e5")),null,"e5");Chess actual=new Chess();actual.play(Chess.Move.parse("d2d4"));assertNull(CoachVisual.onBoard(scenes,actual));}
}
