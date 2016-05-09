package com.bq.thumbseekbar;

import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bq.markerseekbar.MarkerSeekBar;
import com.bq.thumbseekbarsample.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MarkerSeekBar bar1 = (MarkerSeekBar) findViewById(R.id.bar2);
        assert bar1 != null;
        bar1.setTextTransformer(new MarkerSeekBar.TextTransformer() {
            @SuppressLint("DefaultLocale")
            @Override
            public String toText(int progress) {
                return String.format(" ¯\\_(ツ)_/¯ %d ", progress);
            }

            @Override
            public String onMeasureLongestText(int seekBarMax) {
                return toText(seekBarMax);
            }
        });
    }
}
