package com.picstar.picstarapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Set;

public class PSR_PrefsManager {

    private Context context;
    private SharedPreferences prefsManager;

    public PSR_PrefsManager(Context applicationContext) {
        this.context = applicationContext;
        this.prefsManager = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void save(String key, String value) {
        SharedPreferences.Editor editor = prefsManager.edit();
        editor.putString(key, value);
        editor.apply();
    }


    public boolean contains(String key){
        return prefsManager.contains(key);
    }

    public void save(String key, Set<String> value) {
        SharedPreferences.Editor editor = prefsManager.edit();
        editor.putStringSet(key, value);
        editor.apply();
    }


    /*For saving the Integer value*/
    public void save(String key, int value) {
        SharedPreferences.Editor editor = prefsManager.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /*For saving the Long value*/
    public void save(String key, long value) {
        SharedPreferences.Editor editor = prefsManager.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /*For saving the boolean value*/
    public void save(String key, boolean value) {
        SharedPreferences.Editor editor = prefsManager.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /*This method deals with getting the values saved in this SharedPreferences*/
    public String get(String... key) {
        if (key.length == 1) {
            return prefsManager.getString(key[0],"");
        } else {
            return prefsManager.getString(key[0], key[1]);
        }
    }

    public void clearAllPrefs() {
        prefsManager.edit().clear().apply();
    }

    public boolean hasKey(String key) {
        return prefsManager.contains(key);
    }

    public void removeKey(String key) {
        SharedPreferences.Editor editor = prefsManager.edit();
        editor.remove(key);
        editor.apply();
    }

    public boolean getBoolean(String key) {
        return prefsManager.getBoolean(key, false);
    }

    public int getInt(String key) {
        return prefsManager.getInt(key, 0);
    }
}
