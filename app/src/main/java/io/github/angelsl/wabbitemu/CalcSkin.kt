package io.github.angelsl.wabbitemu

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.preference.PreferenceManager
import io.github.angelsl.wabbitemu.utils.PreferenceConstants
import java.util.ArrayList

class CalcSkin(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val mSkinLoader: SkinBitmapLoader = SkinBitmapLoader.getInstance()
    private val mCalcKeyManager: CalcKeyManager
    private val mVibrator: Vibrator
    private val mPaint: Paint
    private val mKeymapDrawRect: MutableList<Rect> = ArrayList()
    private val mKeymapPaint = Paint()
    private val mDrawRect = Rect()
    private var mHasVibrationEnabled: Boolean

    private val mPrefListener =
        OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == PreferenceConstants.USE_VIBRATION.toString()) {
                mHasVibrationEnabled = sharedPreferences.getBoolean(key, true)
            }
        }

    init {
        mCalcKeyManager = CalcKeyManager.getInstance()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener)
        mVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mHasVibrationEnabled =
            sharedPrefs.getBoolean(PreferenceConstants.USE_VIBRATION.toString(), true)

        mPaint = Paint()
        mPaint.isAntiAlias = false
        mPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF)

        mKeymapPaint.isAntiAlias = false
        mKeymapPaint.setARGB(0x80, 0x00, 0x00, 0x00)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        for (i in 0 until event.pointerCount) {
            handled = handled or handleTouchEvent(event, i)
        }

        return handled
    }

    public override fun onDraw(canvas: Canvas) {
        val renderedSkin = mSkinLoader.renderedSkin
        canvas.drawColor(Color.DKGRAY)
        if (renderedSkin != null) {
            val src = mSkinLoader.skinRect
            mDrawRect.set(0, 0, canvas.width, canvas.height)
            canvas.drawBitmap(renderedSkin, src, mDrawRect, mPaint)
        }

        for (rect in mKeymapDrawRect) {
            canvas.drawRect(rect, mKeymapPaint)
        }
    }

    fun destroySkin() {
        mSkinLoader.destroySkin()
    }

    val lCDRect: Rect
        get() = mSkinLoader.lcdRect

    val lCDSkinRect: Rect
        get() = mSkinLoader.lcdSkinRect

    private fun handleTouchEvent(event: MotionEvent, index: Int): Boolean {
        val x = (event.getX(index) - mSkinLoader.skinX).toInt()
        val y = (event.getY(index) - mSkinLoader.skinY).toInt()
        val id = event.getPointerId(index)

        if (mSkinLoader.isOutsideKeymap(x, y)) {
            return false
        }

        val actionMasked = event.actionMasked
        if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_POINTER_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
            for (i in mKeymapDrawRect.indices) {
                val rect = mKeymapDrawRect[i]
                invalidate(rect)
            }
            mKeymapDrawRect.clear()

            mCalcKeyManager.doKeyUp(id)
            return true
        }

        val color = mSkinLoader.getKeymapPixel(x, y)
        if (Color.red(color) == 0xFF) {
            return false
        }
        //TODO refactor
        val group = Color.green(color) shr 4
        val bit = Color.blue(color) shr 4
        Log.d("CalcSkin", "Group clicked: $group and bit: $bit")
        if (group > 7 || bit > 7) {
            return false
        }

        if (actionMasked == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            if (mHasVibrationEnabled) {
                mVibrator.vibrate(50)
            }

            val rect = mSkinLoader.getKeymapRect(x, y) ?: return false

            mKeymapDrawRect.add(rect)

            invalidate(rect)

            mCalcKeyManager.doKeyDown(id, group, bit)
        }
        return true
    }

    interface CalcSkinChangedListener {
        fun onCalcSkinChanged(lcdRect: Rect, lcdSkinRect: Rect)
    }
}
