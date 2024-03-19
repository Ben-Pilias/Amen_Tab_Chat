package com.pilias.app.telecoms.amentab.chat;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Prefs {

    private final Context context;
    private  SharedPreferences sharedPreferences;
    private  SharedPreferences.Editor editor;
    private final String TAG = "Prefs";

    public Prefs(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        //sharedPreferences = activityMain.getPreferences(Context.MODE_PRIVATE);
        //editor = sharedPreferences.edit();
    }



    ///////////////////////////


    public int getIntCredit(String key, int def) {
        return  sharedPreferences.getInt(key, def);
    }



    public void setString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
        Log.d(TAG,key+" +"+value);
    }






    public String getString(String key, String s) {
        String s1 =sharedPreferences.getString(key,s);
        Log.d(TAG,key+" : "+s1);
        return s1;
    }
}
