package com.chesscoach.app;
import android.app.Instrumentation;
import android.os.Bundle;
import org.json.*;
import java.util.*;

/** Instrumentation against real Android SQLite, keystore and packaged native engine. */
public final class NativeSmoke extends Instrumentation {
    private int checks=0;
    private void check(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();
        android.content.Context testContext=new android.content.ContextWrapper(getTargetContext()){
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("instrumentation-"+name,mode);}
        };
        try{
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
}
