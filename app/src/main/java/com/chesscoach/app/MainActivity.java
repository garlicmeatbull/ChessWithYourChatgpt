package com.chesscoach.app;
import android.app.*;
import android.os.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** Native play screen: pinned advice, live model picker, board-first visual feedback. */
public final class MainActivity extends ThemedActivity {
    private Chess game=new Chess();private volatile Stockfish engine,opponent;
    private final ExecutorService engineJobs=EngineWork.queue();private final CoachQueue coachJobs=new CoachQueue();
    private final List<String> moves=new ArrayList<>(),fens=new ArrayList<>();
    private final Map<String,Integer> repetitions=new HashMap<>();private final List<Integer> trend=new ArrayList<>();private final List<Feedback> feedback=new ArrayList<>();
    private volatile int generation=0;private volatile boolean destroyed=false,parked=false;
    private String initial=Chess.START,forcedResult="*";private int branchStart;private boolean playerWhite=true;private boolean botPending;private boolean busy=true,preview=false;private int focus=-1;
    private EvaluationBar evaluation;private BoardView board;private TextView status,coach,moveLabel,planUse;private Button modelChip,suggestion,undoButton;
    private CoachMode coachMode;private OutcomeView outcomeView;
    private final SharedPreferences.OnSharedPreferenceChangeListener modeListener=(p,k)->{if("autoCoach".equals(k)&&!destroyed)automationChanged();};
    private Dialog notationDialog;private CoachPanel coachPanel;private LinearLayout moveStrip,playControls;private SharedPreferences prefs;private CoachClient client;private long recordId;private int BG,INK,MUTED;
    private static class Feedback {String local,ai="",pendingModel="",aiModel="";volatile boolean pending,running;boolean pendingDetail,pendingAutomatic;String stream="";volatile int requestVersion;String visualText="";JSONObject visualDetails;List<CoachVisual.Scene> visuals=Collections.emptyList();JSONObject payload,details;Feedback(String s){local=s;}}
    @Override public void onCreate(Bundle state){super.onCreate(state);prefs=getSharedPreferences("coach",0);coachMode=new CoachMode(prefs.getBoolean("autoCoach",false));client=new CoachClient(this);BG=getColor(R.color.background);INK=getColor(R.color.ink);MUTED=getColor(R.color.muted);buildUi();
        if(getIntent().getBooleanExtra("newGame",false)){repetitions.put(game.key(),1);save();getIntent().removeExtra("newGame");getIntent().removeExtra("resumeRecord");}else {long requested=getIntent().getLongExtra("resumeRecord",0);if(requested!=0){prefs.edit().putLong("activeRecord",requested).commit();getIntent().removeExtra("resumeRecord");}restore();}prefs.registerOnSharedPreferenceChangeListener(modeListener);connectEngine();}
    private int dp(int n){return Ui.dp(this,n);}private TextView text(String s,int size,int color){TextView t=Ui.text(this,s,size,false);t.setTextColor(color);return t;}
    private Button button(String s,Runnable action){return Ui.button(this,s,action);}
    private void buildUi(){
        LinearLayout root=Ui.screen(this,false);LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);header.addView(button("‹",this::finish),new LinearLayout.LayoutParams(dp(44),dp(44)));TextView title=Ui.text(this,"대국",20,true);title.setPadding(dp(10),0,0,0);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));header.addView(button("⋯",this::menu),new LinearLayout.LayoutParams(dp(44),dp(44)));root.addView(header);Ui.gap(root,8);
        LinearLayout advice=Ui.card(this);advice.setPadding(dp(14),dp(10),dp(14),dp(12));LinearLayout coachHeader=new LinearLayout(this);coachHeader.setGravity(Gravity.CENTER_VERTICAL);coachHeader.addView(Ui.text(this,"AI 코치",14,true),new LinearLayout.LayoutParams(0,-2,1));modelChip=Ui.link(this,Ui.modelLabel(this)+" ⌄",this::chooseModel);modelChip.setMinHeight(dp(36));modelChip.setMaxWidth(dp(168));coachHeader.addView(modelChip,new LinearLayout.LayoutParams(-2,dp(40)));advice.addView(coachHeader);planUse=Ui.text(this,"Using ChatGPT plan · Manage usage",11,false);planUse.setTextColor(MUTED);planUse.setOnClickListener(v->Ui.usage(this));planUse.setVisibility(View.GONE);advice.addView(planUse);Ui.gap(advice,4);coachPanel=new CoachPanel(this,advice);coachPanel.automation(prefs,this::manualJudgment);coach=coachPanel.textView();Ui.coachSlot(this,root,advice);Ui.gap(root,8);outcomeView=new OutcomeView(this);
        LinearLayout info=new LinearLayout(this);info.setGravity(Gravity.CENTER_VERTICAL);status=text("Stockfish 준비 중…",12,MUTED);info.addView(status,new LinearLayout.LayoutParams(0,dp(28),1));moveLabel=text("",12,INK);moveLabel.setOnClickListener(v->Ui.legend(this));info.addView(moveLabel);
        FrameLayout frame=new FrameLayout(this);board=new BoardView(this);board.onSquare=this::select;FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-1,Gravity.CENTER);frame.addView(board,bp);root.addView(frame,new LinearLayout.LayoutParams(-1,0,1));
        evaluation=new EvaluationBar(this);root.addView(evaluation,new LinearLayout.LayoutParams(-1,dp(36)));
        HorizontalScrollView strip=new HorizontalScrollView(this);strip.setHorizontalScrollBarEnabled(false);moveStrip=new LinearLayout(this);strip.addView(moveStrip);LinearLayout notationBar=new LinearLayout(this);notationBar.addView(strip,new LinearLayout.LayoutParams(0,dp(38),1));notationBar.addView(Ui.link(this,"기보",this::openNotation),new LinearLayout.LayoutParams(dp(56),dp(38)));root.addView(notationBar,new LinearLayout.LayoutParams(-1,dp(38)));Ui.gap(root,6);
        LinearLayout controls=new LinearLayout(this);playControls=controls;suggestion=button("추천수 보기",()->{preview=!preview;refresh();});undoButton=button("무르기",this::undo);for(Button b:new Button[]{suggestion,undoButton}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);lp.setMargins(dp(4),0,dp(4),0);controls.addView(b,lp);}FrameLayout bottom=new FrameLayout(this);bottom.addView(controls,new FrameLayout.LayoutParams(-1,-1));outcomeView.compact();bottom.addView(outcomeView,new FrameLayout.LayoutParams(-1,-1));root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void openNotation(){
        if(notationDialog!=null&&notationDialog.isShowing())return;
        var saved=Records.get(this).find(recordId);if(saved==null)return;
        List<Pgn.Ply> plies=new ArrayList<>();for(int i=0;i<moves.size();i++){Chess before=new Chess(i==0?initial:fens.get(i-1));String uci=moves.get(i);plies.add(new Pgn.Ply(before.fen(),fens.get(i),uci,before.san(Chess.Move.parse(uci)),before.white,before.fullmove));}
        notationDialog=NotationDialog.show(this,plies,focus+1,saved.pgn(),ply->startActivity(new Intent(this,ReviewActivity.class).putExtra("recordId",recordId).putExtra("initialPly",ply)));
    }
    private void menu(){new AlertDialog.Builder(this).setTitle("대국 메뉴").setItems(new String[]{"대국 기록","새 대국 · 난이도 선택","AI 코치 연결","화면 설정","기권","판 표시 안내"},(d,n)->{if(n==0)startActivity(new Intent(this,RecordsActivity.class));else if(n==1){startActivity(new Intent(this,GameSetupActivity.class));finish();}else if(n==2)startActivity(new Intent(this,ConnectionActivity.class));else if(n==3)Appearance.show(this);else if(n==4)resign();else Ui.legend(this);}).show();}
    private String terminal(){String natural=game.terminal(repetitions.getOrDefault(game.key(),1));if(natural!=null)return natural;return !forcedResult.equals("*")?(forcedResult.equals("1-0")?"백 승리":forcedResult.equals("0-1")?"흑 승리":"무승부")+" · 기권":game.terminal(repetitions.getOrDefault(game.key(),1));}
    private void resign(){if(terminal()!=null)return;new AlertDialog.Builder(this).setTitle("이 대국을 기권할까요?").setMessage("패배로 기록하고 지금까지의 수와 해설을 보관합니다.").setNegativeButton("계속 두기",null).setPositiveButton("기권",(d,w)->{generation++;botPending=false;busy=false;preview=false;forcedResult=playerWhite?"0-1":"1-0";for(Feedback f:feedback){f.requestVersion++;f.pending=false;}board.selected=-1;board.targets.clear();save();refresh();}).show();}
    private void chooseModel(){Ui.models(this,()->{modelChip.setText(Ui.modelLabel(this)+" ⌄");if(focus>=0){for(Feedback other:feedback){other.requestVersion++;other.pending=false;}Feedback f=feedback.get(focus);requestExplanation(f,generation);}renderFeedback();});}
    private void connectEngine(){busy=true;int epoch=generation;engineJobs.submit(()->{if(destroyed||parked)return;try{if(engine==null)engine=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");if(opponent==null)opponent=new Stockfish(getApplicationInfo().nativeLibraryDir+"/libstockfish.so");if(destroyed){closeEngines();return;}runOnUiThread(()->{if(epoch!=generation||destroyed||parked)return;busy=false;refresh();if(terminal()!=null)return;if(!feedback.isEmpty()&&feedback.get(feedback.size()-1).details==null){int ply=feedback.size()-1;Chess before=new Chess(ply==0?initial:fens.get(ply-1));busy=true;analyzePlayed(before,game.copy(),Chess.Move.parse(moves.get(ply)),feedback.get(ply),ply,epoch,before.white!=playerWhite);}else if(game.white!=playerWhite&&terminal()==null)botTurn(epoch);});}catch(Exception e){runOnUiThread(()->{if(epoch==generation&&!destroyed){busy=true;status.setText("엔진 준비 실패");coach.setText("대국 메뉴에서 새 대국으로 다시 시작해 주세요.");}});}});}
    private void select(int s){if(preview){preview=false;refresh();return;}if(busy||game.white!=playerWhite||terminal()!=null)return;List<Chess.Move> legal=game.legalMoves(),choices=new ArrayList<>();for(var m:legal)if(m.from()==board.selected&&m.to()==s)choices.add(m);if(!choices.isEmpty()){if(choices.size()>1)new AlertDialog.Builder(this).setTitle("승격 기물").setItems(new String[]{"퀸","룩","비숍","나이트"},(d,w)->play(choices.get(w),generation,false)).show();else play(choices.get(0),generation,false);return;}board.selected=game.squares[s]!='.'&&Chess.color(game.squares[s])==playerWhite?s:-1;board.targets.clear();for(var m:legal)if(m.from()==board.selected)board.targets.add(m.to());board.invalidate();}
    private void play(Chess.Move move,int epoch,boolean bot){
        if(epoch!=generation||destroyed||parked||terminal()!=null||bot&&game.white==playerWhite)return;Chess before=game.copy();busy=true;preview=false;board.selected=-1;board.targets.clear();game.play(move);moves.add(move.uci());fens.add(game.fen());repetitions.merge(game.key(),1,Integer::sum);board.animateMove(before,move,game);board.setLastMove(move);
        Feedback entry=new Feedback((before.white==playerWhite?"내 수":"상대 수")+" · "+before.san(move)+"\n수의 평가를 계산하고 있어요.");feedback.add(entry);int ply=feedback.size()-1;if(!bot||focus<0)focus=ply;save();persistFeedback(entry,ply);refresh();analyzePlayed(before,game.copy(),move,entry,ply,epoch,bot);
    }
    private void analyzePlayed(Chess before,Chess after,Chess.Move move,Feedback entry,int ply,int epoch,boolean bot){engineJobs.submit(()->{try{var analysis=engine.analyze(before,bot?650:1300,3,null);Stockfish.Line played=null;for(var l:analysis.lines())if(!l.pv().isEmpty()&&l.pv().get(0).equals(move.uci()))played=l;if(played==null)played=engine.analyze(before,850,1,move.uci()).top();Stockfish.Line actual=played;
            // Start the one coaching request as soon as the complete candidate/played search is ready.
            // The extra after-position search only refines the displayed evaluation and can overlap HTTP.
            runOnUiThread(()->{if(epoch!=generation||destroyed)return;try{
                var judgment=MoveJudgment.assess(before,move.uci(),analysis,actual);entry.local=judgment.kind().label+" · "+before.san(move)+"\n"+judgment.reason();
                List<Integer> earlyTrend=new ArrayList<>(trend);if(actual!=null)earlyTrend.add(actual.cp());if(earlyTrend.size()>6)earlyTrend.remove(0);
                entry.payload=CoachClient.payload(before,after,move.uci(),analysis,actual,Ui.model(this),earlyTrend);entry.details=AnalysisJson.details(analysis,actual,null).put("judgment",judgment.kind().name());
                persistFeedback(entry,ply);renderFeedback();requestExplanation(entry,epoch);
            }catch(Exception ignored){}});
            var afterAnalysis=engine.analyze(after,550,1,null);runOnUiThread(()->{if(epoch!=generation||destroyed)return;var judgment=MoveJudgment.assess(before,move.uci(),analysis,actual);entry.local=judgment.kind().label+" · "+before.san(move)+"\n"+judgment.reason();if(afterAnalysis.top()!=null){trend.add(afterAnalysis.top().cp());if(trend.size()>6)trend.remove(0);evaluation.bind(afterAnalysis.top());}try{entry.payload=CoachClient.payload(before,after,move.uci(),analysis,actual,Ui.model(this),new ArrayList<>(trend));entry.details=AnalysisJson.details(analysis,actual,afterAnalysis).put("judgment",judgment.kind().name()).put("opponentDifficulty",Difficulty.from(prefs.getString("difficulty","ELO_1600")).label);}catch(Exception ignored){}save();persistFeedback(entry,ply);busy=false;refresh();if(!parked&&game.white!=playerWhite&&terminal()==null)botTurn(epoch);});}catch(Exception e){runOnUiThread(()->{if(epoch==generation&&!destroyed){busy=true;status.setText("분석 중단 · 수는 저장됨");coach.setText("다시 대국을 열면 이어서 분석할 수 있어요.");closeEngines();}});}});}
    private void botTurn(int epoch){
        if(epoch!=generation||destroyed||parked||botPending||game.white==playerWhite||terminal()!=null)return;
        botPending=true;busy=true;status.setText("Stockfish가 생각하는 중…");Chess position=game.copy();
        engineJobs.submit(()->{try{
            opponent.setDifficulty(Difficulty.from(prefs.getString("difficulty","ELO_1600")));var analysis=opponent.analyze(position,900,1,null);Chess.Move m=Chess.Move.parse(analysis.best());
            runOnUiThread(()->{if(epoch!=generation||destroyed)return;botPending=false;if(!parked&&game.fen().equals(position.fen())&&game.legalMoves().contains(m))play(m,epoch,true);});
        }catch(Exception e){runOnUiThread(()->{if(epoch==generation&&!destroyed){botPending=false;status.setText("응수 준비 실패 · 대국을 다시 열어 주세요");}});}});
    }
    private void manualJudgment(){
        if(!ChatGptAccounts.connected(this)){startActivity(new Intent(this,ConnectionActivity.class));return;}
        if(focus>=0)requestExplanation(feedback.get(focus),generation,false,true);
    }
    private void automationChanged(){
        coachMode.setAutomatic(prefs.getBoolean("autoCoach",false));
        if(!coachMode.automatic())for(Feedback f:feedback)if(f.pending&&f.pendingAutomatic&&!f.running){f.requestVersion++;f.pending=false;f.stream="";}
        renderFeedback();if(coachMode.automatic()&&!parked&&focus>=0)requestExplanation(feedback.get(focus),generation);
    }
    private void requestExplanation(Feedback entry,int epoch){requestExplanation(entry,epoch,false,false);}
    private void requestExplanation(Feedback entry,int epoch,boolean detail,boolean explicit){
        CoachMode.Ticket ticket=coachMode.admit(explicit);if(ticket==null)return;
        if(entry.payload==null)return;String model=Ui.model(this);if(entry.pending&&model.equals(entry.pendingModel))return;
        if(!ChatGptAccounts.connected(this)){entry.ai="AI 코치를 연결하면 방금 수의 이유와 다음 계획을 알려드려요.";renderFeedback();return;}
        JSONObject cache=cached(entry,model);if(CoachText.lesson(cache)){renderFeedback();return;}
        int version=++entry.requestVersion;entry.pending=true;entry.pendingDetail=detail;entry.pendingAutomatic=ticket.automatic();entry.running=false;entry.stream="";entry.pendingModel=model;renderFeedback();final long targetRecord=recordId;final int targetPly=feedback.indexOf(entry);JSONObject request;try{request=new JSONObject(entry.payload.toString()).put("model",model).put("detail",detail);if(!request.has("moveAssessment"))request.put("moveAssessment",entry.local);}catch(Exception e){entry.pending=false;return;}
        coachJobs.submit(()->{if(destroyed||entry.requestVersion!=version||epoch!=generation)return;entry.running=true;
            if(!coachMode.mayStart(ticket)){runOnUiThread(()->{if(!destroyed&&entry.requestVersion==version){entry.pending=false;entry.running=false;renderFeedback();}});return;}
            String result;try{
            var snapshot=Records.get(this).find(targetRecord);if(snapshot==null)throw new java.io.IOException("삭제된 대국입니다.");JSONObject contextual=CoachContext.attach(request,Pgn.parse(snapshot.pgn()),targetPly+1,playerWhite,snapshot.analyses());
            JSONObject response=client.request("/v1/explain",contextual,partial->{
                if(destroyed||entry.requestVersion!=version||epoch!=generation)throw new java.io.InterruptedIOException("해설 요청이 바뀌었습니다.");
                runOnUiThread(()->{if(!destroyed&&entry.requestVersion==version&&epoch==generation){entry.stream=partial;if(focus==targetPly)renderCoach(entry);}});
            });
            if(destroyed||entry.requestVersion!=version||epoch!=generation)return;
            result=AnalysisJson.explanation(response);Records.get(this).explanation(targetRecord,targetPly,response,result);
        }catch(Exception e){result=e.getMessage()==null?"조언 연결을 확인해 주세요.":e.getMessage();}String finalResult=result;runOnUiThread(()->{if(entry.requestVersion!=version)return;entry.pending=false;entry.running=false;entry.stream="";if(epoch==generation&&!destroyed){entry.ai=finalResult;entry.aiModel=model;save();renderFeedback();}});});
    }
    private JSONObject cached(Feedback f,String model){if(recordId==0)return null;var saved=Records.get(this).find(recordId);if(saved==null)return null;JSONObject e=saved.analyses().optJSONObject(Integer.toString(feedback.indexOf(f)));if(e==null)return null;JSONObject all=e.optJSONObject("aiByModel");if(all!=null&&all.optJSONObject(model)!=null)return all.optJSONObject(model);JSONObject response=e.optJSONObject("aiResponse");if(response!=null&&model.equals(response.optString("model")))try{return new JSONObject().put("response",response).put("text",e.optString("aiText"));}catch(Exception ignored){}return null;}
    private void renderCoach(Feedback f){
        String model=Ui.model(this);JSONObject cache=cached(f,model);boolean streaming=f.pending&&model.equals(f.pendingModel);
        String summary=streaming?"이전 기보와 앞으로의 계획을 연결하고 있어요…":!ChatGptAccounts.connected(this)?"AI 코치를 연결하면 준비해 온 전략과 앞으로의 계획을 함께 배울 수 있어요.":!f.ai.isEmpty()&&model.equals(f.aiModel)?f.ai:"AI 판단을 누르면 이전 수와 앞으로의 계획을 연결해 설명해 드려요.";
        String core="";String full=streaming&&!f.stream.isEmpty()?f.stream:cache!=null?CoachText.markdown(cache):summary;
        coachPanel.updateAutomation(f.payload!=null&&!streaming);
        coachPanel.bind(model+":"+focus,core,summary,full,streaming,cache!=null,!f.stream.trim().isEmpty(),()->requestExplanation(f,generation,true,true));
        String visualText=streaming&&!f.stream.isEmpty()?f.stream:cache!=null?CoachText.markdown(cache):"";
        if(!visualText.equals(f.visualText)||f.visualDetails!=f.details){f.visualText=visualText;f.visualDetails=f.details;f.visuals=Collections.emptyList();try{if(f.details!=null){var analysis=AnalysisJson.analysis(f.details.getJSONObject("analysis"));var actual=f.details.isNull("playedScore")?null:AnalysisJson.line(f.details.getJSONObject("playedScore"));int ply=feedback.indexOf(f);f.visuals=CoachVisual.find(new Chess(ply==0?initial:fens.get(ply-1)),moves.get(ply),analysis.lines(),actual,visualText);}}catch(Exception ignored){}}
        coachPanel.visuals(f.visuals,visualText);var scene=CoachVisual.onBoard(f.visuals,board.board);board.aiArrow=scene==null?null:scene.move();board.invalidate();
    }
    private void renderFeedback(){planUse.setText(ChatGptAccounts.connected(this)?"Using ChatGPT plan · Manage usage":"ChatGPT 연결 · Manage usage");if(coach==null)return;modelChip.setText(Ui.modelLabel(this)+" ⌄");board.recommendation=board.playedArrow=board.aiArrow=null;board.judgedSquare=-1;board.judgment=MoveJudgment.Kind.UNKNOWN;moveLabel.setText("");
        if(focus>=0&&focus<feedback.size()){Feedback f=feedback.get(focus);renderCoach(f);try{if(f.details!=null){var a=AnalysisJson.analysis(f.details.getJSONObject("analysis"));var actual=f.details.isNull("playedScore")?null:AnalysisJson.line(f.details.getJSONObject("playedScore"));Chess before=new Chess(focus==0?initial:fens.get(focus-1));var result=MoveJudgment.assess(before,moves.get(focus),a,actual);board.judgment=result.kind();board.judgedSquare=Chess.Move.parse(moves.get(focus)).to();moveLabel.setText(result.kind().symbol+" "+result.kind().label);moveLabel.setTextColor(INK);if(preview&&a.top()!=null&&!a.top().pv().isEmpty()){evaluation.bind(a.top());board.recommendation=a.top().pv().get(0);board.playedArrow=moves.get(focus);}}}catch(Exception ignored){}}
        else {coachPanel.visuals(Collections.emptyList(),"");coachPanel.updateAutomation(false);coachPanel.bind("start","오프닝은 중앙·기물 전개·킹 안전을 함께 준비하는 단계예요. 중앙을 지지하고 나이트·비숍을 전개하며 캐슬링을 준비해 보세요. 첫 수부터 이전 준비와 다음 계획을 연결해 배울 수 있어요.","",false,false,null);}board.invalidate();
    }
    private void refresh(){if(board==null)return;restoreEvaluation();board.board=preview&&focus>=0?new Chess(focus==0?initial:fens.get(focus-1)):game;int lastPlayed=preview&&focus>=0?focus-1:moves.size()-1;board.setLastMove(lastPlayed>=0?Chess.Move.parse(moves.get(lastPlayed)):null);if(preview){board.selected=-1;board.targets.clear();}String terminal=terminal();playControls.setVisibility(terminal==null?View.VISIBLE:View.GONE);outcomeView.bind(GameOutcome.forPlayer(terminal==null?"*":terminal.startsWith("백 승리")?"1-0":terminal.startsWith("흑 승리")?"0-1":"1/2-1/2",playerWhite,terminal));status.setText(preview?"추천 이동 미리보기 · 착수 전 국면":terminal!=null?terminal:busy?"수 분석 중…":game.white==playerWhite?"내 차례 · "+Difficulty.from(prefs.getString("difficulty","ELO_1600")).label:"상대 차례");suggestion.setText(preview?"대국으로 돌아가기":"추천수 보기");suggestion.setEnabled(focus>=0&&feedback.get(focus).details!=null);undoButton.setEnabled(!busy&&moves.size()>branchStart&&forcedResult.equals("*"));renderFeedback();moveStrip.removeAllViews();for(int i=Math.max(0,moves.size()-12);i<moves.size();i++){final int ply=i;Chess before=new Chess(i==0?initial:fens.get(i-1));Button b=button((i/2+1)+(i%2==0?". ":"… ")+before.san(Chess.Move.parse(moves.get(i))),()->{focus=ply;preview=true;refresh();requestExplanation(feedback.get(ply),generation);});b.setTextSize(12);b.setMinHeight(dp(32));Ui.selected(this,b,i==focus);moveStrip.addView(b,new LinearLayout.LayoutParams(-2,dp(36)));}}
    private void undo(){if(busy||moves.size()<=branchStart||!forcedResult.equals("*"))return;generation++;for(Feedback f:feedback){f.requestVersion++;f.pending=false;f.stream="";}int keep=Math.max(branchStart,moves.size()-(game.white==playerWhite?2:1));while(moves.size()>keep){moves.remove(moves.size()-1);fens.remove(fens.size()-1);feedback.remove(feedback.size()-1);}game=new Chess(keep==0?initial:fens.get(keep-1));focus=keep==0?-1:Math.max(0,keep-2);preview=false;rebuildRepetitions();restoreEvaluation();board.selected=-1;board.targets.clear();save();refresh();}
    private void rebuildRepetitions(){repetitions.clear();repetitions.put(new Chess(initial).key(),1);for(String fen:fens)repetitions.merge(new Chess(fen).key(),1,Integer::sum);}
    private void save(){
        if(recordId!=0&&(GameSession.active(this)!=recordId||Records.get(this).find(recordId)==null))return;
        JSONArray entries=new JSONArray();
        try {for(Feedback f:feedback)entries.put(new JSONObject().put("local",f.local).put("ai",f.pending?"설명 대기 · 다시 연결해 재시도하세요":f.ai).put("payload",f.payload==null?JSONObject.NULL:f.payload));}
        catch(JSONException ignored){}
        String terminal=terminal();String result=terminal==null?"*":terminal.startsWith("백 승리")?"1-0":terminal.startsWith("흑 승리")?"0-1":"1/2-1/2";
        List<Pgn.Ply> plies=new ArrayList<>();Chess replay=new Chess(initial);for(String u:moves){Chess.Move m=Chess.Move.parse(u);String before=replay.fen(),san=replay.san(m);boolean white=replay.white;int number=replay.fullmove;replay.play(m);plies.add(new Pgn.Ply(before,replay.fen(),u,san,white,number));}
        Pgn.Game pgn=new Pgn.Game(initial,plies,Map.of("CoachPlayer",playerWhite?"white":"black","CoachBranchStart",Integer.toString(branchStart)),result);
        if(recordId==0)recordId=Records.get(this).create("Stockfish · "+Difficulty.from(prefs.getString("difficulty","ELO_1600")).label,"played",pgn);else if(Records.get(this).find(recordId)!=null)Records.get(this).updateGame(recordId,pgn);
        // Commit the pointer only after SQLite has committed the complete position.
        prefs.edit().putString("moves",String.join(" ",moves)).putString("feedback",entries.toString()).putLong("activeRecord",recordId).commit();
    }
    private void persistFeedback(Feedback entry,int ply){try{JSONObject patch=new JSONObject().put("local",entry.local);if(entry.payload!=null)patch.put("payload",entry.payload);if(entry.details!=null)patch.put("details",entry.details);Records.get(this).patch(recordId,ply,patch);}catch(Exception ignored){}}
    private void restore(){
        try{
            recordId=prefs.getLong("activeRecord",0);
            Records.Saved durable=recordId==0?null:Records.get(this).find(recordId);Pgn.Game stored=durable==null?null:Pgn.parse(durable.pgn());
            if(stored!=null){initial=stored.initial();playerWhite=GameSession.playerWhite(stored);try{branchStart=Integer.parseInt(stored.tags().getOrDefault("CoachBranchStart","0"));}catch(NumberFormatException ignored){}branchStart=Math.max(0,Math.min(branchStart,stored.plies().size()));Chess last=new Chess(stored.plies().isEmpty()?initial:stored.plies().get(stored.plies().size()-1).after());if(!stored.result().equals("*")&&last.terminal(1)==null)forcedResult=stored.result();}
            game=new Chess(initial);String saved=stored==null?prefs.getString("moves",""):String.join(" ",stored.plies().stream().map(Pgn.Ply::uci).collect(java.util.stream.Collectors.toList()));
            if(!saved.trim().isEmpty())for(String u:saved.split(" ")){game.play(Chess.Move.parse(u));moves.add(u);fens.add(game.fen());feedback.add(new Feedback(u+" · 저장된 대국"));}
            JSONArray entries=stored==null?new JSONArray(prefs.getString("feedback","[]")):new JSONArray();
            for(int i=0;i<Math.min(entries.length(),feedback.size());i++){JSONObject entry=entries.getJSONObject(i);Feedback f=feedback.get(i);f.local=entry.getString("local");f.ai=entry.optString("ai");f.payload=entry.optJSONObject("payload");}
            Records.Saved record=recordId==0?null:Records.get(this).find(recordId);
            if(record!=null)for(int i=0;i<feedback.size();i++){JSONObject entry=record.analyses().optJSONObject(Integer.toString(i));if(entry!=null){Feedback f=feedback.get(i);f.local=entry.optString("local",f.local);f.ai=entry.optString("aiText",f.ai);f.payload=entry.optJSONObject("payload");f.details=entry.optJSONObject("details");}}
        }catch(Exception e){recordId=0;initial=Chess.START;playerWhite=true;forcedResult="*";game=new Chess();moves.clear();fens.clear();feedback.clear();save();}
        rebuildRepetitions();restoreEvaluation();focus=feedback.isEmpty()?-1:Math.max(0,feedback.size()-(game.white==playerWhite?2:1));refresh();
    }
    private void restoreEvaluation(){
        trend.clear();Stockfish.Line current=null;
        for(int i=Math.max(0,feedback.size()-6);i<feedback.size();i++){
            JSONObject details=feedback.get(i).details;if(details==null)continue;
            try{JSONObject after=details.optJSONObject("afterAnalysis");if(after==null)continue;
                Stockfish.Line top=AnalysisJson.analysis(after).top();if(top==null)continue;
                trend.add(top.cp());if(i==feedback.size()-1)current=top;
            }catch(JSONException ignored){}
        }
        evaluation.bind(current);
    }
    private void closeEngines(){if(engine!=null)engine.close();if(opponent!=null)opponent.close();engine=null;opponent=null;}
    @Override protected void onStart(){super.onStart();if(recordId!=0&&(GameSession.active(this)!=recordId||Records.get(this).find(recordId)==null)){finish();return;}if(parked){parked=false;connectEngine();}if(modelChip!=null){renderFeedback();if(focus>=0)requestExplanation(feedback.get(focus),generation);}}
    @Override protected void onStop(){parked=true;save();super.onStop();if(!destroyed)engineJobs.submit(()->{if(parked)closeEngines();});}
    @Override protected void onDestroy(){destroyed=true;prefs.unregisterOnSharedPreferenceChangeListener(modeListener);generation++;if(notationDialog!=null)notationDialog.dismiss();coachPanel.close();engineJobs.shutdownNow();coachJobs.shutdownNow();EngineWork.close(this::closeEngines);super.onDestroy();}
}
