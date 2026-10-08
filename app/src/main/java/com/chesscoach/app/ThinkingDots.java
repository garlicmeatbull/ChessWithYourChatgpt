package com.chesscoach.app;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** Small elastic triangular orbit, with no accompanying status label. */
final class ThinkingDots extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private boolean active;
    ThinkingDots(Context c){super(c);setContentDescription("AI가 조언을 작성하고 있어요");setVisibility(INVISIBLE);}
    void active(boolean value){active=value;setVisibility(value?VISIBLE:INVISIBLE);invalidate();}
    @Override protected void onDraw(Canvas canvas){if(!active)return;float d=getResources().getDisplayMetrics().density;boolean dark=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;
        // White dots retain contrast on the white theme inside a quiet gray disc.
        if(!dark){paint.setColor(0xFF667085);canvas.drawCircle(getWidth()/2f,getHeight()/2f,10*d,paint);}paint.setColor(Color.WHITE);
        double t=(SystemClock.uptimeMillis()%1800)/1800.0,angle=2*Math.PI*(t-Math.sin(2*Math.PI*t)*.10);float radius=(float)(5.2+.6*Math.sin(4*Math.PI*t))*d;
        for(int i=0;i<3;i++){double a=angle+i*2*Math.PI/3-Math.PI/2;paint.setAlpha(150+(int)(70*(1+Math.sin(a))/2));canvas.drawCircle(getWidth()/2f+(float)Math.cos(a)*radius,getHeight()/2f+(float)Math.sin(a)*radius,1.65f*d,paint);}
        if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postInvalidateOnAnimation();
    }
}
