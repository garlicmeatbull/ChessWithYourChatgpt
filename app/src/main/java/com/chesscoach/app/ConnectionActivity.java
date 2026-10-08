package com.chesscoach.app;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import org.json.*;
import java.util.concurrent.*;
/** Native account controls; browser login and inference run entirely on this phone. */
public final class ConnectionActivity extends Activity {
    private final ExecutorService jobs=Executors.newSingleThreadExecutor();private final Handler handler=new Handler(Looper.getMainLooper());
    private ChatGptAccounts accounts;private TextView status,account;private Button login,model,test,cancel;private boolean destroyed=false,resumed=false,working=false;private String handled="";
    @Override public void onCreate(Bundle state){super.onCreate(state);accounts=new ChatGptAccounts(this);LinearLayout root=Ui.screen(this,true);Ui.header(this,root,"AI 코치");root.addView(Ui.text(this,"내 ChatGPT 계정으로,\n한 수의 이유를 이해하세요.",27,true));Ui.gap(root,12);root.addView(Ui.text(this,"서버나 API 키 없이 이 휴대폰에서 직접 연결합니다. Stockfish 대국과 분석은 로그인 없이도 이용할 수 있어요.",15,false));Ui.gap(root,20);
        LinearLayout card=Ui.card(this);card.addView(Ui.text(this,"ChatGPT 계정",18,true));Ui.gap(card,8);account=Ui.text(this,"",14,false);card.addView(account);Ui.gap(card,12);
        login=Ui.button(this,"Continue with ChatGPT",()->signIn(false));login.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));login.setTextColor(Color.WHITE);card.addView(login);Ui.gap(card,8);card.addView(Ui.button(this,"계정 선택 · 다른 계정 추가",this::pickAccount));root.addView(card);Ui.gap(root,16);
        root.addView(Ui.text(this,"AI 해설은 허용된 ChatGPT 구독 사용량 또는 계정에서 허용한 크레딧을 사용합니다. 계정 자격·한도가 적용됩니다. 이 앱은 무료이며 API 과금으로 자동 전환하지 않습니다.",13,false));Ui.gap(root,12);
        status=Ui.text(this,"로그인 후 모델 조회와 AI 응답 테스트를 진행하세요.",14,false);root.addView(status);Ui.gap(root,12);
        model=Ui.button(this,"모델 조회 · 선택",()->run(()->{accounts.loadModels();return "계정 모델 조회 완료";},()->Ui.models(this,()->render())));root.addView(model);Ui.gap(root,8);
        test=Ui.primary(this,"연결 테스트 · AI 응답 받기",()->run(()->{accounts.resumeRequests();accounts.loadModels();String answer=new CoachClient(this).testResponse();return "✓ Responses 응답 완료\n"+answer+"\n\n선택 모델: "+Ui.modelLabel(this);},null));root.addView(test);Ui.gap(root,8);
        root.addView(Ui.button(this,"Manage usage · 사용량 관리",()->Ui.usage(this)));Ui.gap(root,8);
        cancel=Ui.button(this,"로그인 취소",()->{stopService(new Intent(this,ChatGptLoginService.class));status.setText("로그인을 취소했습니다.");render();});root.addView(cancel);Ui.gap(root,8);
        root.addView(Ui.button(this,"선택 계정 로그아웃",()->run(()->{stopService(new Intent(this,ChatGptLoginService.class));return accounts.logout()?"이 기기의 계정을 로그아웃하고 갱신 세션을 해제했습니다.":"이 기기에서 로그아웃했습니다. 원격 세션 해제는 확인하지 못했습니다. ChatGPT 사용량 관리에서 앱 연결을 해제하세요.";},null)));render();}
    private void render(){if(destroyed)return;try{JSONObject p=accounts.active();String label=p.optString("email","");account.setText(p.length()==0?"연결된 계정이 없습니다.":(label.isEmpty()?"등록된 계정":label)+"\n등록 "+suffix(p.optString("client_id"))+" · "+(ChatGptProtocol.sharing(p)?"Using ChatGPT plan":"로그인 또는 구독 사용 동의 필요"));}catch(Exception e){account.setText("저장된 계정을 읽을 수 없습니다. 다시 로그인하세요.");}boolean running=ChatGptLoginService.running();login.setEnabled(!working&&!running);cancel.setVisibility(running?android.view.View.VISIBLE:android.view.View.GONE);model.setEnabled(!working&&ChatGptAccounts.connected(this));test.setEnabled(!working&&ChatGptAccounts.connected(this));login.setAlpha(login.isEnabled()?1f:.45f);model.setAlpha(model.isEnabled()?1f:.45f);test.setAlpha(test.isEnabled()?1f:.45f);}
    private String suffix(String id){return id.substring(Math.max(0,id.length()-6));}
    private void signIn(boolean newAccount){if(ChatGptLoginService.running())return;try{JSONObject p=newAccount?new JSONObject():accounts.active();Intent intent=new Intent(this,ChatGptLoginService.class).putExtra("profile",p.optString("client_id",""));if(!newAccount&&!p.optString("access_token").isEmpty()&&!ChatGptProtocol.sharing(p))intent.putExtra("consent",true);
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);
        handled="";startForegroundService(intent);status.setText("로그인 준비 중…");handler.postDelayed(this::poll,300);
    }catch(Exception e){status.setText("로그인을 시작하지 못했습니다. 다시 시도하세요.");}}
    private void pickAccount(){if(working||ChatGptLoginService.running())return;try{JSONArray profiles=accounts.profiles();String[] labels=new String[profiles.length()+1];for(int i=0;i<profiles.length();i++){JSONObject p=profiles.getJSONObject(i);labels[i]=p.optString("email","등록된 계정")+" · "+suffix(p.getString("client_id"));}labels[profiles.length()]="+ 다른 ChatGPT 계정 추가";new AlertDialog.Builder(this).setTitle("ChatGPT 계정").setItems(labels,(d,w)->{if(w==profiles.length()){signIn(true);return;}run(()->{accounts.select(profiles.getJSONObject(w).getString("client_id"));if(ChatGptAccounts.connected(this))accounts.loadModels();return "선택한 계정으로 전환했습니다.";},null);}).show();}catch(Exception e){status.setText("계정 목록을 읽을 수 없습니다.");}}
    private interface Work {String run()throws Exception;}
    private void run(Work work,Runnable after){if(working)return;working=true;status.setText("ChatGPT 연결 확인 중…");render();jobs.submit(()->{String message;boolean ok;try{message=work.run();ok=true;}catch(Exception e){message=e.getMessage()==null?"ChatGPT 연결을 확인하세요.":e.getMessage();ok=false;}String result=message;boolean success=ok;runOnUiThread(()->{if(destroyed)return;working=false;status.setText(result);render();if(success&&after!=null)after.run();if(!success&&result.contains("사용 한도"))new AlertDialog.Builder(this).setTitle("ChatGPT 사용 한도").setMessage(result).setPositiveButton("Manage usage",(d,w)->Ui.usage(this)).setNegativeButton("닫기",null).show();});});}
    private void poll(){if(!resumed||destroyed)return;String phase=ChatGptLoginService.phase();if(ChatGptLoginService.running()){status.setText(ChatGptLoginService.message());render();String url=ChatGptLoginService.takeBrowserUrl();if(!url.isEmpty())try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){stopService(new Intent(this,ChatGptLoginService.class));status.setText("로그인에 사용할 시스템 브라우저를 설치하세요.");}handler.postDelayed(this::poll,500);}
        else if(!phase.equals("idle")&&!phase.equals(handled)){handled=phase;status.setText(ChatGptLoginService.message());render();if(phase.equals("done")&&ChatGptAccounts.connected(this)){var prefs=getSharedPreferences("chatgpt-install",0);if(!prefs.getBoolean("welcome",false)){prefs.edit().putBoolean("welcome",true).apply();new AlertDialog.Builder(this).setTitle("You're using your ChatGPT plan").setMessage("이 앱의 AI 해설은 ChatGPT 구독 사용량 또는 허용한 크레딧을 사용합니다. 사용량 관리에서 한도를 확인할 수 있어요.").setPositiveButton("확인",null).setNeutralButton("Manage usage",(d,w)->Ui.usage(this)).show();}run(()->{accounts.loadModels();return "✓ 계정 연결 및 모델 조회 완료. AI 응답 테스트를 눌러 확인하세요.";},null);}}}
    @Override protected void onResume(){super.onResume();resumed=true;render();handler.removeCallbacksAndMessages(null);poll();}
    @Override protected void onPause(){resumed=false;handler.removeCallbacksAndMessages(null);super.onPause();}
    @Override protected void onDestroy(){destroyed=true;handler.removeCallbacksAndMessages(null);jobs.shutdownNow();super.onDestroy();}
}
