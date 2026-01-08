package io.github.angelsl.wabbitemu.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.angelsl.wabbitemu.BuildConfig
import io.github.angelsl.wabbitemu.utils.PreferenceConstants

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        /*val fileContents = Uri
            .parse("file:///data/user/0/io.github.angelsl.wabbitemu.debug/files/copied_rom.rom")
            .toFile()
            .readText()
        Log.d("Wabbitemu", "File contents: $fileContents")*/
        startActivity(
            Intent(
                this,
                if (prefs.getString(PreferenceConstants.ROM_PATH.toString(), "").isNullOrEmpty())
                    ChooseFileActivity::class.java
                else
                    WabbitemuActivity::class.java
            )
        )
    }

    companion object {
        const val ROM_PATH_KEY = "ROM_PATH"
        const val PREFS_NAME = "settings"
    }
}