package io.github.angelsl.wabbitemu.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import io.github.angelsl.wabbitemu.R;

public class SettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}
}
