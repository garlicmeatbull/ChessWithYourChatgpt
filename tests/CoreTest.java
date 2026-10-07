package com.chesscoach.app;

import java.util.*;

public final class CoreTest {
    static int passed=0;
    static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);passed++;System.out.println("PASS "+label);}
    static long perft(Chess b,int depth){if(depth==0)return 1;long total=0;for(var m:b.legalMoves()){Chess c=b.copy();c.play(m);total+=perft(c,depth-1);}return total;}
    static Chess sequence(String... moves){Chess c=new Chess();for(String u:moves)c.play(Chess.Move.parse(u));return c;}
    public static void main(String[] args)throws Exception {
        Chess start=new Chess();check(start.fen().equals(Chess.START),"FEN roundtrip");
        check(perft(start,1)==20,"start perft 1 = 20");check(perft(start,2)==400,"start perft 2 = 400");check(perft(start,3)==8902,"start perft 3 = 8902");
        Chess kiwi=new Chess("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        check(perft(kiwi,1)==48,"Kiwipete perft 1 = 48");check(perft(kiwi,2)==2039,"Kiwipete perft 2 = 2039");check(perft(kiwi,3)==97862,"Kiwipete perft 3 = 97862");
        Chess ep=new Chess("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        check(perft(ep,3)==2812,"en passant / pin perft 3 = 2812");
        Chess castle=new Chess("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");castle.play(Chess.Move.parse("e1g1"));
        check(castle.squares[5]=='R'&&castle.squares[6]=='K'&&castle.squares[7]=='.',"castling relocates rook");
        Chess pawn=sequence("e2e4","a7a6","e4e5","d7d5");pawn.play(Chess.Move.parse("e5d6"));check(pawn.squares[Chess.index("d5")]=='.',"en passant removes pawn");
        Chess promotion=new Chess("7k/P7/8/8/8/8/8/7K w - - 0 1");
        check(promotion.legalMoves().stream().filter(m->m.from()==48).count()==4,"four promotion choices");promotion.play(Chess.Move.parse("a7a8n"));check(promotion.squares[56]=='N',"underpromotion");
        Chess mate=sequence("f2f3","e7e5","g2g4","d8h4");check(mate.terminal(1).contains("체크메이트"),"Fool's mate");
        check(new Chess("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1").terminal(1).contains("스테일메이트"),"stalemate");
        check(new Chess("7k/8/8/8/8/8/8/7K w - - 0 1").insufficientMaterial(),"king-only draw");
        check(new Chess("7k/8/8/8/8/8/8/5NNK w - - 0 1").insufficientMaterial()==false,"two knights not automatically insufficient");
        check(sequence("e2e4").key().endsWith(" -"),"uncapturable en passant excluded from repetition key");
        Chess repetition=sequence("g1f3","g8f6","f3g1","f6g8");check(repetition.key().equals(start.key()),"repeated position key");check(repetition.terminal(3).contains("반복"),"threefold draw");
        check(new Chess("7k/8/8/8/8/8/8/R6K w - - 100 51").terminal(1).contains("50수"),"fifty-move draw");
        var line=Stockfish.parse("info depth 14 multipv 2 score cp -25 nodes 100 pv e7e5 g1f3",false);
        check(line.cp()==25&&line.rank()==2&&line.pv().size()==2,"UCI Black score normalized to White");
        check(Stockfish.parse("info depth 20 score mate 3 pv e7e5",false).mate()==-3,"UCI mate normalization");
        check(Stockfish.parse("info depth 10 score cp 30 lowerbound pv e2e4",true)==null,"bound scores are not final exact evaluations");
        Pgn.Game pgn=Pgn.parse("[White \"Player A\"]\n[Black \"Player B\"]\n1.e4 {central pawn} e5 2.Nf3 (2. Bc4 Nc6 (2... Nf6)) Nc6 $1 3.Bb5 a6 *");
        check(pgn.plies().size()==6&&pgn.plies().get(4).uci().equals("f1b5"),"PGN SAN mainline, tags, comments, nested RAV and NAG");
        check(Pgn.parse(pgn.export()).plies().equals(pgn.plies()),"PGN export roundtrip preserves every position and move");
        check(Pgn.parse("e2e4 e7e5 g1f3 b8c6").plies().get(2).san().equals("Nf3"),"UCI paste converted to standard SAN");
        check(Pgn.parse("1. f3 e5 2. g4 Qh4# 0-1").result().equals("0-1"),"SAN checkmate and result");
        check(Pgn.parse("[SetUp \"1\"] [FEN \"r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1\"] 1. 0-0 0-0-0 *").plies().get(0).san().equals("O-O"),"custom FEN and castling notation");
        check(new Chess("7k/P7/8/8/8/8/8/7K w - - 0 1").san(Chess.Move.parse("a7a8q")).equals("a8=Q+"),"SAN promotion and check");
        Chess ambiguity=new Chess("7k/8/8/8/8/8/8/1N2KN2 w - - 0 1");
        check(ambiguity.san(Chess.Move.parse("b1d2")).equals("Nbd2"),"SAN file disambiguation");
        for(String invalid:new String[]{"1. e5", "1. e4 (1. d4", "1. e4 e5 1-0 2. Nf3", "[Event \"a\"] [Event \"b\"] 1. e4 *"}){
            boolean rejected=false;try{Pgn.parse(invalid);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid PGN rejected: "+invalid);
        }
        var best=new Stockfish.Line(1,16,50,null,List.of("e2e4","e7e5"));var actual=new Stockfish.Line(1,16,0,null,List.of("d2d4","d7d5"));
        var opportunity=Highlights.find(8,new Chess(),"d2d4",new Stockfish.Analysis("e2e4",List.of(best)),actual);
        check(opportunity!=null&&opportunity.focus().equals("candidate-comparison"),"highlight selects small improvable decision with reason");
        check(Highlights.find(8,new Chess(),"d2d4",new Stockfish.Analysis("e2e4",List.of(best)),new Stockfish.Line(1,16,-400,null,List.of("d2d4")))==null,"large ordinary blunders excluded from learning highlights");
        check(Highlights.find(2,new Chess(),"d2d4",new Stockfish.Analysis("e2e4",List.of(best)),actual)==null,"opening trivia excluded from highlights");
        var spaced=Highlights.select(List.of(opportunity,new Highlights.Finding(9,"other","reason","good-decision",90),new Highlights.Finding(15,"later","reason","good-decision",80)));
        check(spaced.size()==2,"highlights spread across game rather than adjacent repeated mistakes");
        check(Difficulty.from("ELO_1600").elo==1600&&Difficulty.FULL.elo==0&&Difficulty.FULL.skill==20,"difficulty presets distinguish full-strength analysis");
        var judged=new Stockfish.Analysis("e2e4",List.of(best));
        check(MoveJudgment.assess(start,"e2e4",judged,best).kind()==MoveJudgment.Kind.BEST,"ordinary best move is not labeled brilliant");
        check(MoveJudgment.assess(start,"d2d4",judged,actual).kind()==MoveJudgment.Kind.INACCURACY,"moderate evaluation loss classification");
        check(MoveJudgment.assess(start,"d2d4",judged,new Stockfish.Line(1,16,-400,null,List.of("d2d4"))).kind()==MoveJudgment.Kind.BLUNDER,"large evaluation loss is blunder");
        check(MoveJudgment.assess(start,"d2d4",judged,null).kind()==MoveJudgment.Kind.UNKNOWN,"missing played score does not fabricate a judgment");
        Chess blackPosition=sequence("e2e4");var blackBest=new Stockfish.Line(1,16,-50,null,List.of("e7e5"));var blackBad=new Stockfish.Line(1,16,400,null,List.of("a7a6"));
        check(MoveJudgment.assess(blackPosition,"a7a6",new Stockfish.Analysis("e7e5",List.of(blackBest)),blackBad).kind()==MoveJudgment.Kind.BLUNDER,"judgments normalize Black perspective");
        Chess sacrifice=new Chess("4k3/4p3/3p4/4p3/8/5N2/3P4/4K3 w - - 0 1");var sacrificeLine=new Stockfish.Line(1,16,50,null,List.of("f3e5","d6e5","d2d3","e7e6"));var sacrificeAnalysis=new Stockfish.Analysis("f3e5",List.of(sacrificeLine));
        check(MoveJudgment.assess(sacrifice,"f3e5",sacrificeAnalysis,sacrificeLine).kind()==MoveJudgment.Kind.BRILLIANT,"verified PV sacrifice is explicitly a brilliant candidate");
        check(MoveJudgment.assess(sacrifice,"f3e5",sacrificeAnalysis,new Stockfish.Line(1,8,50,null,sacrificeLine.pv())).kind()==MoveJudgment.Kind.BEST,"shallow search cannot claim brilliant candidate");
        if(args.length>0)try(Stockfish fish=new Stockfish(args[0])) {
            var a=fish.analyze(new Chess(),300,3,null);check(a.lines().size()==3,"real Stockfish MultiPV 3");
            check(new Chess().legalMoves().contains(Chess.Move.parse(a.best())),"real engine returns legal move");
            var forced=fish.analyze(new Chess(),300,1,"a2a3");check(forced.best().equals("a2a3"),"forced-move search");
            Chess b=sequence("e2e4");var black=fish.analyze(b,200,1,null);check(b.legalMoves().contains(Chess.Move.parse(black.best())),"real engine Black reply");
            var ending=fish.analyze(mate,100,1,null);check(ending.best().equals("(none)")||ending.best().equals("0000"),"real engine terminal position");
            try(Stockfish opponent=new Stockfish(args[0])){
                opponent.setDifficulty(Difficulty.ELO_1320);var weak=opponent.analyze(new Chess(),200,1,null);check(new Chess().legalMoves().contains(Chess.Move.parse(weak.best())),"independent Elo-limited opponent returns legal move");
                check(fish.analyze(new Chess(),200,3,null).lines().size()==3,"analysis engine unaffected by opponent difficulty");
                opponent.setDifficulty(Difficulty.BEGINNER);check(new Chess().legalMoves().contains(Chess.Move.parse(opponent.analyze(new Chess(),200,1,null).best())),"Skill Level beginner mode");
            }
        }
        System.out.println("Passed "+passed+" core checks");
    }
}
