package io.github.angelsl.wabbitemu;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import io.github.angelsl.wabbitemu.calc.CalcScreenUpdateCallback;
import io.github.angelsl.wabbitemu.calc.MainThread;

import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WabbitLCD extends SurfaceView implements CalcScreenUpdateCallback {

	private final CalcKeyManager mCalcKeyManager;
	private final MainThread mMainThread;
	private final ExecutorService mExecutorService;

	public WabbitLCD(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		mCalcKeyManager = CalcKeyManager.getInstance();
		final SurfaceHolder holder = getHolder();
		mMainThread = new MainThread();
		holder.addCallback(mMainThread);
		setFocusable(true);
		mExecutorService = Executors.newSingleThreadExecutor();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent msg) {
		return mCalcKeyManager.doKeyDownKeyCode(keyCode);
	}

	@Override
	public boolean onKeyUp(final int keyCode, final KeyEvent msg) {
		return mCalcKeyManager.doKeyUpKeyCode(keyCode);
	}

	@Override
	public void onUpdateScreen() {
		mExecutorService.submit(mMainThread);
	}

	@Override
	public IntBuffer getScreenBuffer() {
		return mMainThread.getScreenBuffer();
	}

	public void updateSkin(final Rect lcdRect, final Rect lcdSkinRect) {
		if (mMainThread == null || lcdRect == null || lcdSkinRect == null) {
			return;
		}

		final FrameLayout.LayoutParams layoutParams = (LayoutParams) getLayoutParams();
		layoutParams.width = lcdSkinRect.width();
		layoutParams.height = lcdSkinRect.height();
		layoutParams.setMargins(lcdSkinRect.left, lcdSkinRect.top, 0, 0);
		setLayoutParams(layoutParams);
		getHolder().setFixedSize(lcdSkinRect.width(), lcdSkinRect.height());
		mMainThread.recreateScreen(lcdRect, lcdSkinRect);
	}

	@Nullable
	public Bitmap getScreen() {
		if (mMainThread == null) {
			return null;
		}

		return mMainThread.getScreen();
	}
}
