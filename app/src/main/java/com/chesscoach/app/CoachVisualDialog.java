package com.chesscoach.app;

import android.app.*;
import android.view.*;
import android.widget.*;
import java.util.List;

/** Isolated preview: exploring a variation never changes the game or asks the model. */
final class CoachVisualDialog {
    static Dialog show(Activity activity,List<CoachVisual.Scene> scenes,String advice){
        Dialog dialog=new Dialog(activity);LinearLayout root=Ui.card(activity),header=new LinearLayout(activity);header.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(Ui.dp(activity,22),Ui.dp(activity,16),Ui.dp(activity,22),Ui.dp(activity,20));header.addView(Ui.text(activity,"AI 수 보기",18,true),new LinearLayout.LayoutParams(0,-2,1));header.addView(Ui.link(activity,"×",dialog::dismiss),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));root.addView(header);Ui.gap(root,8);
        ScrollView scroll=new ScrollView(activity);LinearLayout content=Ui.column(activity);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView caption=Ui.text(activity,"",12,false),move=Ui.text(activity,"",17,true);caption.setTextColor(activity.getColor(R.color.muted));move.setTextColor(activity.getColor(R.color.ai));content.addView(caption);content.addView(move);Ui.gap(content,12);
        BoardView board=new BoardView(activity);int side=Math.max(Ui.dp(activity,180),Math.min(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,68),(int)(activity.getResources().getDisplayMetrics().heightPixels*.40)));FrameLayout frame=new FrameLayout(activity);frame.addView(board,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));content.addView(frame,new LinearLayout.LayoutParams(-1,side));Ui.gap(content,12);
        TextView path=Ui.text(activity,"",12,false);path.setMaxLines(2);path.setEllipsize(android.text.TextUtils.TruncateAt.END);content.addView(path);Ui.gap(content,8);
        TextView explanation=Ui.text(activity,"",14,false);explanation.setLineSpacing(Ui.dp(activity,5),1);explanation.setText(CoachMarkdown.render(advice));explanation.setVisibility(View.GONE);Button reading=Ui.link(activity,"조언 함께 읽기  ⌄",()->{});reading.setOnClickListener(v->{boolean open=explanation.getVisibility()!=View.VISIBLE;explanation.setVisibility(open?View.VISIBLE:View.GONE);reading.setText(open?"조언 접기  ⌃":"조언 함께 읽기  ⌄");});content.addView(reading);content.addView(explanation);
        int[] index={0};Runnable[] render={null};LinearLayout navigation=new LinearLayout(activity);Button previous=Ui.link(activity,"‹",()->{index[0]--;render[0].run();}),next=Ui.link(activity,"›",()->{index[0]++;render[0].run();});TextView count=Ui.text(activity,"",12,true);count.setGravity(Gravity.CENTER);navigation.addView(previous,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));navigation.addView(count,new LinearLayout.LayoutParams(Ui.dp(activity,56),Ui.dp(activity,44)));navigation.addView(next,new LinearLayout.LayoutParams(0,Ui.dp(activity,44),1));Ui.gap(root,12);root.addView(navigation);
        render[0]=()->{var scene=scenes.get(index[0]);caption.setText(scene.caption());move.setText("AI · "+scene.san());path.setText(scene.path());board.board=new Chess(scene.fen());board.setLastMove(scene.lastMove()==null?null:Chess.Move.parse(scene.lastMove()));board.aiArrow=scene.move();board.setContentDescription(scene.caption()+"。AI 화살표 "+scene.san());board.invalidate();count.setText((index[0]+1)+" / "+scenes.size());previous.setEnabled(index[0]>0);next.setEnabled(index[0]+1<scenes.size());previous.setAlpha(previous.isEnabled()?1f:.4f);next.setAlpha(next.isEnabled()?1f:.4f);scroll.scrollTo(0,0);};render[0].run();
        dialog.setContentView(root);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,40),Math.min(activity.getResources().getDisplayMetrics().heightPixels-Ui.dp(activity,80),Ui.dp(activity,640)));}return dialog;
    }
}
