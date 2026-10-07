package com.chesscoach.app;

import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
import java.util.function.IntConsumer;

public final class BoardView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public Chess board=new Chess();
    public int selected=-1,lastFrom=-1,lastTo=-1;
    public Set<Integer> targets=new HashSet<>();
    public IntConsumer onSquare;
    private static final String PIECES="KQRBNPkqrbnp";
    private static final String[] GLYPHS={"♔","♕","♖","♗","♘","♙","♚","♛","♜","♝","♞","♟"};
    public BoardView(Context c){super(c);setContentDescription("체스판. 백은 아래쪽. 기물과 목적지 칸을 차례로 누르세요.");setFocusable(true);}
    @Override protected void onMeasure(int w,int h){int size=Math.min(MeasureSpec.getSize(w),MeasureSpec.getSize(h));setMeasuredDimension(size,size);}
    @Override protected void onDraw(Canvas c) {
        float tile=getWidth()/8f;
        for(int r=0;r<8;r++)for(int f=0;f<8;f++) {
            int s=(7-r)*8+f;float x=f*tile,y=r*tile;
            paint.setColor((f+r)%2==0?Color.rgb(223,229,219):Color.rgb(91,126,113));c.drawRect(x,y,x+tile,y+tile,paint);
            if(s==lastFrom||s==lastTo||s==selected){paint.setColor(s==selected?0x99FFC861:0x668CDAB4);c.drawRect(x,y,x+tile,y+tile,paint);}
            int i=PIECES.indexOf(board.squares[s]);
            if(i>=0) {
                paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(tile*.78f);
                float baseline=y+tile*.77f;
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(tile*.035f);paint.setColor(i<6?0xFF202E2A:0xFFE6EEE7);c.drawText(GLYPHS[i],x+tile/2,baseline,paint);
                paint.setStyle(Paint.Style.FILL);paint.setColor(i<6?0xFFFFFBF0:0xFF162B25);c.drawText(GLYPHS[i],x+tile/2,baseline,paint);
            }
            if(targets.contains(s)){paint.setColor(0xAA173F31);c.drawCircle(x+tile/2,y+tile/2,tile*.12f,paint);}
            paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(tile*.16f);paint.setColor((f+r)%2==0?0xFF405E52:0xFFE4EEE6);
            if(f==0)c.drawText(""+(8-r),x+2,y+tile*.18f,paint);
            if(r==7)c.drawText(""+(char)('a'+f),x+tile*.81f,y+tile*.94f,paint);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        int f=(int)(e.getX()/(getWidth()/8f)),r=(int)(e.getY()/(getHeight()/8f));
        if(f>=0&&f<8&&r>=0&&r<8&&onSquare!=null){performClick();onSquare.accept((7-r)*8+f);}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
