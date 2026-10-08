package com.chesscoach.app;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import java.util.*;
import java.util.function.IntConsumer;
public final class BoardView extends View {
    private android.animation.ValueAnimator motion;private Chess movingBefore;private Chess.Move movingMove;private String movingAfter;private float fraction;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public Chess board=new Chess();public int selected=-1,lastFrom=-1,lastTo=-1,judgedSquare=-1;
    public MoveJudgment.Kind judgment=MoveJudgment.Kind.UNKNOWN;
    public String recommendation,playedArrow,aiArrow;
    public Set<Integer> targets=new HashSet<>();public IntConsumer onSquare;
    public BoardView(Context c){super(c);setContentDescription("체스판. 백은 아래쪽. 기물과 목적지 칸을 차례로 누르세요.");setFocusable(true);}
    public void animateMove(Chess before,Chess.Move move,Chess after){
        if(motion!=null)motion.cancel();movingMove=null;
        if(!getContext().getSharedPreferences("appearance",0).getBoolean("animation",false))return;
        movingBefore=before.copy();movingMove=move;movingAfter=after.fen();fraction=0;
        android.animation.ValueAnimator next=android.animation.ValueAnimator.ofFloat(0,1);motion=next;next.setDuration(180);next.setInterpolator(new android.view.animation.DecelerateInterpolator());next.addUpdateListener(v->{fraction=(float)v.getAnimatedValue();invalidate();});next.addListener(new android.animation.AnimatorListenerAdapter(){@Override public void onAnimationEnd(android.animation.Animator a){if(motion==a){movingMove=null;motion=null;invalidate();}}});next.start();
    }
    private boolean moving(){return movingMove!=null&&board.fen().equals(movingAfter);}
    private boolean rookMove(){return moving()&&Character.toLowerCase(movingBefore.squares[movingMove.from()])=='k'&&Math.abs(movingMove.to()-movingMove.from())==2;}
    private int rookFrom(){return movingMove.from()/8*8+(movingMove.to()>movingMove.from()?7:0);}
    private int rookTo(){return movingMove.from()/8*8+(movingMove.to()>movingMove.from()?5:3);}
    private void drawMoving(Canvas c,int from,int to,char piece,float tile){float x=(from%8+(to%8-from%8)*fraction)*tile,y=(7-from/8+(from/8-to/8)*fraction)*tile;PieceRenderer.draw(c,piece,x,y,tile,1);}
    @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();super.onDetachedFromWindow();}
    /** The move that led to the displayed position; null clears starting positions. */
    public void setLastMove(Chess.Move move){lastFrom=move==null?-1:move.from();lastTo=move==null?-1:move.to();invalidate();}
    @Override protected void onMeasure(int w,int h){int height=MeasureSpec.getMode(h)==MeasureSpec.UNSPECIFIED?MeasureSpec.getSize(w):MeasureSpec.getSize(h);int size=Math.min(MeasureSpec.getSize(w),height);setMeasuredDimension(size,size);}
    @Override protected void onDraw(Canvas c){float tile=getWidth()/8f;for(int r=0;r<8;r++)for(int f=0;f<8;f++){int s=(7-r)*8+f;float x=f*tile,y=r*tile;paint.setColor(getContext().getColor((f+r)%2==0?R.color.board_light:R.color.board_dark));c.drawRect(x,y,x+tile,y+tile,paint);if(s==lastFrom||s==lastTo){paint.setColor(0x99F4D35E);c.drawRect(x,y,x+tile,y+tile,paint);}if(s==selected){paint.setColor(0xAAE7BF45);c.drawRect(x,y,x+tile,y+tile,paint);}if(!moving()||s!=movingMove.to()&&(!rookMove()||s!=rookTo()))PieceRenderer.draw(c,board.squares[s],x,y,tile,1);if(targets.contains(s)){paint.setColor(0x882C5945);if(board.squares[s]=='.')c.drawCircle(x+tile/2,y+tile/2,tile*.11f,paint);else{paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(tile*.06f);c.drawCircle(x+tile/2,y+tile/2,tile*.43f,paint);paint.setStyle(Paint.Style.FILL);}}paint.setTypeface(Typeface.DEFAULT);paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(tile*.16f);paint.setColor((f+r)%2==0?0xFF354A49:getContext().getColor(R.color.ink));if(f==0)c.drawText(""+(8-r),x+tile*.04f,y+tile*.18f,paint);if(r==7)c.drawText(""+(char)('a'+f),x+tile*.80f,y+tile*.95f,paint);}
        if(moving()){drawMoving(c,movingMove.from(),movingMove.to(),movingBefore.squares[movingMove.from()],tile);if(rookMove())drawMoving(c,rookFrom(),rookTo(),movingBefore.squares[rookFrom()],tile);}
        if(playedArrow!=null&&!playedArrow.equals(recommendation))arrow(c,playedArrow,0x995B6E68,tile);if(recommendation!=null)arrow(c,recommendation,0xD24C9E43,tile);
        if(aiArrow!=null){arrow(c,aiArrow,getContext().getColor(R.color.ai),tile);aiLabel(c,aiArrow,tile);}
        if(judgedSquare>=0&&judgment!=MoveJudgment.Kind.UNKNOWN){float x=(judgedSquare%8+.80f)*tile,y=(7-judgedSquare/8+.20f)*tile;paint.setColor(judgment.color);c.drawCircle(x,y,tile*.20f,paint);paint.setColor(judgment==MoveJudgment.Kind.INACCURACY?0xFF253B2D:Color.WHITE);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(tile*.22f);c.drawText(judgment.symbol,x,y+tile*.075f,paint);}
    }
    private void aiLabel(Canvas c,String u,float tile){try{Chess.Move m=Chess.Move.parse(u);float x=(m.from()%8+m.to()%8+1)*tile/2,y=(15-m.from()/8-m.to()/8)*tile/2;float dx=m.to()%8-m.from()%8,dy=m.from()/8-m.to()/8,len=(float)Math.hypot(dx,dy);if(len==0)return;x-=dy/len*tile*.24f;y+=dx/len*tile*.24f;x=Math.max(tile*.25f,Math.min(getWidth()-tile*.25f,x));y=Math.max(tile*.15f,Math.min(getHeight()-tile*.15f,y));paint.setStyle(Paint.Style.FILL);paint.setColor(getContext().getColor(R.color.ai));c.drawRoundRect(x-tile*.23f,y-tile*.14f,x+tile*.23f,y+tile*.14f,tile*.08f,tile*.08f,paint);paint.setColor(getContext().getColor(R.color.on_accent));paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(tile*.18f);paint.setTextAlign(Paint.Align.CENTER);c.drawText("AI",x,y+tile*.065f,paint);}catch(IllegalArgumentException ignored){}}
    private void arrow(Canvas c,String u,int color,float tile){try{Chess.Move m=Chess.Move.parse(u);float x=(m.from()%8+.5f)*tile,y=(7-m.from()/8+.5f)*tile,tx=(m.to()%8+.5f)*tile,ty=(7-m.to()/8+.5f)*tile,dx=tx-x,dy=ty-y,len=(float)Math.hypot(dx,dy);if(len<1)return;dx/=len;dy/=len;x+=dx*tile*.23f;y+=dy*tile*.23f;tx-=dx*tile*.10f;ty-=dy*tile*.10f;paint.setColor(color);paint.setStrokeWidth(tile*.13f);paint.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,tx-dx*tile*.23f,ty-dy*tile*.23f,paint);Path head=new Path();head.moveTo(tx,ty);head.lineTo(tx-dx*tile*.36f-dy*tile*.18f,ty-dy*tile*.36f+dx*tile*.18f);head.lineTo(tx-dx*tile*.36f+dy*tile*.18f,ty-dy*tile*.36f-dx*tile*.18f);head.close();c.drawPath(head,paint);}catch(Exception ignored){}}
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;int f=(int)(e.getX()/(getWidth()/8f)),r=(int)(e.getY()/(getHeight()/8f));if(e.getX()>=0&&e.getY()>=0&&f<8&&r<8&&onSquare!=null){performClick();onSquare.accept((7-r)*8+f);}return true;}
    @Override public boolean performClick(){super.performClick();return true;}
}
