package com.example.smasashi.positioning;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private boolean nowPositioning = false;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mField, mGyro;
    private TextView mText;
    MyDbHelper mHelper;
    SQLiteDatabase mDb;
    private Button button;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Date NowTime = new Date();


    public class Position{
        Integer floor, x, y;
        List<Integer> frequency, width, cnt;
        String direction;
        List<String> bssid, essid;
        List<Double> rssi;

        Position(Integer FL, Integer X, Integer Y, String DIR, String BSSID, String ESSID,
                 Integer FREQ, Double RSSI, Integer WIDTH, Integer CNT){
            this.floor = FL;
            this.x = X;
            this.y = Y;
            this.frequency = new ArrayList<>(Arrays.asList(FREQ));
            this.width = new ArrayList<>(Arrays.asList(WIDTH));
            this.cnt = new ArrayList<>(Arrays.asList(CNT));
            this.direction = DIR;
            this.bssid = new ArrayList<>(Arrays.asList(BSSID));
            this.essid = new ArrayList<>(Arrays.asList(ESSID));
            this.rssi = new ArrayList<>(Arrays.asList(RSSI));
        }
    }

    public class pair implements Comparable<pair>{
        Double p;
        Integer x, y, floor;
        String orientation;

        pair(Integer X, Integer Y, Integer FLOOR, String DIR, Double P)
        {
            this.x = X;
            this.y = Y;
            this.floor = FLOOR;
            this.orientation = DIR;
            this.p = P;
        }

        @Override
        public int compareTo(pair another) {
            return (int)((this.p - another.p)*10000000);
        }
    }

    public List<Position> Fingerprint = new ArrayList<>();

    private float[] mGravity;
    private float[] mMagnetic;
    private float[] mGyroscope;

    private Handler mHandler = new Handler();
    private Runnable timerTask = new Runnable(){
        @Override
        public void run(){

            List<Double> estimation = WiFiFingerprinting_directSQL(3);

            Double nowX = estimation.get(0);
            Double nowY = estimation.get(1);

            String A = "you are at ...\n x = " + nowX + " \n y = " + nowY;

            mText.setText(A);

            if(nowPositioning)
            {
                mHandler.postDelayed(timerTask, 1000);
            }

        }
    };

    private void import_database(){
        String sql = "SELECT * FROM " + mHelper.TABLE_NAME_PWiFi + ";";
        Cursor result = mDb.rawQuery(sql, null);

        Integer nx = -1, ny = -1, nf = -1, cnt = -1;
        String nd = null;
        for (boolean next = result.moveToFirst(); next; next = result.moveToNext()) {
            //Log.d("TAG", Integer.toString(result.getInt(0))+Integer.toString(result.getInt(1))+Integer.toString(result.getInt(2)));

            if(!(nf == result.getInt(0) && nx == result.getInt(1) && ny == result.getInt(2) && nd.equals(result.getString(3)))) {
                nf = result.getInt(0);
                nx = result.getInt(1);
                ny = result.getInt(2);
                nd = result.getString(3);

                Fingerprint.add(new Position(result.getInt(0),result.getInt(1), result.getInt(2),
                        result.getString(3), result.getString(4),
                        result.getString(5),
                        result.getInt(6),
                        result.getDouble(7),
                        result.getInt(8),
                        result.getInt(9)));
                cnt++;
            }
            else
            {
                Fingerprint.get(cnt).floor = result.getInt(0);
                Fingerprint.get(cnt).x = result.getInt(1);
                Fingerprint.get(cnt).y = result.getInt(2);
                Fingerprint.get(cnt).direction = result.getString(3);
                Fingerprint.get(cnt).bssid.add(result.getString(4));
                Fingerprint.get(cnt).essid.add(result.getString(5));
                Fingerprint.get(cnt).frequency.add(result.getInt(6));
                Fingerprint.get(cnt).rssi.add(result.getDouble(7));
                Fingerprint.get(cnt).width.add(result.getInt(8));
                Fingerprint.get(cnt).cnt.add(result.getInt(9));
            }
        }
    }
    private void insert_Database(String method, Double x, Double y)
    {
        ContentValues cv = new ContentValues(3);

        cv.put(MyDbHelper.COL_METHOD, method);
        cv.put(MyDbHelper.COL_DATE, dateFormat.format(NowTime));
        cv.put(MyDbHelper.COL_X, x);
        cv.put(MyDbHelper.COL_Y, y);

        mDb.insert(mHelper.TABLE_NAME_Estimation, null, cv);

    }

    private List<Double> WiFiaidedMagneticMatching(int K)
    {
        List<Double> ans = new ArrayList<>();
        List<Double> WiFians = WiFiFingerprinting(K);



        return ans;
    }

    private List<Double> WiFiFingerprinting_directSQL(int K)
    {
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        List<ScanResult> sample_wifi = new ArrayList<>();
        if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            manager.startScan();
            sample_wifi = manager.getScanResults();
        }

        String BSSID_BIND  = "";
        String POSITION_BIND;

        for(int i = 0; i < sample_wifi.size(); i++)
        {
            BSSID_BIND += "\"" + sample_wifi.get(i).BSSID + "\"";

            if(i != sample_wifi.size()-1){
                BSSID_BIND += ", ";
            }
        }


        String sql = "SELECT DISTINCT Floor, xcoordinate, ycoordinate, Direction FROM " + mHelper.TABLE_NAME_PWiFi + " WHERE BSSID IN (" + BSSID_BIND + ");";

        Log.d("Smasashi", sql);

        Cursor result = mDb.rawQuery(sql, null);

        List<pair> estimation = new ArrayList<>();

        for (boolean next = result.moveToFirst(); next; next = result.moveToNext()) {
            POSITION_BIND = "Floor = " + result.getInt(0) + " AND xcoordinate = " + result.getInt(1) + " AND ycoordinate = " + result.getInt(2) + " AND Direction = \"" + result.getString(3) + "\"";

            String search = "SELECT * FROM " + MyDbHelper.TABLE_NAME_PWiFi + " WHERE " + POSITION_BIND + ";";

            Log.d("String", search);

            Cursor pos = mDb.rawQuery(search, null);

            Double point = 0.0;
            for(int j = 0; j < sample_wifi.size(); j++){
                for(boolean nxt = pos.moveToFirst(); nxt; nxt = pos.moveToNext()) {
                    if(sample_wifi.get(j).BSSID.equals(pos.getString(4)))
                    {
                        point += Math.pow(((double)sample_wifi.get(j).level - pos.getDouble(7)), 2);
                        break;
                    }

                    if(pos.isLast())
                    {
                        point += 100;
                    }
                }
            }
            estimation.add(new pair(result.getInt(1),result.getInt(2),result.getInt(0),result.getString(3), point));

        }

        Collections.sort(estimation);

        Double resultX = 0.0, resultY = 0.0;
        for(int i = 0; i < K; i++) {
            resultX += estimation.get(i).x;
            resultY += estimation.get(i).y;
        }

        List<Double> ans = new ArrayList<>(Arrays.asList(resultX/K, resultY/K));

        insert_Database("Wi-Fi Fingerprinting", resultX/K, resultY/K);

        return ans;
    }


    private List<Double> WiFiFingerprinting(int K)
    {
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        List<ScanResult> results = new ArrayList<>();
        if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
            manager.startScan();
            results = manager.getScanResults();
        }

        List<pair> estimation = new ArrayList<>();

        for(int i = 0; i < Fingerprint.size(); i++)
        {
            Double point = 0.0;
            for(int j = 0; j < results.size(); j++)
            {
                for(int k = 0; k < Fingerprint.get(i).bssid.size(); k++)
                {
                    if(results.get(j).BSSID.equals(Fingerprint.get(i).bssid.get(k)))
                    {
                        point += Math.pow(((double)results.get(j).level - Fingerprint.get(i).rssi.get(k)), 2);
                        break;
                    }

                    if( k == Fingerprint.get(i).bssid.size()-1)
                    {
                        point += 100;
                    }
                }

            }
            //estimation.add(new pair(Fingerprint.get(i).x, Fingerprint.get(i).y, Fingerprint.get(i).floor, point));
        }
        Collections.sort(estimation);

        Double resultX = 0.0, resultY = 0.0;
        for(int i = 0; i < K; i++) {
            resultX += estimation.get(i).x;
            resultY += estimation.get(i).y;
        }

        List<Double> ans = new ArrayList<>(Arrays.asList(resultX/K, resultY/K));

        insert_Database("Wi-Fi Fingerprinting", resultX/K, resultY/K);

        return ans;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mText = (TextView)findViewById(R.id.textView);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mHelper = new MyDbHelper(this);

        button = (Button)findViewById(R.id.button);
        button.setText("Begin Positioning");


    }

    @Override
    public void onResume() {
        super.onResume();

        mDb = mHelper.getReadableDatabase();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mField, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public void onPause(){
        super.onPause();

        mDb.close();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void ClickButton(View v)
    {
        if(Fingerprint.size() == 0)
        {
            import_database();
            Log.d("Smasashi", "afjeiw ");
        }

        if(!nowPositioning) {
            nowPositioning = true;
            mHandler.post(timerTask);
            button.setText("Now Positioning!");
        }
        else
        {
            nowPositioning = false;
            button.setText("Begin Positioning");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        switch(e.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mGravity = e.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetic = e.values.clone();
                break;
            case Sensor.TYPE_GYROSCOPE:
                mGyroscope = e.values.clone();
                break;
            default:
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
