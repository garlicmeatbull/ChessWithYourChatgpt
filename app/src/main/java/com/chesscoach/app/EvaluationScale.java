package com.chesscoach.app;
/** Display proportions, not probabilities; evaluations always use White's perspective. */
final class EvaluationScale {
    static double whiteShare(int cp,Integer mate){if(mate!=null)return mate>0?1:mate<0?0:.5;return .5+Math.atan(cp/400.0)/Math.PI;}
}
