package com.chesscoach.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class EvaluationScaleTest {
    @Test public void balancedAndSignedScoresUseWhitePerspective(){assertEquals(.5,EvaluationScale.whiteShare(0,null),0);assertTrue(EvaluationScale.whiteShare(300,null)>.5);assertTrue(EvaluationScale.whiteShare(-300,null)<.5);assertEquals(1,EvaluationScale.whiteShare(500,null)+EvaluationScale.whiteShare(-500,null),.00001);}
    @Test public void mateOverridesContradictoryCentipawns(){assertEquals(1,EvaluationScale.whiteShare(-9999,3),0);assertEquals(0,EvaluationScale.whiteShare(9999,-3),0);}
    @Test public void LargeScoresStayWithinBarWithoutOverflow(){assertTrue(EvaluationScale.whiteShare(Integer.MAX_VALUE,null)<=1);assertTrue(EvaluationScale.whiteShare(Integer.MIN_VALUE,null)>=0);}
}
