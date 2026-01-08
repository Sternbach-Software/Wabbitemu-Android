package io.github.angelsl.wabbitemu.fragment

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.angelsl.wabbitemu.CalcSkin
import io.github.angelsl.wabbitemu.CalcSkin.CalcSkinChangedListener
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.SkinBitmapLoader
import io.github.angelsl.wabbitemu.WabbitLCD
import io.github.angelsl.wabbitemu.calc.CalculatorManager
import io.github.angelsl.wabbitemu.calc.FileLoadedCallback
import io.github.angelsl.wabbitemu.utils.PreferenceConstants
import io.github.angelsl.wabbitemu.utils.ProgressTask
import io.github.angelsl.wabbitemu.utils.ViewUtils
import java.io.File
import java.util.concurrent.CountDownLatch

class EmulatorFragment : Fragment() {
    private val mHandler = Handler(Looper.getMainLooper())
    private val mCalculatorManager = CalculatorManager.getInstance()
    private val mSkinLoader = SkinBitmapLoader.getInstance()
    private val mSkinUpdateListener = SkinUpdateListener()
    private val mImmersiveModeListener = ImmersiveModeListener()
    private var mContext: Context? = null
    private var mSharedPrefs: SharedPreferences? = null
    private var mCalcSkin: CalcSkin? = null
    private var mSendFileTask: ProgressTask? = null
    private var mSurfaceView: WabbitLCD? = null
    private var mIsInitialized = false
    private var mFileToHandle: File? = null
    private var mRunnableToHandle: Runnable? = null

    fun handleFile(f: File, runnable: Runnable?) {
        if (!mIsInitialized) {
            mFileToHandle = f
            mRunnableToHandle = runnable
            return
        }

        val name = f.name
        val isRom = name.endsWith(".rom") || name.endsWith(".sav")
        val stringId = if (isRom) R.string.sendingRom else R.string.sendingFile
        val description = mContext!!.resources.getString(stringId)

        mSendFileTask = LoadFileAsyncTask(mContext, description, false, runnable, f, isRom)
        mSendFileTask!!.execute()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.emulator, container)
        mSurfaceView = ViewUtils.findViewById(view, R.id.textureView, WabbitLCD::class.java)
        mCalcSkin = ViewUtils.findViewById(view, R.id.skinView, CalcSkin::class.java)
        mCalculatorManager.setScreenCallback(mSurfaceView)
        mCalculatorManager.setCalcSkin(mCalcSkin)

        mSkinLoader.registerSkinChangedListener(mSkinUpdateListener)

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mSkinLoader.unregisterSkinChangedListener(mSkinUpdateListener)
        mSharedPrefs!!.unregisterOnSharedPreferenceChangeListener(mImmersiveModeListener)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mContext = activity
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext!!)
        mSharedPrefs!!.registerOnSharedPreferenceChangeListener(mImmersiveModeListener)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestWritePermissions()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestWritePermissions() {
        if (activity!!.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        mCalculatorManager.setCalcSkin(mCalcSkin)
        mCalculatorManager.unPauseCalc(ACTIVITY_PAUSE_KEY)
        mSurfaceView!!.updateSkin(mSkinLoader.lcdRect, mSkinLoader.lcdSkinRect)
        mCalcSkin!!.invalidate()

        mIsInitialized = true
        if (mFileToHandle != null) {
            handleFile(mFileToHandle!!, mRunnableToHandle)
            mFileToHandle = null
            mRunnableToHandle = null
        }
        super.onResume()

        updateSettings()
    }

    override fun onPause() {
        mCalculatorManager.pauseCalc(ACTIVITY_PAUSE_KEY)
        super.onPause()

        mCalculatorManager.saveCurrentRom()
        mIsInitialized = false

        if (mSendFileTask != null) {
            mSendFileTask!!.cancel(false)
        }
    }

    val screenshot: Bitmap?
        get() = mSurfaceView!!.screen

    fun resetCalc() {
        mCalculatorManager.resetCalc()
    }

    private fun updateSettings() {
        if (mSharedPrefs!!.getBoolean(PreferenceConstants.STAY_AWAKE.toString(), false)) {
            activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private inner class ImmersiveModeListener : OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            if (PreferenceConstants.IMMERSIVE_MODE.toString() == key) {
                mSkinLoader.destroySkin()
                mSkinLoader.loadSkinAndKeymap(CalculatorManager.getInstance().model)
            }
        }
    }

    private inner class SkinUpdateListener : CalcSkinChangedListener {
        override fun onCalcSkinChanged(lcdRect: Rect, lcdSkinRect: Rect) {
            mHandler.post {
                mSurfaceView!!.updateSkin(lcdRect, lcdSkinRect)
                Log.d("View", "Request update")
                mCalcSkin!!.invalidate()
            }
        }
    }

    private inner class LoadFileAsyncTask(
        context: Context?,
        descriptionString: String?,
        isCancelable: Boolean,
        private val mRunnable: Runnable?,
        private val mFile: File,
        private val mIsRom: Boolean
    ) : ProgressTask(context, descriptionString, isCancelable) {
        override fun onPreExecute() {
            super.onPreExecute()

            if (mIsRom) {
                mCalcSkin!!.destroySkin()
                mCalcSkin!!.invalidate()
            }
        }

        private var mSuccess: Boolean? = null

        override fun doInBackground(vararg params: Void?): Boolean? {
            val latch = CountDownLatch(1)
            mSuccess = java.lang.Boolean.FALSE
            val callback = FileLoadedCallback { errorCode ->
                mSuccess = errorCode == 0
                latch.countDown()
            }

            if (mIsRom) {
                mCalculatorManager.loadRomFile(mFile, callback)
            } else {
                mCalculatorManager.loadFile(mFile, callback)
            }

            try {
                latch.await()
            } catch (e: InterruptedException) {
                return java.lang.Boolean.FALSE
            }

            return mSuccess
        }

        override fun onPostExecute(wasSuccessful: Boolean) {
            if (!wasSuccessful && mRunnable != null) {
                mRunnable.run()
            }

            super.onPostExecute(wasSuccessful)
        }
    }

    companion object {
        private const val ACTIVITY_PAUSE_KEY = "EmulatorFragment"
        const val REQUEST_CODE = 21
    }
}
