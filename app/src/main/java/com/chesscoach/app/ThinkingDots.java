package com.chesscoach.app;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
/** Three staggered pulses; only draws while attached and a request is pending. */
final class ThinkingDots extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private boolean active;
    ThinkingDots(Context c){super(c);setContentDescription("AI가 조언을 작성하고 있어요");}
    void active(boolean value){active=value;setVisibility(value?VISIBLE:INVISIBLE);invalidate();}
    @Override protected void onDraw(Canvas canvas){if(!active)return;paint.setColor(getContext().getColor(R.color.ai));float d=getResources().getDisplayMetrics().density;double phase=SystemClock.uptimeMillis()/170.0;for(int i=0;i<3;i++){float wave=(float)((Math.sin(phase-i*.85)+1)/2);paint.setAlpha((int)(85+170*wave));canvas.drawCircle((6+i*10)*d,getHeight()/2f-wave*3*d,2.3f*d,paint);}if(isAttachedToWindow()&&getWindowVisibility()==VISIBLE)postInvalidateOnAnimation();}
}
