package com.chesscoach.app;

import java.util.*;
import java.util.regex.*;

/** PGN main-line + SAN and UCI support; annotations/RAV are safely skipped. */
public final class Pgn {
    public record Ply(String before,String after,String uci,String san,boolean white,int number) {}
    public record Game(String initial,List<Ply> plies,Map<String,String> tags,String result) {
        public String export() {
            StringBuilder s=new StringBuilder("[Event \"Chess Coach\"]\n");
            if(!initial.equals(Chess.START))s.append("[SetUp \"1\"]\n[FEN \"").append(initial).append("\"]\n");
            s.append("[Result \"").append(result).append("\"]\n\n");
            for(int i=0;i<plies.size();i++){Ply p=plies.get(i);if(p.white)s.append(p.number).append(". ");else if(i==0)s.append(p.number).append("... ");s.append(p.san).append(' ');}
            return s.append(result).toString();
        }
    }
    private static final Pattern TAG=Pattern.compile("\\[([A-Za-z0-9_]+)\\s+\"((?:\\\\.|[^\"\\\\])*)\"\\]");
    public static Game parse(String source) {
        if(source==null||source.trim().isEmpty())throw new IllegalArgumentException("PGN 또는 UCI 대국 기록을 붙여넣으세요.");
        if(source.length()>100000)throw new IllegalArgumentException("대국 기록은 100KB 이하로 입력하세요.");
        Map<String,String> tags=new LinkedHashMap<>();Matcher matcher=TAG.matcher(source);
        while(matcher.find()){if(tags.containsKey(matcher.group(1)))throw new IllegalArgumentException("한 번에 한 대국만 가져올 수 있습니다.");tags.put(matcher.group(1),matcher.group(2).replace("\\\"","\"").replace("\\\\","\\"));}
        String initial=tags.getOrDefault("FEN",Chess.START);Chess board;
        try{board=new Chess(initial);if(board.squares.length!=64||board.inCheck(true)&&board.inCheck(false))throw new IllegalArgumentException();}
        catch(Exception e){throw new IllegalArgumentException("PGN의 FEN 시작 국면이 올바르지 않습니다.");}
        initial=board.fen();String text=strip(matcher.replaceAll(" "));List<Ply> plies=new ArrayList<>();String result="*";boolean ended=false;
        for(String raw:text.trim().split("\\s+")) {
            String token=raw.replaceFirst("^\\d+\\.(?:\\.\\.)?","").replaceAll("[!?]+$","");
            if(token.isEmpty()||token.equals("...")||token.matches("\\$\\d+"))continue;
            if(Arrays.asList("1-0","0-1","1/2-1/2","*").contains(token)){result=token;ended=true;continue;}
            if(ended)throw new IllegalArgumentException("대국 결과 뒤에 수가 있습니다. 한 대국만 입력하세요.");
            if(plies.size()>=600)throw new IllegalArgumentException("최대 600개의 반수를 지원합니다.");
            Chess.Move chosen=null;
            if(token.matches("[a-h][1-8][a-h][1-8][qrbn]?")) {
                Chess.Move m=Chess.Move.parse(token);if(board.legalMoves().contains(m))chosen=m;
            }else {
                String san=normalize(token);
                for(var move:board.legalMoves())if(normalize(board.san(move)).equals(san)) {if(chosen!=null)throw new IllegalArgumentException("모호한 SAN: "+token);chosen=move;}
            }
            if(chosen==null)throw new IllegalArgumentException((plies.size()+1)+"번째 수 '"+token+"'를 해석할 수 없습니다. SAN 또는 UCI 표기를 확인하세요.");
            String before=board.fen(),san=board.san(chosen);boolean white=board.white;int number=board.fullmove;board.play(chosen);
            plies.add(new Ply(before,board.fen(),chosen.uci(),san,white,number));
        }
        if(plies.isEmpty()&&tags.isEmpty())throw new IllegalArgumentException("분석할 수가 없습니다.");
        if(result.equals("*")&&tags.containsKey("Result"))result=tags.get("Result");
        if(!Arrays.asList("1-0","0-1","1/2-1/2","*").contains(result))throw new IllegalArgumentException("올바르지 않은 대국 결과");
        return new Game(initial,Collections.unmodifiableList(plies),Collections.unmodifiableMap(tags),result);
    }
    private static String normalize(String s){return s.replace('0','O').replaceAll("[+#]+$","");}
    private static String strip(String source) {
        StringBuilder out=new StringBuilder();int braces=0,variations=0;boolean line=false;
        for(char c:source.toCharArray()) {
            if(line){if(c=='\n'||c=='\r'){line=false;out.append(' ');}continue;}
            if(braces>0){if(c=='{')braces++;else if(c=='}')braces--;continue;}
            if(c=='{'){braces++;out.append(' ');continue;}
            if(c==';'){line=true;out.append(' ');continue;}
            if(c=='('){variations++;out.append(' ');continue;}
            if(c==')'){if(--variations<0)throw new IllegalArgumentException("닫는 괄호가 올바르지 않습니다.");out.append(' ');continue;}
            if(c=='}')throw new IllegalArgumentException("주석 괄호가 올바르지 않습니다.");
            if(variations==0)out.append(c);
        }
        if(braces!=0||variations!=0)throw new IllegalArgumentException("주석 또는 변화 괄호가 닫히지 않았습니다.");
        return out.toString().replaceAll("\\$\\d+"," ");
    }
}
