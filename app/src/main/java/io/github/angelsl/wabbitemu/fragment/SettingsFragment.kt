package io.github.angelsl.wabbitemu.fragment

import android.os.Bundle
import android.preference.PreferenceFragment
import io.github.angelsl.wabbitemu.R

class SettingsFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }
}
