package io.github.angelsl.wabbitemu.calc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import java.nio.ByteBuffer
import java.nio.IntBuffer

class MainThread : SurfaceHolder.Callback, Runnable {
    private val mPaint: Paint
    private val mScreenLock = Any()
    private var mCurrentScreenBuffer: IntBuffer? = null

    @Volatile
    private var mScreenBitmap: Bitmap? = null

    @Volatile
    private var mHasCreatedLcd = false
    private var mLcdRect: Rect? = null
    private var mScreenRect: Rect? = null

    @Volatile
    private var mSurfaceHolder: SurfaceHolder? = null

    init {
        mPaint = Paint()
        mPaint.isAntiAlias = false
        mPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF)
    }

    fun recreateScreen(lcdRect: Rect, screenRect: Rect) {
        mLcdRect = lcdRect
        mScreenRect = Rect(screenRect)
        mScreenRect!!.offset(-mScreenRect!!.left, -mScreenRect!!.top)

        mScreenBitmap =
            Bitmap.createBitmap(mLcdRect!!.width(), mLcdRect!!.height(), Bitmap.Config.ARGB_8888)
        mCurrentScreenBuffer =
            ByteBuffer.allocateDirect(mLcdRect!!.width() * mLcdRect!!.height() * 4).asIntBuffer()
        mHasCreatedLcd = true
    }

    val screenBuffer: IntBuffer?
        get() = mCurrentScreenBuffer

    val screen: Bitmap?
        get() {
            synchronized(mScreenLock) {
                return mScreenBitmap
            }
        }

    override fun run() {
        if (mSurfaceHolder == null || !mHasCreatedLcd) {
            return
        }

        synchronized(mScreenLock) {
            var canvas: Canvas? = null
            try {
                canvas = mSurfaceHolder!!.lockCanvas()
                if (canvas == null) {
                    return
                }

                mScreenBitmap!!.copyPixelsFromBuffer(mCurrentScreenBuffer!!)
                if (screen != null) {
                    canvas.drawBitmap(mScreenBitmap!!, mLcdRect!!, mScreenRect!!, mPaint)
                }
            } finally {
                if (canvas != null) {
                    mSurfaceHolder!!.unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mSurfaceHolder = holder
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // no-op
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        synchronized(mScreenLock) { mSurfaceHolder = null }
    }
}
