package com.chesscoach.app;
import java.util.*;
/** Transparent engine-based labels; brilliant is explicitly a sacrifice candidate. */
public final class MoveJudgment {
    public enum Kind {
        UNKNOWN("분석 중", "…",0xFF71837A), BEST("최선수","★",0xFF529C3B), EXCELLENT("훌륭한 수","✓",0xFF70AC56), GOOD("좋은 수","✓",0xFF8CAA65), INACCURACY("아쉬운 수","?!",0xFFE5B638), MISTAKE("실수","?",0xFFE78936), BLUNDER("블런더","??",0xFFD95959), BRILLIANT("탁월 후보","!!",0xFF25A6AA);
        public final String label,symbol;public final int color;Kind(String l,String s,int c){label=l;symbol=s;color=c;}
    }
    public record Result(Kind kind,int loss,String reason) {}
    public static Result assess(Chess before,String played,Stockfish.Analysis analysis,Stockfish.Line actual){
        if(analysis==null||analysis.top()==null||actual==null)return new Result(Kind.UNKNOWN,0,"분석을 기다리고 있어요.");
        var best=analysis.top();int loss=Math.max(0,(best.value()-actual.value())*(before.white?1:-1));Kind k;
        boolean first=!best.pv().isEmpty()&&best.pv().get(0).equals(played);
        if(first&&sacrifice(before,played,actual))k=Kind.BRILLIANT;else if(first)k=Kind.BEST;else if(loss<=15)k=Kind.EXCELLENT;else if(loss<=30)k=Kind.GOOD;else if(loss<=100)k=Kind.INACCURACY;else if(loss<=250)k=Kind.MISTAKE;else k=Kind.BLUNDER;
        String reason=k==Kind.BRILLIANT?"기물을 내준 뒤에도 보상이 유지되는 희생 후보입니다. 확정 판정은 아니에요.":k==Kind.BEST?"현재 탐색에서 가장 높은 평가를 받은 수예요.":best.mate()!=null||actual.mate()!=null?"강제 메이트의 유무가 달라지는 수입니다.":String.format(Locale.US,"추천 수와 평가 차이 %.2f · 깊이 %d",loss/100.0,best.depth());
        return new Result(k,loss,reason);
    }
    private static boolean sacrifice(Chess before,String played,Stockfish.Line line){
        if(line.depth()<12||line.mate()!=null||line.cp()*(before.white?1:-1)<-50||line.cp()*(before.white?1:-1)>400||line.pv().size()<4)return false;
        try{Chess.Move m=Chess.Move.parse(played);char piece=before.squares[m.from()];if(Character.toUpperCase(piece)=='K'||value(piece)<3)return false;
            Chess replay=before.copy();int initial=balance(replay),sign=before.white?1:-1;int afterCapture=0;
            for(int i=0;i<4;i++){Chess.Move next=Chess.Move.parse(line.pv().get(i));if(!replay.legalMoves().contains(next))return false;if(i==0&&!next.uci().equals(played))return false;if(i==1&&next.to()!=m.to())return false;replay.play(next);if(i==1)afterCapture=(initial-balance(replay))*sign;}
            return afterCapture>=2&&(initial-balance(replay))*sign>=2;
        }catch(Exception ignored){return false;}
    }
    private static int value(char p){switch(Character.toUpperCase(p)){case 'P':return 1;case 'N':case 'B':return 3;case 'R':return 5;case 'Q':return 9;default:return 0;}}
    private static int balance(Chess b){int score=0;for(char p:b.squares)score+=value(p)*(Character.isUpperCase(p)?1:-1);return score;}
}
