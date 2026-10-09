package com.chesscoach.app;
import android.app.*;
import android.widget.*;
final class Appearance {
    static void show(Activity a){
        a.startActivity(new android.content.Intent(a,SettingsActivity.class));
    }
    static void controls(Activity a,LinearLayout card,Runnable themeChanged){
        var prefs=a.getSharedPreferences("appearance",0);Ui.gap(card,12);
        RadioGroup group=new RadioGroup(a);String[] values={"system","light","dark"},labels={"시스템 설정","화이트 모드","다크 모드"};
        for(int i=0;i<values.length;i++){String value=values[i];RadioButton radio=new RadioButton(a);radio.setText(labels[i]);radio.setTextColor(a.getColor(R.color.ink));radio.setMinHeight(Ui.dp(a,48));group.addView(radio);radio.setChecked(value.equals(prefs.getString("theme","system")));radio.setOnClickListener(v->{prefs.edit().putString("theme",value).commit();themeChanged.run();});}card.addView(group);Ui.gap(card,12);
        Switch animation=new Switch(a);animation.setText("기물 이동 애니메이션");animation.setTextColor(a.getColor(R.color.ink));animation.setMinHeight(Ui.dp(a,48));animation.setChecked(prefs.getBoolean("animation",true));animation.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("animation",on).apply());card.addView(animation);
        TextView note=Ui.text(a,"이동 애니메이션은 기본으로 켜져 있어요.",12,false);note.setTextColor(a.getColor(R.color.muted));card.addView(note);
    }
}
