package com.example.android.prm391x_googlemap_anhnlfx00594;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);

        Intent intent1 = getIntent();
        String pathDistance = intent1.getStringExtra(MapsActivity.PATH_DISTANCE);
        String pathDuration = intent1.getStringExtra(MapsActivity.PATH_DURATION);

        TextView distanceTextView = findViewById(R.id.path_distance);
        TextView durationTextView = findViewById(R.id.path_duration);

        distanceTextView.setText(pathDistance);
        durationTextView.setText(pathDuration);
    }
}
