package com.chesscoach.app;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** One serialized UCI process. All scores normalized to White's perspective. */
public final class Stockfish implements AutoCloseable {
    public record Line(int rank,int depth,int cp,Integer mate,List<String> pv) {
        public int value() { return mate==null?cp:(mate>0?100000-Math.abs(mate): -100000+Math.abs(mate)); }
        public String score() { return mate==null?String.format(Locale.US,"%+.2f",cp/100.0):"M"+mate; }
    }
    public record Analysis(String best,List<Line> lines) { public Line top(){return lines.isEmpty()?null:lines.get(0);} }
    private final Process process;
    private final BufferedWriter input;
    private final BlockingQueue<String> output=new LinkedBlockingQueue<>();
    private volatile boolean closed;
    public Stockfish(String executable) throws Exception {
        process=new ProcessBuilder(executable).redirectErrorStream(true).start();
        input=new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        Thread reader=new Thread(()->{
            try(BufferedReader r=new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String l; while((l=r.readLine())!=null)output.offer(l);
            }catch(IOException ignored){} finally{output.offer("__EOF__");}
        },"stockfish-output");reader.setDaemon(true);reader.start();
        try {
            send("uci"); await("uciok",60000);
            send("setoption name Threads value 1");send("setoption name Hash value 32");
            send("setoption name UCI_LimitStrength value false");send("setoption name Skill Level value 20");
            send("isready");await("readyok",60000);
        } catch(Exception e) { close(); throw e; }
    }
    public synchronized void setDifficulty(Difficulty difficulty)throws Exception {
        send("setoption name UCI_LimitStrength value "+(difficulty.elo>0?"true":"false"));
        send("setoption name Skill Level value "+difficulty.skill);
        if(difficulty.elo>0)send("setoption name UCI_Elo value "+difficulty.elo);
        send("isready");await("readyok",60000);
    }
    private void send(String text)throws IOException { input.write(text);input.newLine();input.flush(); }
    private String next(long deadline)throws Exception {
        String l=output.poll(Math.max(1,deadline-System.currentTimeMillis()),TimeUnit.MILLISECONDS);
        if(l==null)throw new IOException("Stockfish timed out");
        if(l.equals("__EOF__"))throw new IOException("Stockfish exited");return l;
    }
    private void await(String expected,int timeout)throws Exception {
        long deadline=System.currentTimeMillis()+timeout; while(!next(deadline).equals(expected)){}
    }
    public synchronized Analysis analyze(Chess board,int millis,int candidates,String forced)throws Exception {
        if(closed)throw new IOException("Stockfish is closed");
        send("setoption name MultiPV value "+candidates);send("isready");await("readyok",60000);
        send("position fen "+board.fen());
        send("go movetime "+millis+(forced==null?"":" searchmoves "+forced));
        Map<Integer,Line> latest=new TreeMap<>(); long deadline=System.currentTimeMillis()+millis+60000;
        try {
            while(true) {
                String raw=next(deadline);
                if(raw.startsWith("bestmove "))return new Analysis(raw.split(" ")[1],new ArrayList<>(latest.values()));
                Line l=parse(raw,board.white); if(l!=null)latest.put(l.rank,l);
            }
        }catch(Exception e){close();throw e;}
    }
    public static Line parse(String raw,boolean whiteToMove) {
        if(!raw.startsWith("info ")||!raw.contains(" pv ")||!raw.contains(" score ")||raw.contains("bound"))return null;
        String[] p=raw.split(" +");int rank=1,depth=0,cp=0;Integer mate=null;List<String> pv=new ArrayList<>();
        for(int i=1;i<p.length;i++) {
            if(p[i].equals("depth"))depth=Integer.parseInt(p[++i]);
            else if(p[i].equals("multipv"))rank=Integer.parseInt(p[++i]);
            else if(p[i].equals("score")) { String kind=p[++i];int n=Integer.parseInt(p[++i])*(whiteToMove?1:-1);if(kind.equals("mate"))mate=n;else cp=n; }
            else if(p[i].equals("pv")) { while(++i<p.length && pv.size()<8)pv.add(p[i]);break; }
        }
        return new Line(rank,depth,cp,mate,Collections.unmodifiableList(new ArrayList<>(pv)));
    }
    public void close() { closed=true;try{input.close();}catch(IOException ignored){}process.destroy(); }
}
