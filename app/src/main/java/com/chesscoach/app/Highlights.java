package com.chesscoach.app;

import java.util.*;

/** Transparent learning opportunities. Not a claim to know a user's rating. */
public final class Highlights {
    public record Finding(int ply,String title,String reason,String focus,int priority) {}
    public static Finding find(int ply,Chess before,String played,Stockfish.Analysis a,Stockfish.Line actual) {
        if(a.top()==null||actual==null||a.top().pv().isEmpty()||ply<6)return null;
        var best=a.top();int side=before.white?1:-1,loss=Math.max(0,(best.value()-actual.value())*side);
        if(best.mate()!=null||actual.mate()!=null||loss>180)return null; // Avoid simply listing every large blunder.
        String candidate=best.pv().get(0);Chess after=before.copy();after.play(Chess.Move.parse(candidate));
        double pawn=loss/100.0;
        if(!candidate.equals(played)&&loss>=25) {
            String evidence=String.format(Locale.US,"실전 수 %s와 추천 %s의 평가 차이는 %.2f입니다 (깊이 %d). ",played,candidate,pawn,best.depth());
            if(best.cp()*side< -80)return new Finding(ply,"불리한 국면의 방어 자원",evidence+"큰 실수보다 버틸 수 있는 후보를 찾는 훈련에 적합합니다. 추천 변화에서 상대 위협에 대응하는 순서를 비교해 보세요.","defensive-resource",100);
            if(after.inCheck(after.white))return new Finding(ply,"강제수 후보를 보는 눈",evidence+"추천수는 실제 체크입니다. 체크를 먼저 계산하는 습관을 익힐 수 있는 작은 차이의 국면입니다.","forcing-move",90);
            return new Finding(ply,"거의 맞춘 계획, 한 단계 더",evidence+"평가 손실이 작아 후보수 선택을 다듬기 좋습니다. 두 수가 바꾸는 기물의 활동성과 다음 계획을 비교해 보세요.","candidate-comparison",70);
        }
        if(candidate.equals(played)&&a.lines().size()>1) {
            int gap=(best.value()-a.lines().get(1).value())*side;
            if(gap>=60)return new Finding(ply,"좋은 판단을 다시 재현하기","실전 수가 1순위이며 2순위 후보보다 "+String.format(Locale.US,"%.2f",gap/100.0)+" 유리합니다. 왜 이 수를 선택했는지 설명해 보고 다음 대국에서도 같은 판단 기준을 재현해 보세요. 제한된 탐색에 따른 학습 후보입니다.","good-decision",80);
        }
        return null;
    }
    public static List<Finding> select(List<Finding> findings) {
        List<Finding> ranked=new ArrayList<>(findings);ranked.sort(Comparator.comparingInt(Finding::priority).reversed());List<Finding> chosen=new ArrayList<>();
        for(Finding f:ranked)if(chosen.stream().noneMatch(old->Math.abs(old.ply-f.ply)<4)){chosen.add(f);if(chosen.size()==6)break;}
        chosen.sort(Comparator.comparingInt(Finding::ply));return chosen;
    }
}
