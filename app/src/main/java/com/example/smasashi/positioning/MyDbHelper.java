package com.example.smasashi.positioning;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Smasashi on 15/09/29.
 */
public class MyDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "measurement.db";
    private Context mCtx; //<-- declare a Context reference
    private static final int DB_VERSION = 1;
    private static final int DB_WiFi = 0;
    private static final int DB_Geomagnetic = 1;
    private static final int DB_Gravity = 2;
    private static final int DB_Gyro = 3;

    public static final String FLOOR_NUM = "Floor";
    public static final String TABLE_NAME_WiFi = "WiFi";
    public static final String TABLE_NAME_Geo =  "Geomagnetic_flux_density";
    public static final String TABLE_NAME_Gravity = "Gravity";
    public static final String TABLE_NAME_Gyro = "Gyro";
    public static final String TABLE_NAME_PWiFi = "PROCESSED_WiFI";
    public static final String TABLE_NAME_Pos = "posture_estimation";


    public static final String COL_X = "xcoordinate";
    public static final String COL_Y = "ycoordinate";
    public static final String COL_NUM = "number";
    public static final String COL_DIRECTION = "Direction";
    public static final String COL_DATE = "Date";
    public static final String COL_ESSID = "ESSID";
    public static final String COL_BSSID = "BSSID";
    public static final String COL_RSSI = "RSSI";
    public static final String COL_FREQ = "Frequency";
    public static final String COL_GYRO_X = "Gyro_X";
    public static final String COL_GYRO_Y = "Gyro_Y";
    public static final String COL_GYRO_Z = "Gyro_Z";
    public static final String COL_GRAVITY_X = "Gravity_X";
    public static final String COL_GRAVITY_Y = "Gravity_Y";
    public static final String COL_GRAVITY_Z = "Gravity_Z";
    public static final String COL_GM_X = "Geomagnetic_flux_density_X";
    public static final String COL_GM_Y= "Geomagnetic_flux_density_Y";
    public static final String COL_GM_Z = "Geomagnetic_flux_density_Z";
    public static final String COL_AVGGM_X = "avg_Geo_X";
    public static final String COL_AVGGM_Y = "avg_Geo_Y";
    public static final String COL_AVGGM_Z = "avg_Geo_Z";
    public static final String COL_AVGRSSI = "avg_RSSI";
    public static final String COL_DIFRSSI = "dif_RSSI";
    public static final String COL_AVGGRAVITY_X = "avg_Gra_X";
    public static final String COL_AVGGRAVITY_Y = "avg_Gra_Y";
    public static final String COL_AVGGRAVITY_Z = "avg_Gra_Z";

    private static final String STRING_CREATE_WiFi =
            "CREATE TABLE " + TABLE_NAME_WiFi + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " + FLOOR_NUM + " INTEGER, " + COL_X + " REAL, " + COL_Y + " REAL, " + COL_DATE + " DATE, " + COL_DIRECTION + " TEXT, " + COL_ESSID + " TEXT, "  + COL_BSSID + " TEXT, " + COL_RSSI + " INTEGER," + COL_FREQ + " INTEGER);";
    private static final String STRING_CREATE_Geo =
            "CREATE TABLE " + TABLE_NAME_Geo  + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "  + FLOOR_NUM + " INTEGER, " + COL_X + " REAL, " + COL_Y + " REAL, " + COL_DATE + " DATE, " + COL_DIRECTION + " TEXT, " + COL_GM_X + " REAL, " + COL_GM_Y + " REAL, " + COL_GM_Z + " REAL);";
    private static final String STRING_CREATE_Gravity =
            "CREATE TABLE " + TABLE_NAME_Gravity  + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "  + FLOOR_NUM + " INTEGER, " + COL_X + " REAL, " + COL_Y + " REAL, " + COL_DATE + " DATE, " + COL_DIRECTION + " TEXT, " + COL_GRAVITY_X + " REAL, " + COL_GRAVITY_Y + " REAL, " + COL_GRAVITY_Z + " REAL);";
    private static final String STRING_CREATE_Gyro =
            "CREATE TABLE " + TABLE_NAME_Gyro  + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, "  + FLOOR_NUM + " INTEGER, " + COL_X + " REAL, " + COL_Y + " REAL, " + COL_DATE + " DATE, " + COL_DIRECTION + " TEXT, " + COL_GYRO_X + " REAL, " + COL_GYRO_Y+ " REAL, " + COL_GYRO_Z + " REAL);";



    public MyDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        Log.d("tag", "wowwow");
        mCtx = context; //<-- fill it with the Context you are passe
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //run-as com.example.smasashi.coordinate cat /data/data/com.example.smasashi.coordinate/databases/measurement.db > /sdcard/measurement.db
        InputStream is = null;
        BufferedReader br = null;

        Log.d("tag", "hello");

        try {
            try {
                // assetsフォルダ内の sample.txt をオープンする
                is = mCtx.getAssets().open("dump.sql");
                br = new BufferedReader(new InputStreamReader(is));
                Log.d("tag", "hello");
                // １行ずつ読み込み、改行を付加する
                String str;
                while ((str = br.readLine()) != null) {
                    db.execSQL(str);
                }
            } finally {
                if (is != null) is.close();
                if (br != null) br.close();
            }
        } catch (Exception e){
            // エラー発生時の処理
            Log.d("tag", "goodbye");
        }
        Log.d("tag", "hello");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME_WiFi);
        onCreate(db);
    }
}

