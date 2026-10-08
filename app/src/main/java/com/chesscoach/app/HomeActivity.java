package com.chesscoach.app;
import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.widget.*;
public final class HomeActivity extends ThemedActivity {
    private TextView connection;private Button resume;
    private AlertDialog coachingIntro;
    private boolean introShown;
    @Override public void onCreate(Bundle state){super.onCreate(state);LinearLayout root=Ui.screen(this,true);Ui.gap(root,20);TextView brand=Ui.text(this,"CHESS COACH",12,true);brand.setTextColor(getColor(R.color.muted));root.addView(brand);Ui.gap(root,12);root.addView(Ui.text(this,"다음 수가 보이는 체스.",32,true));Ui.gap(root,8);root.addView(Ui.text(this,"차분하게 두고, 한 수의 이유를 이해하세요.",15,false));Ui.gap(root,32);
        LinearLayout play=Ui.card(this);TextView playTag=Ui.text(this,"대국",12,true);playTag.setTextColor(getColor(R.color.accent));play.addView(playTag);Ui.gap(play,12);play.addView(Ui.text(this,"Stockfish와 대국",24,true));Ui.gap(play,8);play.addView(Ui.text(this,"나에게 맞는 난이도로 대국하고\n방금 둔 수의 평가와 조언을 받아요.",15,false));Ui.gap(play,20);play.addView(Ui.primary(this,"난이도 선택하고 시작  →",()->startActivity(new Intent(this,GameSetupActivity.class))));root.addView(play);Ui.gap(root,8);resume=Ui.primary(this,"진행 중인 대국 이어하기",()->startActivity(new Intent(this,MainActivity.class)));root.addView(resume);Ui.gap(root,16);
        LinearLayout records=Ui.card(this);TextView reviewTag=Ui.text(this,"복기",12,true);reviewTag.setTextColor(getColor(R.color.accent));records.addView(reviewTag);Ui.gap(records,12);records.addView(Ui.text(this,"나의 대국 기록",24,true));Ui.gap(records,8);records.addView(Ui.text(this,"직접 둔 대국과 가져온 기보를 함께.\n하이라이트로 성장할 국면을 다시 봐요.",15,false));Ui.gap(records,20);records.addView(Ui.button(this,"기록 살펴보기  →",()->startActivity(new Intent(this,RecordsActivity.class))));root.addView(records);Ui.gap(root,20);
        connection=Ui.text(this,"",14,false);connection.setTextColor(getColor(R.color.muted));root.addView(connection);root.addView(Ui.button(this,"AI 코치 연결 · 모델",()->startActivity(new Intent(this,ConnectionActivity.class))));Ui.gap(root,12);root.addView(Ui.button(this,"화면 설정 · 테마와 애니메이션",()->Appearance.show(this)));Ui.gap(root,8);root.addView(Ui.button(this,"오픈소스 라이선스",this::license));
    }
    @Override public void onResume(){super.onResume();resume.setVisibility(GameSession.available(this)?android.view.View.VISIBLE:android.view.View.GONE);connection.setText(!ChatGptAccounts.connected(this)?"Stockfish는 오프라인으로 이용할 수 있어요.":"선택한 코치 · "+Ui.modelLabel(this));showCoachingIntro();}
    private void showCoachingIntro(){
        var prefs=getSharedPreferences("onboarding",MODE_PRIVATE);
        if(prefs.getBoolean("coachingIntroDone",false))return;
        if(ChatGptAccounts.connected(this)&&!Ui.model(this).isEmpty()){
            prefs.edit().putBoolean("coachingIntroDone",true).apply();
            return;
        }
        if(introShown)return;
        introShown=true;
        LinearLayout content=Ui.card(this);
        content.addView(Ui.text(this,"한 수마다, AI 코칭",24,true));Ui.gap(content,12);
        content.addView(Ui.text(this,"ChatGPT 계정과 모델을 연결하면 방금 둔 수의 평가와 Stockfish 추천 수의 이유를 쉽게 설명해 드려요.",16,false));Ui.gap(content,10);
        TextView note=Ui.text(this,"브라우저에서 내 계정으로 로그인하고 모델을 선택하세요. 계정 이용 자격과 사용 한도가 적용됩니다. 연결 없이도 Stockfish와 대국할 수 있어요.",13,false);
        note.setTextColor(getColor(R.color.muted));content.addView(note);Ui.gap(content,24);
        content.addView(Ui.primary(this,"ChatGPT 계정 · 모델 연결하기",()->{
            coachingIntro.dismiss();
            startActivity(new Intent(this,ConnectionActivity.class));
        }),new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));
        Ui.gap(content,8);
        TextView skip=Ui.text(this,"괜찮아요, 연결 없이 사용할게요",13,false);
        skip.setTextColor(getColor(R.color.muted));
        skip.setGravity(android.view.Gravity.CENTER);
        skip.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22888888),null,null));
        skip.setOnClickListener(v->{prefs.edit().putBoolean("coachingIntroDone",true).apply();coachingIntro.dismiss();});
        content.addView(skip,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));
        ScrollView scroll=new ScrollView(this);scroll.addView(content);
        coachingIntro=new AlertDialog.Builder(this).setView(scroll).setCancelable(false).create();
        coachingIntro.show();
        if(coachingIntro.getWindow()!=null)coachingIntro.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }
    @Override protected void onDestroy(){if(coachingIntro!=null)coachingIntro.dismiss();super.onDestroy();}
    private void license(){try(var in=getAssets().open("THIRD_PARTY_NOTICES.txt")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);ScrollView scroll=new ScrollView(this);TextView t=Ui.text(this,new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8),12,false);t.setPadding(20,20,20,20);scroll.addView(t);new AlertDialog.Builder(this).setTitle("오픈소스 라이선스").setView(scroll).setPositiveButton("닫기",null).show();}catch(Exception e){Toast.makeText(this,"라이선스 문서를 열지 못했습니다",Toast.LENGTH_SHORT).show();}}
}
