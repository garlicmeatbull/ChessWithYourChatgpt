package com.chesscoach.app;

import java.util.*;

/** Pure Java rules, independent of UI and Stockfish. a1=0, h8=63. */
public final class Chess {
    public static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public final char[] squares = new char[64];
    public boolean white;
    public String castling;
    public int ep, halfmove, fullmove;

    public record Move(int from, int to, char promotion) {
        public String uci() { return name(from) + name(to) + (promotion == 0 ? "" : promotion); }
        public static Move parse(String value) {
            if (!value.matches("[a-h][1-8][a-h][1-8][qrbn]?")) throw new IllegalArgumentException("Invalid move");
            return new Move(index(value.substring(0,2)), index(value.substring(2,4)), value.length()==5?value.charAt(4):0);
        }
    }
    public Chess() { this(START); }
    public Chess(String fen) {
        if(fen==null||fen.length()>110)throw new IllegalArgumentException("Invalid FEN");
        Arrays.fill(squares, '.');
        String[] parts=fen.split(" ");
        if(parts.length!=6||!parts[1].matches("[wb]")||!parts[2].matches("-|K?Q?k?q?")||!parts[3].matches("-|[a-h][36]")||!parts[4].matches("[0-9]{1,5}")||!parts[5].matches("[1-9][0-9]{0,4}"))throw new IllegalArgumentException("Invalid FEN fields");
        String[] rows=parts[0].split("/");
        if(rows.length!=8)throw new IllegalArgumentException("Invalid FEN ranks");
        for (int r=0;r<8;r++) {
            int f=0;
            for (char c:rows[r].toCharArray()) { if(c>='1'&&c<='8')f+=c-'0';else {if("prnbqkPRNBQK".indexOf(c)<0||f>=8)throw new IllegalArgumentException("Invalid FEN piece");squares[(7-r)*8+f++]=c;} }
            if(f!=8)throw new IllegalArgumentException("Invalid FEN rank width");
        }
        int kings=0,blackKings=0;for(char c:squares){if(c=='K')kings++;if(c=='k')blackKings++;}if(kings!=1||blackKings!=1)throw new IllegalArgumentException("FEN needs both kings");
        white=parts[1].equals("w"); castling=parts[2]; ep=parts[3].equals("-")?-1:index(parts[3]);
        halfmove=Integer.parseInt(parts[4]); fullmove=Integer.parseInt(parts[5]);
    }
    public static String name(int s) { return ""+(char)('a'+s%8)+(char)('1'+s/8); }
    public static int index(String s) { return s.charAt(0)-'a'+8*(s.charAt(1)-'1'); }
    public static boolean color(char c) { return Character.isUpperCase(c); }
    private boolean own(int s) { return squares[s]!='.' && color(squares[s])==white; }
    public Chess copy() { return new Chess(fen()); }
    public String fen() {
        StringBuilder out=new StringBuilder();
        for (int r=7;r>=0;r--) {
            int empty=0;
            for(int f=0;f<8;f++) { char c=squares[r*8+f]; if(c=='.') empty++; else { if(empty>0) out.append(empty); empty=0; out.append(c); } }
            if(empty>0)out.append(empty); if(r>0)out.append('/');
        }
        return out+" "+(white?"w":"b")+" "+(castling.isEmpty()?"-":castling)+" "+(ep<0?"-":name(ep))+" "+halfmove+" "+fullmove;
    }
    public String key() { // Only a legally capturable en-passant target affects repetition.
        String[] p=fen().split(" ");
        if(ep>=0 && legalMoves().stream().noneMatch(m -> m.to==ep && Character.toLowerCase(squares[m.from])=='p' && m.from%8!=m.to%8))p[3]="-";
        return String.join(" ",Arrays.copyOf(p,4));
    }
    public List<Move> legalMoves() {
        List<Move> out=new ArrayList<>();
        for(Move m:pseudo()) { Chess c=copy(); c.applyUnchecked(m); if(!c.inCheck(white))out.add(m); }
        return out;
    }
    public void play(Move m) { if(!legalMoves().contains(m))throw new IllegalArgumentException("Illegal move: "+m.uci()); applyUnchecked(m); }
    public String san(Move move) {
        List<Move> legal=legalMoves();if(!legal.contains(move))throw new IllegalArgumentException("Illegal move");
        char p=Character.toLowerCase(squares[move.from]);boolean capture=squares[move.to]!='.'||(p=='p'&&move.from%8!=move.to%8);
        StringBuilder s=new StringBuilder();
        if(p=='k'&&Math.abs(move.to-move.from)==2)s.append(move.to>move.from?"O-O":"O-O-O");
        else {
            if(p!='p') {
                s.append(Character.toUpperCase(p));List<Move> others=new ArrayList<>();
                for(Move m:legal)if(m.from!=move.from&&m.to==move.to&&squares[m.from]==squares[move.from])others.add(m);
                if(!others.isEmpty()) {
                    boolean sameFile=others.stream().anyMatch(m->m.from%8==move.from%8),sameRank=others.stream().anyMatch(m->m.from/8==move.from/8);
                    if(!sameFile)s.append((char)('a'+move.from%8));else if(!sameRank)s.append((char)('1'+move.from/8));else s.append(name(move.from));
                }
            }else if(capture)s.append((char)('a'+move.from%8));
            if(capture)s.append('x');s.append(name(move.to));if(move.promotion!=0)s.append('=').append(Character.toUpperCase(move.promotion));
        }
        Chess after=copy();after.applyUnchecked(move);if(after.inCheck(after.white))s.append(after.legalMoves().isEmpty()?'#':'+');
        return s.toString();
    }
    private void add(List<Move> out,int from,int to) {
        if(own(to) || Character.toLowerCase(squares[to])=='k')return;
        if(Character.toLowerCase(squares[from])=='p' && (to/8==0 || to/8==7)) for(char c:new char[]{'q','r','b','n'})out.add(new Move(from,to,c));
        else out.add(new Move(from,to,(char)0));
    }
    private List<Move> pseudo() {
        List<Move> out=new ArrayList<>();
        for(int s=0;s<64;s++) {
            if(!own(s))continue;
            char p=Character.toLowerCase(squares[s]); int f=s%8,r=s/8;
            if(p=='p') {
                int d=white?1:-1,t=s+8*d;
                if(t>=0 && t<64 && squares[t]=='.') {
                    add(out,s,t); int t2=s+16*d;
                    if(r==(white?1:6) && squares[t2]=='.')add(out,s,t2);
                }
                for(int df:new int[]{-1,1}) {
                    int ff=f+df,rr=r+d;
                    if(ff<0||ff>7||rr<0||rr>7)continue;
                    int to=rr*8+ff;
                    boolean enPassant=to==ep && squares[to]=='.' && squares[to-8*d]==(white?'p':'P');
                    if((squares[to]!='.' && !own(to))||enPassant)add(out,s,to);
                }
                continue;
            }
            int[][] dirs=p=='n'?new int[][]{{1,2},{2,1},{-1,2},{-2,1},{1,-2},{2,-1},{-1,-2},{-2,-1}}:
                p=='b'?new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}}:
                p=='r'?new int[][]{{1,0},{-1,0},{0,1},{0,-1}}:
                new int[][]{{1,1},{1,-1},{-1,1},{-1,-1},{1,0},{-1,0},{0,1},{0,-1}};
            for(int[] d:dirs) {
                int ff=f+d[0],rr=r+d[1];
                while(ff>=0 && ff<8 && rr>=0 && rr<8) {
                    int to=rr*8+ff; add(out,s,to);
                    if(squares[to]!='.' || p=='n' || p=='k')break;
                    ff+=d[0]; rr+=d[1];
                }
            }
            if(p=='k' && s==(white?4:60) && !inCheck(white)) {
                int base=white?0:56; boolean enemy=!white;
                if(castling.indexOf(white?'K':'k')>=0 && squares[base+7]==(white?'R':'r') && squares[base+5]=='.' && squares[base+6]=='.' && !attacked(base+5,enemy) && !attacked(base+6,enemy))add(out,s,base+6);
                if(castling.indexOf(white?'Q':'q')>=0 && squares[base]==(white?'R':'r') && squares[base+1]=='.' && squares[base+2]=='.' && squares[base+3]=='.' && !attacked(base+3,enemy) && !attacked(base+2,enemy))add(out,s,base+2);
            }
        }
        return out;
    }
    public boolean inCheck(boolean side) {
        for(int s=0;s<64;s++)if(squares[s]==(side?'K':'k'))return attacked(s,!side);
        return true;
    }
    public boolean attacked(int target,boolean byWhite) {
        int tf=target%8,tr=target/8;
        for(int s=0;s<64;s++) {
            char c=squares[s]; if(c=='.'||color(c)!=byWhite)continue;
            char p=Character.toLowerCase(c); int df=tf-s%8,dr=tr-s/8;
            if(p=='p' && dr==(byWhite?1:-1) && Math.abs(df)==1)return true;
            if(p=='n' && Math.abs(df)*Math.abs(dr)==2)return true;
            if(p=='k' && Math.max(Math.abs(df),Math.abs(dr))==1)return true;
            boolean diagonal=Math.abs(df)==Math.abs(dr) && df!=0;
            boolean straight=(df==0) != (dr==0);
            if((p=='b'&&diagonal)||(p=='r'&&straight)||(p=='q'&&(diagonal||straight))) {
                int sf=Integer.signum(df),sr=Integer.signum(dr),f=s%8+sf,r=s/8+sr;
                while(f!=tf||r!=tr) { if(squares[r*8+f]!='.')break; f+=sf;r+=sr; }
                if(f==tf&&r==tr)return true;
            }
        }
        return false;
    }
    void applyUnchecked(Move m) {
        char p=squares[m.from],captured=squares[m.to];
        if(Character.toLowerCase(p)=='p' && m.to==ep && captured=='.' && m.from%8!=m.to%8)squares[m.to+(white?-8:8)]='.';
        squares[m.to]=m.promotion==0?p:(white?Character.toUpperCase(m.promotion):m.promotion); squares[m.from]='.';
        if(Character.toLowerCase(p)=='k') {
            castling=castling.replace(white?"K":"k","").replace(white?"Q":"q","");
            if(Math.abs(m.to-m.from)==2) { int rook=m.to>m.from?m.from+3:m.from-4; int dest=m.to>m.from?m.from+1:m.from-1; squares[dest]=squares[rook];squares[rook]='.'; }
        }
        for(int s:new int[]{m.from,m.to}) {
            String right=s==0?"Q":s==7?"K":s==56?"q":s==63?"k":"";
            if(!right.isEmpty())castling=castling.replace(right,"");
        }
        ep=Character.toLowerCase(p)=='p' && Math.abs(m.to-m.from)==16?(m.from+m.to)/2:-1;
        halfmove=Character.toLowerCase(p)=='p'||captured!='.'?0:halfmove+1;
        if(!white)fullmove++;white=!white;
    }
    public boolean insufficientMaterial() {
        int knights=0,bishops=0; Integer bishopColor=null;
        for(int s=0;s<64;s++) {
            char p=Character.toLowerCase(squares[s]);
            if(p=='.'||p=='k')continue;
            if(p=='n')knights++;
            else if(p=='b') { bishops++; int c=(s%8+s/8)%2; if(bishopColor==null)bishopColor=c; else if(bishopColor!=c)return false; }
            else return false;
        }
        return bishops==0&&knights<=1 || knights==0;
    }
    public String terminal(int repetitions) {
        if(legalMoves().isEmpty())return inCheck(white)?(white?"흑 승리 · 체크메이트":"백 승리 · 체크메이트"):"무승부 · 스테일메이트";
        if(insufficientMaterial())return "무승부 · 기물 부족";
        if(halfmove>=100)return "무승부 · 50수 규칙 (자동 청구)";
        if(repetitions>=3)return "무승부 · 3회 반복 (자동 청구)";
        return null;
    }
}
