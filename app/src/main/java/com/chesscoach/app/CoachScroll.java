package com.chesscoach.app;
import android.content.Context;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** Coalesce live changes while dragging/flinging; never replace text under a moving finger. */
final class CoachScroll extends ScrollView {
    private boolean moving;private Runnable pending;
    private final Runnable settle=()->{moving=false;Runnable latest=pending;pending=null;if(latest!=null)latest.run();};
    CoachScroll(Context context){super(context);setFillViewport(false);}
    void reset(){removeCallbacks(settle);pending=null;moving=false;scrollTo(0,0);}
    void apply(Runnable latest){if(moving)pending=latest;else latest.run();}
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_DOWN){moving=true;removeCallbacks(settle);}boolean handled=super.dispatchTouchEvent(event);if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){removeCallbacks(settle);postDelayed(settle,220);}return handled;}
    @Override protected void onScrollChanged(int x,int y,int oldX,int oldY){super.onScrollChanged(x,y,oldX,oldY);if(moving){removeCallbacks(settle);postDelayed(settle,220);}}
    @Override protected void onDetachedFromWindow(){removeCallbacks(settle);pending=null;moving=false;super.onDetachedFromWindow();}
}
