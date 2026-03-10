package io.github.angelsl.wabbitemu.wizard.controller

import android.os.AsyncTask
import android.util.Log
import android.view.View
import dorkbox.cabParser.CabException
import dorkbox.cabParser.CabParser
import dorkbox.cabParser.CabStreamSaver
import dorkbox.cabParser.structure.CabFileEntry
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.activity.WabbitemuActivity
import io.github.angelsl.wabbitemu.calc.CalcModel
import io.github.angelsl.wabbitemu.extract.MsiDatabase
import io.github.angelsl.wabbitemu.extract.MsiHandler
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController
import io.github.angelsl.wabbitemu.wizard.WizardPageController
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData
import io.github.angelsl.wabbitemu.wizard.data.OSDownloadData
import io.github.angelsl.wabbitemu.wizard.view.ChooseOsPageView
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.*
import java.net.CookieManager
import java.util.*
import java.util.regex.Pattern

class ChooseOsPageController(private val mView: ChooseOsPageView) :
    WizardPageController {
    private var mCalcModel: CalcModel? = null
    private var mAuthUrl: String? = null
    private var mLoadOsPageTask: FindOSDownloadsAsyncTask? = null
    private var mNextPage = 0
    private var mNavController: WizardNavigationController? = null
    private var mIsFinished = false
    private var mIsFinalPage = false
    private var mHasNextPage = false
    private var mDownloadedOsPath: String? = null
    override fun configureButtons(navController: WizardNavigationController) {
        mNavController = navController
        navController.hideNextButton()
        if (mIsFinished) {
            mNavController!!.finishWizard()
        }
    }

    override fun hasPreviousPage(): Boolean {
        return true
    }

    override fun hasNextPage(): Boolean {
        return mHasNextPage
    }

    override fun isFinalPage(): Boolean {
        return mIsFinalPage
    }

    override fun getNextPage(): Int {
        return mNextPage
    }

    override fun getPreviousPage(): Int {
        return R.id.model_page
    }

    override fun onHiding() {
        if (mLoadOsPageTask != null) {
            mLoadOsPageTask!!.cancel(true)
            mLoadOsPageTask = null
        }
    }

    override fun onShowing(previousData: Any) {
        mCalcModel = previousData as CalcModel
        mHasNextPage = false
        mIsFinalPage = false
        mIsFinished = false
        mView.message.setText(R.string.long_loading)
        mView.loadingSpinner.visibility = View.VISIBLE
        mLoadOsPageTask = FindOSDownloadsAsyncTask()
        mLoadOsPageTask!!.execute()
    }

    override fun getTitleId(): Int {
        return R.string.osLoadingTitle
    }

    override fun getControllerData(): Any {
        return if (mIsFinalPage) FinishWizardData(
            mCalcModel,
            mDownloadedOsPath,
            false
        ) else OSDownloadData(
            mCalcModel!!, mAuthUrl!!
        )
    }

    private enum class NextAction {
        LOAD_MSI, LOAD_AUTHENTICATED_OS_PAGE, LOAD_UNAUTHENTICATED_OS_PAGE, ERROR
    }

    private class NextOsAction(val mNextAction: NextAction, val mData: String?)
    private inner class FindOSDownloadsAsyncTask : AsyncTask<Void?, Void?, NextOsAction>() {
        protected override fun doInBackground(vararg params: Void?): NextOsAction {
            val osAction = tryLoadOsPage()
            if (osAction != null) {
                Log.d("Wabbitemu", "Returned successfully")
                return osAction
            }
            val msiAction = tryLoadMsi()
            return msiAction
                ?: NextOsAction(
                    NextAction.ERROR,
                    null
                )
        }

        private var outputStream: ByteArrayOutputStream? = null
        private fun tryLoadMsi(): NextOsAction? {
            val extensionPattern: Pattern
            val extension: String
            when (mCalcModel) {
                CalcModel.TI_73 -> {
                    extensionPattern = Pattern.compile(".*\\.73u", Pattern.CASE_INSENSITIVE)
                    extension = ".73u"
                }
                CalcModel.TI_83P, CalcModel.TI_83PSE -> {
                    extensionPattern = Pattern.compile(".*83Plus.*\\.8xu", Pattern.CASE_INSENSITIVE)
                    extension = ".8xu"
                }
                CalcModel.TI_84P, CalcModel.TI_84PSE -> {
                    extensionPattern = Pattern.compile(".*84Plus.*\\.8xu", Pattern.CASE_INSENSITIVE)
                    extension = ".8xu"
                }
                CalcModel.TI_84PCSE -> {
                    extensionPattern = Pattern.compile(".*\\.8cu", Pattern.CASE_INSENSITIVE)
                    extension = ".8cu"
                }
                else -> {
                    Log.wtf("Wabbitemu", "Why was calc model $mCalcModel??")
                    return null
                }
            }
            var counter = 0
            Log.v("Wabbitemu", counter++.toString())
            val cookieManager = CookieManager()
            Log.v("Wabbitemu", counter++.toString())
            val connection: OkHttpClient = OkHttpClient.Builder()
                .cookieJar(JavaNetCookieJar(cookieManager))
                .build()
            Log.v("Wabbitemu", counter++.toString())
            val msiLink = tryLoadMsiPage(connection) ?: return null
            Log.v("Wabbitemu", counter++.toString())
            val request: Request = Request.Builder()
                .url(msiLink)
                .addHeader("User-Agent", OsDownloadPageController.USER_AGENT)
                .build()
            var randomAccessFile: RandomAccessFile? = null
            try {
                Log.v("Wabbitemu", counter++.toString())
                val response = connection.newCall(request).execute()
                Log.v("Wabbitemu", counter++.toString())
                val msiFile = File(WabbitemuActivity.sBestCacheDir, "msiFile.msi")
                Log.v("Wabbitemu", counter++.toString())
                val fileOutputStream = FileOutputStream(msiFile)
                Log.v("Wabbitemu", counter++.toString())
                fileOutputStream.write(response.body!!.bytes())
                Log.v("Wabbitemu", counter++.toString())
                fileOutputStream.close()
                Log.v("Wabbitemu", counter++.toString())
                randomAccessFile = RandomAccessFile(msiFile, "r")
                Log.v("Wabbitemu", counter++.toString())
                val msiDatabase = MsiDatabase()
                Log.v("Wabbitemu", counter++.toString())
                msiDatabase.open(randomAccessFile)
                Log.v("Wabbitemu", counter++.toString())
                val msiHandler = MsiHandler(msiDatabase)
                Log.v("Wabbitemu", counter++.toString())
                outputStream = null //is initialized by cabParser
                for ((i, item) in msiDatabase.Items.withIndex()) {
                    if (item.realName.endsWith(".cab")) {
                        val test = msiHandler.GetStream(randomAccessFile, item, i)
                        val stream = ByteArrayInputStream(test)
                        val cabParser = CabParser(stream, MyCabStreamSaver(extensionPattern))
                        cabParser.extractStream()
                        val osFile = File(WabbitemuActivity.sBestCacheDir, "osFile$extension")
                        outputStream?.let {
                            val osStream = FileOutputStream(osFile)
                            osStream.write(it.toByteArray())
                            osStream.close()
                            it.close()
                        } ?: return null
                        return NextOsAction(NextAction.LOAD_MSI, osFile.absolutePath)
                    }
                }
                Log.v("Wabbitemu", counter++.toString())
            } catch (_: IOException) {
            } catch (_: CabException) {
            } catch (t: Throwable) {
                Log.e("Wabbitemu", "Failed to download", t)
            }
            finally {
                try {
                    randomAccessFile?.close()
                } catch (e: IOException) {
                    Log.w("Wabbitemu", "Failed to close file $e")
                }
            }
            return null
        }

        private fun tryLoadMsiPage(connection: OkHttpClient): String? {
            val document = try {
                val pageRequest: Request = Request.Builder()
                    .url("https://epsstore.ti.com/OA_HTML/csksxvm.jsp;jsessionid=b401c39d98b4886b458efc7dd5d8327db3bc7777671e65db5db506a2e6bafa8c.e34TbNuKax4RaO0Mah0LaxaTchyRe0?jfn=ZGC7FD5432DD1749EE35764C594E5B43B3511EE256D94188614786F910B87AE331265643E242F68AFA6CE2579F26775AF7EC&lepopus=bE7LmZ2FxS3jD0l7eyTp1L9xkc&lepopus_pses=ZG6B09CB0A3807E876346D50579677E97CB0AB13CC596FF029053030558FF7479CA28505CDAD2053EE19E6BE618AC72AA757DCFBE5884A6B21&oas=eFTq1K0o_gpUMQl6PJojPw..&nSetId=130494&nBrowseCategoryId=10464&cskViewSolSourcePage=cskmbasicsrch.jsp%3FcategoryId%3D10464%26fRange%3Dnull%26fStartRow%3D0%26fSortBy%3D2%26fSortByOrder%3D1")
                    .addHeader("User-Agent", OsDownloadPageController.USER_AGENT)
                    .build()
                Log.d("Wabbitemu", "Built request")
                val pageResponse = connection.newCall(pageRequest).execute()
                Log.d("Wabbitemu", "Got response")
                Jsoup.parse(pageResponse.body!!.string()).also {
                Log.d("Wabbitemu", "Parsed doc: ${it.html()}")

                }
            } catch (e: IOException) {
                Log.e("Wabbitemu", "Failed to download", e)
                return null
            }
            val elements = document.select("#rightcol a")
            if (elements.isEmpty()) {
                Log.e("Wabbitemu", "No elements")
                return null
            }
            val element = elements.iterator().next()
            return "https://epsstore.ti.com//OA_HTML/" + element.attr("href")
        }

        private fun tryLoadOsPage(): NextOsAction? {
            val urlString = osPageUrl ?: return null
            val connection = OkHttpClient()
            val request: Request = Request.Builder()
                .url(urlString)
                .addHeader("User-Agent", OsDownloadPageController.USER_AGENT)
                .build()
            try {
                val response = connection.newCall(request).execute()
                val document = Jsoup.parse(response.body!!.string())
                val elements = document.select(".column-downloaditem")
                for (element in elements) {
                    val linkChildren = element.select("a")
                    for (linkChild in linkChildren) {
                        val href = linkChild.attr("href") ?: continue
                        if (href.lowercase(Locale.getDefault()).endsWith("8xu") ||
                            href.lowercase(Locale.getDefault()).endsWith("8cu") ||
                            href.lowercase(Locale.getDefault()).endsWith("73u")
                        ) {
                            val isProtected = element.classNames().contains("protected-download")
                            val nextAction =
                                if (isProtected) NextAction.LOAD_AUTHENTICATED_OS_PAGE else NextAction.LOAD_UNAUTHENTICATED_OS_PAGE
                            return NextOsAction(nextAction, href)
                        }
                    }
                }
            } catch (e: IOException) {
            }
            return null
        }

        private val osPageUrl: String?
            private get() = when (mCalcModel) {
                CalcModel.TI_73 -> "https://education.ti.com/en/us/software/details/en/956CE30854A74767893104FCDF195B76/73ti73exploreroperatingsystem"
                CalcModel.TI_84P, CalcModel.TI_84PSE -> "https://education.ti.com/en/us/software/details/en/B7DADA7FD4AA40CE9D7911B004B8C460/ti84plusoperatingsystem"
                CalcModel.TI_83P, CalcModel.TI_83PSE -> "https://web.archive.org/web/20150915040752/https://education.ti.com/en/us/software/details/en/C95956E744FB4C0A899F5A63EBEA60DD/83ti83plusoperatingsystemsoftware"
                CalcModel.TI_84PCSE -> "https://education.ti.com/en/asia/software/details/en/812E5FCF48C6456CB156A03DE5D07016/singaporeapprovedosapps"
                else -> null
            }

        override fun onPostExecute(action: NextOsAction) {
            val nextAction = action.mNextAction
            when (nextAction) {
                NextAction.LOAD_MSI -> {
                    mIsFinalPage = true
                    mDownloadedOsPath = action.mData
                }
                NextAction.LOAD_AUTHENTICATED_OS_PAGE, NextAction.LOAD_UNAUTHENTICATED_OS_PAGE -> {
                    mIsFinalPage = false
                    val authUrl = action.mData
                    mAuthUrl =
                        if (authUrl != null && authUrl.startsWith("/")) "https://education.ti.com$authUrl" else authUrl
                    mNextPage =
                        if (nextAction == NextAction.LOAD_AUTHENTICATED_OS_PAGE) R.id.os_download_page else R.id.os_page
                    mHasNextPage = true
                }
                NextAction.ERROR -> {
                    mNavController!!.hideNextButton()
                    mView.loadingSpinner.visibility = View.GONE
                    Log.e("Wabbitemu", "" + action.mData);

                    mView.message.setText(R.string.errorWebPageDownloadError)
                }
            }

            // May happen if the async task finishes before the animation
            if (mNavController == null) {
                mIsFinished = true
            } else {
                mNavController!!.finishWizard()
            }
        }

        private inner class MyCabStreamSaver(private val mExtension: Pattern) : CabStreamSaver {
            override fun openOutputStream(cabFileEntry: CabFileEntry): OutputStream? {
                val name = cabFileEntry.name
                if (!mExtension.matcher(name).matches() || outputStream != null) {
                    return null
                }
                outputStream = ByteArrayOutputStream(cabFileEntry.size.toInt())
                return outputStream!!
            }

            override fun closeOutputStream(
                outputStream: OutputStream?,
                cabFileEntry: CabFileEntry
            ) {
                kotlin.runCatching {
                    outputStream?.close()
                }
            }

            override fun saveReservedAreaData(bytes: ByteArray, i: Int): Boolean {
                return false
            }
        }
    }
}