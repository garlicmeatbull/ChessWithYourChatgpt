package com.chesscoach.app;
import android.app.Activity;
import android.content.*;
import android.content.res.Configuration;
import android.os.Bundle;
/** Applies an app preference without changing the phone's theme. */
public class ThemedActivity extends Activity {
    private String appliedTheme;
    @Override protected void attachBaseContext(Context base){
        appliedTheme=base.getSharedPreferences("appearance",0).getString("theme","system");
        Configuration config=new Configuration(base.getResources().getConfiguration());
        if(!appliedTheme.equals("system"))config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|(appliedTheme.equals("dark")?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO);
        super.attachBaseContext(base.createConfigurationContext(config));
    }
    @Override protected void onResume(){super.onResume();if(!getSharedPreferences("appearance",0).getString("theme","system").equals(appliedTheme))recreate();}
}
