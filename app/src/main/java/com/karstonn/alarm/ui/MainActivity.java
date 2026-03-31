package com.karstonn.alarm.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.karstonn.alarm.R;

public class MainActivity extends AppCompatActivity {

    //RepoSchedulerBinder binder = new RepoSchedulerBinder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOn = findViewById(R.id.btnOn);
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Alarm set for 1 from now", Toast.LENGTH_SHORT).show();

            }
        });
    }

}

