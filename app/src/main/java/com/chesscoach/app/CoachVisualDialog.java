package com.chesscoach.app;
import android.app.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Live explanation blocks with nearby validated boards; no gameplay or inference. */
final class CoachVisualDialog {
    static final class Live extends Dialog {
        private final Activity activity;private final CoachScroll scroll;private final LinearLayout content;private final List<Block> blocks=new ArrayList<>();
        private static final class Block {final LinearLayout root;final TextView text,path;final FrameLayout frame;final BoardView board;String sceneKey="";Block(Activity a){root=Ui.column(a);text=Ui.text(a,"",15,false);text.setLineSpacing(Ui.dp(a,5),1);root.addView(text);frame=new FrameLayout(a);board=new BoardView(a);board.onSquare=null;frame.addView(board,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));int size=Math.min(a.getResources().getDisplayMetrics().widthPixels-Ui.dp(a,76),Ui.dp(a,280));LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,size);params.topMargin=Ui.dp(a,12);root.addView(frame,params);path=Ui.text(a,"",12,false);path.setTextColor(a.getColor(R.color.muted));root.addView(path);root.setPadding(0,0,0,Ui.dp(a,16));}}
        Live(Activity a){super(a);activity=a;LinearLayout root=Ui.card(a),header=new LinearLayout(a);root.setPadding(Ui.dp(a,20),Ui.dp(a,16),Ui.dp(a,20),Ui.dp(a,20));header.setGravity(Gravity.CENTER_VERTICAL);header.addView(Ui.text(a,"설명과 판 함께 보기",18,true),new LinearLayout.LayoutParams(0,-2,1));header.addView(Ui.link(a,"×",this::dismiss),new LinearLayout.LayoutParams(Ui.dp(a,44),Ui.dp(a,44)));root.addView(header);Ui.gap(root,12);scroll=new CoachScroll(a);content=Ui.column(a);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
        void update(List<CoachVisual.Scene> legacy,String raw,Map<String,CoachVisual.Scene> catalog){scroll.apply(()->render(legacy,raw,catalog));}
        private void render(List<CoachVisual.Scene> legacy,String raw,Map<String,CoachVisual.Scene> catalog){
            List<CoachDiagrams.Part> parts=CoachDiagrams.parts(raw);boolean annotated=false;for(var part:parts)if(!part.diagramId().isEmpty())annotated=true;int shown=0;
            for(int i=0;i<parts.size();i++){if(i==blocks.size()){Block b=new Block(activity);blocks.add(b);content.addView(b.root);}Block block=blocks.get(i);var part=parts.get(i);CoachMarkdown.update(block.text,part.text());CoachVisual.Scene scene=catalog.get(part.diagramId());if(!annotated&&i==parts.size()-1&&!legacy.isEmpty())scene=legacy.get(0);if(scene!=null&&shown++>=6)scene=null;block.frame.setVisibility(scene==null?View.GONE:View.VISIBLE);block.path.setVisibility(scene==null?View.GONE:View.VISIBLE);
                if(scene!=null){String key=scene.fen()+scene.move();if(!key.equals(block.sceneKey)){block.sceneKey=key;block.board.board=new Chess(scene.fen());block.board.setLastMove(scene.lastMove()==null?null:Chess.Move.parse(scene.lastMove()));block.board.aiArrow=scene.move();block.board.setContentDescription(scene.caption()+"。AI 화살표 "+scene.san());block.board.invalidate();block.path.setText(scene.caption()+" · "+scene.path());}}
            }
            while(blocks.size()>parts.size()){Block removed=blocks.remove(blocks.size()-1);content.removeView(removed.root);} // Keep the user's scroll offset; never jump to the first board.
        }
    }
    static Live show(Activity activity,List<CoachVisual.Scene> scenes,String advice,Map<String,CoachVisual.Scene> catalog){Live dialog=new Live(activity);dialog.update(scenes,advice,catalog);dialog.show();Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout(activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,32),Math.min(activity.getResources().getDisplayMetrics().heightPixels-Ui.dp(activity,72),Ui.dp(activity,760)));}return dialog;}
}
