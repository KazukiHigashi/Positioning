package com.example.smasashi.positioning;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private boolean nowPositioning = false;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mField, mGyro;
    private TextView mText;
    MyDbHelper mHelper;
    SQLiteDatabase mDb;

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

    public List<Position> Fingerprint = new ArrayList<>();

    private float[] mGravity;
    private float[] mMagnetic;
    private float[] mGyroscope;

    private Handler mHandler = new Handler();
    private Runnable timerTask = new Runnable(){
        @Override
        public void run(){

            String a = "gravity :x = " + mGravity[0] + " y = " + mGravity[1] + "z = " + mGravity[2];

            mText.setText(a);

            if(nowPositioning)
            {
                mHandler.postDelayed(timerTask, 3000);
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

        if(Fingerprint == null)
        {
            import_database();
        }
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
        if(!nowPositioning) {
            nowPositioning = true;
            mHandler.post(timerTask);
        }
        else
        {
            nowPositioning = false;
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
