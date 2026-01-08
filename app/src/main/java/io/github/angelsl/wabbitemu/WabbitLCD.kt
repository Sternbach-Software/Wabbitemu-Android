package io.github.angelsl.wabbitemu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.SurfaceView
import android.widget.FrameLayout
import io.github.angelsl.wabbitemu.calc.CalcScreenUpdateCallback
import io.github.angelsl.wabbitemu.calc.MainThread
import java.nio.IntBuffer
import java.util.concurrent.Executors

class WabbitLCD(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs),
    CalcScreenUpdateCallback {
    private val mCalcKeyManager: CalcKeyManager
    private val mMainThread: MainThread?
    private val mExecutorService = Executors.newSingleThreadExecutor()

    init {
        mCalcKeyManager = CalcKeyManager.getInstance()
        val holder = holder
        mMainThread = MainThread()
        holder.addCallback(mMainThread)
        isFocusable = true
    }

    override fun onKeyDown(keyCode: Int, msg: KeyEvent): Boolean {
        return mCalcKeyManager.doKeyDownKeyCode(keyCode)
    }

    override fun onKeyUp(keyCode: Int, msg: KeyEvent): Boolean {
        return mCalcKeyManager.doKeyUpKeyCode(keyCode)
    }

    override fun onUpdateScreen() {
        mMainThread?.let { mExecutorService.submit(it) }
    }

    override val screenBuffer: IntBuffer?
        get() = mMainThread!!.screenBuffer

    fun updateSkin(lcdRect: Rect?, lcdSkinRect: Rect?) {
        if (mMainThread == null || lcdRect == null || lcdSkinRect == null) {
            return
        }

        val layoutParams = layoutParams as FrameLayout.LayoutParams
        layoutParams.width = lcdSkinRect.width()
        layoutParams.height = lcdSkinRect.height()
        layoutParams.setMargins(lcdSkinRect.left, lcdSkinRect.top, 0, 0)
        setLayoutParams(layoutParams)
        holder.setFixedSize(lcdSkinRect.width(), lcdSkinRect.height())
        mMainThread.recreateScreen(lcdRect, lcdSkinRect)
    }

    val screen: Bitmap?
        get() {
            if (mMainThread == null) {
                return null
            }

            return mMainThread.screen
        }
}
