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
    public static LinearLayout card(Context c){LinearLayout l=column(c);l.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,16));GradientDrawable bg=surface(c,18);bg.setStroke(dp(c,1),c.getColor(R.color.border));l.setBackground(bg);return l;}
    public static void gap(LinearLayout l,int h){View v=new View(l.getContext());l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),h)));}
    public static LinearLayout screen(Activity a,boolean scroll){LinearLayout root=column(a);root.setBackgroundColor(a.getColor(R.color.background));root.setPadding(dp(a,20),dp(a,12),dp(a,20),dp(a,16));if(scroll){ScrollView s=new ScrollView(a);s.setFillViewport(true);s.setBackgroundColor(a.getColor(R.color.background));s.addView(root);a.setContentView(s);}else a.setContentView(root);root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(a,20),i.getSystemWindowInsetTop()+dp(a,12),dp(a,20),i.getSystemWindowInsetBottom()+dp(a,16));return i;});return root;}
    public static Button button(Context c,String label,Runnable action){Button b=new Button(c);b.setText(label);b.setTextColor(c.getColor(R.color.ink));b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(c,48));b.setMinimumWidth(0);b.setPadding(dp(c,12),0,dp(c,12),0);b.setTypeface(null,Typeface.BOLD);GradientDrawable bg=surface(c,12);bg.setColor(c.getColor(R.color.secondary));b.setBackground(new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.accent_surface)),bg,null));b.setOnClickListener(v->action.run());return b;}
    public static Button primary(Context c,String label,Runnable action){Button b=button(c,label,action);selected(c,b,true);return b;}
    public static void selected(Context c,Button button,boolean selected){GradientDrawable bg=surface(c,12);bg.setColor(c.getColor(selected?R.color.accent:R.color.secondary));button.setBackground(new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.accent_surface)),bg,null));button.setTextColor(c.getColor(selected?R.color.on_accent:R.color.ink));button.setSelected(selected);}
    public static void input(EditText input){Context c=input.getContext();input.setTextColor(c.getColor(R.color.ink));input.setHintTextColor(c.getColor(R.color.muted));GradientDrawable bg=surface(c,12);bg.setColor(c.getColor(R.color.secondary));bg.setStroke(dp(c,1),c.getColor(R.color.border));input.setBackground(bg);input.setPadding(dp(c,14),dp(c,14),dp(c,14),dp(c,14));input.setMinHeight(dp(c,52));}
    public static void header(Activity a,LinearLayout root,String title){LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.addView(button(a,"‹",a::finish),new LinearLayout.LayoutParams(dp(a,44),dp(a,44)));TextView t=text(a,title,22,true);t.setPadding(dp(a,12),0,0,0);row.addView(t,new LinearLayout.LayoutParams(0,-2,1));root.addView(row);gap(root,20);}
    public static void usage(Activity a){try{a.startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(ChatGptProtocol.USAGE)));}catch(ActivityNotFoundException e){Toast.makeText(a,"시스템 브라우저를 확인하세요.",Toast.LENGTH_SHORT).show();}}
    public static String model(Context c){return c.getSharedPreferences("coach",0).getString("model","");}
    public static String modelLabel(Context c){String selected=model(c);try{org.json.JSONArray catalog=new org.json.JSONArray(c.getSharedPreferences("coach",0).getString("modelCatalog","[]"));for(int i=0;i<catalog.length();i++){org.json.JSONObject m=catalog.getJSONObject(i);if(selected.equals(m.getString("slug")))return m.getString("display_name");}}catch(Exception ignored){}return selected.isEmpty()?"모델 선택":selected.replace("gpt-","GPT-");}
    public static void models(Activity a,Runnable changed){
        Dialog dialog=new Dialog(a);LinearLayout sheet=card(a);sheet.addView(text(a,"코치 모델",22,true));gap(sheet,6);
        TextView note=text(a,ChatGptAccounts.connected(a)?"Using ChatGPT plan · 선택은 즉시 적용됩니다.":"ChatGPT 계정 연결 후 사용 가능한 모델을 조회하세요.",13,false);note.setTextColor(a.getColor(R.color.muted));sheet.addView(note);gap(sheet,12);
        ScrollView scroll=new ScrollView(a);LinearLayout rows=column(a);scroll.addView(rows);sheet.addView(scroll,new LinearLayout.LayoutParams(-1,Math.min(dp(a,320),a.getResources().getDisplayMetrics().heightPixels/2)));
        try{org.json.JSONArray catalog=new org.json.JSONArray(a.getSharedPreferences("coach",0).getString("modelCatalog","[]"));for(int i=0;i<catalog.length();i++){org.json.JSONObject m=catalog.getJSONObject(i);String slug=m.getString("slug");Button row=button(a,m.getString("display_name")+(slug.equals(model(a))?"  ✓":""),()->{a.getSharedPreferences("coach",0).edit().putString("model",slug).apply();dialog.dismiss();changed.run();});row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setPadding(dp(a,16),0,dp(a,16),0);rows.addView(row,new LinearLayout.LayoutParams(-1,dp(a,56)));gap(rows,8);}}catch(Exception ignored){}
        sheet.addView(button(a,"Manage usage · 사용량 관리",()->usage(a)));gap(sheet,8);sheet.addView(button(a,"ChatGPT 계정 · 모델 새로 조회",()->{dialog.dismiss();a.startActivity(new Intent(a,ConnectionActivity.class));}));dialog.setContentView(sheet);dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setGravity(Gravity.BOTTOM);w.setLayout(-1,-2);}
    }
    public static void legend(Activity a){new AlertDialog.Builder(a).setTitle("판 위의 수 평가").setMessage("노란 두 칸: 직전 수의 출발·도착 칸\n색상 배지: 해당 수의 평가\n\n★ 초록: 최선수\n✓ 연두: 좋은 수\n?! 노랑: 아쉬운 수\n? 주황: 실수\n?? 빨강: 블런더\n!! 청록: 희생에 보상이 있는 탁월 후보\n\n초록 화살표는 Stockfish 추천 이동, 회색 화살표는 실전 이동입니다. 기호를 함께 표시해 색상만으로 구분하지 않아도 됩니다.\n탁월 후보는 계산 깊이와 희생 확인에 기반한 추정이며 확정 판정은 아닙니다.").setPositiveButton("확인",null).show();}
}
