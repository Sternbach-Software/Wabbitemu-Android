package io.github.angelsl.wabbitemu.activity

import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.preference.PreferenceManager
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.SkinBitmapLoader
import io.github.angelsl.wabbitemu.calc.CalcModel
import io.github.angelsl.wabbitemu.calc.CalculatorManager
import io.github.angelsl.wabbitemu.fragment.EmulatorFragment
import io.github.angelsl.wabbitemu.utils.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class WabbitemuActivity : AppCompatActivity() {
    private enum class MainMenuItem(val position: Int) {
        LOAD_FILE_MENU_ITEM(0),
        WIZARD_MENU_ITEM(1),
        RESET_MENU_ITEM(2),
        SCREENSHOT_MENU_ITEM(3),
        SETTINGS_MENU_ITEM(4),
        ABOUT_MENU_ITEM(5);

        companion object {
            fun fromPosition(position: Int): MainMenuItem? {
                return values().find { it.position == position }
            }
        }
    }

    private val mCalcManager = CalculatorManager.getInstance()
    private val mSkinLoader = SkinBitmapLoader.getInstance()
    private val mVisibilityListener = VisibilityChangeListener()
    private val mSharedPrefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }
    private var mEmulatorFragment: EmulatorFragment? = null
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mDrawerList: ListView
    private var mWasUserLaunched = false

    private fun handleFile(f: File, runnable: Runnable?) {
        mEmulatorFragment?.handleFile(f, runnable)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HttpURLConnection.setFollowRedirects(true)
        workaroundAsyncTaskIssue()
        if (!testNativeLibraryLoad()) {
            ErrorUtils.showErrorDialog(
                this,
                R.string.error_failed_load_native_lib,
                FinishActivityClickListener()
            )
            return
        }

        sBestCacheDir = findBestCacheDir()
        mCalcManager.initialize(this, sBestCacheDir)
        mSkinLoader.initialize(this)
        val fileName = lastRomSetting
        val currentLaunchRunnable = launchRunnable
        if (fileName != null) {
            val file = File(fileName)
            mCalcManager.loadRomFile(file) { errorCode ->
                if (errorCode != 0) {
                    Log.e(
                        "Wabbitemu",
                        String.format(
                            "Loading last ROM '%s' failed with error code %d",
                            fileName,
                            errorCode
                        )
                    )
                    currentLaunchRunnable.run()
                }
            }
        }
        setContentView(R.layout.main)
        mEmulatorFragment = supportFragmentManager.findFragmentById(R.id.content_frame) as EmulatorFragment
        attachMenu()

        if (isFirstRun) {
            mWasUserLaunched = false
            val wizardIntent = Intent(this, WizardActivity::class.java)
            startActivityForResult(wizardIntent, SETUP_WIZARD)
            return
        }

        val lastRomModel = CalcModel.fromModel(lastRomModel)
        if (lastRomModel != CalcModel.NO_CALC) {
            mSkinLoader.loadSkinAndKeymap(lastRomModel)
        } else if (fileName.isNullOrEmpty()) {
            currentLaunchRunnable.run()
        }
    }

    private fun findBestCacheDir(): String? {
        val cacheDir = applicationContext.cacheDir
        if (cacheDir != null) {
            return cacheDir.absolutePath
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (file in applicationContext.externalCacheDirs) {
                if (file != null) {
                    return file.absolutePath
                }
            }
        }
        return null
    }

    private fun workaroundAsyncTaskIssue() {
        try {
            Class.forName("android.os.AsyncTask")
        } catch (ignore: Throwable) {
            // ignored
        }
    }

    private fun testNativeLibraryLoad(): Boolean {
        try {
            mCalcManager.testLoadLib()
        } catch (ex: UnsatisfiedLinkError) {
            return false
        }
        return true
    }

    public override fun onResume() {
        super.onResume()
        if (mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
            window.decorView.setOnSystemUiVisibilityChangeListener(mVisibilityListener)
            setImmersiveMode(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
            setImmersiveMode(false)
            window.decorView.setOnSystemUiVisibilityChangeListener(null)
        }
    }

    private fun attachMenu() {
        mDrawerLayout = ViewUtils.findViewById(this, R.id.drawer_layout, DrawerLayout::class.java)
        mDrawerList = ViewUtils.findViewById(this, R.id.left_drawer, ListView::class.java)
        val menuItems = resources.getStringArray(R.array.menu_array)
        mDrawerList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menuItems)
        mDrawerLayout.setScrimColor(Color.parseColor("#DD000000"))
        mDrawerList.setOnItemClickListener { _, _, position, _ ->
            handleMenuItem(
                MainMenuItem.fromPosition(position)
            )
        }
        mDrawerLayout.addDrawerListener(object : DrawerListener {
            override fun onDrawerStateChanged(arg0: Int) {
                // no-op
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                mDrawerLayout.bringChildToFront(drawerView)
                mDrawerLayout.requestLayout()
            }

            override fun onDrawerOpened(arg0: View) {}
            override fun onDrawerClosed(arg0: View) {}
        })
    }

    private val launchRunnable: Runnable
        get() = Runnable {
            val handler = Handler(Looper.getMainLooper())
            handler.post { ErrorUtils.showErrorDialog(this@WabbitemuActivity, R.string.errorLink) }
            val wizardIntent = Intent(this@WabbitemuActivity, WizardActivity::class.java)
            startActivityForResult(wizardIntent, SETUP_WIZARD)
        }
    private val lastRomSetting: String?
        get() = mSharedPrefs.getString(PreferenceConstants.ROM_PATH.toString(), null)
    private val lastRomModel: Int
        get() = mSharedPrefs.getInt(PreferenceConstants.ROM_MODEL.toString(), -1)
    private val isFirstRun: Boolean
        get() = mSharedPrefs.getBoolean(PreferenceConstants.FIRST_RUN.toString(), true)

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            LOAD_FILE_CODE -> if (resultCode == RESULT_OK && data != null) {
                val uri = data.data
                if (uri != null) {
                    Thread {
                        val file = copyFileFromUri(uri)
                        runOnUiThread {
                            if (file != null) {
                                handleFile(file) {
                                    ErrorUtils.showErrorDialog(
                                        this@WabbitemuActivity,
                                        R.string.errorLink
                                    )
                                }
                            } else {
                                ErrorUtils.showErrorDialog(
                                    this@WabbitemuActivity,
                                    R.string.errorLink
                                )
                            }
                        }
                    }.start()
                }
            }
            SETUP_WIZARD -> if (resultCode == RESULT_OK) {
                val fileName = data?.getStringExtra(IntentConstants.FILENAME_EXTRA_STRING)
                handleFile(File(fileName), launchRunnable)
                if (isFirstRun) {
                    val editor = mSharedPrefs.edit()
                    editor.putBoolean(PreferenceConstants.FIRST_RUN.toString(), false)
                    editor.apply()
                    mDrawerLayout.openDrawer(mDrawerList)
                }
            } else if (!mWasUserLaunched) {
                finish()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList)
        } else {
            mDrawerLayout.openDrawer(mDrawerList)
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val position = when (item.itemId) {
            R.id.aboutMenuItem -> MainMenuItem.ABOUT_MENU_ITEM
            R.id.settingsMenuItem -> MainMenuItem.SETTINGS_MENU_ITEM
            R.id.resetMenuItem -> MainMenuItem.RESET_MENU_ITEM
            R.id.rerunWizardMenuItem -> MainMenuItem.WIZARD_MENU_ITEM
            R.id.loadFileMenuItem -> MainMenuItem.LOAD_FILE_MENU_ITEM
            else -> return super.onOptionsItemSelected(item)
        }
        return handleMenuItem(position)
    }

    private fun handleMenuItem(position: MainMenuItem?): Boolean {
        mDrawerLayout.closeDrawer(mDrawerList)
        return when (position) {
            MainMenuItem.SETTINGS_MENU_ITEM -> {
                launchSettings()
                true
            }
            MainMenuItem.RESET_MENU_ITEM -> {
                resetCalc()
                true
            }
            MainMenuItem.SCREENSHOT_MENU_ITEM -> {
                screenshotCalc()
                true
            }
            MainMenuItem.WIZARD_MENU_ITEM -> {
                launchWizard()
                true
            }
            MainMenuItem.LOAD_FILE_MENU_ITEM -> {
                launchBrowse()
                true
            }
            MainMenuItem.ABOUT_MENU_ITEM -> {
                launchAbout()
                true
            }
            else -> throw IllegalStateException("Invalid menu item")
        }
    }

    private fun screenshotCalc() {
        val screenshot: Bitmap? = mEmulatorFragment?.screenshot
        if (screenshot == null) {
            ErrorUtils.showErrorDialog(this, R.string.errorScreenshot)
            return
        }
        val scaledScreenshot = Bitmap.createScaledBitmap(
            screenshot, screenshot.width * 2,
            screenshot.height * 2, true
        )
        val outputDir: File
        val outputFile: File
        if (StorageUtils.hasExternalStorage()) {
            outputDir = File(File(StorageUtils.primaryStoragePath, "Wabbitemu"), "Screenshots")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val now = sdf.format(Date())
            val fileName = "screenshot$now.png"
            outputFile = File(outputDir, fileName)
        } else {
            ErrorUtils.showErrorDialog(this, R.string.errorMissingSdCard)
            return
        }
        try {
            val out = FileOutputStream(outputFile)
            scaledScreenshot.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.close()
        } catch (e: Exception) {
            ErrorUtils.showErrorDialog(this, R.string.errorScreenshot)
            return
        }
        val formatString = resources.getString(R.string.screenshotSuccess)
        val successString = String.format(formatString, outputFile)
        val toast = Toast.makeText(this, successString, Toast.LENGTH_LONG)
        toast.show()
    }

    private fun launchAbout() {
        val aboutIntent = Intent(this, AboutActivity::class.java)
        startActivity(aboutIntent)
    }

    private fun launchBrowse() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("*/*")
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        try {
            startActivityForResult(
                Intent.createChooser(intent, resources.getString(R.string.browseFileDescription)),
                LOAD_FILE_CODE
            )
        } catch (ex: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Please install a file manager.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchWizard() {
        mWasUserLaunched = true
        val wizardIntent = Intent(this, WizardActivity::class.java)
        startActivityForResult(wizardIntent, SETUP_WIZARD)
    }

    private fun resetCalc() {
        mEmulatorFragment?.resetCalc()
    }

    private fun launchSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun setImmersiveMode(isImmersive: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return
        }

        val decorView = window.decorView
        val uiOptions = decorView.systemUiVisibility

        decorView.systemUiVisibility = if (isImmersive) {
            uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            uiOptions and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
        }
    }

    private inner class VisibilityChangeListener : OnSystemUiVisibilityChangeListener {
        override fun onSystemUiVisibilityChange(visibility: Int) {
            setImmersiveMode(true)
        }
    }

    private inner class FinishActivityClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            dialog.dismiss()
            finish()
        }
    }

    private fun copyFileFromUri(uri: Uri): File? {
        var fileName = getFileName(uri)
        if (fileName == null) {
            fileName = "temp_file.8xp"
        }
        val cacheDir = cacheDir
        val file = File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream!!.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                    return file
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) {
                            result = cursor.getString(index)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result
    }

    companion object {
        private const val LOAD_FILE_CODE = 1
        private const val SETUP_WIZARD = 2
        private const val DEFAULT_FILE_REGEX =
            "\\.(rom|sav|[7|8][2|3|x|c|5|6][b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$"
        @JvmField
        var sBestCacheDir: String? = null
    }
}
