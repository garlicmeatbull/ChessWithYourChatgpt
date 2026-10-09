package com.chesscoach.app;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.function.IntConsumer;

/** Read-only scoresheet; opening or copying it never requests an AI explanation. */
final class NotationDialog {
    private static final class Row {
        final int number;int white=-1,black=-1;
        Row(int number){this.number=number;}
    }
    static Dialog show(Activity activity,List<Pgn.Ply> source,int selected,String pgn,IntConsumer select){
        List<Pgn.Ply> plies=new ArrayList<>(source);List<Row> rows=new ArrayList<>();int selectedRow=0;
        for(int i=0;i<plies.size();i++){
            Pgn.Ply ply=plies.get(i);Row row=rows.isEmpty()?null:rows.get(rows.size()-1);
            if(row==null||row.number!=ply.number()){row=new Row(ply.number());rows.add(row);}
            if(ply.white())row.white=i;else row.black=i;
            if(i+1==selected)selectedRow=rows.size()-1;
        }
        Dialog dialog=new Dialog(activity);LinearLayout root=Ui.card(activity),title=new LinearLayout(activity);title.setGravity(Gravity.CENTER_VERTICAL);
        title.addView(Ui.text(activity,"기보",20,true),new LinearLayout.LayoutParams(0,-2,1));title.addView(Ui.link(activity,"×",dialog::dismiss),new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,44)));root.addView(title);
        TextView hint=Ui.text(activity,"수를 누르면 해당 위치를 볼 수 있어요.",12,false);hint.setTextColor(activity.getColor(R.color.muted));root.addView(hint);Ui.gap(root,16);
        LinearLayout headings=new LinearLayout(activity);
        for(String label:new String[]{"수","백","흑"}){TextView cell=Ui.text(activity,label,12,true);cell.setTextColor(activity.getColor(R.color.muted));cell.setGravity(Gravity.CENTER);headings.addView(cell,label.equals("수")?new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,32)):new LinearLayout.LayoutParams(0,Ui.dp(activity,32),1));}root.addView(headings);
        ListView list=new ListView(activity);list.setDivider(null);list.setVerticalScrollBarEnabled(false);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=Ui.text(activity,"아직 둔 수가 없어요.",14,false);empty.setGravity(Gravity.CENTER);root.addView(empty);list.setEmptyView(empty);
        list.setAdapter(new BaseAdapter(){
            public int getCount(){return rows.size();}public Object getItem(int position){return rows.get(position);}public long getItemId(int position){return position;}
            public View getView(int position,View reusable,android.view.ViewGroup parent){
                LinearLayout row=(LinearLayout)reusable;
                if(row==null){row=new LinearLayout(activity);row.setGravity(Gravity.CENTER_VERTICAL);TextView number=Ui.text(activity,"",12,false);number.setGravity(Gravity.CENTER);number.setTextColor(activity.getColor(R.color.muted));row.addView(number,new LinearLayout.LayoutParams(Ui.dp(activity,44),Ui.dp(activity,48)));for(int j=0;j<2;j++){Button move=Ui.link(activity,"",()->{});row.addView(move,new LinearLayout.LayoutParams(0,Ui.dp(activity,48),1));}}
                Row data=rows.get(position);row.setBackgroundColor(activity.getColor(position%2==0?R.color.surface:R.color.background));((TextView)row.getChildAt(0)).setText(data.number+".");
                for(int j=0;j<2;j++){int ply=j==0?data.white:data.black;Button move=(Button)row.getChildAt(j+1);move.setText(ply<0?"—":plies.get(ply).san());move.setEnabled(ply>=0);move.setSelected(false);move.setTextColor(activity.getColor(ply<0?R.color.muted:R.color.ink));move.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.accent_surface)),null,null));if(ply+1==selected&&ply>=0)Ui.selected(activity,move,true);move.setContentDescription(data.number+"수 "+(j==0?"백 ":"흑 ")+move.getText());move.setOnClickListener(v->{if(ply>=0){dialog.dismiss();select.accept(ply+1);}});}
                return row;
            }
        });
        int anchor=selectedRow;list.post(()->list.setSelection(Math.max(0,anchor-2)));Ui.gap(root,12);
        LinearLayout footer=new LinearLayout(activity);Ui.action(footer,Ui.link(activity,"처음 위치",()->{dialog.dismiss();select.accept(0);}),44);Ui.action(footer,Ui.link(activity,"PGN 복사",()->{((android.content.ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PGN",pgn));Toast.makeText(activity,"PGN을 복사했습니다",Toast.LENGTH_SHORT).show();}),44);root.addView(footer);
        dialog.setContentView(root);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,32),Math.min((int)(activity.getResources().getDisplayMetrics().heightPixels*.8),Ui.dp(activity,680)));}return dialog;
    }
}
