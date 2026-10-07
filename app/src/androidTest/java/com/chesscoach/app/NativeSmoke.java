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
            CoachClient client=new CoachClient(testContext);client.saveToken("instrumentation-only-pairing");
            check(!testContext.getSharedPreferences("coach",0).getString("token","").equals("instrumentation-only-pairing"),"pairing token encrypted at rest");
            var decrypt=CoachClient.class.getDeclaredMethod("token");decrypt.setAccessible(true);
            check(decrypt.invoke(client).equals("instrumentation-only-pairing"),"Android Keystore pairing roundtrip");
            CoachClient.validateEndpoint("https://coach.example.org");check(true,"HTTPS endpoint accepted");
            boolean rejected=false;try{CoachClient.validateEndpoint("http://remote.example.org");}catch(Exception e){rejected=true;}check(rejected,"remote cleartext blocked");
            try(Stockfish engine=new Stockfish(getTargetContext().getApplicationInfo().nativeLibraryDir+"/libstockfish.so")){
                var analysis=engine.analyze(new Chess(),1000,3,null);check(analysis.lines().size()==3,"packaged native engine MultiPV");check(new Chess().legalMoves().contains(Chess.Move.parse(analysis.best())),"native engine legal reply");
                JSONObject details=AnalysisJson.details(analysis,analysis.top(),analysis);check(AnalysisJson.analysis(details.getJSONObject("analysis")).lines().size()==3,"analysis JSON roundtrip");
                engine.setDifficulty(Difficulty.ELO_1600);check(new Chess().legalMoves().contains(Chess.Move.parse(engine.analyze(new Chess(),500,1,null).best())),"native Elo control");
            }
            result.putString("result","PASS");result.putInt("checks",checks);finish(-1,result);
        }catch(Throwable e){result.putString("result","FAIL");result.putString("error",e.toString());finish(1,result);}
        finally{getTargetContext().deleteDatabase("instrumentation-records.db");testContext.getSharedPreferences("coach",0).edit().clear().commit();}
    }
}
