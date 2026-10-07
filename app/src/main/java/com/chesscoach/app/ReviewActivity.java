package com.chesscoach.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.ClipboardManager;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Same viewer and persistent analysis for games played in-app and imported PGN. */
public final class ReviewActivity extends Activity {
    private final ExecutorService engineJobs=Executors.newSingleThreadExecutor(),coachJobs=Executors.newSingleThreadExecutor();
    private volatile boolean destroyed=false,cancel=false;
    private volatile Stockfish engine;
    private volatile boolean analyzing=false;
    private final Set<Integer> pending=new HashSet<>();
    private long recordId;
    private Pgn.Game game;
    private Records records;
    private CoachClient client;
    private int index=0,ink,muted;
    private BoardView board;
    private TextView position,feedback,status,movesText;
    private LinearLayout highlightList;
    private Button analyzeButton;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setPadding(0,dp(5),0,dp(6));return t;}
    private Button button(String s,Runnable action){return Ui.button(this,s,action);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);records=Records.get(this);client=new CoachClient(this);recordId=getIntent().getLongExtra("recordId",0);Records.Saved saved=records.find(recordId);
        if(saved==null){finish();return;}try{game=Pgn.parse(saved.pgn());}catch(Exception e){finish();return;}
        ink=getColor(R.color.ink);muted=getColor(R.color.muted);index=state==null?0:state.getInt("index",0);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(getColor(R.color.background));LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(24));scroll.addView(root);setContentView(scroll);
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(16),i.getSystemWindowInsetTop()+dp(12),dp(16),i.getSystemWindowInsetBottom()+dp(24));return i;});
        root.addView(button("‹ 대국 기록",this::finish));root.addView(text(saved.title(),23,ink));status=text("저장된 분석과 해설을 확인하세요.",13,muted);root.addView(status);
        board=new BoardView(this);board.onSquare=null;root.addView(board,new LinearLayout.LayoutParams(-1,getResources().getDisplayMetrics().widthPixels-dp(32)));
        position=text("",15,ink);root.addView(position);
        LinearLayout navigation=new LinearLayout(this);for(Button b:new Button[]{button("처음",()->show(0)),button("‹ 이전",()->show(index-1)),button("다음 ›",()->show(index+1)),button("마지막",()->show(game.plies().size()))})navigation.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(navigation);
        movesText=text("",15,muted);movesText.setMovementMethod(LinkMovementMethod.getInstance());root.addView(movesText);
        LinearLayout actions=new LinearLayout(this);Button explain=button("이 수 해설",this::explainSelected),export=button("PGN 복사",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PGN",game.export()));Toast.makeText(this,"PGN을 복사했습니다",Toast.LENGTH_SHORT).show();});actions.addView(explain,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(export,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(actions);
        feedback=text("",15,ink);root.addView(feedback);
        root.addView(text("학습 하이라이트",21,ink));root.addView(text("작은 평가 차이에서 발전 가능한 판단을 찾습니다. 큰 실수 목록이나 확정적인 실력 진단이 아닙니다.",12,muted));
        analyzeButton=button("전체 분석 · 하이라이트 생성",()->{if(analyzing){cancel=true;status.setText("현재 수를 마친 뒤 중단합니다. 완료된 분석은 저장됩니다.");}else analyzeAll();});root.addView(analyzeButton);highlightList=new LinearLayout(this);highlightList.setOrientation(LinearLayout.VERTICAL);root.addView(highlightList);
        show(index);renderHighlights();
        if(getIntent().getBooleanExtra("autoAnalyze",false)){getIntent().removeExtra("autoAnalyze");analyzeAll();}
    }
    private void ensureEngine()throws Exception {if(engine==null)engine=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");}
    private void show(int ply){
        index=Math.max(0,Math.min(game.plies().size(),ply));board.board=new Chess(index==0?game.initial():game.plies().get(index-1).after());board.lastFrom=board.lastTo=-1;
        if(index>0){Chess.Move m=Chess.Move.parse(game.plies().get(index-1).uci());board.lastFrom=m.from();board.lastTo=m.to();}
        board.invalidate();position.setText(index==0?"시작 국면":index+" / "+game.plies().size()+" 반수 · "+label(index-1));
        SpannableStringBuilder notation=new SpannableStringBuilder();
        for(int i=0;i<game.plies().size();i++){
            var p=game.plies().get(i);if(p.white()||i==0)notation.append(p.number()+(p.white()?". ":"... "));int start=notation.length();notation.append(p.san());int selected=i;
            notation.setSpan(new ClickableSpan(){@Override public void onClick(View v){show(selected+1);}@Override public void updateDrawState(TextPaint ds){ds.setColor(selected+1==index?getColor(android.R.color.holo_green_dark):muted);ds.setUnderlineText(selected+1==index);}},start,notation.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);notation.append("   ");
        }movesText.setText(notation);renderFeedback();
    }
    private String label(int ply){var p=game.plies().get(ply);return p.number()+(p.white()?". ":"... ")+p.san()+" ("+p.uci()+")";}
    private JSONObject entry(int ply){var record=records.find(recordId);return record==null?null:record.analyses().optJSONObject(Integer.toString(ply));}
    private void renderFeedback(){
        if(index==0){feedback.setText("수순을 누르거나 다음 버튼으로 이동하세요. 하이라이트를 누르면 해당 수 직전 국면으로 이동합니다.");return;}
        JSONObject entry=entry(index-1);if(entry==null){feedback.setText("아직 이 수의 분석이 없습니다. 전체 분석 또는 이 수 해설을 선택하세요.");return;}
        StringBuilder s=new StringBuilder(entry.optString("local","저장된 분석"));
        try{Stockfish.Analysis a=AnalysisJson.analysis(entry.getJSONObject("details").getJSONObject("analysis"));for(var l:a.lines())s.append("\n후보 ").append(l.rank()).append(" · ").append(l.pv().get(0)).append(" · ").append(l.score()).append(" · 깊이 ").append(l.depth()).append("\n").append(String.join(" → ",l.pv()));}catch(Exception ignored){}
        if(entry.has("aiResponse"))s.append("\n\n").append(entry.optString("aiText"));else s.append("\n\n").append(entry.optString("aiText","Codex 설명은 이 수 해설 버튼으로 생성할 수 있습니다."));
        feedback.setText(s.toString());
    }
    private JSONObject analyzePly(int ply)throws Exception {
        JSONObject saved=entry(ply);if(saved!=null&&saved.has("details")&&saved.has("payload"))return saved;
        ensureEngine();var p=game.plies().get(ply);Chess before=new Chess(p.before()),after=new Chess(p.after());
        var a=engine.analyze(before,1500,3,null);Stockfish.Line actual=null;for(var l:a.lines())if(l.pv().get(0).equals(p.uci()))actual=l;
        if(actual==null)actual=engine.analyze(before,1500,1,p.uci()).top();var afterA=engine.analyze(after,500,1,null);
        var prefs=getSharedPreferences("coach",0);JSONObject payload=CoachClient.payload(before,after,p.uci(),a,actual,prefs.getString("model",""),Collections.emptyList());
        String local="실전 "+p.san()+" ("+p.uci()+")";
        if(a.top()!=null)local+="\n추천 "+a.top().pv().get(0)+" · 백 기준 "+a.top().score()+"\n분석 엔진: Stockfish 19 · 실력 제한 없음";
        JSONObject patch=new JSONObject().put("details",AnalysisJson.details(a,actual,afterA)).put("payload",payload).put("local",local);
        records.patch(recordId,ply,patch);return entry(ply);
    }
    private void analyzeAll(){
        if(analyzing)return;analyzing=true;cancel=false;analyzeButton.setText("분석 중단");status.setText("분석 준비 중…");
        engineJobs.submit(()->{
            List<Highlights.Finding> findings=new ArrayList<>();
            try{
                for(int i=0;i<game.plies().size();i++){
                    if(cancel||destroyed)break;int ply=i;
                    runOnUiThread(()->{if(!destroyed)status.setText("Stockfish 분석 "+(ply+1)+" / "+game.plies().size()+" · 완료된 수는 저장됨");});
                    JSONObject saved=analyzePly(i);JSONObject details=saved.getJSONObject("details");var analysis=AnalysisJson.analysis(details.getJSONObject("analysis"));Stockfish.Line actual=details.isNull("playedScore")?null:AnalysisJson.line(details.getJSONObject("playedScore"));
                    var finding=Highlights.find(i,new Chess(game.plies().get(i).before()),game.plies().get(i).uci(),analysis,actual);if(finding!=null)findings.add(finding);
                    runOnUiThread(()->{if(!destroyed){renderFeedback();requestExplanation(ply);}});
                }
                if(!cancel&&!destroyed)records.highlights(recordId,Highlights.select(findings));
                runOnUiThread(()->{if(!destroyed){status.setText(cancel?"분석 중단 · 저장된 수부터 재개할 수 있습니다.":"Stockfish 분석 완료 · 학습 하이라이트 저장됨");renderHighlights();}});
            }catch(Exception e){runOnUiThread(()->{if(!destroyed)status.setText("분석 실패 · "+e.getMessage()+" · 완료된 분석은 보존됩니다.");});}
            finally{analyzing=false;runOnUiThread(()->{if(!destroyed)analyzeButton.setText("전체 분석 · 하이라이트 생성");});}
        });
    }
    private void explainSelected(){if(game.plies().isEmpty())return;int ply=index==0?0:index-1;status.setText("이 수 분석·해설 준비 중…");engineJobs.submit(()->{try{analyzePly(ply);runOnUiThread(()->{if(!destroyed){show(ply+1);requestExplanation(ply);status.setText("분석 저장됨 · 해설 진행 상황은 아래에 표시됩니다.");}});}catch(Exception e){runOnUiThread(()->status.setText("분석 실패 · "+e.getMessage()));}});}
    private void requestExplanation(int ply){
        JSONObject saved=entry(ply);if(saved==null||pending.contains(ply))return;
        String model=getSharedPreferences("coach",0).getString("model","");JSONObject response=saved.optJSONObject("aiResponse");
        if(response!=null&&(model.isEmpty()||model.equals(response.optString("model"))))return;
        if(getSharedPreferences("coach",0).getString("endpoint","").trim().isEmpty())return;
        JSONObject payload=saved.optJSONObject("payload");if(payload==null)return;
        try{payload.put("model",model);JSONArray highlights=records.find(recordId).highlights();for(int i=0;i<highlights.length();i++){JSONObject f=highlights.getJSONObject(i);if(f.getInt("ply")==ply)payload.put("learningFocus",f.getString("focus"));}}catch(Exception ignored){}
        pending.add(ply);try{records.patch(recordId,ply,new JSONObject().put("aiText","Codex 해설 생성 중…"));}catch(Exception ignored){}renderFeedback();
        coachJobs.submit(()->{
            if(destroyed)return;
            try{JSONObject result=client.request("/v1/explain",payload);records.patch(recordId,ply,new JSONObject().put("aiResponse",result).put("aiText",AnalysisJson.explanation(result)));}
            catch(Exception e){try{records.patch(recordId,ply,new JSONObject().put("aiText","Codex 해설 실패 · "+e.getMessage()+" · 이 수 해설로 재시도할 수 있습니다."));}catch(Exception ignored){}}
            finally{runOnUiThread(()->{pending.remove(ply);if(!destroyed)renderFeedback();});}
        });
    }
    private void renderHighlights(){
        highlightList.removeAllViews();var saved=records.find(recordId);if(saved==null)return;
        if(saved.highlights().length()==0){highlightList.addView(text("아직 선정된 하이라이트가 없습니다. 직접 둔 대국도 생성 버튼으로 같은 분석을 받을 수 있습니다. 분석 결과에 적합한 국면이 없으면 목록은 비어 있을 수 있습니다.",13,muted));return;}
        for(int i=0;i<saved.highlights().length();i++){JSONObject f=saved.highlights().optJSONObject(i);if(f==null)continue;int ply=f.optInt("ply");LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.addView(button(label(ply)+" · "+f.optString("title"),()->{show(ply);new AlertDialog.Builder(this).setTitle(f.optString("title")).setMessage(f.optString("reason")+"\n\n표시된 국면은 해당 수를 두기 직전입니다.").setPositiveButton("실전 수와 해설 보기",(d,w)->{show(ply+1);requestExplanation(ply);}).setNegativeButton("직전 국면 보기",null).show();}));card.addView(text(f.optString("reason"),13,muted));highlightList.addView(card);}
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("index",index);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){destroyed=true;cancel=true;engineJobs.shutdownNow();coachJobs.shutdownNow();if(engine!=null)engine.close();super.onDestroy();}
}
