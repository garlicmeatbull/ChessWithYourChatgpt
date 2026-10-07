package com.chesscoach.app;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
public final class GameSetupActivity extends Activity {
    private Difficulty selected;private RadioGroup choices;
    @Override public void onCreate(Bundle state){super.onCreate(state);selected=Difficulty.from(getSharedPreferences("coach",0).getString("difficulty","ELO_1600"));LinearLayout root=Ui.screen(this,false);Ui.header(this,root,"새 대국");root.addView(Ui.text(this,"어떤 상대와 둘까요?",26,true));Ui.gap(root,8);root.addView(Ui.text(this,"상대의 난이도만 조절합니다. 수를 평가하는 분석 엔진은 항상 최강 설정입니다.",14,false));Ui.gap(root,20);choices=new RadioGroup(this);
        for(Difficulty d:Difficulty.values()){RadioButton r=new RadioButton(this);r.setId(android.view.View.generateViewId());r.setText(d.label+"\n"+description(d));r.setTextColor(getColor(R.color.ink));r.setTextSize(16);r.setPadding(Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14));r.setMinHeight(Ui.dp(this,68));choices.addView(r);if(d==selected)r.setChecked(true);r.setOnCheckedChangeListener((v,on)->{if(on)selected=d;});}ScrollView list=new ScrollView(this);list.addView(choices);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));Ui.gap(root,16);TextView note=Ui.text(this,"Elo는 엔진의 목표치이며 사이트의 실전 레이팅과 다를 수 있어요. 백으로 대국합니다.",12,false);note.setTextColor(getColor(R.color.muted));root.addView(note);Ui.gap(root,16);root.addView(Ui.primary(this,"대국 시작",()->{getSharedPreferences("coach",0).edit().putString("difficulty",selected.name()).apply();startActivity(new Intent(this,MainActivity.class).putExtra("newGame",true));finish();}));if(!getSharedPreferences("coach",0).getString("moves","").isEmpty()){Ui.gap(root,8);root.addView(Ui.button(this,"진행 중인 대국 이어하기",()->{startActivity(new Intent(this,MainActivity.class));finish();}));}}
    private String description(Difficulty d){switch(d){case BEGINNER:return "기물과 기본 규칙부터 익히기";case ELO_1320:return "차분하게 기본기를 연습하기";case ELO_1600:return "전술과 계획을 함께 연습하기";case ELO_2000:return "작은 실수도 놓치지 않는 상대";case ELO_2400:return "깊은 계산에 도전하기";case ELO_2800:return "강한 엔진과 겨루기";case ELO_3190:return "최상위 목표 난이도";default:return "실력 제한 없이 가장 강하게";}}
}
