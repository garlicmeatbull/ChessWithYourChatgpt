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
    private final ExecutorService engineJobs=EngineWork.queue(),coachJobs=Executors.newSingleThreadExecutor();
    private volatile boolean destroyed=false,cancel=false;
    private volatile Stockfish engine;
    private volatile boolean analyzing=false;
    private final Map<Integer,String> pending=new HashMap<>();
    private long recordId;
    private Pgn.Game game;
    private Records records;
    private CoachClient client;
    private int index=0,ink,muted;
    private BoardView board;
    private TextView position,feedback,status,movesText,advice;
    private ScrollView reviewScroll;
    private Button modelChip; private boolean recommendation=false; private LinearLayout candidates;
    private LinearLayout highlightList;
    private Button analyzeButton;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setPadding(0,dp(5),0,dp(6));return t;}
    private Button button(String s,Runnable action){return Ui.button(this,s,action);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);records=Records.get(this);client=new CoachClient(this);recordId=getIntent().getLongExtra("recordId",0);Records.Saved saved=records.find(recordId);
        if(saved==null){finish();return;}try{game=Pgn.parse(saved.pgn());}catch(Exception e){finish();return;}
        ink=getColor(R.color.ink);muted=getColor(R.color.muted);index=state==null?0:state.getInt("index",0);
        LinearLayout outer=Ui.screen(this,false);Ui.header(this,outer,"대국 복기");
        LinearLayout coach=Ui.card(this),top=new LinearLayout(this);top.addView(Ui.text(this,"AI 코치",15,true),new LinearLayout.LayoutParams(0,-2,1));modelChip=button(Ui.modelLabel(this)+" ▾",()->Ui.models(this,()->{renderFeedback();if(index>0)requestExplanation(index-1);}));top.addView(modelChip);coach.addView(top);advice=Ui.text(this,"",15,false);advice.setMaxLines(4);advice.setEllipsize(TextUtils.TruncateAt.END);advice.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("AI 코치 · "+Ui.modelLabel(this)).setMessage(advice.getText()).setPositiveButton("확인",null).setNeutralButton("연결 관리",(d,w)->startActivity(new Intent(this,ConnectionActivity.class))).show());coach.addView(advice);outer.addView(coach);Ui.gap(outer,12);
        ScrollView scroll=new ScrollView(this);reviewScroll=scroll;LinearLayout root=Ui.column(this);scroll.addView(root);outer.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));root.addView(text(saved.title(),21,ink));status=text("저장된 분석과 해설을 확인하세요.",12,muted);root.addView(status);
        board=new BoardView(this);board.onSquare=null;FrameLayout boardFrame=new FrameLayout(this);boardFrame.addView(board,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));root.addView(boardFrame,new LinearLayout.LayoutParams(-1,getResources().getDisplayMetrics().widthPixels-dp(40)));
        position=text("",15,ink);root.addView(position);
        LinearLayout navigation=new LinearLayout(this);for(Button b:new Button[]{button("처음",()->show(0)),button("‹ 이전",()->show(index-1)),button("다음 ›",()->show(index+1)),button("마지막",()->show(game.plies().size()))})navigation.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));outer.addView(navigation);
        LinearLayout actions=new LinearLayout(this);actions.addView(button("추천수 보기",()->{recommendation=!recommendation;show(index);}),new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(button("이 수 해설",this::explainSelected),new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(button("하이라이트",()->reviewScroll.smoothScrollTo(0,Math.max(0,highlightList.getTop()-dp(90)))),new LinearLayout.LayoutParams(0,dp(48),1));outer.addView(actions);
        feedback=text("",14,ink);root.addView(feedback);candidates=Ui.column(this);root.addView(candidates);
        movesText=text("",15,muted);movesText.setMovementMethod(LinkMovementMethod.getInstance());root.addView(movesText);
        root.addView(button("PGN 복사",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PGN",game.export()));Toast.makeText(this,"PGN을 복사했습니다",Toast.LENGTH_SHORT).show();}));root.addView(button("판 표시 안내",()->Ui.legend(this)));
        root.addView(text("학습 하이라이트",21,ink));root.addView(text("발전 가능한 판단을 골라 정리합니다. 항목을 누르면 해당 국면으로 이동해요.",12,muted));
        analyzeButton=button("전체 분석 · 하이라이트 생성",()->{if(analyzing){cancel=true;status.setText("현재 수를 마친 뒤 중단합니다. 완료된 분석은 저장됩니다.");}else analyzeAll();});root.addView(analyzeButton);highlightList=Ui.column(this);root.addView(highlightList);
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot){int size=Math.max(dp(180),Math.min(getResources().getDisplayMetrics().widthPixels-dp(40),b-t-dp(90)));boardFrame.getLayoutParams().height=size;boardFrame.requestLayout();}});
        show(index);renderHighlights();
        if(getIntent().getBooleanExtra("autoAnalyze",false)){getIntent().removeExtra("autoAnalyze");analyzeAll();}
    }
    private void ensureEngine()throws Exception {if(engine==null)engine=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");}
    private void show(int ply){
        reviewScroll.smoothScrollTo(0,0);index=Math.max(0,Math.min(game.plies().size(),ply));board.board=new Chess(index==0?game.initial():game.plies().get(index-1).after());board.lastFrom=board.lastTo=-1;
        if(index>0){Chess.Move m=Chess.Move.parse(game.plies().get(index-1).uci());board.lastFrom=m.from();board.lastTo=m.to();}
        board.judgment=MoveJudgment.Kind.UNKNOWN;board.judgedSquare=-1;board.recommendation=null;board.playedArrow=null;
        board.invalidate();position.setText(index==0?"시작 국면":index+" / "+game.plies().size()+" 반수 · "+label(index-1));
        SpannableStringBuilder notation=new SpannableStringBuilder();
        for(int i=0;i<game.plies().size();i++){
            var p=game.plies().get(i);if(p.white()||i==0)notation.append(p.number()+(p.white()?". ":"... "));int start=notation.length();notation.append(p.san());int selected=i;
            notation.setSpan(new ClickableSpan(){@Override public void onClick(View v){show(selected+1);}@Override public void updateDrawState(TextPaint ds){ds.setColor(selected+1==index?getColor(android.R.color.holo_green_dark):muted);ds.setUnderlineText(selected+1==index);}},start,notation.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);notation.append("   ");
        }movesText.setText(notation);renderFeedback();
    }
    private String label(int ply){var p=game.plies().get(ply);return p.number()+(p.white()?". ":"... ")+p.san();}
    private JSONObject entry(int ply){var record=records.find(recordId);return record==null?null:record.analyses().optJSONObject(Integer.toString(ply));}
    private JSONObject cached(JSONObject e,String model){if(e==null)return null;JSONObject map=e.optJSONObject("aiByModel"),found=map==null?null:map.optJSONObject(model);if(found!=null)return found;JSONObject r=e.optJSONObject("aiResponse");if(r!=null&&model.equals(r.optString("model")))try{return new JSONObject().put("text",e.optString("aiText"));}catch(Exception ignored){}return null;}
    private void renderFeedback(){
        modelChip.setText(Ui.modelLabel(this)+" ▾");candidates.removeAllViews();String model=Ui.model(this);JSONObject e=index==0?null:entry(index-1),ai=cached(e,model);
        String aiText=ai==null?"":ai.optString("text");if(aiText.startsWith("CODEX /")){int line=aiText.indexOf('\n');if(line>=0)aiText=aiText.substring(line+1);}
        advice.setText(ai!=null?aiText:index>0&&model.equals(pending.get(index-1))?"이 수의 흐름과 다음 계획을 정리하고 있어요…":"AI 코치를 연결하면 이 수의 조언을 볼 수 있어요. 연결 후 ‘이 수 해설’을 눌러주세요.");
        if(index==0){feedback.setText("수순을 누르거나 다음 버튼으로 이동하세요.");return;}
        if(e==null){feedback.setText("아직 분석하지 않은 수예요. ‘이 수 해설’ 또는 전체 분석을 선택하세요.");return;}
        feedback.setText(e.optString("local","저장된 분석"));
        try{var p=game.plies().get(index-1);JSONObject detail=e.getJSONObject("details");var a=AnalysisJson.analysis(detail.getJSONObject("analysis"));var actual=detail.isNull("playedScore")?null:AnalysisJson.line(detail.getJSONObject("playedScore"));var judgment=MoveJudgment.assess(new Chess(p.before()),p.uci(),a,actual);board.judgment=judgment.kind();board.judgedSquare=Chess.Move.parse(p.uci()).to();feedback.setText(judgment.kind().symbol+" "+judgment.kind().label+" · "+judgment.reason());
            if(recommendation){board.board=new Chess(p.before());board.recommendation=a.best();board.playedArrow=p.uci();board.judgedSquare=-1;position.setText(label(index-1)+" · 착수 전 추천 이동");}
            for(var line:a.lines()){if(line.pv().isEmpty())continue;String uci=line.pv().get(0),san=new Chess(p.before()).san(Chess.Move.parse(uci));Button candidate=button("↗ "+san+"   "+line.score(),()->{board.board=new Chess(p.before());board.recommendation=uci;board.playedArrow=p.uci();board.judgedSquare=-1;board.invalidate();reviewScroll.smoothScrollTo(0,0);position.setText("착수 전 · 후보 "+san);});candidates.addView(candidate);}
            board.invalidate();
        }catch(Exception ignored){}
    }
    private JSONObject analyzePly(int ply)throws Exception {
        JSONObject saved=entry(ply);if(saved!=null&&saved.has("details")&&saved.has("payload"))return saved;
        ensureEngine();var p=game.plies().get(ply);Chess before=new Chess(p.before()),after=new Chess(p.after());
        var a=engine.analyze(before,1500,3,null);Stockfish.Line actual=null;for(var l:a.lines())if(l.pv().get(0).equals(p.uci()))actual=l;
        if(actual==null)actual=engine.analyze(before,1500,1,p.uci()).top();var afterA=engine.analyze(after,500,1,null);
        var prefs=getSharedPreferences("coach",0);JSONObject payload=CoachClient.payload(before,after,p.uci(),a,actual,prefs.getString("model",""),Collections.emptyList());
        String local="실전 "+p.san();
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
    private void explainSelected(){if(game.plies().isEmpty())return;int ply=index==0?0:index-1;status.setText("이 수 분석·해설 준비 중…");engineJobs.submit(()->{try{analyzePly(ply);runOnUiThread(()->{if(!destroyed){show(ply+1);requestExplanation(ply);status.setText("분석 저장됨 · 해설은 상단에 표시됩니다.");}});}catch(Exception e){runOnUiThread(()->status.setText("분석 실패 · "+e.getMessage()));}});}
    private void requestExplanation(int ply){
        JSONObject saved=entry(ply);String model=Ui.model(this);if(saved==null||model.equals(pending.get(ply))||cached(saved,model)!=null)return;
        if(getSharedPreferences("coach",0).getString("endpoint","").trim().isEmpty())return;
        JSONObject payload;try{payload=new JSONObject(saved.getJSONObject("payload").toString());payload.put("model",model);JSONArray hs=records.find(recordId).highlights();for(int i=0;i<hs.length();i++){JSONObject f=hs.getJSONObject(i);if(f.getInt("ply")==ply)payload.put("learningFocus",f.getString("focus"));}}catch(Exception e){return;}
        pending.put(ply,model);renderFeedback();
        coachJobs.submit(()->{if(destroyed)return;try{JSONObject result=client.request("/v1/explain",payload);records.explanation(recordId,ply,result,AnalysisJson.explanation(result));}
            catch(Exception e){runOnUiThread(()->{if(!destroyed)status.setText("해설 연결 실패 · 연결 관리에서 확인하고 다시 요청해주세요.");});}
            finally{runOnUiThread(()->{if(model.equals(pending.get(ply)))pending.remove(ply);if(!destroyed)renderFeedback();});}});
    }
    private void renderHighlights(){
        highlightList.removeAllViews();var saved=records.find(recordId);if(saved==null)return;
        if(saved.highlights().length()==0){highlightList.addView(text("아직 선정된 하이라이트가 없습니다. 직접 둔 대국도 생성 버튼으로 같은 분석을 받을 수 있습니다. 분석 결과에 적합한 국면이 없으면 목록은 비어 있을 수 있습니다.",13,muted));return;}
        for(int i=0;i<saved.highlights().length();i++){JSONObject f=saved.highlights().optJSONObject(i);if(f==null)continue;int ply=f.optInt("ply");LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.addView(button(label(ply)+" · "+f.optString("title"),()->{show(ply);new AlertDialog.Builder(this).setTitle(f.optString("title")).setMessage(f.optString("reason")+"\n\n표시된 국면은 해당 수를 두기 직전입니다.").setPositiveButton("실전 수와 해설 보기",(d,w)->{show(ply+1);requestExplanation(ply);}).setNegativeButton("직전 국면 보기",null).show();}));card.addView(text(f.optString("reason"),13,muted));highlightList.addView(card);}
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("index",index);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){destroyed=true;cancel=true;engineJobs.shutdownNow();coachJobs.shutdownNow();EngineWork.close(()->{if(engine!=null)engine.close();});super.onDestroy();}
}
