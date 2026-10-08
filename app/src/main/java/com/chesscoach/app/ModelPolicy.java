package com.chesscoach.app;
import java.util.*;
/** Transparent task-fit heuristic, not a model quality benchmark. */
final class ModelPolicy {
    static String recommend(List<String> slugs){return slugs.stream().min(Comparator.comparingInt(ModelPolicy::rank).thenComparing(Comparator.reverseOrder())).orElse("");}
    static String choose(List<String> slugs,String preferred){return slugs.contains(preferred)?preferred:recommend(slugs);}
    static int rank(String model){String s=model.toLowerCase(Locale.ROOT);if(s.startsWith("gpt-5.6-terra"))return 0;if(s.contains("mini")&&!s.contains("codex"))return 1;if(s.contains("nano"))return 2;if(!s.contains("codex")&&!s.startsWith("o")&&!s.contains("pro"))return 3;return 4;}
    static String comment(String model){String s=model.toLowerCase(Locale.ROOT);
        if(s.contains("nano"))return "매우 빠른 속도 · 가벼운 설명 · 짧은 한 줄 조언";
        if(s.contains("mini"))return "빠른 속도 · 간결한 설명 · 매 수 코칭에 적합";
        if(s.contains("pro")||s.contains("thinking")||s.startsWith("o"))return "느린 속도 · 깊은 추론 · 어려운 수의 근거를 자세히 비교";
        if(s.contains("codex"))return "보통~느린 속도 · 논리적인 설명 · 계산 과정과 근거 중심";
        if(s.contains("terra"))return "빠른 속도 · 균형 잡힌 설명 · 실시간 코칭 추천";
        if(s.contains("luna"))return "보통 속도 · 자세한 설명 · 전술과 다음 계획 중심";
        return "보통 속도 · 균형 잡힌 설명 · 일반적인 대국 복기";
    }
}
