package io.github.angelsl.wabbitemu;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import io.github.angelsl.wabbitemu.utils.PreferenceConstants;

public class CalcSkin extends View {

	private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
	private final CalcKeyManager mCalcKeyManager;
	private final Vibrator mVibrator;
	private final Paint mPaint;
	private final List<Rect> mKeymapDrawRect = new ArrayList<>();
	private final Paint mKeymapPaint = new Paint();
	private final Rect mDrawRect = new Rect();

	private boolean mHasVibrationEnabled;

	public CalcSkin(Context context, AttributeSet attrs) {
		super(context, attrs);

		mCalcKeyManager = CalcKeyManager.getInstance();
		final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		sharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
		mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		mHasVibrationEnabled = sharedPrefs.getBoolean(PreferenceConstants.USE_VIBRATION.toString(), true);

		mPaint = new Paint();
		mPaint.setAntiAlias(false);
		mPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);

		mKeymapPaint.setAntiAlias(false);
		mKeymapPaint.setARGB(0x80, 0x00, 0x00, 0x00);
	}

	private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (key.equals(PreferenceConstants.USE_VIBRATION.toString())) {
				mHasVibrationEnabled = sharedPreferences.getBoolean(key, true);
			}
		}
	};

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		boolean handled = false;
		for (int i = 0; i < event.getPointerCount(); i++) {
			handled |= handleTouchEvent(event, i);
		}

		return handled;
	}

	@Override
	public void onDraw(final Canvas canvas) {
		final Bitmap renderedSkin = mSkinLoader.getRenderedSkin();
		canvas.drawColor(Color.DKGRAY);
		if (renderedSkin != null) {
			final Rect src = mSkinLoader.getSkinRect();
			mDrawRect.set(0, 0, canvas.getWidth(), canvas.getHeight());
			canvas.drawBitmap(renderedSkin, src, mDrawRect, mPaint);

		}
		
		for (Rect rect : mKeymapDrawRect) {
			canvas.drawRect(rect, mKeymapPaint);
		}
	}

	public void destroySkin() {
		mSkinLoader.destroySkin();
	}

	public Rect getLCDRect() {
		return mSkinLoader.getLcdRect();
	}

	public Rect getLCDSkinRect() {
		return mSkinLoader.getLcdSkinRect();
	}

	private boolean handleTouchEvent(final MotionEvent event, final int index) {
		final int x = (int) (event.getX(index) - mSkinLoader.getSkinX());
		final int y = (int) (event.getY(index) - mSkinLoader.getSkinY());
		final int id = event.getPointerId(index);

		if (mSkinLoader.isOutsideKeymap(x, y)) {
			return false;
		}

		final int actionMasked = event.getActionMasked();
		if (actionMasked == MotionEvent.ACTION_UP
				|| actionMasked == MotionEvent.ACTION_POINTER_UP
				|| actionMasked == MotionEvent.ACTION_CANCEL)
		{
			for (int i = 0; i < mKeymapDrawRect.size(); i++) {
				final Rect rect = mKeymapDrawRect.get(i);
				invalidate(rect);
			}
			mKeymapDrawRect.clear();

			mCalcKeyManager.doKeyUp(id);
			return true;
		}

		final int color = mSkinLoader.getKeymapPixel(x, y);
		if (Color.red(color) == 0xFF) {
			return false;
		}
		//TODO refactor
		final int group = Color.green(color) >> 4;
		final int bit = Color.blue(color) >> 4;
		Log.d("CalcSkin", "Group clicked: " + group + " and bit: " + bit);
		if ((group > 7) || (bit > 7)) {
			return false;
		}

		if (actionMasked == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
			if (mHasVibrationEnabled) {
				mVibrator.vibrate(50);
			}

			final Rect rect = mSkinLoader.getKeymapRect(x, y);
			if (rect == null) {
				return false;
			}

			mKeymapDrawRect.add(rect);

			invalidate(rect);

			mCalcKeyManager.doKeyDown(id, group, bit);
		}
		return true;
	}

	public interface CalcSkinChangedListener {
		public void onCalcSkinChanged(Rect lcdRect, Rect lcdSkinRect);
	}
}
