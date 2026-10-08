package com.chesscoach.app;
import android.app.Instrumentation;
import android.os.Bundle;
import org.json.*;
import java.util.*;

/** Instrumentation against real Android SQLite, keystore and packaged native engine. */
public final class NativeSmoke extends Instrumentation {
    private int checks=0;
    private boolean coachOnly,modeOnly,uiOnly;
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;if(modeOnly){Bundle progress=new Bundle();progress.putInt("checks",checks);progress.putString("check",message);sendStatus(0,progress);}}
    @Override public void onCreate(Bundle args){super.onCreate(args);coachOnly=args!=null&&"true".equals(args.getString("coachOnly"));uiOnly=args!=null&&"true".equals(args.getString("uiOnly"));modeOnly=uiOnly||args!=null&&"true".equals(args.getString("modeOnly"));start();}
    @Override public void onStart(){Bundle result=new Bundle();
        android.content.Context testContext=new android.content.ContextWrapper(getTargetContext()){
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("instrumentation-"+name,mode);}
        };
        try{
            if(modeOnly){if(uiOnly){sessionChecks();playChecks();}modeChecks();if(uiOnly)visualChecks();result.putString("result","PASS");result.putInt("checks",checks);java.nio.file.Files.write(new java.io.File(getTargetContext().getExternalFilesDir(null),"native-ui-result.txt").toPath(),("PASS checks="+checks).getBytes(java.nio.charset.StandardCharsets.UTF_8));finish(-1,result);return;}
            coachChecks();
            if(coachOnly){result.putString("result","PASS");result.putInt("checks",checks);finish(-1,result);return;}
            Pgn.Game game=Pgn.parse("1. e4 e5 2. Nf3 Nc6 *");
            try(Records db=new Records(getTargetContext(),"instrumentation-records.db")){
                long played=db.create("Played test","played",game),imported=db.create("Imported test","imported",game);
                check(db.list().size()>=2,"both origins in one library");
                db.patch(played,0,new JSONObject().put("local","best e2e4").put("details",new JSONObject().put("test",true)));
                db.patch(played,0,new JSONObject().put("aiResponse",new JSONObject().put("model","test-model")).put("aiText","test explanation"));
                check(db.find(played).analyses().getJSONObject("0").getString("local").equals("best e2e4"),"late AI preserves Stockfish data");
                check(db.find(played).analyses().getJSONObject("0").getJSONObject("aiResponse").getString("model").equals("test-model"),"model and explanation persist");
                db.explanation(played,0,new JSONObject().put("model","model-a"),"first coach");
                db.explanation(played,0,new JSONObject().put("model","model-b"),"second coach");
                JSONObject byModel=db.find(played).analyses().getJSONObject("0").getJSONObject("aiByModel");
                check(byModel.length()==2&&byModel.getJSONObject("model-a").getString("text").equals("first coach"),"switching models retains both explanations");
                check(db.find(played).analyses().getJSONObject("0").getJSONObject("details").getBoolean("test"),"multi-model explanations retain engine analysis");
                db.highlights(played,Collections.singletonList(new Highlights.Finding(2,"played highlight","reason","candidate-comparison",70)));
                db.highlights(imported,Collections.singletonList(new Highlights.Finding(2,"import highlight","reason","good-decision",80)));
                check(db.find(played).highlights().length()==1&&db.find(imported).highlights().length()==1,"both game sources support highlights");
                db.patch(played,3,new JSONObject().put("local","removed move"));db.updateGame(played,Pgn.parse("1. e4 e5 *"));
                check(!db.find(played).analyses().has("3")&&db.find(played).highlights().length()==0,"undo truncates invalid analysis and highlight references");
            }
            try(Records reopened=new Records(getTargetContext(),"instrumentation-records.db")){check(reopened.list().size()>=2,"records survive database reopen");}
            ChatGptStore encrypted=new ChatGptStore(testContext,"instrumentation-chatgpt");
            JSONObject credentials=new JSONObject().put("profiles",new JSONArray().put(new JSONObject().put("client_id","test-client").put("access_token","instrumentation-only-access").put("refresh_token","instrumentation-only-refresh"))).put("active","test-client");
            encrypted.write(credentials);
            byte[] onDisk=java.nio.file.Files.readAllBytes(new java.io.File(testContext.getFilesDir(),"instrumentation-chatgpt.enc").toPath());
            check(!new String(onDisk,java.nio.charset.StandardCharsets.ISO_8859_1).contains("instrumentation-only"),"OAuth credentials encrypted at rest");
            check(encrypted.read().getJSONArray("profiles").getJSONObject(0).getString("refresh_token").equals("instrumentation-only-refresh"),"Android Keystore OAuth roundtrip");
            credentials.getJSONArray("profiles").getJSONObject(0).put("refresh_token","rotated-test-refresh");encrypted.write(credentials);
            check(new ChatGptStore(testContext,"instrumentation-chatgpt").read().getJSONArray("profiles").getJSONObject(0).getString("refresh_token").equals("rotated-test-refresh"),"atomic rotated credentials survive reopen");
            var attempt=ChatGptProtocol.attempt(12345,"dynamic_agent_client","");
            check(ChatGptProtocol.authorize(attempt,"urn:uuid:test-host","",false).startsWith("https://auth.openai.com/api/accounts/authorize?"),"native OAuth URL and PKCE construction");
            var loginContext=getTargetContext();int profilesBefore=new ChatGptAccounts(loginContext).profiles().length();
            loginContext.startForegroundService(new android.content.Intent(loginContext,ChatGptLoginService.class));
            long waitUntil=System.currentTimeMillis()+20000;String authUrl="";
            while(System.currentTimeMillis()<waitUntil&&authUrl.isEmpty()){Thread.sleep(100);authUrl=ChatGptLoginService.takeBrowserUrl();}
            check(!authUrl.isEmpty(),"foreground loopback listener ready without a relay");
            android.net.Uri authorization=android.net.Uri.parse(authUrl);java.net.URI redirect=new java.net.URI(authorization.getQueryParameter("redirect_uri"));
            check(redirect.getHost().equals("127.0.0.1")&&redirect.getPath().equals("/auth/callback"),"callback bound to exact IPv4 loopback path");
            try(java.net.Socket socket=new java.net.Socket("127.0.0.1",redirect.getPort())){socket.setSoTimeout(10000);socket.getOutputStream().write("GET /auth/callback?state=wrong&code=fake&client_id=oaiapp_test HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));String reply=new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream())).readLine();check(reply.contains("400"),"forged browser callback rejected before token exchange");}
            waitUntil=System.currentTimeMillis()+10000;while(ChatGptLoginService.running()&&System.currentTimeMillis()<waitUntil)Thread.sleep(100);
            check(new ChatGptAccounts(loginContext).profiles().length()==profilesBefore&&!ChatGptLoginService.running(),"invalid callback leaves existing account registrations unchanged");
            try(Stockfish engine=new Stockfish(getTargetContext().getApplicationInfo().nativeLibraryDir+"/libstockfish.so")){
                var analysis=engine.analyze(new Chess(),1000,3,null);check(analysis.lines().size()==3,"packaged native engine MultiPV");check(new Chess().legalMoves().contains(Chess.Move.parse(analysis.best())),"native engine legal reply");
                JSONObject details=AnalysisJson.details(analysis,analysis.top(),analysis);check(AnalysisJson.analysis(details.getJSONObject("analysis")).lines().size()==3,"analysis JSON roundtrip");
                engine.setDifficulty(Difficulty.ELO_1600);check(new Chess().legalMoves().contains(Chess.Move.parse(engine.analyze(new Chess(),500,1,null).best())),"native Elo control");
            }
            result.putString("result","PASS");result.putInt("checks",checks);finish(-1,result);
        }catch(Throwable e){result.putString("result","FAIL");result.putString("error",e.toString());finish(1,result);}
        finally{getTargetContext().stopService(new android.content.Intent(getTargetContext(),ChatGptLoginService.class));testContext.deleteFile("instrumentation-chatgpt.enc");getTargetContext().deleteDatabase("instrumentation-records.db");testContext.getSharedPreferences("coach",0).edit().clear().commit();}
    }
    private android.view.View find(android.view.View root,boolean toggle){
        if(toggle&&root instanceof android.widget.Switch&&"AI 자동 코칭".contentEquals(root.getContentDescription()))return root;
        if(!toggle&&root instanceof android.widget.Button b&&"AI 판단".contentEquals(b.getText()))return root;
        if(root instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++){android.view.View found=find(group.getChildAt(i),toggle);if(found!=null)return found;}
        return null;
    }
    private void awaitBoard(android.app.Activity a)throws Exception{long until=System.currentTimeMillis()+45000;boolean[] ready={false};while(System.currentTimeMillis()<until){runOnMainSync(()->ready[0]=type(a.getWindow().getDecorView(),BoardView.class)!=null);if(ready[0]){settle(a);return;}Thread.sleep(100);}throw new AssertionError("Review did not finish loading");}
    private void settle(android.app.Activity a)throws Exception{java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);runOnMainSync(()->a.getWindow().getDecorView().postOnAnimation(()->a.getWindow().getDecorView().post(frame::countDown)));if(!frame.await(30,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("Layout frame timed out");waitForIdleSync();}
    private android.graphics.Rect boardBounds(android.app.Activity a){var board=(BoardView)type(a.getWindow().getDecorView(),BoardView.class);int[] location=new int[2];board.getLocationOnScreen(location);return new android.graphics.Rect(location[0],location[1],location[0]+board.getWidth(),location[1]+board.getHeight());}
    private void sessionChecks()throws Exception{
        String name="instrumentation-session-records.db";getTargetContext().deleteDatabase(name);long parent,child;
        try(Records db=new Records(getTargetContext(),name)){
            Pgn.Game source=Pgn.parse("1. e4 e5 2. Nf3 Nc6 1-0");parent=db.create("Original","imported",source);db.patch(parent,0,new JSONObject().put("local","engine evidence").put("aiText","saved coach"));db.highlights(parent,Collections.singletonList(new Highlights.Finding(0,"focus","why","planning",70)));
            child=db.branch(parent,source,1);check(db.find(child).title().endsWith(" · 분기"),"branch creates a separately named record");check(db.find(parent).plies()==4&&db.find(child).plies()==1,"branch leaves original mainline intact");check(db.find(child).analyses().getJSONObject("0").getString("aiText").equals("saved coach")&&db.find(child).highlights().length()==1,"branch keeps prefix explanations and highlights");db.rename(child,"My training");check(db.find(child).title().equals("My training"),"record rename stored in SQLite");
        }
        try(Records db=new Records(getTargetContext(),name)){
            var branch=Pgn.parse(db.find(child).pgn());check(branch.result().equals("*")&&!GameSession.playerWhite(branch)&&branch.plies().get(0).after().equals(Pgn.parse(db.find(parent).pgn()).plies().get(0).after()),"position and player color survive database reopen");db.updateGame(child,new Pgn.Game(branch.initial(),branch.plies(),branch.tags(),"1-0"));check(Pgn.parse(db.find(child).pgn()).result().equals("1-0"),"resignation result survives persistent record update");db.delete(child);check(db.find(child)==null&&db.find(parent)!=null,"delete removes selected record and retains parent");
        }finally{getTargetContext().deleteDatabase(name);}
        var isolated=new android.content.ContextWrapper(getTargetContext()){@Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("instrumentation-session-"+name,mode);}};var prefs=isolated.getSharedPreferences("coach",0);Records db=Records.get(getTargetContext());long active=db.create("instrumentation resume","played",new Pgn.Game(Chess.START,Collections.emptyList(),Map.of("CoachPlayer","white"),"*"));
        try{prefs.edit().putLong("activeRecord",active).commit();check(GameSession.available(isolated),"even an unfinished zero-move game is resumable from disk");db.updateGame(active,new Pgn.Game(Chess.START,Collections.emptyList(),Map.of("CoachPlayer","white"),"0-1"));check(!GameSession.available(isolated),"finished game does not offer resume");db.delete(active);check(!GameSession.available(isolated),"deleted game does not offer resume");}finally{db.delete(active);prefs.edit().clear().commit();}
    }
    private void playChecks()throws Exception{
        var prefs=getTargetContext().getSharedPreferences("coach",0);var appearance=getTargetContext().getSharedPreferences("appearance",0);
        java.util.Map<String,Object> backup=new java.util.HashMap<>();for(String key:new String[]{"activeRecord","moves","feedback","autoCoach"})backup.put(key,prefs.getAll().get(key));String oldTheme=appearance.getString("theme",null);
        Records db=Records.get(getTargetContext());var prefix=GameSession.prefix(Pgn.parse("1. e4 e5 2. Nf3 Nc6 *"),1);long id=db.create("instrumentation black resume","played",prefix);db.patch(id,0,new JSONObject().put("details",new JSONObject()).put("payload",new JSONObject()));android.app.Activity[] activity={null};
        try{
            prefs.edit().putBoolean("autoCoach",false).commit();appearance.edit().putString("theme","light").commit();activity[0]=startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class).putExtra("resumeRecord",id).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));awaitBoard(activity[0]);
            java.lang.reflect.Field gameField=MainActivity.class.getDeclaredField("game"),side=MainActivity.class.getDeclaredField("playerWhite"),busy=MainActivity.class.getDeclaredField("busy");gameField.setAccessible(true);side.setAccessible(true);busy.setAccessible(true);java.lang.reflect.Method select=MainActivity.class.getDeclaredMethod("select",int.class);select.setAccessible(true);
            runOnMainSync(()->{try{check(((Chess)gameField.get(activity[0])).fen().equals(prefix.plies().get(0).after())&&!side.getBoolean(activity[0]),"play restores recorded position and Black player from SQLite");check((activity[0].getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_NO,"in-app light preference overrides system theme");busy.setBoolean(activity[0],false);select.invoke(activity[0],52);var board=(BoardView)type(activity[0].getWindow().getDecorView(),BoardView.class);check(board.selected==52,"Black player can select own pawn after branching");select.invoke(activity[0],16);check(board.selected==-1&&board.targets.isEmpty(),"empty non-destination square clears piece and targets");activity[0].finish();}catch(ReflectiveOperationException e){throw new AssertionError(e);}});waitForIdleSync();
            appearance.edit().putString("theme","dark").commit();activity[0]=startActivitySync(new android.content.Intent(getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));awaitBoard(activity[0]);
            runOnMainSync(()->{try{check(((Chess)gameField.get(activity[0])).fen().equals(prefix.plies().get(0).after())&&(activity[0].getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES,"Activity restart retains position and applies saved dark preference");}catch(ReflectiveOperationException e){throw new AssertionError(e);}});
        }finally{
            if(activity[0]!=null){runOnMainSync(()->activity[0].finish());waitForIdleSync();}db.delete(id);var edit=prefs.edit();for(var entry:backup.entrySet()){Object value=entry.getValue();if(value==null)edit.remove(entry.getKey());else if(value instanceof Long number)edit.putLong(entry.getKey(),number);else if(value instanceof Boolean flag)edit.putBoolean(entry.getKey(),flag);else edit.putString(entry.getKey(),value.toString());}edit.commit();if(oldTheme==null)appearance.edit().remove("theme").commit();else appearance.edit().putString("theme",oldTheme).commit();
        }
    }
    private void modeChecks()throws Exception {
        var prefs=getTargetContext().getSharedPreferences("coach",0);boolean existed=prefs.contains("autoCoach"),old=prefs.getBoolean("autoCoach",false);
        // Stay at the starting position: no selected ply, engine analysis or AI request.
        Records db=Records.get(getTargetContext());long id=db.create("instrumentation mode UI","imported",Pgn.parse("1. e4 *"));android.app.Activity[] activity={null};
        try{
            prefs.edit().putBoolean("autoCoach",false).commit();
            var intent=new android.content.Intent(getTargetContext(),ReviewActivity.class).putExtra("recordId",id).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            activity[0]=startActivitySync(intent);awaitBoard(activity[0]);
            runOnMainSync(()->{
                var root=activity[0].getWindow().getDecorView();var toggle=(android.widget.Switch)find(root,true);var judge=find(root,false);
                check(toggle!=null&&judge!=null,"native automation switch and manual judgment button exist");
                check(!toggle.isChecked()&&judge.getVisibility()==android.view.View.VISIBLE,"OFF displays manual judgment button");
                toggle.performClick();check(prefs.getBoolean("autoCoach",false)&&judge.getVisibility()==android.view.View.GONE,"switch click persists ON and hides judgment immediately");
                activity[0].finish();
            });
            activity[0]=startActivitySync(intent);awaitBoard(activity[0]);
            runOnMainSync(()->{
                var root=activity[0].getWindow().getDecorView();var toggle=(android.widget.Switch)find(root,true);var judge=find(root,false);
                check(toggle.isChecked()&&judge.getVisibility()==android.view.View.GONE,"reopened Activity restores ON and hidden judgment button");
                toggle.performClick();check(!prefs.getBoolean("autoCoach",true)&&judge.getVisibility()==android.view.View.VISIBLE,"switch click restores manual mode and button");
            });
        }finally{
            if(activity[0]!=null)runOnMainSync(()->activity[0].finish());
            if(existed)prefs.edit().putBoolean("autoCoach",old).commit();else prefs.edit().remove("autoCoach").commit();
            db.getWritableDatabase().delete("games","id=?",new String[]{Long.toString(id)});
        }
    }
    private android.view.View label(android.view.View root,String text){
        if(root instanceof android.widget.TextView view&&view.getText().toString().startsWith(text))return root;
        if(root instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++){var found=label(group.getChildAt(i),text);if(found!=null)return found;}return null;
    }
    private android.view.View type(android.view.View root,Class<?> target){
        if(target.isInstance(root))return root;
        if(root instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++){var found=type(group.getChildAt(i),target);if(found!=null)return found;}return null;
    }
    private void visualChecks()throws Exception {
        var prefs=getTargetContext().getSharedPreferences("coach",0);boolean existed=prefs.contains("autoCoach"),old=prefs.getBoolean("autoCoach",false);prefs.edit().putBoolean("autoCoach",false).commit();
        var game=Pgn.parse("1. f3 e5 2. g4 Qh4# 0-1");Records db=Records.get(getTargetContext());long id=db.create("한 수의 이유 · 연습 복기","played",game);android.app.Activity[] activity={null};
        try{
            var best=new Stockfish.Line(1,20,0,null,Arrays.asList("b1c3","d7d6"));var actual=new Stockfish.Line(1,20,-30000,-1,Arrays.asList("g2g4","d8h4"));var analysis=new Stockfish.Analysis("b1c3",Collections.singletonList(best));
            db.patch(id,2,new JSONObject().put("details",AnalysisJson.details(analysis,actual,null)).put("payload",new JSONObject()).put("local","블런더 · g4"));
            var response=new JSONObject().put("model",Ui.model(getTargetContext())).put("explanation",new JSONObject().put("headline","h4의 체크메이트를 놓쳤어요").put("summary","g4로 킹이 노출됐어요. 흑의 `Qh4#`에 체크메이트가 되므로 먼저 킹의 안전을 확인하세요."));db.explanation(id,2,response,"fixture");
            activity[0]=startActivitySync(new android.content.Intent(getTargetContext(),ReviewActivity.class).putExtra("recordId",id).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));awaitBoard(activity[0]);
            runOnMainSync(()->{var next=label(activity[0].getWindow().getDecorView(),"다음 ›");for(int i=0;i<3;i++)next.performClick();});waitForIdleSync();
            runOnMainSync(()->{
                var root=activity[0].getWindow().getDecorView();var board=(BoardView)type(root,BoardView.class);check("d8h4".equals(board.aiArrow)&&board.board.fen().equals(game.plies().get(2).after()),"AI threat arrow belongs to the displayed position");
                var outcome=type(root,OutcomeView.class);check(outcome.getVisibility()==android.view.View.GONE,"rewinding leaves the board available instead of a game-over banner");
                var link=label(root,"↗ AI 수 보기");check(link!=null&&link.getVisibility()==android.view.View.VISIBLE,"legal explanation has a native variation preview link");
            });
            var panelFieldFixed=ReviewActivity.class.getDeclaredField("coachPanel");panelFieldFixed.setAccessible(true);CoachPanel fixedPanel=(CoachPanel)panelFieldFixed.get(activity[0]);android.graphics.Rect[] anchor={null};runOnMainSync(()->anchor[0]=boardBounds(activity[0]));
            runOnMainSync(()->fixedPanel.bind("layout-test","짧은 핵심", String.join("",Collections.nCopies(100,"긴 조언 ")),"fixture",true,false,null));settle(activity[0]);runOnMainSync(()->check(anchor[0].equals(boardBounds(activity[0])),"long streaming advice does not move or resize board"));
            runOnMainSync(()->fixedPanel.bind("layout-test","", "", "",false,false,null));settle(activity[0]);runOnMainSync(()->check(anchor[0].equals(boardBounds(activity[0])),"empty advice does not move or resize board"));
            runOnMainSync(()->label(activity[0].getWindow().getDecorView(),"‹ 이전").performClick());runOnMainSync(()->label(activity[0].getWindow().getDecorView(),"다음 ›").performClick());settle(activity[0]);
            boolean night=(getTargetContext().getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;String theme=night?"dark":"light";
            screenshot(activity[0].getWindow(),"native-preview-"+theme+".png");
            runOnMainSync(()->{var root=activity[0].getWindow().getDecorView();label(root,"다음 ›").performClick();var outcome=type(root,OutcomeView.class);check(outcome.getVisibility()==android.view.View.VISIBLE&&outcome.getContentDescription().toString().startsWith("패배"),"played game loss is prominently visible at the final move");});waitForIdleSync();
            settle(activity[0]);runOnMainSync(()->check(anchor[0].equals(boardBounds(activity[0])),"game result does not move or resize review board"));
            screenshot(activity[0].getWindow(),"native-result-"+theme+".png");
            runOnMainSync(()->{var root=activity[0].getWindow().getDecorView();label(root,"‹ 이전").performClick();label(root,"↗ AI 수 보기").performClick();});waitForIdleSync();
            var panelField=ReviewActivity.class.getDeclaredField("coachPanel");panelField.setAccessible(true);CoachPanel panel=(CoachPanel)panelField.get(activity[0]);var dialogField=CoachPanel.class.getDeclaredField("visualDialog");dialogField.setAccessible(true);android.app.Dialog popup=(android.app.Dialog)dialogField.get(panel);
            runOnMainSync(()->check(popup!=null&&popup.isShowing(),"native AI variation preview opens without model inference"));screenshot(popup.getWindow(),"native-variation-"+theme+".png");
            runOnMainSync(panel::close);waitForIdleSync();
        }finally{
            if(activity[0]!=null){runOnMainSync(()->activity[0].finish());waitForIdleSync();}
            db.getWritableDatabase().delete("games","id=?",new String[]{Long.toString(id)});if(existed)prefs.edit().putBoolean("autoCoach",old).commit();else prefs.edit().remove("autoCoach").commit();
        }
    }
    private void screenshot(android.view.Window window,String name)throws Exception {
        android.graphics.Bitmap[] picture={null};android.os.HandlerThread copyThread=new android.os.HandlerThread("native-screenshot");copyThread.start();
        try{
            java.util.concurrent.CountDownLatch frame=new java.util.concurrent.CountDownLatch(1);runOnMainSync(()->window.getDecorView().postOnAnimation(()->window.getDecorView().post(frame::countDown)));if(!frame.await(30,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException("Native screenshot layout did not settle");
            java.util.concurrent.CountDownLatch copied=new java.util.concurrent.CountDownLatch(1);int[] status={-1};
            runOnMainSync(()->{var root=window.getDecorView();picture[0]=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);android.view.PixelCopy.request(window,picture[0],result->{status[0]=result;copied.countDown();},new android.os.Handler(copyThread.getLooper()));});
            if(!copied.await(30,java.util.concurrent.TimeUnit.SECONDS)||status[0]!=android.view.PixelCopy.SUCCESS)throw new java.io.IOException("Native screenshot copy failed: "+status[0]);
            // Capture asynchronously; compress/write on the instrumentation thread, never block focus events.
            try(var output=new java.io.FileOutputStream(new java.io.File(getTargetContext().getExternalFilesDir(null),name))){picture[0].compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}
        }finally{if(picture[0]!=null)picture[0].recycle();copyThread.quitSafely();}
    }
    private void coachChecks()throws Exception {
        CharSequence rendered=CoachMarkdown.render("## 이번 수\n**중앙**을 지키고 *전개*하세요.\n- `Nf3`를 확인하세요.");
        check(rendered.toString().equals("이번 수\n중앙을 지키고 전개하세요.\n• Nf3를 확인하세요."),"Markdown shown without syntax, with native bullet list");
        android.text.Spanned spans=(android.text.Spanned)rendered;
        check(spans.getSpans(0,rendered.length(),android.text.style.StyleSpan.class).length==3,"native heading, bold and italic spans");
        check(spans.getSpans(0,rendered.length(),android.text.style.TypefaceSpan.class).length==1,"chess notation uses native monospace span");
        long id;
        try(Records db=new Records(getTargetContext(),"instrumentation-coach-stream.db")){
            id=db.create("stream cache","imported",Pgn.parse("1. e4 e5 *"));
            JSONObject summary=new JSONObject().put("model","stream-model").put("explanation",new JSONObject().put("summary","**중앙**을 차지하세요.")).put("usage",new JSONObject().put("output_tokens",20));
            db.explanation(id,0,summary,AnalysisJson.explanation(summary));
            JSONObject cache=db.find(id).analyses().getJSONObject("0").getJSONObject("aiByModel").getJSONObject("stream-model");
            check(!CoachText.detailed(cache)&&CoachText.summary(cache).equals("**중앙**을 차지하세요."),"summary persisted without claiming detailed answer");
            JSONObject detail=new JSONObject().put("model","stream-model").put("explanation",new JSONObject().put("flow","판단").put("bestMoveReason","근거").put("plan","계획")).put("usage",new JSONObject().put("output_tokens",90));
            db.explanation(id,0,detail,AnalysisJson.explanation(detail));
        }
        try(Records db=new Records(getTargetContext(),"instrumentation-coach-stream.db")){
            JSONObject cache=db.find(id).analyses().getJSONObject("0").getJSONObject("aiByModel").getJSONObject("stream-model");
            check(CoachText.detailed(cache)&&CoachText.summary(cache).equals("**중앙**을 차지하세요."),"summary and details survive real SQLite reopen");
            check(cache.getJSONObject("response").getJSONObject("summaryUsage").getInt("output_tokens")==20&&cache.getJSONObject("response").getJSONObject("detailUsage").getInt("output_tokens")==90,"both requests retain usage in stored record");
        }finally{getTargetContext().deleteDatabase("instrumentation-coach-stream.db");}
    }
}
