package com.chesscoach.app;

import android.app.*;
import android.view.*;
import android.widget.*;

/** Shared scrollable strategic coaching and a larger reading dialog, using one surface in either system theme. */
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
    private boolean loading,waiting,canExplain;private final ThinkingDots dots;
    private final TextView visualLink;
    private java.util.List<CoachVisual.Scene> scenes=java.util.Collections.emptyList();
    private String visualAdvice="";
    private Dialog visualDialog;
    CoachPanel(Activity a,LinearLayout parent){
        activity=a;this.parent=parent;headline=Ui.text(a,"",15,true);headline.setVisibility(View.GONE);parent.addView(headline);summary=Ui.text(a,"",15,false);summary.setLineSpacing(Ui.dp(a,5),1);ScrollView reading=new ScrollView(a);reading.setVerticalScrollBarEnabled(true);reading.addView(summary);parent.addView(reading,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout row=new LinearLayout(a);actions=row;row.setGravity(Gravity.CENTER_VERTICAL);
        dots=new ThinkingDots(a);row.addView(dots,new LinearLayout.LayoutParams(Ui.dp(a,24),Ui.dp(a,32)));state=Ui.text(a,"",11,false);state.setTextColor(a.getColor(R.color.muted));state.setMaxLines(1);state.setEllipsize(android.text.TextUtils.TruncateAt.END);state.setVisibility(View.INVISIBLE);row.addView(state,new LinearLayout.LayoutParams(0,-2,1));
        visualLink=Ui.link(a,"↗ AI 수 보기",()->{if(!scenes.isEmpty()){if(visualDialog!=null)visualDialog.dismiss();visualDialog=CoachVisualDialog.show(activity,scenes,visualAdvice);}});visualLink.setTextColor(a.getColor(R.color.ai));visualLink.setTextSize(12);visualLink.setVisibility(View.GONE);row.addView(visualLink,new LinearLayout.LayoutParams(Ui.dp(a,88),Ui.dp(a,40)));
        more=Ui.link(a,"크게 보기",this::open);more.setTextSize(12);more.setMinHeight(Ui.dp(a,40));row.addView(more,new LinearLayout.LayoutParams(Ui.dp(a,76),Ui.dp(a,40)));parent.addView(row);parent.setOnClickListener(v->open());headline.setOnClickListener(v->open());summary.setOnClickListener(v->open());state.setOnClickListener(v->open());
    }
    TextView textView(){return summary;}
    void visuals(java.util.List<CoachVisual.Scene> value,String advice){scenes=value;visualAdvice=advice;visualLink.setVisibility(value.isEmpty()?View.GONE:View.VISIBLE);if(!value.isEmpty())visualLink.setContentDescription("AI 수 보기 · "+value.get(0).san()+" · "+value.size()+"개 국면");}
    void automation(android.content.SharedPreferences prefs,Runnable manual){
        preferences=prefs;automatic=new Switch(activity);automatic.setText("AI 자동");automatic.setContentDescription("AI 자동 코칭");automatic.setTextSize(12);automatic.setTextColor(activity.getColor(R.color.ink));automatic.setMinHeight(Ui.dp(activity,44));automatic.setSwitchPadding(Ui.dp(activity,6));
        LinearLayout header=(LinearLayout)parent.getChildAt(0);header.removeViewAt(0);header.addView(automatic,0,new LinearLayout.LayoutParams(0,-2,1));
        judge=Ui.link(activity,"AI 판단",manual);judge.setTextSize(12);judge.setMinHeight(Ui.dp(activity,40));actions.addView(judge,1,new LinearLayout.LayoutParams(Ui.dp(activity,72),Ui.dp(activity,40)));
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
        bind(identity,core,shortText,markdown,pending,available,false,request);
    }
    void bind(String identity,String core,String shortText,String markdown,boolean pending,boolean available,boolean hasOutput,Runnable request){
        if(!key.equals(identity)){close();key=identity;}
        explain=request;full=markdown;loading=pending;waiting=pending&&!hasOutput;canExplain=available;dots.active(waiting);
        headline.setVisibility(View.GONE);
        summary.setText(CoachMarkdown.render(markdown.isEmpty()?shortText:markdown));state.setText("");
        more.setEnabled(true);more.setAlpha(available||pending?1f:.6f);
        updateDialog();
    }
    private void open(){
        if(dialog!=null&&dialog.isShowing())return;
        dialog=new Dialog(activity);LinearLayout content=Ui.card(activity);content.setPadding(Ui.dp(activity,24),Ui.dp(activity,18),Ui.dp(activity,24),Ui.dp(activity,24));
        LinearLayout title=new LinearLayout(activity);title.setGravity(Gravity.CENTER_VERTICAL);
        title.addView(Ui.text(activity,"전략 코칭",20,true),new LinearLayout.LayoutParams(0,-2,1));
        title.addView(Ui.link(activity,"×",this::close),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));content.addView(title);
        detailState=Ui.text(activity,"",12,false);detailState.setTextColor(activity.getColor(R.color.muted));content.addView(detailState);Ui.gap(content,12);
        ScrollView scroll=new ScrollView(activity);detailText=Ui.text(activity,"",15,false);detailText.setLineSpacing(Ui.dp(activity,6),1);detailText.setTextIsSelectable(true);scroll.addView(detailText);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));dialog.setContentView(content);Dialog opened=dialog;dialog.setOnDismissListener(d->{if(dialog==opened){dialog=null;detailText=detailState=null;}});
        dialog.show();Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawableResource(android.R.color.transparent);w.setLayout(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,40),(int)(activity.getResources().getDisplayMetrics().heightPixels*.72));}
        updateDialog();
    }
    private void updateDialog(){if(detailText!=null){detailText.setText(CoachMarkdown.render(full));detailState.setText(waiting?"조언을 기다리고 있어요":"");detailState.setVisibility(waiting?View.VISIBLE:View.GONE);}}
    void close(){if(visualDialog!=null){visualDialog.dismiss();visualDialog=null;}if(dialog!=null){Dialog old=dialog;dialog=null;old.dismiss();detailText=detailState=null;}}
}
