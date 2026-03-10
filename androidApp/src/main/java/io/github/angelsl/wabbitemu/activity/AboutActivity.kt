package io.github.angelsl.wabbitemu.activity

import android.os.Bundle
import io.github.angelsl.wabbitemu.R
import android.view.ViewGroup
import android.widget.TextView
import android.text.method.LinkMovementMethod
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.angelsl.wabbitemu.utils.ViewUtils

class AboutActivity : AppCompatActivity() {
    public override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setContentView(R.layout.about)
        setTitle(R.string.about)
        val textLinkContainer = findViewById<ViewGroup>(R.id.openSourceLinks)
        for (i in 0 until textLinkContainer.childCount) {
            val view = textLinkContainer.getChildAt(i) as? TextView ?: continue
            view.movementMethod = LinkMovementMethod.getInstance()
        }
        try {
            val view = findViewById<TextView>(R.id.aboutVersion)
            val version = packageManager.getPackageInfo(packageName, 0).versionName
            view.text = version
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("About", "Version exception", e)
        }
    }
}