package com.example.timerapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();
    private EditText workTime;
    private EditText pauseTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Log.d(LOG_TAG, "onCreate");
        workTime = findViewById(R.id.workTime);
        pauseTime = findViewById(R.id.pauseTime);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        assert bundle != null;
        workTime.setText(String.valueOf(bundle.getInt("workTime")));
        pauseTime.setText(String.valueOf(bundle.getInt("pauseTime")));
    }

    public void saveAndReturn(View view) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        if (workTime.getText().toString().equals("") || pauseTime.getText().toString().equals("")) {
            Toast.makeText(this, "no input", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        int work = Integer.parseInt(workTime.getText().toString());
        int pause = Integer.parseInt(pauseTime.getText().toString());
        if (work > 1000 || pause > 1000 || work <= 0 || pause <= 0) {
            Toast.makeText(this, "wrong input", Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        bundle.putInt("workTime", work);
        bundle.putInt("pauseTime", pause);
        bundle.putBoolean("modified", true);
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void returnWithoutSave(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }
}