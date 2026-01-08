package io.github.angelsl.wabbitemu.calc

import android.os.SystemClock
import java.util.Collections
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CalcThread : Thread() {
    private val mIsPaused = AtomicBoolean(false)
    private val mPauseList: MutableList<String>
    private val mRunnables = Collections.synchronizedList(ArrayList<Runnable>())

    private var mScreenUpdateCallback: CalcScreenUpdateCallback? = null
    private var mPreviousTimerMillis: Long = 0
    private var mDifference: Long = 0

    init {
        mPauseList = ArrayList()
    }

    override fun run() {
        while (true) {
            if (isInterrupted) {
                break
            }

            synchronized(mRunnables) {
                for (runnable in mRunnables) {
                    runnable.run()
                }
                mRunnables.clear()
            }

            if (mIsPaused.get()) {
                try {
                    sleep(100)
                } catch (e: InterruptedException) {
                    currentThread().interrupt()
                    break
                }
                continue
            }

            val newTimeMillis = SystemClock.elapsedRealtime()
            mDifference += ((newTimeMillis - mPreviousTimerMillis) and 0x3F) - TPF
            mPreviousTimerMillis = newTimeMillis

            if (mDifference > -TPF) {
                CalcInterface.RunCalcs()

                if (mScreenUpdateCallback != null) {
                    val screenBuffer = mScreenUpdateCallback!!.screenBuffer
                    if (screenBuffer != null) {
                        screenBuffer.rewind()
                        CalcInterface.GetLCD(screenBuffer)
                        screenBuffer.rewind()
                        mScreenUpdateCallback!!.onUpdateScreen()
                    }
                }
                while (mDifference >= TPF) {
                    CalcInterface.RunCalcs()
                    mDifference -= TPF
                }
            } else {
                mDifference += TPF
                // Log.d("Wabbitemu", "Frame skip");
            }

            // if (framesSkipped == MAX_FRAME_SKIP) {
            // Log.d("", "Frame skip: " + framesSkipped);
            // }
        }
    }

    fun setPaused(key: String, shouldBePaused: Boolean) {
        if (shouldBePaused) {
            if (!mPauseList.contains(key)) {
                mPauseList.add(key)
            }

            mIsPaused.set(true)
        } else {
            mPauseList.remove(key)
            if (mPauseList.size == 0) {
                mIsPaused.set(false)
            }
        }
    }

    fun resetCalc() {
        mRunnables.add { CalcInterface.ResetCalc() }
    }

    fun setScreenUpdateCallback(callback: CalcScreenUpdateCallback?) {
        mScreenUpdateCallback = callback
    }

    fun queueRunnable(runnable: Runnable) {
        mRunnables.add(runnable)
    }

    companion object {
        private const val FPS = 50L
        private val TPF = TimeUnit.SECONDS.toMillis(1) / FPS
    }
}
