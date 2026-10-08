package com.chesscoach.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.util.*;

/** One local library for played/imported games. Per-ply patches preserve late AI responses. */
public final class Records extends SQLiteOpenHelper {
    private static Records instance;
    public static synchronized Records get(Context context){if(instance==null)instance=new Records(context.getApplicationContext());return instance;}
    private Records(Context c){this(c,"chess-records.db");}
    Records(Context c,String databaseName){super(c,databaseName,null,1);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE games (id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,origin TEXT NOT NULL,pgn TEXT NOT NULL,plies INTEGER NOT NULL,created INTEGER NOT NULL,updated INTEGER NOT NULL,analyses TEXT NOT NULL DEFAULT '{}',highlights TEXT NOT NULL DEFAULT '[]')");}
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("Migration required");}
    public record Saved(long id,String title,String origin,String pgn,int plies,long created,long updated,JSONObject analyses,JSONArray highlights) {}
    private Saved read(Cursor c)throws JSONException{return new Saved(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getLong(5),c.getLong(6),new JSONObject(c.getString(7)),new JSONArray(c.getString(8)));}
    public synchronized List<Saved> list(){List<Saved> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM games ORDER BY updated DESC",null)){while(c.moveToNext())out.add(read(c));}catch(JSONException e){throw new IllegalStateException(e);}return out;}
    public synchronized Saved find(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT * FROM games WHERE id=?",new String[]{Long.toString(id)})){return c.moveToFirst()?read(c):null;}catch(JSONException e){throw new IllegalStateException(e);}}
    public synchronized long create(String title,String origin,Pgn.Game game){
        if(!Arrays.asList("played","imported").contains(origin))throw new IllegalArgumentException("Invalid origin");
        ContentValues v=new ContentValues();v.put("title",title);v.put("origin",origin);v.put("pgn",game.export());v.put("plies",game.plies().size());long now=System.currentTimeMillis();v.put("created",now);v.put("updated",now);
        return getWritableDatabase().insertOrThrow("games",null,v);
    }
    public synchronized void rename(long id,String title){String clean=title.trim();if(clean.isEmpty()||clean.length()>100)throw new IllegalArgumentException("이름은 1~100자로 입력하세요.");ContentValues v=new ContentValues();v.put("title",clean);v.put("updated",System.currentTimeMillis());getWritableDatabase().update("games",v,"id=?",new String[]{Long.toString(id)});}
    public synchronized void delete(long id){getWritableDatabase().delete("games","id=?",new String[]{Long.toString(id)});}
    public synchronized long branch(long parent,Pgn.Game source,int count){Saved saved=find(parent);if(saved==null)throw new IllegalArgumentException("기록을 찾을 수 없어요.");Pgn.Game prefix=GameSession.prefix(source,count);SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{long id=create(saved.title()+" · 분기", "played",prefix);for(int i=0;i<prefix.plies().size();i++){JSONObject entry=saved.analyses().optJSONObject(Integer.toString(i));if(entry!=null)patch(id,i,entry);}JSONArray kept=new JSONArray();for(int i=0;i<saved.highlights().length();i++){JSONObject h=saved.highlights().optJSONObject(i);if(h!=null&&h.optInt("ply")<count)kept.put(h);}ContentValues v=new ContentValues();v.put("highlights",kept.toString());db.update("games",v,"id=?",new String[]{Long.toString(id)});db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    public synchronized void updateGame(long id,Pgn.Game game){
        Saved old=find(id);if(old==null)throw new IllegalArgumentException("Missing record");
        ContentValues values=new ContentValues();values.put("pgn",game.export());values.put("plies",game.plies().size());values.put("updated",System.currentTimeMillis());
        if(game.plies().size()<old.plies){
            JSONObject analyses=old.analyses;List<String> keys=new ArrayList<>();analyses.keys().forEachRemaining(keys::add);
            for(String k:keys)if(Integer.parseInt(k)>=game.plies().size())analyses.remove(k);
            JSONArray retained=new JSONArray();for(int i=0;i<old.highlights.length();i++){JSONObject f=old.highlights.optJSONObject(i);if(f!=null&&f.optInt("ply")<game.plies().size())retained.put(f);}
            values.put("analyses",analyses.toString());values.put("highlights",retained.toString());
        }
        getWritableDatabase().update("games",values,"id=?",new String[]{Long.toString(id)});
    }
    public synchronized void patch(long id,int ply,JSONObject patch){
        Saved old=find(id);if(old==null||ply<0||ply>=old.plies)return;
        try{JSONObject entry=old.analyses.optJSONObject(Integer.toString(ply));if(entry==null)entry=new JSONObject();Iterator<String> keys=patch.keys();while(keys.hasNext()){String k=keys.next();entry.put(k,patch.get(k));}old.analyses.put(Integer.toString(ply),entry);
            ContentValues v=new ContentValues();v.put("analyses",old.analyses.toString());v.put("updated",System.currentTimeMillis());getWritableDatabase().update("games",v,"id=?",new String[]{Long.toString(id)});
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    public synchronized void explanation(long id,int ply,JSONObject response,String text)throws JSONException {
        Saved old=find(id);if(old==null||ply<0||ply>=old.plies)return;
        JSONObject entry=old.analyses.optJSONObject(Integer.toString(ply));JSONObject all=entry==null?null:entry.optJSONObject("aiByModel");if(all==null)all=new JSONObject();
        JSONObject cached=all.optJSONObject(response.getString("model"));JSONObject previous=cached==null?null:cached.optJSONObject("response");
        if(previous==null&&entry!=null&&entry.optJSONObject("aiResponse")!=null&&response.getString("model").equals(entry.getJSONObject("aiResponse").optString("model")))previous=entry.getJSONObject("aiResponse");
        response=CoachText.merge(previous,response);
        if(response.has("explanation"))text=AnalysisJson.explanation(response);
        all.put(response.getString("model"),new JSONObject().put("response",response).put("text",text));
        patch(id,ply,new JSONObject().put("aiByModel",all).put("aiResponse",response).put("aiText",text));
    }
    public synchronized void highlights(long id,List<Highlights.Finding> findings){
        JSONArray array=new JSONArray();try{for(var f:findings)array.put(new JSONObject().put("ply",f.ply()).put("title",f.title()).put("reason",f.reason()).put("focus",f.focus()).put("priority",f.priority()));}catch(JSONException e){throw new IllegalStateException(e);}
        ContentValues v=new ContentValues();v.put("highlights",array.toString());v.put("updated",System.currentTimeMillis());getWritableDatabase().update("games",v,"id=?",new String[]{Long.toString(id)});
    }
}
