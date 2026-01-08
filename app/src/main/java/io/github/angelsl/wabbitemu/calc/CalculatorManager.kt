package io.github.angelsl.wabbitemu.calc

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import io.github.angelsl.wabbitemu.CalcSkin
import io.github.angelsl.wabbitemu.SkinBitmapLoader
import io.github.angelsl.wabbitemu.utils.PreferenceConstants
import java.io.File
import java.util.TimerTask
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CalculatorManager private constructor() {
    private val mSkinLoader = SkinBitmapLoader.getInstance()
    private val mHasLoadedRom = AtomicBoolean()
    private val mCalcThread = CalcThread()
    private val mKeyTimePressed = Array(8) { LongArray(8) }
    private val mIsKeyPressPending = Array(8) { BooleanArray(8) }
    private val mRepressExecutor: ScheduledExecutorService = ScheduledThreadPoolExecutor(1)


    private var mContext: Context? = null
    private var mSharedPrefs: SharedPreferences? = null
    private var mCurrentRomFile: String? = null
    var model: CalcModel? = null
        private set
    private var mCalcSkin: CalcSkin? = null

    init {
        // disallow instantiation
        pauseCalc(PAUSE_KEY)
        mCalcThread.start()
    }

    fun initialize(context: Context?, cacheDir: String?) {
        mContext = context
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext!!)
        mCalcThread.queueRunnable(InitializeRunnable(cacheDir))
    }

    fun loadRomFile(file: File, callback: FileLoadedCallback) {
        if (mHasLoadedRom.get() && mCurrentRomFile == file.path) {
            handleRomLoaded(callback, 0)
            return
        }

        mHasLoadedRom.set(false)
        mCurrentRomFile = file.path

        mCalcThread.queueRunnable(LoadRomRunnable(callback))
    }

    fun loadFile(file: File, callback: FileLoadedCallback) {
        mCalcThread.queueRunnable(LoadFileRunnable(file, callback))
    }

    fun setScreenCallback(callback: CalcScreenUpdateCallback?) {
        mCalcThread.setScreenUpdateCallback(callback)
    }

    fun setCalcSkin(calcSkin: CalcSkin?) {
        mCalcSkin = calcSkin
    }

    fun pauseCalc(pauseKey: String?) {
        mCalcThread.setPaused(pauseKey!!, true)
    }

    fun unPauseCalc(pauseKey: String?) {
        mCalcThread.setPaused(pauseKey!!, false)
    }

    fun pressKey(group: Int, bit: Int) {
        synchronized(mKeyTimePressed) { mIsKeyPressPending[group][bit] = true }
        mCalcThread.queueRunnable {
            CalcInterface.PressKey(group, bit)
            synchronized(mKeyTimePressed) {
                mKeyTimePressed[group][bit] = CalcInterface.Tstates()
                mIsKeyPressPending[group][bit] = false
            }
        }
    }

    fun releaseKey(group: Int, bit: Int) {
        if (shouldDelayRelease(group, bit)) {
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    releaseKey(group, bit)
                }
            }
            mRepressExecutor.schedule(task, 40, TimeUnit.MILLISECONDS)
        } else {
            mCalcThread.queueRunnable { CalcInterface.ReleaseKey(group, bit) }
        }
    }


    private fun shouldDelayRelease(group: Int, bit: Int): Boolean {
        synchronized(mKeyTimePressed) {
            if (mIsKeyPressPending[group][bit]) {
                return true
            }

            val tstates = CalcInterface.Tstates()
            val timePressed = mKeyTimePressed[group][bit]
            val elapsed = tstates - timePressed

            return if (group == CalcInterface.ON_KEY_GROUP && bit == CalcInterface.ON_KEY_BIT) {
                elapsed < MIN_TSTATE_ON_KEY
            } else {
                elapsed < MIN_TSTATE_KEY
            }
        }
    }

    fun resetCalc() {
        mCalcThread.resetCalc()
    }

    fun saveCurrentRom() {
        if (mCurrentRomFile == null || mCurrentRomFile == "" && mHasLoadedRom.get()) {
            return
        }

        val command = SaveCurrentRomRunnable()
        mCalcThread.queueRunnable(command)
    }

    fun createRom(
        osFilePath: String?,
        bootPagePath: String?,
        createdFilePath: String?,
        calcModel: CalcModel,
        callback: FileLoadedCallback
    ) {
        mCalcThread.queueRunnable {
            val errorCode = CalcInterface.CreateRom(
                osFilePath,
                bootPagePath,
                createdFilePath,
                calcModel.modelInt
            )
            callback.onFileLoaded(errorCode)
        }
    }

    fun testLoadLib() {
        CalcInterface.GetModel()
    }

    private fun updateCurrentRomSetting() {
        mSharedPrefs!!.edit()
            .putString(PreferenceConstants.ROM_PATH.toString(), mCurrentRomFile)
            .putInt(PreferenceConstants.ROM_MODEL.toString(), CalcInterface.GetModel())
            .apply()
    }

    private fun handleRomLoaded(callback: FileLoadedCallback, errorCode: Int) {
        val model = model
        val wasSuccessful = errorCode == 0
        if (mCalcSkin != null && wasSuccessful && model != null) {
            mSkinLoader.loadSkinAndKeymap(model)
        }
        mHasLoadedRom.set(wasSuccessful)
        callback.onFileLoaded(errorCode)
    }

    private class InitializeRunnable(private val mBestCacheDir: String?) : Runnable {
        override fun run() {
            CalcInterface.Initialize(mBestCacheDir)
        }
    }

    private inner class SaveCurrentRomRunnable : Runnable {
        override fun run() {
            val tempDir = mContext!!.filesDir
            mCurrentRomFile = tempDir.absoluteFile.toString() + "/Wabbitemu.sav"
            val wasSuccessful = CalcInterface.SaveCalcState(mCurrentRomFile)
            if (wasSuccessful) {
                updateCurrentRomSetting()
            }

            Log.e("CalculatorManager", "Finished writing ROM")
        }
    }

    private inner class LoadRomRunnable(private val mCallback: FileLoadedCallback) : Runnable {
        override fun run() {
            CalcInterface.SetAutoTurnOn(
                mSharedPrefs!!.getBoolean(
                    PreferenceConstants.AUTO_TURN_ON.toString(),
                    true
                )
            )
            val errorCode = CalcInterface.LoadFile(mCurrentRomFile)
            val wasSuccess = errorCode == 0
            model = CalcModel.fromModel(CalcInterface.GetModel())
            if (wasSuccess) {
                unPauseCalc(PAUSE_KEY)
            }
            handleRomLoaded(mCallback, errorCode)
        }
    }

    private class LoadFileRunnable(
        private val file: File,
        private val callback: FileLoadedCallback
    ) : Runnable {
        override fun run() {
            val linkResult = CalcInterface.LoadFile(file.path)
            callback.onFileLoaded(linkResult)
        }
    }

    private object SingletonHolder {
        val SINGLETON = CalculatorManager()
    }

    companion object {
        private const val PAUSE_KEY = "pauseKey"

        @JvmStatic
        fun getInstance(): CalculatorManager {
            return SingletonHolder.SINGLETON
        }

        private const val MIN_TSTATE_KEY: Long = 600
        private const val MIN_TSTATE_ON_KEY: Long = 25000
    }
}
