package com.chesscoach.app;
import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Typeface;
import android.widget.*;
import java.util.*;

public final class RecordsActivity extends Activity {
    private LinearLayout list;
    private int ink,muted;
    @Override public void onCreate(Bundle state){super.onCreate(state);ink=getColor(R.color.ink);muted=getColor(R.color.muted);ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(getColor(R.color.background));list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),Ui.dp(this,24));scroll.addView(list);setContentView(scroll);list.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(Ui.dp(this,16),i.getSystemWindowInsetTop()+Ui.dp(this,12),Ui.dp(this,16),i.getSystemWindowInsetBottom()+Ui.dp(this,24));return i;});}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,8,0,12);return t;}
    private Button button(String title,Runnable action){return Ui.button(this,title,action);}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        list.removeAllViews();list.addView(button("‹ 대국으로 돌아가기",this::finish));TextView title=text("나의 대국 기록",27,ink);title.setTypeface(null,Typeface.BOLD);list.addView(title);
        list.addView(text("직접 둔 대국과 가져온 대국을 한곳에서. 추천수와 해설은 기기에 저장됩니다.",14,muted));
        list.addView(button("PGN · UCI 대국 가져오기",this::importGame));
        List<Records.Saved> games=Records.get(this).list();if(games.isEmpty())list.addView(text("아직 기록이 없습니다. 대국을 두거나 기록을 가져오세요.",16,muted));
        for(var game:games){
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16));
            android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(getColor(R.color.surface));bg.setCornerRadius(16);card.setBackground(bg);
            card.addView(text(game.title(),19,ink));String date=new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm",Locale.getDefault()).format(new Date(game.created()));
            int explanations=0;Iterator<String> keys=game.analyses().keys();while(keys.hasNext()){var entry=game.analyses().optJSONObject(keys.next());if(entry!=null&&entry.has("aiResponse"))explanations++;}
            card.addView(text((game.origin().equals("played")?"직접 대국":"가져온 대국")+" · "+game.plies()+" 반수 · "+date+"\n해설 "+explanations+"개 · 하이라이트 "+game.highlights().length()+"개",12,muted));
            card.setOnClickListener(v->startActivity(new Intent(this,ReviewActivity.class).putExtra("recordId",game.id())));card.setClickable(true);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,14,0,0);list.addView(card,p);
        }
    }
    private void importGame(){
        EditText input=new EditText(this);input.setTextColor(ink);input.setMinLines(5);input.setMaxLines(10);input.setHint("1. e4 e5 2. Nf3 Nc6 …\n또는 e2e4 e7e5 g1f3 b8c6");
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(20,10,20,10);form.addView(text("PGN의 메인 라인을 가져옵니다. 주석과 가지 변화는 제외합니다. 가져온 뒤 Stockfish 분석과 학습 하이라이트를 생성합니다.",14,muted));form.addView(input);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("대국 가져오기").setView(form).setPositiveButton("가져오기",null).setNegativeButton("취소",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{Pgn.Game game=Pgn.parse(input.getText().toString());if(game.plies().isEmpty())throw new IllegalArgumentException("분석할 수가 없습니다.");String title=game.tags().getOrDefault("White","백")+" vs "+game.tags().getOrDefault("Black","흑");long id=Records.get(this).create(title,"imported",game);dialog.dismiss();startActivity(new Intent(this,ReviewActivity.class).putExtra("recordId",id).putExtra("autoAnalyze",true));}
            catch(Exception e){input.setError(e.getMessage());}
        }));dialog.show();
    }
}
