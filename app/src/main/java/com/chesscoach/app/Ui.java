package com.chesscoach.app;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.widget.Button;
public final class Ui {
    public static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density);}
    public static Button button(Context c,String label,Runnable action){Button b=new Button(c);b.setText(label);b.setTextColor(c.getColor(R.color.ink));b.setTextSize(13);b.setAllCaps(false);b.setMinHeight(dp(c,44));GradientDrawable surface=new GradientDrawable();surface.setColor(c.getColor(R.color.surface));surface.setCornerRadius(dp(c,12));b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x334C9670),surface,null));b.setOnClickListener(v->action.run());return b;}
}
