package com.chesscoach.app;
import android.app.*;
import android.content.*;
import android.os.*;
import android.content.pm.ServiceInfo;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
/** Short-lived foreground listener survives the switch to the user's system browser. */
public final class ChatGptLoginService extends Service {
    private static volatile String phase="idle",message="",browserUrl="";
    private volatile ServerSocket server;private volatile boolean cancelled=false;
    private final ExecutorService jobs=Executors.newSingleThreadExecutor();
    public static String phase(){return phase;}public static String message(){return message;}
    public static synchronized String takeBrowserUrl(){String value=browserUrl;browserUrl="";return value;}
    public static boolean running(){return phase.equals("starting")||phase.equals("waiting")||phase.equals("exchanging");}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){if(running())return START_NOT_STICKY;phase="starting";message="로그인 준비 중…";browserUrl="";cancelled=false;
        NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("chatgpt-login","ChatGPT 로그인",NotificationManager.IMPORTANCE_LOW));
        Intent back=new Intent(this,ConnectionActivity.class);PendingIntent pending=PendingIntent.getActivity(this,0,back,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification note=new Notification.Builder(this,"chatgpt-login").setSmallIcon(R.mipmap.ic_launcher).setContentTitle("ChatGPT 로그인 진행 중").setContentText("브라우저에서 로그인한 뒤 앱으로 돌아오세요.").setContentIntent(pending).setOngoing(true).build();
        if(Build.VERSION.SDK_INT>=34)startForeground(42,note,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);else startForeground(42,note);
        String profile=intent==null?"":intent.getStringExtra("profile");boolean consent=intent!=null&&intent.getBooleanExtra("consent",false);jobs.submit(()->login(profile==null?"":profile,consent));return START_NOT_STICKY;
    }
    private void login(String id,boolean consent){try{
        ChatGptAccounts accounts=new ChatGptAccounts(this);var saved=accounts.profile(id);String client=saved.optString("client_id","dynamic_agent_client");
        server=new ServerSocket();server.setReuseAddress(false);server.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"),0));server.setSoTimeout(1000);
        ChatGptProtocol.Attempt attempt=ChatGptProtocol.attempt(server.getLocalPort(),client,saved.optString("subject",""));
        // ID-token hints are optional; omit them to avoid placing retained credentials in Android intent diagnostics.
        browserUrl=ChatGptProtocol.authorize(attempt,accounts.host(),"",consent);phase="waiting";message="브라우저에서 로그인·권한 동의를 완료하세요.";
        while(!cancelled&&System.currentTimeMillis()<attempt.deadline()){
            try(Socket socket=server.accept()){
                socket.setSoTimeout(5000);Reader in=new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII);String request=requestLine(in);
                if(request==null||request.length()>16384){reply(socket,400,"Invalid callback");continue;}
                String[] parts=request.split(" ",3);if(parts.length!=3||!parts[0].equals("GET")||!parts[1].startsWith("/auth/callback?")){reply(socket,404,"Not found");continue;}
                ChatGptProtocol.Callback callback;
                try{callback=ChatGptProtocol.callback(attempt,parts[1]);}catch(Exception e){reply(socket,400,"로그인 응답을 확인하지 못했습니다. 앱으로 돌아가 다시 시도하세요.");throw e;}
                reply(socket,200,"로그인 응답을 받았습니다. Chess Coach 앱으로 돌아가면 연결을 확인합니다.");phase="exchanging";browserUrl="";message="계정 서명과 사용 권한 확인 중…";
                accounts.accept(attempt,callback);if(cancelled)return;phase="done";message=ChatGptAccounts.connected(this)?"ChatGPT 계정 연결 완료 · 구독 사용 권한 허용됨":"계정 로그인 완료 · 구독 사용 권한을 추가로 허용하세요.";return;
            }catch(SocketTimeoutException timeout){if(phase.equals("exchanging"))throw timeout;}
        }
        if(!cancelled){phase="failed";message="로그인 시간이 만료됐습니다. 다시 시작하세요.";}
    }catch(Exception e){if(!cancelled){phase="failed";message=e instanceof ChatGptProtocol.ApiError?e.getMessage():"로그인을 완료하지 못했습니다. 브라우저와 네트워크를 확인하고 다시 시도하세요.";}}
    finally{browserUrl="";close();stopSelf();}}
    private static String requestLine(Reader in)throws IOException {StringBuilder line=new StringBuilder();int ch;while((ch=in.read())!=-1){if(ch=='\n')return line.toString().replace("\r","");if(line.length()>=16384)throw new IOException("Callback too large");line.append((char)ch);}return line.length()==0?null:line.toString();}
    private static void reply(Socket socket,int status,String message)throws Exception {byte[] body=("<!doctype html><meta charset=utf-8><meta name=viewport content='width=device-width'><title>Chess Coach</title><p>"+message+"</p>").getBytes(StandardCharsets.UTF_8);OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 "+status+" "+(status==200?"OK":status==400?"Bad Request":"Not Found")+"\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "+body.length+"\r\nCache-Control: no-store\r\nContent-Security-Policy: default-src 'none'\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(body);out.flush();}
    private void close(){try{if(server!=null)server.close();}catch(IOException ignored){}}
    @Override public void onDestroy(){cancelled=true;close();jobs.shutdownNow();browserUrl="";if(running()){phase="cancelled";message="로그인을 취소했습니다.";}stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
