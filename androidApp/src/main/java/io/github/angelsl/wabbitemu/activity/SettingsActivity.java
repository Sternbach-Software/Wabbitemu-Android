package io.github.angelsl.wabbitemu.activity;

import android.app.Activity;
import android.os.Bundle;

import io.github.angelsl.wabbitemu.R;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        setContentView(R.layout.settings);
    }
}
