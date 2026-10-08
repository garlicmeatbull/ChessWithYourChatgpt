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
public final class ReviewActivity extends ThemedActivity {
    private final ExecutorService engineJobs=EngineWork.queue();private final CoachQueue coachJobs=new CoachQueue();
    private volatile boolean destroyed=false,cancel=false;
    private volatile Stockfish engine;
    private volatile boolean analyzing=false;
    private static final class Pending {final String model;final boolean detail,foreground;final CoachMode.Ticket ticket;volatile boolean running;String text="";Pending(String m,boolean d,boolean f,CoachMode.Ticket t){model=m;detail=d;foreground=f;ticket=t;}}
    private final Map<Integer,Pending> pending=new ConcurrentHashMap<>();
    private SharedPreferences coachPrefs;private CoachMode coachMode;private boolean selectedAnalyzing;
    private final SharedPreferences.OnSharedPreferenceChangeListener modeListener=(p,k)->{if("autoCoach".equals(k)&&!destroyed)automationChanged();};
    private OutcomeView outcomeView;private GameOutcome finalOutcome;private CoachPanel coachPanel;private List<CoachVisual.Scene> coachScenes=Collections.emptyList();private String visualKey="";
    private long recordId;
    private Pgn.Game game;
    private Records records;
    private CoachClient client;
    private int index=0,ink,muted;
    private BoardView board;private FrameLayout boardFrame;
    private TextView position,feedback,status,movesText,advice,planUse;
    private ScrollView reviewScroll;
    private Button modelChip; private boolean recommendation=false; private LinearLayout candidates;
    private LinearLayout highlightList;
    private Button analyzeButton;private Dialog toolsDialog;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setPadding(0,dp(5),0,dp(6));return t;}
    private Button button(String s,Runnable action){return Ui.button(this,s,action);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);records=Records.get(this);client=new CoachClient(this);recordId=getIntent().getLongExtra("recordId",0);
        LinearLayout loading=Ui.screen(this,false);Ui.header(this,loading,"대국 복기");loading.addView(new ProgressBar(this));loading.addView(Ui.text(this,"대국 기록을 불러오는 중…",15,false));
        engineJobs.submit(()->{try{Records.Saved saved=records.find(recordId);if(saved==null)throw new IllegalArgumentException("삭제되었거나 없는 기록입니다.");Pgn.Game parsed=Pgn.parse(saved.pgn());runOnUiThread(()->{if(!destroyed){game=parsed;loaded(saved,state);}});}catch(Exception e){runOnUiThread(()->{if(!destroyed){loading.removeAllViews();Ui.header(this,loading,"대국 복기");loading.addView(Ui.text(this,"기록을 불러오지 못했어요. "+e.getMessage(),15,false));}});}});
    }
    private void loaded(Records.Saved saved,Bundle state){
        coachPrefs=getSharedPreferences("coach",0);coachMode=new CoachMode(coachPrefs.getBoolean("autoCoach",false));ink=getColor(R.color.ink);muted=getColor(R.color.muted);index=state==null?0:state.getInt("index",0);
        LinearLayout outer=Ui.screen(this,false);Ui.header(this,outer,"대국 복기");outer.removeViewAt(1);Ui.gap(outer,8);
        LinearLayout coach=Ui.card(this),top=new LinearLayout(this);top.addView(Ui.text(this,"AI 코치",15,true),new LinearLayout.LayoutParams(0,-2,1));modelChip=Ui.link(this,Ui.modelLabel(this)+" ▾",()->Ui.models(this,()->{renderFeedback();if(index>0)requestExplanation(index-1);}));modelChip.setMaxWidth(dp(168));top.addView(modelChip,new LinearLayout.LayoutParams(-2,dp(40)));coach.setPadding(dp(14),dp(10),dp(14),dp(10));coach.addView(top);planUse=Ui.text(this,"Using ChatGPT plan · Manage usage",11,false);planUse.setTextColor(muted);planUse.setOnClickListener(v->Ui.usage(this));planUse.setVisibility(View.GONE);coach.addView(planUse);coachPanel=new CoachPanel(this,coach);coachPanel.automation(coachPrefs,this::manualJudgment);advice=coachPanel.textView();Ui.coachSlot(this,outer,coach);Ui.gap(outer,8);
        ScrollView scroll=new ScrollView(this);reviewScroll=scroll;LinearLayout root=Ui.column(this);scroll.addView(root);outcomeView=new OutcomeView(this);finalOutcome=saved.origin().equals("played")?GameOutcome.forPlayer(game.result(),GameSession.playerWhite(game),new Chess(game.plies().isEmpty()?game.initial():game.plies().get(game.plies().size()-1).after()).terminal(1)):GameOutcome.from(game.result(),false,null);TextView title=text(saved.title(),15,ink);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);outer.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));status=text("저장된 분석과 해설을 확인하세요.",12,muted);status.setMaxLines(1);outer.addView(status,new LinearLayout.LayoutParams(-1,dp(26)));
        board=new BoardView(this);board.onSquare=null;boardFrame=new FrameLayout(this);boardFrame.addView(board,new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER));outer.addView(boardFrame,new LinearLayout.LayoutParams(-1,0,1));
        position=text("",13,ink);position.setMaxLines(1);FrameLayout resultSlot=new FrameLayout(this);resultSlot.addView(position,new FrameLayout.LayoutParams(-1,-1));outcomeView.compact();resultSlot.addView(outcomeView,new FrameLayout.LayoutParams(-1,-1));outer.addView(resultSlot,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout navigation=new LinearLayout(this);for(Button b:new Button[]{button("처음",()->show(0)),button("‹ 이전",()->show(index-1)),button("다음 ›",()->show(index+1)),button("마지막",()->show(game.plies().size()))})Ui.action(navigation,b,44);outer.addView(navigation);
        Ui.gap(outer,8);LinearLayout actions=new LinearLayout(this);Ui.action(actions,Ui.link(this,"추천수",()->{recommendation=!recommendation;show(index);}),44);Ui.action(actions,Ui.link(this,"복기 도구",this::openTools),44);outer.addView(actions);root.addView(button("선택한 수 Stockfish 분석",()->analyzeSelected(false)));Ui.gap(root,10);
        root.addView(button("이 위치에서 이어 두기",this::branch));Ui.gap(root,12);feedback=text("",14,ink);root.addView(feedback);candidates=Ui.column(this);root.addView(candidates);
        movesText=text("",15,muted);movesText.setMovementMethod(LinkMovementMethod.getInstance());root.addView(movesText);
        root.addView(button("PGN 복사",()->{((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PGN",game.export()));Toast.makeText(this,"PGN을 복사했습니다",Toast.LENGTH_SHORT).show();}));root.addView(button("판 표시 안내",()->Ui.legend(this)));
        root.addView(text("학습 하이라이트",21,ink));root.addView(text("발전 가능한 판단을 골라 정리합니다. 항목을 누르면 해당 국면으로 이동해요.",12,muted));
        analyzeButton=button("전체 분석 · 하이라이트 생성",()->{if(analyzing){cancel=true;status.setText("현재 수를 마친 뒤 중단합니다. 완료된 분석은 저장됩니다.");}else analyzeAll();});root.addView(analyzeButton);highlightList=Ui.column(this);root.addView(highlightList);

        show(index);renderHighlights();coachPrefs.registerOnSharedPreferenceChangeListener(modeListener);
        if(getIntent().getBooleanExtra("autoAnalyze",false)){getIntent().removeExtra("autoAnalyze");analyzeAll();}
    }
    private void ensureEngine()throws Exception {if(engine==null)engine=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");}
    private void show(int ply){show(ply,true);}
    private void show(int ply,boolean autoExplain){
        int previousIndex=index;Chess previousBoard=board.board.copy();reviewScroll.smoothScrollTo(0,0);index=Math.max(0,Math.min(game.plies().size(),ply));outcomeView.bind(index==game.plies().size()?finalOutcome:null);board.board=new Chess(index==0?game.initial():game.plies().get(index-1).after());board.setLastMove(index>0?Chess.Move.parse(game.plies().get(index-1).uci()):null);if(index==previousIndex+1&&previousBoard.fen().equals(game.plies().get(index-1).before()))board.animateMove(previousBoard,Chess.Move.parse(game.plies().get(index-1).uci()),board.board);
        board.judgment=MoveJudgment.Kind.UNKNOWN;board.judgedSquare=-1;board.recommendation=null;board.playedArrow=board.aiArrow=null;
        board.invalidate();position.setText(index==0?"시작 국면":index+" / "+game.plies().size()+" 반수 · "+label(index-1));
        SpannableStringBuilder notation=new SpannableStringBuilder();
        for(int i=0;i<game.plies().size();i++){
            var p=game.plies().get(i);if(p.white()||i==0)notation.append(p.number()+(p.white()?". ":"... "));int start=notation.length();notation.append(p.san());int selected=i;
            notation.setSpan(new ClickableSpan(){@Override public void onClick(View v){show(selected+1);}@Override public void updateDrawState(TextPaint ds){ds.setColor(selected+1==index?getColor(R.color.accent):muted);ds.setUnderlineText(selected+1==index);}},start,notation.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);notation.append("   ");
        }movesText.setText(notation);renderFeedback();if(autoExplain&&index>0)requestExplanation(index-1);
    }
    private void openTools(){if(toolsDialog!=null&&toolsDialog.isShowing())return;toolsDialog=new Dialog(this);LinearLayout content=Ui.card(this),header=new LinearLayout(this);header.addView(Ui.text(this,"복기 도구 · 하이라이트",19,true),new LinearLayout.LayoutParams(0,-2,1));header.addView(button("닫기",()->toolsDialog.dismiss()));content.addView(header);if(reviewScroll.getParent()!=null)((android.view.ViewGroup)reviewScroll.getParent()).removeView(reviewScroll);content.addView(reviewScroll,new LinearLayout.LayoutParams(-1,0,1));toolsDialog.setContentView(content);toolsDialog.show();if(toolsDialog.getWindow()!=null){toolsDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);toolsDialog.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(24),(int)(getResources().getDisplayMetrics().heightPixels*.8));}}
    private void branch(){try{GameSession.prefix(game,index);}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return;}new AlertDialog.Builder(this).setTitle("이 위치에서 이어 둘까요?").setMessage("선택한 "+index+"번째 수까지 복사해 별도 기록을 만듭니다. 현재 차례의 색으로 Stockfish와 대국합니다. 기존 기록은 그대로 보관합니다.").setNegativeButton("취소",null).setPositiveButton("이어 두기",(d,w)->{long id=records.branch(recordId,game,index);startActivity(new Intent(this,MainActivity.class).putExtra("resumeRecord",id));}).show();}
    private String label(int ply){var p=game.plies().get(ply);return p.number()+(p.white()?". ":"... ")+p.san();}
    private JSONObject entry(int ply){var record=records.find(recordId);return record==null?null:record.analyses().optJSONObject(Integer.toString(ply));}
    private JSONObject cached(JSONObject e,String model){if(e==null)return null;JSONObject map=e.optJSONObject("aiByModel"),found=map==null?null:map.optJSONObject(model);if(found!=null)return found;JSONObject r=e.optJSONObject("aiResponse");if(r!=null&&model.equals(r.optString("model")))try{return new JSONObject().put("response",r).put("text",e.optString("aiText"));}catch(Exception ignored){}return null;}
    private void renderFeedback(){planUse.setText(ChatGptAccounts.connected(this)?"Using ChatGPT plan · Manage usage":"ChatGPT 연결 · Manage usage");
        modelChip.setText(Ui.modelLabel(this)+" ▾");candidates.removeAllViews();String model=Ui.model(this);JSONObject e=index==0?null:entry(index-1),ai=cached(e,model);
        renderCoach(e,ai,model);
        if(index==0){feedback.setText("수순을 누르거나 다음 버튼으로 이동하세요.");return;}
        if(e==null){feedback.setText("아직 분석하지 않은 수예요. ‘Stockfish 분석’ 또는 전체 분석을 선택하세요.");return;}
        feedback.setText(e.optString("local","저장된 분석"));
        try{var p=game.plies().get(index-1);JSONObject detail=e.getJSONObject("details");var a=AnalysisJson.analysis(detail.getJSONObject("analysis"));var actual=detail.isNull("playedScore")?null:AnalysisJson.line(detail.getJSONObject("playedScore"));var judgment=MoveJudgment.assess(new Chess(p.before()),p.uci(),a,actual);board.judgment=judgment.kind();board.judgedSquare=Chess.Move.parse(p.uci()).to();feedback.setText(judgment.kind().symbol+" "+judgment.kind().label+" · "+judgment.reason());
            if(recommendation){board.board=new Chess(p.before());board.setLastMove(index>1?Chess.Move.parse(game.plies().get(index-2).uci()):null);board.recommendation=a.best();board.playedArrow=p.uci();board.judgedSquare=-1;position.setText(label(index-1)+" · 착수 전 추천 이동");}
            for(var line:a.lines()){if(line.pv().isEmpty())continue;String uci=line.pv().get(0),san=new Chess(p.before()).san(Chess.Move.parse(uci));Button candidate=button("↗ "+san+"   "+line.score(),()->{board.board=new Chess(p.before());board.setLastMove(index>1?Chess.Move.parse(game.plies().get(index-2).uci()):null);board.recommendation=uci;board.playedArrow=p.uci();board.judgedSquare=-1;updateVisualOnBoard();board.invalidate();reviewScroll.smoothScrollTo(0,0);position.setText("착수 전 · 후보 "+san);});candidates.addView(candidate);}
            updateVisualOnBoard();board.invalidate();
        }catch(Exception ignored){}
    }
    private void renderCoach(JSONObject e,JSONObject ai,String model){
        Pending job=index>0?pending.get(index-1):null;boolean streaming=job!=null&&model.equals(job.model);
        String summary=ai!=null?CoachText.summary(ai):streaming&&!job.detail&&!job.text.isEmpty()?CoachText.previewSummary(job.text):streaming?"이번 수의 핵심 조언을 준비하고 있어요…":coachMode.automatic()?"이 수의 AI 조언을 준비합니다.":"Stockfish 평가는 항상 받을 수 있어요. AI 판단을 누르면 선택한 수만 설명해 드려요.";
        String core=ai!=null?CoachText.headline(ai):streaming&&!job.detail?CoachText.previewHeadline(job.text):"";
        String full=streaming&&!job.text.isEmpty()?job.text:ai!=null?CoachText.markdown(ai):summary;

        if(e!=null&&!streaming&&!CoachText.detailed(ai)&&model.equals(e.optString("detailErrorModel")))full+="\n\n"+e.optString("detailError");
        coachPanel.updateAutomation(!game.plies().isEmpty()&&!selectedAnalyzing&&!streaming&&(!analyzing||e!=null&&e.has("details")));
        coachPanel.bind(model+":"+index,core,summary,full,streaming,ai!=null,job!=null&&!job.text.trim().isEmpty(),()->{if(index>0)requestExplanation(index-1,true,true,true);});
        String visualText=streaming&&!job.text.isEmpty()?job.text:ai!=null?CoachText.markdown(ai):"";JSONObject detail=e==null?null:e.optJSONObject("details");String key=index+":"+model+":"+visualText+":"+detail;
        if(!key.equals(visualKey)){visualKey=key;coachScenes=Collections.emptyList();try{if(index>0&&detail!=null){var ply=game.plies().get(index-1);var analysis=AnalysisJson.analysis(detail.getJSONObject("analysis"));var actual=detail.isNull("playedScore")?null:AnalysisJson.line(detail.getJSONObject("playedScore"));coachScenes=CoachVisual.find(new Chess(ply.before()),ply.uci(),analysis.lines(),actual,visualText);}}catch(Exception ignored){}}
        coachPanel.visuals(coachScenes,visualText);updateVisualOnBoard();

    }
    private void updateVisualOnBoard(){var scene=CoachVisual.onBoard(coachScenes,board.board);board.aiArrow=scene==null?null:scene.move();board.invalidate();}
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
                    runOnUiThread(()->{if(!destroyed){renderFeedback();requestExplanation(ply,false,false,false);}});
                }
                if(!cancel&&!destroyed)records.highlights(recordId,Highlights.select(findings));
                runOnUiThread(()->{if(!destroyed){status.setText(cancel?"분석 중단 · 저장된 수부터 재개할 수 있습니다.":"Stockfish 분석 완료 · 학습 하이라이트 저장됨");renderHighlights();}});
            }catch(Exception e){runOnUiThread(()->{if(!destroyed)status.setText("분석 실패 · "+e.getMessage()+" · 완료된 분석은 보존됩니다.");});}
            finally{analyzing=false;runOnUiThread(()->{if(!destroyed){analyzeButton.setText("전체 분석 · 하이라이트 생성");renderFeedback();}});}
        });
    }
    private void manualJudgment(){
        if(!ChatGptAccounts.connected(this)){startActivity(new Intent(this,ConnectionActivity.class));return;}
        JSONObject saved=index>0?entry(index-1):null;if(saved!=null&&saved.has("details")&&saved.has("payload")){requestExplanation(index-1,false,true,true);return;}
        analyzeSelected(true);
    }
    private void automationChanged(){
        coachMode.setAutomatic(coachPrefs.getBoolean("autoCoach",false));
        if(!coachMode.automatic())for(var item:pending.entrySet()){Pending job=item.getValue();if(job.ticket.automatic()&&!job.running)pending.remove(item.getKey(),job);}
        renderFeedback();if(coachMode.automatic()&&hasWindowFocus()&&index>0)requestExplanation(index-1);
    }
    private void analyzeSelected(boolean explicit){
        if(game.plies().isEmpty()||selectedAnalyzing)return;int ply=index==0?0:index-1;selectedAnalyzing=true;renderFeedback();status.setText("Stockfish 분석 준비 중…");
        engineJobs.submit(()->{try{analyzePly(ply);runOnUiThread(()->{if(!destroyed){selectedAnalyzing=false;show(ply+1,!explicit);if(explicit)requestExplanation(ply,false,true,true);status.setText("Stockfish 평가 저장됨");}});}catch(Exception e){runOnUiThread(()->{if(!destroyed){selectedAnalyzing=false;status.setText("분석 실패 · "+e.getMessage());renderFeedback();}});}});
    }
    private void requestExplanation(int ply){requestExplanation(ply,false,true,false);}
    private void requestExplanation(int ply,boolean detail,boolean foreground,boolean explicit){
        CoachMode.Ticket ticket=coachMode.admit(explicit);if(ticket==null)return;
        JSONObject saved=entry(ply);String model=Ui.model(this);JSONObject ai=cached(saved,model);Pending old=pending.get(ply);
        if(saved==null||ai!=null&&(!detail||CoachText.detailed(ai)))return;
        if(old!=null&&model.equals(old.model)&&(!foreground||old.foreground||old.running))return;
        if(!ChatGptAccounts.connected(this))return;
        JSONObject payload;try{payload=new JSONObject(saved.getJSONObject("payload").toString());payload.put("model",model).put("detail",detail);JSONArray hs=records.find(recordId).highlights();for(int i=0;i<hs.length();i++){JSONObject f=hs.getJSONObject(i);if(f.getInt("ply")==ply)payload.put("learningFocus",f.getString("focus"));}}catch(Exception e){return;}
        Pending job=new Pending(model,detail,foreground,ticket);pending.put(ply,job);renderFeedback();
        coachJobs.submit(()->{if(destroyed||pending.get(ply)!=job)return;job.running=true;if(!coachMode.mayStart(ticket)){runOnUiThread(()->{pending.remove(ply,job);if(!destroyed)renderFeedback();});return;}try{
            JSONObject result=client.request("/v1/explain",payload,partial->{
                if(destroyed||pending.get(ply)!=job)throw new java.io.InterruptedIOException("해설 요청이 바뀌었습니다.");
                runOnUiThread(()->{if(!destroyed&&pending.get(ply)==job){job.text=partial;if(index==ply+1&&model.equals(Ui.model(this))){JSONObject e=entry(ply);renderCoach(e,cached(e,model),model);}}});
            });
            if(!destroyed&&pending.get(ply)==job)records.explanation(recordId,ply,result,AnalysisJson.explanation(result));
        }catch(Exception e){String message=e.getMessage()==null?"해설 연결을 확인해 주세요.":e.getMessage();runOnUiThread(()->{if(!destroyed&&pending.get(ply)==job){status.setText(message);if(detail)try{records.patch(recordId,ply,new JSONObject().put("detailErrorModel",model).put("detailError",message));}catch(Exception ignored){}}});}
        finally{runOnUiThread(()->{pending.remove(ply,job);if(!destroyed)renderFeedback();});}},foreground);
    }
    private void renderHighlights(){
        highlightList.removeAllViews();var saved=records.find(recordId);if(saved==null)return;
        if(saved.highlights().length()==0){highlightList.addView(text("아직 선정된 하이라이트가 없습니다. 직접 둔 대국도 생성 버튼으로 같은 분석을 받을 수 있습니다. 분석 결과에 적합한 국면이 없으면 목록은 비어 있을 수 있습니다.",13,muted));return;}
        for(int i=0;i<saved.highlights().length();i++){JSONObject f=saved.highlights().optJSONObject(i);if(f==null)continue;int ply=f.optInt("ply");LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.addView(button(label(ply)+" · "+f.optString("title"),()->{if(toolsDialog!=null)toolsDialog.dismiss();show(ply);Dialog detail=new Dialog(this);LinearLayout content=Ui.card(this);content.addView(Ui.text(this,f.optString("title"),22,true));Ui.gap(content,12);content.addView(Ui.text(this,f.optString("reason"),15,false));Ui.gap(content,16);content.addView(button("이 수를 둔 후 보기",()->{detail.dismiss();show(ply+1);requestExplanation(ply);}));Ui.gap(content,8);content.addView(button("이 수를 두기 전 보기",()->{detail.dismiss();show(ply);}));detail.setContentView(content);detail.show();if(detail.getWindow()!=null){detail.getWindow().setBackgroundDrawableResource(android.R.color.transparent);detail.getWindow().setLayout(getResources().getDisplayMetrics().widthPixels-dp(32),-2);}}));card.addView(text(f.optString("reason"),13,muted));highlightList.addView(card);}
    }
    @Override protected void onStart(){super.onStart();if(modelChip!=null){renderFeedback();if(index>0)requestExplanation(index-1);}}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("index",index);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){destroyed=true;if(coachPrefs!=null)coachPrefs.unregisterOnSharedPreferenceChangeListener(modeListener);cancel=true;if(toolsDialog!=null)toolsDialog.dismiss();if(coachPanel!=null)coachPanel.close();engineJobs.shutdownNow();coachJobs.shutdownNow();EngineWork.close(()->{if(engine!=null)engine.close();});super.onDestroy();}
}
