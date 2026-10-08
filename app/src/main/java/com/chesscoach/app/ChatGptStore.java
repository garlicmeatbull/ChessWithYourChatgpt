package com.chesscoach.app;
import android.content.Context;
import android.security.keystore.*;
import android.util.AtomicFile;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Arrays;
import org.json.*;
/** Entire profile/token set encrypted; atomic replacement protects rotating refresh tokens. */
final class ChatGptStore {
    static final Object LOCK=new Object();
    private final AtomicFile file;private final String alias;
    ChatGptStore(Context c){this(c,"chatgpt-accounts");}
    ChatGptStore(Context c,String name){file=new AtomicFile(new File(c.getFilesDir(),name+".enc"));alias="chess-"+name;}
    private SecretKey key()throws Exception {KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias(alias)){KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}return (SecretKey)ks.getKey(alias,null);}
    JSONObject read()throws Exception {synchronized(LOCK){if(!file.getBaseFile().exists())return new JSONObject().put("profiles",new JSONArray()).put("active","");byte[] b=file.readFully();if(b.length<29)throw new IOException("저장된 계정 정보를 읽을 수 없습니다.");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Arrays.copyOfRange(b,0,12)));return new JSONObject(new String(cipher.doFinal(Arrays.copyOfRange(b,12,b.length)),StandardCharsets.UTF_8));}}
    void write(JSONObject value)throws Exception {synchronized(LOCK){Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());FileOutputStream out=null;try{out=file.startWrite();out.write(cipher.getIV());out.write(cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8)));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);throw e;}}}
}
