package com.chesscoach.app;
import java.util.*;
/** Transparent task-fit heuristic, not a model quality benchmark. */
final class ModelPolicy {
    static String recommend(List<String> slugs){return slugs.stream().min(Comparator.comparingInt(ModelPolicy::rank).thenComparing(Comparator.reverseOrder())).orElse("");}
    static String choose(List<String> slugs,String preferred){return slugs.contains(preferred)?preferred:recommend(slugs);}
    static int rank(String model){String s=model.toLowerCase(Locale.ROOT);if(s.startsWith("gpt-5.6-terra"))return 0;if(s.contains("mini")&&!s.contains("codex"))return 1;if(s.contains("nano"))return 2;if(!s.contains("codex")&&!s.startsWith("o")&&!s.contains("pro"))return 3;return 4;}
    static String comment(String model){String s=model.toLowerCase(Locale.ROOT);if(s.contains("terra"))return "실시간 코칭 추천 · 짧은 해설과 빠른 응답에 초점";if(s.contains("mini"))return "빠른 간단 해설 · 엔진 분석을 짧게 풀어 읽기";if(s.contains("nano"))return "아주 짧은 조언 위주 · 복잡한 설명은 확인 필요";if(s.contains("codex"))return "코딩 중심 모델 · 체스 해설에는 일반 모델 우선";if(s.contains("pro")||s.startsWith("o"))return "깊은 설명을 비교할 때 · 응답이 오래 걸릴 수 있어요";return "일반 해설 · 속도와 설명을 직접 비교해 보세요";}
}
