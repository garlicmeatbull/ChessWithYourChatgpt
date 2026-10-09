package com.chesscoach.app;
import android.content.Context;
import android.graphics.*;
import android.view.Gravity;
import android.widget.TextView;
/** Horizontal engine evaluation below the board, including a readable numeric score. */
final class EvaluationBar extends TextView {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private double share=.5;
    EvaluationBar(Context context){super(context);setTextSize(12);setTextColor(context.getColor(R.color.muted));setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL);setPadding(0,0,0,Ui.dp(context,12));setText("평가 대기 · 백 기준");setContentDescription("Stockfish 평가 대기. 바의 길이는 승률이 아닙니다.");}
    void bind(Stockfish.Line line){if(line==null){share=.5;setText("평가 대기 · 백 기준");}else{share=EvaluationScale.whiteShare(line.cp(),line.mate());setText("백 기준 "+line.score()+" · 깊이 "+line.depth());}setContentDescription(getText()+". 흰색은 백, 검은색은 흑의 평가입니다. 바의 길이는 승률이 아닙니다.");invalidate();}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float top=getHeight()-Ui.dp(getContext(),10),bottom=getHeight()-Ui.dp(getContext(),2),radius=Ui.dp(getContext(),4);RectF bar=new RectF(0,top,getWidth(),bottom);paint.setColor(0xFF24262D);canvas.drawRoundRect(bar,radius,radius,paint);canvas.save();Path clip=new Path();clip.addRoundRect(bar,radius,radius,Path.Direction.CW);canvas.clipPath(clip);paint.setColor(0xFFF1F2F5);canvas.drawRect(0,top,(float)(getWidth()*share),bottom,paint);canvas.restore();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(getResources().getDisplayMetrics().density);paint.setColor(getContext().getColor(R.color.border));canvas.drawRoundRect(bar,radius,radius,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(0xFF8E96A3);canvas.drawRect(getWidth()/2f-.5f,top,getWidth()/2f+.5f,bottom,paint);}
}
