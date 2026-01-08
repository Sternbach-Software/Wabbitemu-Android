package io.github.angelsl.wabbitemu.activity

import android.app.Activity
import android.os.Bundle
import io.github.angelsl.wabbitemu.R

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.settings)
        setContentView(R.layout.settings)
    }
}
