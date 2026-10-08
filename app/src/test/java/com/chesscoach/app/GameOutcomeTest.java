package com.chesscoach.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class GameOutcomeTest {
    @Test public void liveResultsUseTheHumanPlayersPerspective(){assertEquals("승리",GameOutcome.live("백 승리 · 체크메이트").title());assertEquals(GameOutcome.Kind.LOSS,GameOutcome.live("흑 승리 · 체크메이트").kind());assertEquals("패배",GameOutcome.live("흑 승리 · 체크메이트").title());assertNull(GameOutcome.live(null));}
    @Test public void importedResultsRemainNeutral(){assertEquals("흑 승리",GameOutcome.from("0-1",false,null).title());assertEquals(GameOutcome.Kind.WIN,GameOutcome.from("0-1",false,null).kind());assertEquals("백 승리",GameOutcome.from("1-0",false,null).title());}
    @Test public void ongoingGamesNeverLookFinished(){assertNull(GameOutcome.from("*",true,"백 승리 · 체크메이트"));assertNull(GameOutcome.from("invalid",false,null));}
    @Test public void drawReasonIsPreserved(){var outcome=GameOutcome.live("무승부 · 3회 반복 (자동 청구)");assertEquals("무승부",outcome.title());assertEquals(GameOutcome.Kind.DRAW,outcome.kind());assertTrue(outcome.detail().contains("3회 반복"));}
    @Test public void contradictoryImportedResultDoesNotInventCheckmateReason(){assertFalse(GameOutcome.from("1-0",false,"흑 승리 · 체크메이트").detail().contains("체크메이트"));}
    @Test public void readsSavedResultWithoutReplayingAnEntireRecord(){String pgn="[White \"연습\"]\n[Result \"0-1\"]\n\n1. e4 e5 0-1";assertEquals("0-1",GameOutcome.savedResult(pgn));assertEquals("*",GameOutcome.savedResult("[Result \"*\"]\n"));assertEquals("*",GameOutcome.savedResult("없는 결과"));}
}
