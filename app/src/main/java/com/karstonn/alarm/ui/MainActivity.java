package com.karstonn.alarm.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.karstonn.alarm.R;
import com.karstonn.alarm.ui.scheduleList.ScheduleListFragment;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new ScheduleListFragment())
                    .commit();
        }
    }
}