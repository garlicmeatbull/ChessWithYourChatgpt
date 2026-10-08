package com.chesscoach.app;

/** Player-relative results for games played as White; neutral results for imported games. */
record GameOutcome(String title,String detail,Kind kind) {
    enum Kind { WIN, LOSS, DRAW }
    private static final java.util.regex.Pattern RESULT=java.util.regex.Pattern.compile("(?m)^\\[Result \\\"(1-0|0-1|1/2-1/2|\\*)\\\"\\]$");
    static String savedResult(String pgn){var match=RESULT.matcher(pgn);return match.find()?match.group(1):"*";}
    static GameOutcome from(String result,boolean played,String terminal){
        if(result==null||result.equals("*"))return null;
        boolean agrees=terminal!=null&&(result.equals("1-0")&&terminal.startsWith("백 승리")||result.equals("0-1")&&terminal.startsWith("흑 승리")||result.equals("1/2-1/2")&&terminal.startsWith("무승부"));
        String reason=agrees&&terminal.contains(" · ")?terminal.substring(terminal.indexOf(" · ")+3):"기보에 저장된 결과";
        return switch(result){
            case "1-0"->new GameOutcome(played?"승리":"백 승리","1–0 · "+reason,Kind.WIN);
            case "0-1"->new GameOutcome(played?"패배":"흑 승리","0–1 · "+reason,played?Kind.LOSS:Kind.WIN);
            case "1/2-1/2"->new GameOutcome("무승부","½–½ · "+reason,Kind.DRAW);
            default->null;
        };
    }
    static GameOutcome live(String terminal){return from(terminal==null?"*":terminal.startsWith("백 승리")?"1-0":terminal.startsWith("흑 승리")?"0-1":"1/2-1/2",true,terminal);}
}
