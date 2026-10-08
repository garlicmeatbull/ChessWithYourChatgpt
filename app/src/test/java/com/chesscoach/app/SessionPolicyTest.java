package com.chesscoach.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class SessionPolicyTest {
    @Test public void branchKeepsPrefixAndOriginalUnchanged(){var source=Pgn.parse("1. e4 e5 2. Nf3 Nc6 1-0");var branch=GameSession.prefix(source,3);assertEquals(3,branch.plies().size());assertEquals("*",branch.result());assertEquals(4,source.plies().size());assertEquals("1-0",source.result());assertFalse(GameSession.playerWhite(branch));assertEquals(source.plies().get(2).after(),branch.plies().get(2).after());}
    @Test public void branchSurvivesPgnRoundTripIncludingHumanColorAndUndoBoundary(){var branch=GameSession.prefix(Pgn.parse("1. e4 e5 *"),1);var reopened=Pgn.parse(branch.export());assertFalse(GameSession.playerWhite(reopened));assertEquals("1",reopened.tags().get("CoachBranchStart"));assertEquals(branch.plies().get(0).after(),reopened.plies().get(0).after());}
    @Test public void branchCanStartBeforeAnyMoveWithCustomFen(){var source=Pgn.parse("[FEN \"8/5k2/8/8/8/8/4p3/6K1 b - - 0 1\"]\n1... e1=Q+ *");var branch=Pgn.parse(GameSession.prefix(source,0).export());assertEquals(source.initial(),branch.initial());assertTrue(branch.plies().isEmpty());assertFalse(GameSession.playerWhite(branch));}
    @Test(expected=IllegalArgumentException.class) public void checkmateCannotBeContinued(){GameSession.prefix(Pgn.parse("1. f3 e5 2. g4 Qh4# 0-1"),4);}
    @Test(expected=IllegalArgumentException.class) public void repetitionEndingCannotBeContinued(){GameSession.prefix(Pgn.parse("1. Nf3 Nf6 2. Ng1 Ng8 3. Nf3 Nf6 4. Ng1 Ng8 *"),8);}
    @Test public void blackPlayerWinAndResignationHaveCorrectResults(){assertEquals("승리",GameOutcome.forPlayer("0-1",false,"흑 승리 · 체크메이트").title());assertEquals(GameOutcome.Kind.LOSS,GameOutcome.forPlayer("1-0",false,"백 승리 · 기권").kind());assertEquals(GameOutcome.Kind.LOSS,GameOutcome.forPlayer("0-1",true,"흑 승리 · 기권").kind());assertNull(GameOutcome.forPlayer("*",false,null));}
    @Test public void recommendationOnlyUsesAvailableModels(){assertEquals("gpt-5.6-terra",ModelPolicy.recommend(Arrays.asList("gpt-5.6-luna","gpt-5-mini","gpt-5.6-terra")));assertEquals("gpt-5-mini",ModelPolicy.recommend(Arrays.asList("gpt-5.3-codex","gpt-5-mini")));assertEquals("",ModelPolicy.recommend(Collections.emptyList()));assertEquals("gpt-5.3-codex",ModelPolicy.recommend(Collections.singletonList("gpt-5.3-codex")));}
    @Test public void explicitPreferenceWinsOverRecommendation(){var models=Arrays.asList("gpt-5.6-terra","gpt-5.6-luna");assertEquals("gpt-5.6-luna",ModelPolicy.choose(models,"gpt-5.6-luna"));assertEquals("gpt-5.6-terra",ModelPolicy.choose(models,"unavailable-model"));}
}
