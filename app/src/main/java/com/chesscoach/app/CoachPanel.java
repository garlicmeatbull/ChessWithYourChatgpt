package com.chesscoach.app;

import android.app.*;
import android.view.*;
import android.widget.*;

/** Shared compact summary and live detail dialog, using one surface in either system theme. */
final class CoachPanel {
    private final Activity activity;
    private final TextView headline,summary,state;
    private final Button more;
    private final LinearLayout parent,actions;
    private Switch automatic;
    private Button judge;
    private android.content.SharedPreferences preferences;
    private boolean syncing;
    private Dialog dialog;
    private TextView detailText,detailState;
    private String key="",full="";
    private Runnable explain;
    private boolean loading;
    private final TextView visualLink;
    private java.util.List<CoachVisual.Scene> scenes=java.util.Collections.emptyList();
    private String visualAdvice="";
    private Dialog visualDialog;
    CoachPanel(Activity a,LinearLayout parent){
        activity=a;this.parent=parent;headline=Ui.text(a,"",16,true);headline.setMaxLines(2);headline.setEllipsize(android.text.TextUtils.TruncateAt.END);headline.setVisibility(View.GONE);parent.addView(headline);summary=Ui.text(a,"",14,false);summary.setMinHeight(Ui.dp(a,38));summary.setMaxLines(3);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);parent.addView(summary);
        LinearLayout row=new LinearLayout(a);actions=row;row.setGravity(Gravity.CENTER_VERTICAL);
        state=Ui.text(a,"",11,false);state.setTextColor(a.getColor(R.color.muted));state.setMaxLines(1);state.setEllipsize(android.text.TextUtils.TruncateAt.END);row.addView(state,new LinearLayout.LayoutParams(0,-2,1));
        visualLink=Ui.button(a,"↗ AI 수 보기",()->{if(!scenes.isEmpty()){if(visualDialog!=null)visualDialog.dismiss();visualDialog=CoachVisualDialog.show(activity,scenes,visualAdvice);}});visualLink.setTextColor(a.getColor(R.color.ai));visualLink.setTextSize(12);visualLink.setVisibility(View.GONE);row.addView(visualLink,new LinearLayout.LayoutParams(Ui.dp(a,90),Ui.dp(a,40)));
        more=Ui.button(a,"자세히 보기",this::open);more.setTextSize(12);more.setMinHeight(Ui.dp(a,40));row.addView(more,new LinearLayout.LayoutParams(-2,Ui.dp(a,40)));parent.addView(row);
    }
    TextView textView(){return summary;}
    void visuals(java.util.List<CoachVisual.Scene> value,String advice){scenes=value;visualAdvice=advice;visualLink.setVisibility(value.isEmpty()?View.GONE:View.VISIBLE);if(!value.isEmpty())visualLink.setContentDescription("AI 수 보기 · "+value.get(0).san()+" · "+value.size()+"개 국면");}
    void automation(android.content.SharedPreferences prefs,Runnable manual){
        preferences=prefs;automatic=new Switch(activity);automatic.setText("AI 자동");automatic.setContentDescription("AI 자동 코칭");automatic.setTextSize(12);automatic.setTextColor(activity.getColor(R.color.ink));automatic.setMinHeight(Ui.dp(activity,44));automatic.setSwitchPadding(Ui.dp(activity,6));
        LinearLayout header=(LinearLayout)parent.getChildAt(0);header.removeViewAt(0);header.addView(automatic,0,new LinearLayout.LayoutParams(0,-2,1));
        judge=Ui.button(activity,"AI 판단",manual);judge.setTextSize(12);judge.setMinHeight(Ui.dp(activity,40));actions.addView(judge,1,new LinearLayout.LayoutParams(Ui.dp(activity,88),Ui.dp(activity,40)));
        automatic.setOnCheckedChangeListener((button,enabled)->{if(!syncing)preferences.edit().putBoolean("autoCoach",enabled).apply();});updateAutomation(false);
    }
    void updateAutomation(boolean ready){
        if(automatic==null)return;boolean enabled=preferences.getBoolean("autoCoach",false);syncing=true;automatic.setChecked(enabled);syncing=false;
        judge.setVisibility(enabled?View.GONE:View.VISIBLE);judge.setEnabled(ready);judge.setAlpha(ready?1f:.45f);
    }
    void bind(String identity,String shortText,String markdown,boolean pending,boolean available,Runnable request){
        bind(identity,"",shortText,markdown,pending,available,request);
    }
    void bind(String identity,String core,String shortText,String markdown,boolean pending,boolean available,Runnable request){
        if(!key.equals(identity)){close();key=identity;}
        explain=request;full=markdown;loading=pending;
        headline.setVisibility(core.isEmpty()?View.GONE:View.VISIBLE);headline.setText(CoachMarkdown.render(core));
        summary.setText(CoachMarkdown.render(shortText));state.setText(pending?"생성 중 · 실시간 표시":"");
        more.setEnabled(available);more.setAlpha(available?1f:.45f);
        updateDialog();
    }
    private void open(){
        if(dialog!=null&&dialog.isShowing())return;
        dialog=new Dialog(activity);LinearLayout content=Ui.card(activity);
        LinearLayout title=new LinearLayout(activity);title.setGravity(Gravity.CENTER_VERTICAL);
        title.addView(Ui.text(activity,"이번 수의 조언",22,true),new LinearLayout.LayoutParams(0,-2,1));
        title.addView(Ui.button(activity,"닫기",this::close),new LinearLayout.LayoutParams(-2,Ui.dp(activity,48)));content.addView(title);
        detailState=Ui.text(activity,"",12,false);detailState.setTextColor(activity.getColor(R.color.muted));content.addView(detailState);Ui.gap(content,12);
        ScrollView scroll=new ScrollView(activity);detailText=Ui.text(activity,"",16,false);detailText.setTextIsSelectable(true);scroll.addView(detailText);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(content);Dialog opened=dialog;dialog.setOnDismissListener(d->{if(dialog==opened){dialog=null;detailText=detailState=null;}});
        dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,32),(int)(activity.getResources().getDisplayMetrics().heightPixels*.75));}
        updateDialog();if(explain!=null)explain.run();
    }
    private void updateDialog(){if(detailText!=null){detailText.setText(CoachMarkdown.render(full));detailState.setText(loading?"상세 설명 생성 중 · 실시간 표시":"");}}
    void close(){if(visualDialog!=null){visualDialog.dismiss();visualDialog=null;}if(dialog!=null){Dialog old=dialog;dialog=null;old.dismiss();detailText=detailState=null;}}
}
