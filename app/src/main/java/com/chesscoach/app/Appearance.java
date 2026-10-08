package com.chesscoach.app;
import android.app.*;
import android.widget.*;
final class Appearance {
    static void show(Activity a){
        var prefs=a.getSharedPreferences("appearance",0);Dialog dialog=new Dialog(a);LinearLayout card=Ui.card(a);card.addView(Ui.text(a,"화면 설정",22,true));Ui.gap(card,12);
        RadioGroup group=new RadioGroup(a);String[] values={"system","light","dark"},labels={"시스템 설정","화이트 모드","다크 모드"};
        for(int i=0;i<values.length;i++){String value=values[i];RadioButton radio=new RadioButton(a);radio.setText(labels[i]);radio.setTextColor(a.getColor(R.color.ink));radio.setMinHeight(Ui.dp(a,48));group.addView(radio);radio.setChecked(value.equals(prefs.getString("theme","system")));radio.setOnClickListener(v->{prefs.edit().putString("theme",value).commit();dialog.dismiss();a.recreate();});}card.addView(group);Ui.gap(card,12);
        Switch animation=new Switch(a);animation.setText("기물 이동 애니메이션");animation.setTextColor(a.getColor(R.color.ink));animation.setMinHeight(Ui.dp(a,48));animation.setChecked(prefs.getBoolean("animation",true));animation.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("animation",on).apply());card.addView(animation);
        TextView note=Ui.text(a,"이동 애니메이션은 기본으로 켜져 있어요.",12,false);note.setTextColor(a.getColor(R.color.muted));card.addView(note);Ui.gap(card,16);card.addView(Ui.button(a,"닫기",dialog::dismiss));dialog.setContentView(card);dialog.show();if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(a.getResources().getDisplayMetrics().widthPixels-Ui.dp(a,32),-2);}
    }
}
