package com.example.timerapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private TextView chronometer;
    private Status status;
    private int workTime;
    private int pauseTime;
    private int workMinutes;
    private int pauseMinutes;
    private int workSeconds;
    private int pauseSeconds;
    private boolean running;
    private boolean stopped;
    private long startTime;
    private long timeInMilliseconds;
    private long timeSwapBuff;
    private long updateTime;
    private Runnable updateTimeThread;
    private Handler handler;
    private TextView actionType;
    private MediaPlayer startSound = null;
    private MediaPlayer stopSound = null;
    private static final String WORK_FILE_NAME = "storeWorkTime.txt";
    private static final String PAUSE_FILE_NAME = "storePauseTime.txt";

    private void initTimes() {
        status = Status.Pause;
        running = false;
        stopped = true;
        startTime = 0L;
        timeInMilliseconds = 0L;
        timeSwapBuff = 0L;
        updateTime = 0L;
        handler = new Handler();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(LOG_TAG, "onCreate");
        chronometer = findViewById(R.id.chronometer);
        actionType = findViewById(R.id.actionType);
        actionType.setText(Status.Stopped.toString());
        String loadWorkTime = loadData(WORK_FILE_NAME);
        String loadPauseTime = loadData(PAUSE_FILE_NAME);
        if (loadWorkTime == null) {
            workTime = 45;
        } else {
            workTime = Integer.parseInt(loadWorkTime);
        }
        if (loadPauseTime == null) {
            pauseTime = 45;
        } else {
            pauseTime = Integer.parseInt(loadPauseTime);
        }
        workMinutes = workTime / 60;
        pauseMinutes = pauseTime / 60;
        workSeconds = workTime % 60;
        pauseSeconds = pauseTime % 60;
        initTimes();
        updateTimeThread = new Runnable() {
            @SuppressLint({"SetTextI18n", "DefaultLocale"})
            @Override
            public void run() {
                timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
                updateTime = timeSwapBuff + timeInMilliseconds;
                int seconds = (int) (updateTime / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                int milliseconds = (int) (updateTime % 100);
                if (status == Status.Pause) {
                    if (minutes == pauseMinutes && seconds == pauseSeconds) {
                        minutes = 0;
                        seconds = 0;
                        startTime = SystemClock.uptimeMillis();
                        timeSwapBuff = 0;
                        status = Status.Working;
                        initStartSound();
                        startSound.start();
                    }
                } else if (status == Status.Working) {
                    if (minutes == workMinutes && seconds == workSeconds) {
                        minutes = 0;
                        seconds = 0;
                        startTime = SystemClock.uptimeMillis();
                        timeSwapBuff = 0;
                        status = Status.Pause;
                        initStopSound();
                        stopSound.start();
                    }
                }
                actionType.setText(status.toString());
                if (!stopped) {
                    chronometer.setText("" + String.format("%02d", minutes)
                            + ":" + String.format("%02d", seconds)
                            + ":" + String.format("%02d", milliseconds));
                } else {
                    chronometer.setText("00:00:00");
                }
                handler.postDelayed(updateTimeThread, 0);
            }
        };
    }

    public void startChronometer(View view) {
        Log.d(LOG_TAG, "startChronometer");
        if (!running) {
            if (chronometer.getText().toString().equals("00:00:00")) {
                initStartSound();
                startSound.start();
                status = Status.Working;
            }
            startTime = SystemClock.uptimeMillis();
            handler.postDelayed(updateTimeThread, 0);
            stopped = false;
            running = true;
        }
    }

    public void pauseChronometer(View view) {
        Log.d(LOG_TAG, "pauseChronometer");
        if (running) {
            timeSwapBuff += timeInMilliseconds;
            handler.removeCallbacks(updateTimeThread);
            running = false;
        }
    }

    public void stopChronometer(View view) {
        Log.d(LOG_TAG, "stopChronometer");
        handler.removeCallbacks(updateTimeThread);
        initTimes();
        handler.postDelayed(updateTimeThread, 0);
        stopped = true;
        status = Status.Stopped;
        actionType.setText(status.toString());
    }

    public void openSettings(View view) {
        Log.d(LOG_TAG, "openSettings");
        pauseChronometer(view);
        Bundle bundle = new Bundle();
        bundle.putInt("workTime", workTime);
        bundle.putInt("pauseTime", pauseTime);
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                assert data != null;
                Bundle bundle = data.getExtras();
                assert bundle != null;
                workTime = bundle.getInt("workTime");
                pauseTime = bundle.getInt("pauseTime");
                workMinutes = workTime / 60;
                workSeconds = workTime % 60;
                pauseMinutes = pauseTime / 60;
                pauseSeconds = pauseTime % 60;
                boolean modified = bundle.getBoolean("modified");
                if (modified) {
                    storeData(WORK_FILE_NAME, String.valueOf(workTime));
                    storeData(PAUSE_FILE_NAME, String.valueOf(pauseTime));
                    handler.removeCallbacks(updateTimeThread);
                    initTimes();
                    handler.postDelayed(updateTimeThread, 0);
                    stopped = true;
                }
            }
        }
    }

    private void stopStartSound() {
        if (startSound != null) {
            startSound.release();
            startSound = null;
        }
    }

    private void stopStopSound() {
        if (stopSound != null) {
            stopSound.release();
            stopSound = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopStartSound();
        stopStopSound();
    }

    private void initStartSound() {
        if (startSound == null) {
            startSound = MediaPlayer.create(this, R.raw.yeah_buddy);
            startSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopStartSound();
                }
            });
        }
    }

    private void initStopSound() {
        if (stopSound == null) {
            stopSound = MediaPlayer.create(this, R.raw.lightweight);
            stopSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    stopStopSound();
                }
            });
        }
    }

    private void storeData(String fileName, String data) {
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String loadData(String fileName) {
        FileInputStream fis = null;
        String data = null;
        try {
            fis = openFileInput(fileName);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bfr = new BufferedReader(isr);
            data = bfr.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data;
    }
}