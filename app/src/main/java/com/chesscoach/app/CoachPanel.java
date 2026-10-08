package com.chesscoach.app;

import android.app.*;
import android.view.*;
import android.widget.*;

/** Shared compact summary and live detail dialog, using one surface in either system theme. */
final class CoachPanel {
    private final Activity activity;
    private final TextView headline,summary,state;
    private final Button more;
    private Dialog dialog;
    private TextView detailText,detailState;
    private String key="",full="";
    private Runnable explain;
    private boolean loading;
    CoachPanel(Activity a,LinearLayout parent){
        activity=a;headline=Ui.text(a,"",16,true);headline.setMaxLines(2);headline.setEllipsize(android.text.TextUtils.TruncateAt.END);headline.setVisibility(View.GONE);parent.addView(headline);summary=Ui.text(a,"",14,false);summary.setMinHeight(Ui.dp(a,38));summary.setMaxLines(3);summary.setEllipsize(android.text.TextUtils.TruncateAt.END);parent.addView(summary);
        LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
        state=Ui.text(a,"",11,false);state.setTextColor(a.getColor(R.color.muted));row.addView(state,new LinearLayout.LayoutParams(0,-2,1));
        more=Ui.button(a,"자세히 보기",this::open);more.setTextSize(12);more.setMinHeight(Ui.dp(a,40));row.addView(more,new LinearLayout.LayoutParams(-2,Ui.dp(a,40)));parent.addView(row);
    }
    TextView textView(){return summary;}
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
    void close(){if(dialog!=null){Dialog old=dialog;dialog=null;old.dismiss();detailText=detailState=null;}}
}
