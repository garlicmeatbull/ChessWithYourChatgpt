package com.chesscoach.app;
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class Ui {
    public static int dp(Context c,int n){return (int)(n*c.getResources().getDisplayMetrics().density);}
    public static TextView text(Context c,String value,int size,boolean bold){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(c.getColor(R.color.ink));if(bold)t.setTypeface(null,Typeface.BOLD);t.setLineSpacing(dp(c,3),1);return t;}
    public static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static GradientDrawable surface(Context c,int radius){GradientDrawable bg=new GradientDrawable();bg.setColor(c.getColor(R.color.surface));bg.setCornerRadius(dp(c,radius));return bg;}
    public static LinearLayout card(Context c){LinearLayout l=column(c);l.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,16));l.setBackground(surface(c,20));return l;}
    public static void gap(LinearLayout l,int h){View v=new View(l.getContext());l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),h)));}
    public static LinearLayout screen(Activity a,boolean scroll){LinearLayout root=column(a);root.setBackgroundColor(a.getColor(R.color.background));root.setPadding(dp(a,20),dp(a,12),dp(a,20),dp(a,16));if(scroll){ScrollView s=new ScrollView(a);s.setFillViewport(true);s.setBackgroundColor(a.getColor(R.color.background));s.addView(root);a.setContentView(s);}else a.setContentView(root);root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(a,20),i.getSystemWindowInsetTop()+dp(a,12),dp(a,20),i.getSystemWindowInsetBottom()+dp(a,16));return i;});return root;}
    public static Button button(Context c,String label,Runnable action){Button b=new Button(c);b.setText(label);b.setTextColor(c.getColor(R.color.ink));b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(c,48));b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x334C9670),surface(c,14),null));b.setOnClickListener(v->action.run());return b;}
    public static Button primary(Context c,String label,Runnable action){Button b=button(c,label,action);GradientDrawable bg=surface(c,14);bg.setColor(0xFF286B50);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),bg,null));b.setTextColor(0xFFFFFFFF);return b;}
    public static void header(Activity a,LinearLayout root,String title){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(button(a,"‹",a::finish),new LinearLayout.LayoutParams(dp(a,44),dp(a,44)));TextView t=text(a,title,22,true);t.setPadding(dp(a,12),0,0,0);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);gap(root,20);}
    public static String model(Context c){String m=c.getSharedPreferences("coach",0).getString("model","gpt-5.4");return m==null||m.trim().isEmpty()?"gpt-5.4":m;}
    public static String modelLabel(Context c){return model(c).replace("gpt-","GPT-");}
    public static void models(Activity a,Runnable changed){
        Dialog dialog=new Dialog(a);LinearLayout sheet=card(a);sheet.addView(text(a,"코치 모델",22,true));gap(sheet,6);TextView note=text(a,"선택은 즉시 적용됩니다. 진행 중인 해설은 마친 뒤 새 모델의 해설을 요청합니다.",13,false);note.setTextColor(a.getColor(R.color.muted));sheet.addView(note);gap(sheet,16);
        String saved=a.getSharedPreferences("coach",0).getString("models","");List<String> models=saved.trim().isEmpty()?Arrays.asList("gpt-5.4","gpt-5.3-codex"):Arrays.asList(saved.split(","));
        for(String m:models){Button row=button(a,m.replace("gpt-","GPT-")+(m.equals(model(a))?"  ✓":""),()->{a.getSharedPreferences("coach",0).edit().putString("model",m).apply();dialog.dismiss();changed.run();});row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setPadding(dp(a,16),0,dp(a,16),0);sheet.addView(row,new LinearLayout.LayoutParams(-1,dp(a,56)));gap(sheet,8);}
        sheet.addView(button(a,"AI 코치 연결 관리",()->{dialog.dismiss();a.startActivity(new Intent(a,ConnectionActivity.class));}));dialog.setContentView(sheet);dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}
    }
    public static void legend(Activity a){new AlertDialog.Builder(a).setTitle("판 위의 수 평가").setMessage("★ 초록: 최선수\n✓ 연두: 좋은 수\n?! 노랑: 아쉬운 수\n? 주황: 실수\n?? 빨강: 블런더\n!! 청록: 희생에 보상이 있는 탁월 후보\n\n초록 화살표는 Stockfish 추천 이동, 회색 화살표는 실전 이동입니다. 기호를 함께 표시해 색상만으로 구분하지 않아도 됩니다.\n탁월 후보는 계산 깊이와 희생 확인에 기반한 추정이며 확정 판정은 아닙니다.").setPositiveButton("확인",null).show();}
}
