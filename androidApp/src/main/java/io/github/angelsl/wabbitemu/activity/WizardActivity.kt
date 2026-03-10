package io.github.angelsl.wabbitemu.activity

import android.app.Activity
import android.app.AlertDialog
import io.github.angelsl.wabbitemu.calc.CalculatorManager
import io.github.angelsl.wabbitemu.wizard.WizardController
import io.github.angelsl.wabbitemu.utils.OSDownloader
import android.os.Bundle
import io.github.angelsl.wabbitemu.R
import android.widget.ViewAnimator
import android.view.ViewGroup
import io.github.angelsl.wabbitemu.wizard.OnWizardFinishedListener
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData
import io.github.angelsl.wabbitemu.utils.ErrorUtils
import io.github.angelsl.wabbitemu.calc.CalcModel
import io.github.angelsl.wabbitemu.wizard.view.LandingPageView
import io.github.angelsl.wabbitemu.wizard.controller.LandingPageController
import io.github.angelsl.wabbitemu.wizard.view.ModelPageView
import io.github.angelsl.wabbitemu.wizard.controller.CalcModelPageController
import io.github.angelsl.wabbitemu.wizard.view.ChooseOsPageView
import io.github.angelsl.wabbitemu.wizard.controller.ChooseOsPageController
import io.github.angelsl.wabbitemu.wizard.view.OsPageView
import io.github.angelsl.wabbitemu.wizard.controller.OsPageController
import io.github.angelsl.wabbitemu.wizard.view.OsDownloadPageView
import io.github.angelsl.wabbitemu.wizard.controller.OsDownloadPageController
import io.github.angelsl.wabbitemu.wizard.view.BrowseOsPageView
import io.github.angelsl.wabbitemu.wizard.controller.BrowseOsPageController
import io.github.angelsl.wabbitemu.wizard.view.BrowseRomPageView
import io.github.angelsl.wabbitemu.wizard.controller.BrowseRomPageController
import android.content.DialogInterface
import android.os.AsyncTask
import android.net.ConnectivityManager
import android.net.NetworkInfo
import io.github.angelsl.wabbitemu.calc.FileLoadedCallback
import android.widget.Spinner
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.github.angelsl.wabbitemu.utils.IntentConstants
import io.github.angelsl.wabbitemu.utils.ViewUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class WizardActivity : AppCompatActivity() {
    private val mCalcManager = CalculatorManager.getInstance()
    private var mWizardController: WizardController? = null
    private var mCreatedFilePath: String? = null
    private var mIsWizardFinishing = false
    private var mOsDownloader: OSDownloader? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wizard)
        val viewAnimator = ViewUtils.findViewById(this, R.id.viewFlipper, ViewAnimator::class.java)
        val navContainer = ViewUtils.findViewById(this, R.id.navContainer, ViewGroup::class.java)
        mWizardController = WizardController(
            this,
            viewAnimator,
            navContainer,
            OnWizardFinishedListener { finalData ->
                if (mIsWizardFinishing) {
                    return@OnWizardFinishedListener
                }
                mIsWizardFinishing = true
                val finishInfo = finalData as FinishWizardData
                if (finishInfo == null) {
                    ErrorUtils.showErrorDialog(this@WizardActivity, R.string.errorRomImage)
                    return@OnWizardFinishedListener
                }
                val calcModel = finishInfo.calcModel
                if (finishInfo.shouldDownloadOs()) {
                    tryDownloadAndCreateRom(
                        calcModel,
                        finishInfo.downloadCode,
                        finishInfo.osDownloadUrl
                    )
                } else if (calcModel == CalcModel.NO_CALC) {
                    finishSuccess(finishInfo.filePath)
                } else {
                    createRomCopyOs(calcModel, finishInfo.filePath)
                }
            })
        val landingPageView =
            ViewUtils.findViewById(this, R.id.landing_page, LandingPageView::class.java)
        mWizardController!!.registerView(R.id.landing_page, LandingPageController(landingPageView))
        val modelPageView = ViewUtils.findViewById(this, R.id.model_page, ModelPageView::class.java)
        mWizardController!!.registerView(R.id.model_page, CalcModelPageController(modelPageView))
        val chooseOsView =
            ViewUtils.findViewById(this, R.id.choose_os_page, ChooseOsPageView::class.java)
        mWizardController!!.registerView(R.id.choose_os_page, ChooseOsPageController(chooseOsView))
        val osPageView = ViewUtils.findViewById(this, R.id.os_page, OsPageView::class.java)
        mWizardController!!.registerView(R.id.os_page, OsPageController(osPageView))
        val osDownloadPageView = ViewUtils.findViewById(
            this, R.id.os_download_page,
            OsDownloadPageView::class.java
        )
        mWizardController!!.registerView(
            R.id.os_download_page,
            OsDownloadPageController(osDownloadPageView)
        )
        val browseOsPageView = ViewUtils.findViewById(
            this, R.id.browse_os_page,
            BrowseOsPageView::class.java
        )
        mWizardController!!.registerView(
            R.id.browse_os_page, BrowseOsPageController(
                browseOsPageView,
                supportFragmentManager
            )
        )
        val browseRomPageView = ViewUtils.findViewById(
            this, R.id.browse_rom_page,
            BrowseRomPageView::class.java
        )
        mWizardController!!.registerView(
            R.id.browse_rom_page, BrowseRomPageController(
                browseRomPageView,
                supportFragmentManager
            )
        )
    }

    override fun onPause() {
        super.onPause()
        cancelDownloadTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelDownloadTask()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.wizard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.helpMenuItem -> {
                val builder = AlertDialog.Builder(this)
                val dialog = builder.setMessage(R.string.aboutRomDescription)
                    .setTitle(R.string.aboutRomTitle)
                    .setPositiveButton(android.R.string.ok) { dialog, id -> dialog.dismiss() }
                    .create()
                dialog.show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (!mWizardController!!.movePreviousPage()) {
            super.onBackPressed()
        }
    }

    private fun tryDownloadAndCreateRom(
        calcModel: CalcModel,
        downloadCode: String?,
        osDownloadUrl: String?
    ) {
        check(!(mOsDownloader != null && mOsDownloader!!.status == AsyncTask.Status.RUNNING)) { "Invalid state, download running" }
        if (!isOnline) {
            val dialog = AlertDialog.Builder(this@WizardActivity)
                .setMessage(resources.getString(R.string.noNetwork))
                .setPositiveButton(android.R.string.ok) { dialog, id ->
                    mIsWizardFinishing = false
                    dialog.dismiss()
                }
                .create()
            dialog.show()
            return
        }
        createRomDownloadOs(calcModel, downloadCode, osDownloadUrl)
    }

    private val isOnline: Boolean
        private get() {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnectedOrConnecting
        }

    private fun createRomCopyOs(calcModel: CalcModel, osFilePath: String?) {
        val bootPagePath = extractBootpage(calcModel)
        if (bootPagePath == null) {
            finishRomError()
            return
        }
        mCalcManager.createRom(
            osFilePath, bootPagePath, mCreatedFilePath, calcModel
        ) { error ->
            if (error == 0) {
                finishSuccess(mCreatedFilePath)
            } else {
                runOnUiThread { finishRomError() }
            }
        }
    }

    private fun extractBootpage(calcModel: CalcModel): String? {
        val resources = resources
        val cache = cacheDir
        mCreatedFilePath = cache.absolutePath + "/"
        val bootPagePath = try {
            File.createTempFile("boot", ".hex", cache)
        } catch (e: IOException) {
            return null
        }
        val bootStream = when (calcModel) {
            CalcModel.TI_73 -> {
                mCreatedFilePath += resources.getString(R.string.ti73)
                resources.openRawResource(R.raw.bf73)
            }
            CalcModel.TI_83P -> {
                mCreatedFilePath += resources.getString(R.string.ti83p)
                resources.openRawResource(R.raw.bf83pbe)
            }
            CalcModel.TI_83PSE -> {
                mCreatedFilePath += resources.getString(R.string.ti83pse)
                resources.openRawResource(R.raw.bf83pse)
            }
            CalcModel.TI_84P -> {
                mCreatedFilePath += resources.getString(R.string.ti84p)
                resources.openRawResource(R.raw.bf84pbe)
            }
            CalcModel.TI_84PSE -> {
                mCreatedFilePath += resources.getString(R.string.ti84pse)
                resources.openRawResource(R.raw.bf84pse)
            }
            CalcModel.TI_84PCSE -> {
                mCreatedFilePath += resources.getString(R.string.ti84pcse)
                resources.openRawResource(R.raw.bf84pcse)
            }
            else -> {
                mCreatedFilePath += resources.getString(R.string.ti83p)
                resources.openRawResource(R.raw.bf83pbe)
            }
        }
        mCreatedFilePath += ".rom"
        var outputStream: FileOutputStream? = null
        try {
            val buffer = ByteArray(4096)
            outputStream = FileOutputStream(bootPagePath)
            while (bootStream.read(buffer) != -1) {
                outputStream.write(buffer, 0, 4096)
            }
        } catch (e: IOException) {
            finishRomError()
        } finally {
            try {
                outputStream?.close()
            } catch (e: IOException) {
                finishRomError()
            }
        }
        return bootPagePath.absolutePath
    }

    private fun createRomDownloadOs(
        calcModel: CalcModel,
        downloadCode: String?,
        osDownloadUrl: String?
    ) {
        val bootPagePath = extractBootpage(calcModel)
        if (bootPagePath == null) {
            finishRomError()
            return
        }
        val spinner = findViewById<View>(R.id.osVersionSpinner) as Spinner
        val osVersion = spinner.selectedItemPosition
        val cache = cacheDir
        val osDownloadPath: File
        osDownloadPath = try {
            File.createTempFile("tios", ".8xu", cache)
        } catch (e: IOException) {
            return
        }
        val osFilePath = osDownloadPath.absolutePath
        mOsDownloader =
            object : OSDownloader(this, osDownloadUrl, osFilePath, calcModel, downloadCode) {
                override fun onPostExecute(success: Boolean) {
                    super.onPostExecute(success)
                    createRom(success, osFilePath, bootPagePath, calcModel)
                }

                override fun onCancelled() {
                    super.onCancelled()
                    mIsWizardFinishing = false
                }
            }
        mOsDownloader!!.execute()
    }

    private fun createRom(
        success: Boolean,
        osFilePath: String,
        bootPagePath: String,
        calcModel: CalcModel
    ) {
        if (success) {
            val calculatorManager = CalculatorManager.getInstance()
            calculatorManager.createRom(
                osFilePath, bootPagePath, mCreatedFilePath, calcModel
            ) { error ->
                if (error == 0) {
                    finishSuccess(mCreatedFilePath)
                } else {
                    finishRomError()
                }
            }
        } else {
            finishOsError()
        }
    }

    private fun finishOsError() {
        showOsError()
    }

    private fun finishRomError() {
        showRomError()
    }

    private fun finishSuccess(fileName: String?) {
        val resultIntent = Intent()
        resultIntent.putExtra(IntentConstants.FILENAME_EXTRA_STRING, fileName)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showOsError() {
        mIsWizardFinishing = false
        ErrorUtils.showErrorDialog(this, R.string.errorOsDownloadDescription)
    }

    private fun showRomError() {
        mIsWizardFinishing = false
        ErrorUtils.showErrorDialog(this, R.string.errorRomCreateDescription)
    }

    private fun cancelDownloadTask() {
        if (mOsDownloader != null) {
            mOsDownloader!!.cancel(true)
            mOsDownloader = null
        }
    }
}