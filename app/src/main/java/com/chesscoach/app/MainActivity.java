package com.chesscoach.app;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private Chess game=new Chess();
    private Stockfish engine;
    private Stockfish opponent;
    private final ExecutorService engineJobs=Executors.newSingleThreadExecutor(),coachJobs=Executors.newSingleThreadExecutor();
    private final List<String> moves=new ArrayList<>(),fens=new ArrayList<>();
    private final Map<String,Integer> repetitions=new HashMap<>();
    private final List<Integer> trend=new ArrayList<>();
    private final List<Feedback> feedback=new ArrayList<>();
    private volatile int generation=0;
    private boolean busy=true;
    private volatile boolean destroyed=false;
    private BoardView board;
    private TextView status,evaluation,history,coach,headline;
    private Button retry;
    private SharedPreferences prefs;
    private CoachClient client;
    private long recordId;
    private int BG,INK,MUTED;
    private static class Feedback { String local,ai=""; boolean pending; JSONObject payload,details; Feedback(String s){local=s;} }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);prefs=getSharedPreferences("coach",0);client=new CoachClient(this);
        BG=getColor(R.color.background);INK=getColor(R.color.ink);MUTED=getColor(R.color.muted);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        buildUi();restore();connectEngine();
    }
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color) { TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setPadding(0,dp(5),0,dp(5));return v; }
    private Button button(String title,Runnable action) {
        Button b=new Button(this);b.setText(title);b.setTextColor(INK);b.setTextSize(13);b.setAllCaps(false);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(getColor(R.color.surface));bg.setCornerRadius(dp(12));
        b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x334C9670),bg,null));
        b.setOnClickListener(v->action.run());return b;
    }
    private void buildUi() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(20));scroll.addView(root);setContentView(scroll);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(16),insets.getSystemWindowInsetTop()+dp(12),dp(16),insets.getSystemWindowInsetBottom()+dp(20));return insets;});
        root.addView(text("CHESS / COACH",12,MUTED));headline=text("한 수씩, 더 깊게.",27,INK);root.addView(headline);
        status=text("Stockfish 시작 중…",14,MUTED);root.addView(status);
        board=new BoardView(this);board.onSquare=this::select;
        int size=getResources().getDisplayMetrics().widthPixels-dp(32);root.addView(board,new LinearLayout.LayoutParams(-1,size));
        evaluation=text("백 기준 평가 · 분석 대기",14,MUTED);root.addView(evaluation);
        LinearLayout buttons=new LinearLayout(this);
        for(Button b:new Button[]{button("새 대국",()->new AlertDialog.Builder(this).setMessage("새 대국을 시작할까요?").setPositiveButton("시작",(d,w)->reset()).setNegativeButton("취소",null).show()),button("무르기",this::undo),button("설정",this::settings)}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(3),dp(8),dp(3),dp(10));buttons.addView(b,p);}
        root.addView(buttons);
        root.addView(button("나의 대국 기록 · 가져오기",()->startActivity(new android.content.Intent(this,RecordsActivity.class))));root.addView(text("이번 수의 피드백",20,INK));
        coach=text("기물을 선택한 뒤 목적지 칸을 누르세요. 백으로 Stockfish와 대국합니다.",16,INK);root.addView(coach);
        retry=button("Codex 설명 재시도",()->{if(!feedback.isEmpty())requestExplanation(feedback.get(feedback.size()-1),generation);});root.addView(retry);retry.setVisibility(View.GONE);
        root.addView(text("대국 흐름",20,INK));history=text("아직 둔 수가 없습니다.",14,MUTED);root.addView(history);
        TextView license=text("Stockfish 19 · 오픈소스 라이선스\nCodex 설명은 선택한 모델과 계정 이용 한도를 사용합니다.",11,MUTED);
        license.setOnClickListener(v->{try(var in=getAssets().open("THIRD_PARTY_NOTICES.txt")){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);TextView content=text(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8),12,INK);content.setPadding(dp(16),dp(16),dp(16),dp(16));ScrollView view=new ScrollView(this);view.addView(content);new AlertDialog.Builder(this).setTitle("오픈소스 라이선스").setView(view).setPositiveButton("닫기",null).show();}catch(Exception ignored){}});root.addView(license);
    }
    private void connectEngine() {
        busy=true;int epoch=generation;engineJobs.submit(()->{
            if(destroyed)return;
            try { engine=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");opponent=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");if(destroyed){engine.close();opponent.close();return;}runOnUiThread(()->{if(epoch!=generation||destroyed)return;busy=false;refresh();if(!game.white&&game.terminal(repetitions.getOrDefault(game.key(),1))==null)botTurn(generation);}); }
            catch(Exception e){runOnUiThread(()->{if(epoch!=generation||destroyed)return;busy=true;status.setText("Stockfish 시작 실패 · "+e.getMessage());coach.setText("엔진이 포함된 APK를 다시 설치하세요.");});}
        });
    }
    private void select(int s) {
        if(busy||!game.white||game.terminal(repetitions.getOrDefault(game.key(),1))!=null)return;
        List<Chess.Move> legal=game.legalMoves();
        List<Chess.Move> choices=new ArrayList<>();for(var m:legal)if(m.from()==board.selected&&m.to()==s)choices.add(m);
        if(!choices.isEmpty()) {
            if(choices.size()>1)new AlertDialog.Builder(this).setTitle("승격 기물").setItems(new String[]{"퀸","룩","비숍","나이트"},(d,w)->play(choices.get(w),generation,false)).show();
            else play(choices.get(0),generation,false);return;
        }
        board.selected=game.squares[s]!='.'&&Chess.color(game.squares[s])?s:-1;board.targets.clear();
        for(var m:legal)if(m.from()==board.selected)board.targets.add(m.to());board.invalidate();
    }
    private void play(Chess.Move move,int epoch,boolean bot) {
        if(epoch!=generation||destroyed)return;
        busy=true;board.selected=-1;board.targets.clear();board.invalidate();status.setText(bot?"Stockfish 응수 분석 중…":"선택한 수와 후보를 비교하는 중…");
        Chess before=game.copy();
        engineJobs.submit(()->{
            try {
                var analysis=engine.analyze(before,bot?500:1100,3,null);
                Stockfish.Line played=null;
                for(var l:analysis.lines())if(!l.pv().isEmpty()&&l.pv().get(0).equals(move.uci()))played=l;
                if(played==null)played=engine.analyze(before,700,1,move.uci()).top();
                Chess after=before.copy();after.play(move);
                var afterAnalysis=engine.analyze(after,500,1,null);
                Stockfish.Line actual=played;
                runOnUiThread(()->{
                    if(epoch!=generation||destroyed)return;
                    game=after;moves.add(move.uci());fens.add(game.fen());repetitions.merge(game.key(),1,Integer::sum);
                    board.lastFrom=move.from();board.lastTo=move.to();
                    var top=analysis.top();String local=localFeedback(before,move.uci(),top,actual,afterAnalysis.top());
                    Feedback entry=new Feedback((before.white?"백":"흑")+" "+move.uci()+"\n"+local);feedback.add(entry);
                    if(afterAnalysis.top()!=null){trend.add(afterAnalysis.top().cp());if(trend.size()>6)trend.remove(0);evaluation.setText("백 기준 "+afterAnalysis.top().score()+" · 탐색 깊이 "+afterAnalysis.top().depth());}
                    try{entry.payload=CoachClient.payload(before,after,move.uci(),analysis,actual,prefs.getString("model",""),new ArrayList<>(trend));entry.details=AnalysisJson.details(analysis,actual,afterAnalysis).put("opponentDifficulty",Difficulty.from(prefs.getString("difficulty","ELO_1600")).label);}catch(Exception ignored){}
                    save();persistFeedback(entry,moves.size()-1);busy=false;refresh();requestExplanation(entry,epoch);
                    if(game.terminal(repetitions.getOrDefault(game.key(),1))==null&&game.white==false)botTurn(epoch);
                });
            }catch(Exception e){runOnUiThread(()->{if(epoch==generation){busy=true;status.setText("분석 실패 · "+e.getMessage());coach.setText("새 대국으로 엔진을 다시 시작하세요.");}});}
        });
    }
    private void botTurn(int epoch) {
        if(epoch!=generation||destroyed)return;busy=true;status.setText("Stockfish가 생각하는 중…");Chess position=game.copy();
        engineJobs.submit(()->{
            try { opponent.setDifficulty(Difficulty.from(prefs.getString("difficulty","ELO_1600")));var analysis=opponent.analyze(position,900,1,null);Chess.Move move=Chess.Move.parse(analysis.best());runOnUiThread(()->{if(epoch==generation)play(move,epoch,true);}); }
            catch(Exception e){runOnUiThread(()->{if(epoch==generation){busy=true;status.setText("Stockfish 응수 실패 · 새 대국으로 재시작하세요");}});}
        });
    }
    private String localFeedback(Chess before,String played,Stockfish.Line best,Stockfish.Line actual,Stockfish.Line after) {
        if(best==null||actual==null)return "종료 국면 · 후보 평가 없음";
        int loss=Math.max(0,(best.value()-actual.value())*(before.white?1:-1));
        String label=best.pv().get(0).equals(played)?"최선수":loss<=30?"좋은 수":loss<=100?"아쉬운 수":loss<=250?"실수":"큰 실수";
        String delta=best.mate()!=null||actual.mate()!=null?"메이트 가능성 포함":String.format(Locale.US,"평가 손실 %.2f",loss/100.0);
        String advantage=after==null?"대국 종료":after.mate()!=null?"강제 메이트 평가 "+after.score():Math.abs(after.cp())<40?"현재 흐름은 균형":(after.cp()>0?"백":"흑")+"에게 유리한 흐름";
        return label+" · "+delta+"\n"+advantage+"\n추천 "+best.pv().get(0)+" · "+best.score()+" · 깊이 "+best.depth()+"\n예상 변화 "+String.join(" → ",best.pv())+"\n탐색 시간에 따른 추정입니다. 희생수의 탁월함을 자동 확정하지 않습니다.";
    }
    private void requestExplanation(Feedback entry,int epoch) {
        if(entry.payload==null||entry.pending)return;
        if(prefs.getString("endpoint","").trim().isEmpty()) { entry.ai="Codex 미연결 · 설정에서 OAuth 설명 서버를 연결하세요.";renderFeedback();return; }
        entry.ai="Codex가 추천수의 근거를 설명하는 중…";renderFeedback();
        entry.pending=true;
        final long targetRecord=recordId;final int targetPly=feedback.indexOf(entry);
        String model=prefs.getString("model","");
        try { entry.payload.put("model",model); }catch(JSONException ignored){}
        JSONObject request;try{request=new JSONObject(entry.payload.toString());}catch(Exception e){return;}
        coachJobs.submit(()->{
            if(destroyed)return;
            String result;
            try { JSONObject response=client.request("/v1/explain",request);result=AnalysisJson.explanation(response);Records.get(this).patch(targetRecord,targetPly,new JSONObject().put("aiResponse",response).put("aiText",result));
            }catch(Exception e){result="Codex 설명 불가 · "+e.getMessage()+"\nStockfish 대국과 분석은 계속 가능합니다.";try{Records.get(this).patch(targetRecord,targetPly,new JSONObject().put("aiText",result));}catch(Exception ignored){}}
            String finalResult=result;runOnUiThread(()->{entry.pending=false;if(epoch==generation&&!destroyed){entry.ai=finalResult;save();renderFeedback();}});
        });
    }
    private void renderFeedback() {
        if(feedback.isEmpty()){coach.setText("기물을 선택한 뒤 목적지를 누르세요.");retry.setVisibility(View.GONE);return;}
        Feedback last=feedback.get(feedback.size()-1);coach.setText(last.local+"\n\n"+last.ai);retry.setVisibility(last.payload!=null?View.VISIBLE:View.GONE);
        StringBuilder out=new StringBuilder();for(int i=Math.max(0,feedback.size()-8);i<feedback.size();i++){Feedback f=feedback.get(i);out.append(i+1).append(". ").append(f.local).append("\n").append(f.ai).append("\n\n");}history.setText(out.toString());
    }
    private void refresh(){board.board=game;board.invalidate();String terminal=game.terminal(repetitions.getOrDefault(game.key(),1));status.setText(terminal!=null?terminal:busy?"분석 중…":game.white?"백 차례 · "+Difficulty.from(prefs.getString("difficulty","ELO_1600")).label:"흑 차례 · Stockfish");renderFeedback();}
    private void reset() {
        generation++;recordId=0;game=new Chess();moves.clear();fens.clear();feedback.clear();trend.clear();repetitions.clear();repetitions.put(game.key(),1);board.selected=board.lastFrom=board.lastTo=-1;board.targets.clear();save();restoreEvaluation();refresh();
        busy=true;engineJobs.submit(()->{if(engine!=null)engine.close();if(opponent!=null)opponent.close();engine=null;opponent=null;});connectEngine();
    }
    private void undo() {
        if(busy||moves.isEmpty())return;generation++;
        int keep=Math.max(0,moves.size()-(game.white?2:1));while(moves.size()>keep){moves.remove(moves.size()-1);fens.remove(fens.size()-1);if(!feedback.isEmpty())feedback.remove(feedback.size()-1);}
        game=new Chess(keep==0?Chess.START:fens.get(keep-1));rebuildRepetitions();trend.clear();board.lastFrom=board.lastTo=board.selected=-1;board.targets.clear();save();refresh();evaluation.setText("백 기준 평가 · 다음 수에서 다시 분석");
    }
    private void rebuildRepetitions(){repetitions.clear();repetitions.put(new Chess().key(),1);for(String fen:fens)repetitions.merge(new Chess(fen).key(),1,Integer::sum);}
    private void save(){
        JSONArray entries=new JSONArray();
        try {for(Feedback f:feedback)entries.put(new JSONObject().put("local",f.local).put("ai",f.pending?"설명 대기 · 다시 연결해 재시도하세요":f.ai).put("payload",f.payload==null?JSONObject.NULL:f.payload));}
        catch(JSONException ignored){}
        if(!moves.isEmpty()){
            Pgn.Game pgn=Pgn.parse(String.join(" ",moves));String terminal=game.terminal(repetitions.getOrDefault(game.key(),1));String result=terminal==null?"*":terminal.startsWith("백 승리")?"1-0":terminal.startsWith("흑 승리")?"0-1":"1/2-1/2";pgn=new Pgn.Game(pgn.initial(),pgn.plies(),pgn.tags(),result);
            if(recordId==0)recordId=Records.get(this).create("Stockfish · "+Difficulty.from(prefs.getString("difficulty","ELO_1600")).label,"played",pgn);else Records.get(this).updateGame(recordId,pgn);
        }else if(recordId!=0)Records.get(this).updateGame(recordId,new Pgn.Game(Chess.START,Collections.emptyList(),Collections.emptyMap(),"*"));
        prefs.edit().putString("moves",String.join(" ",moves)).putString("feedback",entries.toString()).putLong("activeRecord",recordId).apply();
    }
    private void persistFeedback(Feedback entry,int ply){try{JSONObject patch=new JSONObject().put("local",entry.local);if(entry.payload!=null)patch.put("payload",entry.payload);if(entry.details!=null)patch.put("details",entry.details);Records.get(this).patch(recordId,ply,patch);}catch(Exception ignored){}}
    private void restore(){
        try{
            recordId=prefs.getLong("activeRecord",0);
            String saved=prefs.getString("moves","");
            if(!saved.trim().isEmpty())for(String u:saved.split(" ")){game.play(Chess.Move.parse(u));moves.add(u);fens.add(game.fen());feedback.add(new Feedback((moves.size()%2==1?"백 ":"흑 ")+u+" · 저장된 대국"));}
            JSONArray entries=new JSONArray(prefs.getString("feedback","[]"));
            for(int i=0;i<Math.min(entries.length(),feedback.size());i++){JSONObject entry=entries.getJSONObject(i);Feedback f=feedback.get(i);f.local=entry.getString("local");f.ai=entry.optString("ai");f.payload=entry.optJSONObject("payload");}
            Records.Saved record=recordId==0?null:Records.get(this).find(recordId);
            if(record!=null)for(int i=0;i<feedback.size();i++){JSONObject entry=record.analyses().optJSONObject(Integer.toString(i));if(entry!=null){Feedback f=feedback.get(i);f.local=entry.optString("local",f.local);f.ai=entry.optString("aiText",f.ai);f.payload=entry.optJSONObject("payload");f.details=entry.optJSONObject("details");}}
        }catch(Exception e){game=new Chess();moves.clear();fens.clear();feedback.clear();save();}
        rebuildRepetitions();restoreEvaluation();refresh();
    }
    private void restoreEvaluation(){
        trend.clear();String label="백 기준 평가 · 분석 대기";
        for(int i=Math.max(0,feedback.size()-6);i<feedback.size();i++){
            JSONObject details=feedback.get(i).details;if(details==null)continue;
            try{JSONObject after=details.optJSONObject("afterAnalysis");if(after==null)continue;
                Stockfish.Line top=AnalysisJson.analysis(after).top();if(top==null)continue;
                trend.add(top.cp());if(i==feedback.size()-1)label="백 기준 "+top.score()+" · 탐색 깊이 "+top.depth()+" · 저장된 평가";
            }catch(JSONException ignored){}
        }
        evaluation.setText(label);
    }
    private EditText field(LinearLayout form,String title,String value){form.addView(text(title,13,MUTED));EditText e=new EditText(this);e.setTextColor(INK);e.setSingleLine(true);e.setText(value);form.addView(e);return e;}
    private void settings() {
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(20),dp(8),dp(20),dp(8));
        form.addView(text("대국 난이도 · 분석 엔진은 항상 최강",13,MUTED));Spinner difficulty=new Spinner(this);List<String> labels=new ArrayList<>();for(var d:Difficulty.values())labels.add(d.label);difficulty.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));difficulty.setSelection(Difficulty.from(prefs.getString("difficulty","ELO_1600")).ordinal());form.addView(difficulty);
        form.addView(text("Elo는 엔진의 목표 설정값입니다. 실전 사이트 레이팅과 다르며 기기·시간에 따라 달라집니다.",11,MUTED));
        form.addView(text("본인 PC/서버에서 codex login으로 로그인한 뒤 서버를 연결하세요. OAuth 토큰은 휴대폰으로 전송하지 않습니다.",14,MUTED));
        EditText url=field(form,"설명 서버 HTTPS 주소",prefs.getString("endpoint",""));EditText token=field(form,"페어링 토큰 (비우면 기존 값 유지)","");token.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(text("설명 모델 (서버에서 허용한 모델)",13,MUTED));Spinner spinner=new Spinner(this);
        List<String> models=new ArrayList<>();String stored=prefs.getString("models","");if(!stored.trim().isEmpty())models.addAll(Arrays.asList(stored.split(",")));if(models.isEmpty())models.add("서버 연결 후 선택");
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,models);spinner.setAdapter(adapter);int selected=models.indexOf(prefs.getString("model",""));if(selected>=0)spinner.setSelection(selected);form.addView(spinner);
        TextView testStatus=text("",13,MUTED);form.addView(testStatus);
        Button test=button("저장하고 연결 · 모델 목록 확인",()->{
            try {CoachClient.validateEndpoint(url.getText().toString().trim());if(!token.getText().toString().trim().isEmpty())client.saveToken(token.getText().toString().trim());prefs.edit().putString("endpoint",url.getText().toString().trim()).apply();}
            catch(Exception e){testStatus.setText(e.getMessage());return;}
            testStatus.setText("연결 확인 중…");coachJobs.submit(()->{
                try {JSONObject response=client.request("/v1/models",null);JSONObject auth=client.request("/v1/auth/status",null);JSONArray list=response.getJSONArray("models");List<String> available=new ArrayList<>();for(int i=0;i<list.length();i++)available.add(list.getString(i));if(available.isEmpty())throw new Exception("서버에 모델이 없습니다");
                    runOnUiThread(()->{models.clear();models.addAll(available);adapter.notifyDataSetChanged();int index=models.indexOf(prefs.getString("model",response.optString("defaultModel")));spinner.setSelection(Math.max(0,index));prefs.edit().putString("models",String.join(",",available)).apply();testStatus.setText(auth.optBoolean("signedIn")?"서버 연결 · Codex OAuth 로그인 확인 완료":"서버 연결됨 · 서버에서 Codex OAuth 로그인이 필요합니다");});
                }catch(Exception e){runOnUiThread(()->testStatus.setText("연결 실패 · "+e.getMessage()));}
            });
        });form.addView(test);
        ScrollView view=new ScrollView(this);view.addView(form);
        new AlertDialog.Builder(this).setTitle("대국 및 Codex 설정").setView(view).setPositiveButton("완료",(d,w)->{prefs.edit().putString("difficulty",Difficulty.values()[difficulty.getSelectedItemPosition()].name()).apply();if(!models.get(0).equals("서버 연결 후 선택"))prefs.edit().putString("model",models.get(spinner.getSelectedItemPosition())).apply();refresh();}).setNeutralButton("연결 해제",(d,w)->prefs.edit().remove("endpoint").remove("token").remove("iv").apply()).setNegativeButton("닫기",null).show();
    }
    @Override protected void onDestroy(){destroyed=true;generation++;engineJobs.shutdownNow();coachJobs.shutdownNow();if(engine!=null)engine.close();if(opponent!=null)opponent.close();super.onDestroy();}
}
