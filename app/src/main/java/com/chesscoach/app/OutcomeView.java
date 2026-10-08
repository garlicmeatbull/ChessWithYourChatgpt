package com.chesscoach.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;

/** Persistent, prominent result banner rather than a transient message. */
final class OutcomeView extends LinearLayout {
    private final TextView title,detail;
    OutcomeView(Context c){super(c);setOrientation(VERTICAL);setPadding(Ui.dp(c,16),Ui.dp(c,12),Ui.dp(c,16),Ui.dp(c,12));title=Ui.text(c,"",24,true);detail=Ui.text(c,"",12,false);addView(title);addView(detail);setVisibility(GONE);setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);}
    void compact(){setPadding(Ui.dp(getContext(),12),0,Ui.dp(getContext(),12),0);setOrientation(HORIZONTAL);setGravity(android.view.Gravity.CENTER_VERTICAL);title.setTextSize(22);detail.setTextSize(11);detail.setPadding(Ui.dp(getContext(),12),0,0,0);detail.setMaxLines(1);detail.setEllipsize(android.text.TextUtils.TruncateAt.END);detail.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}
    void bind(GameOutcome outcome){
        if(outcome==null){setVisibility(GONE);return;}
        setVisibility(VISIBLE);Context c=getContext();boolean win=outcome.kind()==GameOutcome.Kind.WIN,loss=outcome.kind()==GameOutcome.Kind.LOSS;
        int color=c.getColor(win?R.color.success:loss?R.color.danger:R.color.accent),tint=c.getColor(win?R.color.success_surface:loss?R.color.danger_surface:R.color.accent_surface);
        GradientDrawable bg=Ui.surface(c,16);bg.setColor(tint);bg.setStroke(Ui.dp(c,1),color);setBackground(bg);
        title.setText((win?"✓  ":loss?"×  ":"=  ")+outcome.title());title.setTextColor(color);detail.setText(outcome.detail());detail.setTextColor(c.getColor(R.color.ink));setContentDescription(outcome.title()+"。"+outcome.detail());
    }
}
