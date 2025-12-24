package io.github.angelsl.wabbitemu.activity

import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnSystemUiVisibilityChangeListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.preference.PreferenceManager
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.SkinBitmapLoader
import io.github.angelsl.wabbitemu.calc.CalcModel
import io.github.angelsl.wabbitemu.calc.CalculatorManager
import io.github.angelsl.wabbitemu.fragment.EmulatorButtonsFragment
import io.github.angelsl.wabbitemu.fragment.EmulatorFragment
import io.github.angelsl.wabbitemu.utils.*
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*

class WabbitemuActivity : AppCompatActivity() {
    private enum class MainMenuItem(private val mPosition: Int) {
        LOAD_FILE_MENU_ITEM(0),
        WIZARD_MENU_ITEM(1),
        RESET_MENU_ITEM(2),
        SCREENSHOT_MENU_ITEM(3),
        SETTINGS_MENU_ITEM(
            4
        ),
        ABOUT_MENU_ITEM(5);

        companion object {
            fun fromPosition(position: Int): MainMenuItem? {
                return values().find { it.mPosition == position } //TODO just use index?
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
    private fun handleFile(f: File, runnable: Runnable) {
//        mEmulatorFragment!!.handleFile(f, runnable)
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
        if (fileName != null) {
            val file = File(fileName)
            Log.d("Wabbitemu", "onCreate: Loading rom file.")
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
//                    launchRunnable.run()
//                    Toast.makeText(this, "Unable to load ROM; error code $errorCode", Toast.LENGTH_LONG).show()
                }
            }
        }
        setTheme(R.style.Wabbitemu)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setContentView(R.layout.main)
        mEmulatorFragment = supportFragmentManager.findFragmentById(R.id.content_frame) as EmulatorFragment
        attachMenu()
        Log.d("Wabbitemu", "onCreate: isFirstRun: $isFirstRun, lastRomModel=$lastRomModel")
        if (isFirstRun) {
            mWasUserLaunched = false
            val wizardIntent = Intent(this, WizardActivity::class.java)
            startActivityForResult(wizardIntent, SETUP_WIZARD)
            return
        }

        // we expect an absolute filename
        val lastRomModel = CalcModel.fromModel(lastRomModel)
        if (lastRomModel != CalcModel.NO_CALC) {
            mSkinLoader.loadSkinAndKeymap(lastRomModel)
        } else if (fileName == null || fileName == "") {
//            runnable.run()
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
        mDrawerLayout = findViewById(R.id.drawer_layout)
        mDrawerList = findViewById(R.id.left_drawer)
        val menuItems = resources.getStringArray(R.array.menu_array)
        mDrawerList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, menuItems)
        mDrawerLayout.setScrimColor(Color.parseColor("#DD000000"))
        mDrawerList.setOnItemClickListener { parent, view, position, id ->
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
            LOAD_FILE_CODE -> if (resultCode == RESULT_OK) {
                val fileName = data?.getStringExtra(IntentConstants.FILENAME_EXTRA_STRING)
                handleFile(File(fileName)) {
                    ErrorUtils.showErrorDialog(
                        this@WabbitemuActivity,
                        R.string.errorLink
                    )
                }
            }
            SETUP_WIZARD -> if (resultCode == RESULT_OK) {
                val fileName = data?.getStringExtra(IntentConstants.FILENAME_EXTRA_STRING)
                handleFile(File(fileName), launchRunnable)
                if (isFirstRun) {
                    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
                    val editor = sharedPrefs.edit()
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
        val screenshot: Bitmap? = null//mEmulatorFragment!!.screenshot
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
            outputDir = File(File(StorageUtils.getPrimaryStoragePath(), "Wabbitemu"), "Screenshots")
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
        startActivity(Intent(this, ChooseFileActivity::class.java))
        /*
        val setupIntent = Intent(this, BrowseActivity::class.java)
        // not perfect but it will work well enough
        val extensions = when (mCalcManager.model) {
            CalcModel.TI_73 -> "\\.(rom|sav|73[b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$"
            CalcModel.TI_82 -> "\\.(rom|sav|82[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$"
            CalcModel.TI_83 -> "\\.(rom|sav|83[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$"
            CalcModel.TI_83P,
            CalcModel.TI_83PSE,
            CalcModel.TI_84P,
            CalcModel.TI_84PSE -> "\\.(rom|sav|8x[b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$"
            CalcModel.TI_84PCSE -> "\\.(rom|sav|8[x|c][b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$"
            CalcModel.TI_85 -> "\\.(rom|sav|85[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$"
            CalcModel.TI_86 -> "\\.(rom|sav|86[b|c|d|g|i|l|m|n|p|q|s|t|u|v|w|y|z])$"
            else -> DEFAULT_FILE_REGEX
        }
        val description = resources.getString(R.string.browseFileDescription)
        setupIntent.putExtra(IntentConstants.EXTENSION_EXTRA_REGEX, extensions)
        setupIntent.putExtra(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING, description)
        startActivityForResult(setupIntent, LOAD_FILE_CODE)*/
    }

    private fun launchWizard() {
        mWasUserLaunched = true
        val wizardIntent = Intent(this, WizardActivity::class.java)
        startActivityForResult(wizardIntent, SETUP_WIZARD)
    }

    private fun resetCalc() {
//        mEmulatorFragment!!.resetCalc()
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

        // The UI options currently enabled are represented by a bitfield.
        // getSystemUiVisibility() gives us that bitfield.
        val decorView = window.decorView
        val uiOptions = decorView.systemUiVisibility

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments
        // the behavior of HIDE_NAVIGATION and FLAG_FULLSCREEN. For the purposes
        // of this sample all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is
        // referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and
        // status bars semi-transparent, and the UI flag does not get cleared
        // when the user interacts with the screen.
        decorView.systemUiVisibility = if (isImmersive) {
            uiOptions or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        } else {
            uiOptions and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION.inv() and View.SYSTEM_UI_FLAG_FULLSCREEN.inv() and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY.inv()
        }
    }

    private inner class VisibilityChangeListener : OnSystemUiVisibilityChangeListener {
        @Deprecated("Deprecated in Java", ReplaceWith("setImmersiveMode(true)"))
        override fun onSystemUiVisibilityChange(visibility: Int) {
            // If someone tries to to change the visibility after have set it,
            // we basically want to ignore it. Usually this is caused by
            // something like the loading dialog.
            setImmersiveMode(true)
        }
    }

    private inner class FinishActivityClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            dialog.dismiss()
            finish()
        }
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