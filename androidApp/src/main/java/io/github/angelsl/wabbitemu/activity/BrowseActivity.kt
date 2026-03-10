package io.github.angelsl.wabbitemu.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.fragment.BrowseFragment
import io.github.angelsl.wabbitemu.utils.IntentConstants
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected

class BrowseActivity : AppCompatActivity(), OnBrowseItemSelected {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val regex = intent.getStringExtra(IntentConstants.EXTENSION_EXTRA_REGEX)
        val description = intent.getStringExtra(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING)
        val bundle = Bundle()
        bundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, regex)
        bundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, description)
        val fragment = BrowseFragment()
        fragment.setCallback(this)
        fragment.arguments = bundle
        setTitle(R.string.selectFile)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commit()
    }

    override fun onBrowseItemSelected(fileName: String) {
        val returnIntent = Intent()
        returnIntent.putExtra(IntentConstants.FILENAME_EXTRA_STRING, fileName)
        setResult(RESULT_OK, returnIntent)
        finish()
    }
}