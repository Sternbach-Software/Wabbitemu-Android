package io.github.angelsl.wabbitemu.calc;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.Nullable;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class MainThread implements SurfaceHolder.Callback, Runnable {

    private final Paint mPaint;
    private final Object mScreenLock = new Object();
    private IntBuffer mCurrentScreenBuffer;
    private volatile Bitmap mScreenBitmap;
    private volatile boolean mHasCreatedLcd;

    private Rect mLcdRect;
    private Rect mScreenRect;
    private volatile SurfaceHolder mSurfaceHolder;

    public MainThread() {
        mPaint = new Paint();
        mPaint.setAntiAlias(false);
        mPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
    }

    public void recreateScreen(final Rect lcdRect, final Rect screenRect) {
        mLcdRect = lcdRect;
        mScreenRect = new Rect(screenRect);
        mScreenRect.offset(-mScreenRect.left, -mScreenRect.top);

        mScreenBitmap = Bitmap.createBitmap(mLcdRect.width(), mLcdRect.height(), Bitmap.Config.ARGB_8888);
        mCurrentScreenBuffer = ByteBuffer.allocateDirect(mLcdRect.width() * mLcdRect.height() * 4).asIntBuffer();
        mHasCreatedLcd = true;
    }

    public IntBuffer getScreenBuffer() {
        return mCurrentScreenBuffer;
    }

    @Nullable
    public Bitmap getScreen() {
        synchronized (mScreenLock) {
            return mScreenBitmap;
        }
    }

    @Override
    public void run() {
        if (mSurfaceHolder == null || !mHasCreatedLcd) {
            return;
        }

        synchronized (mScreenLock) {
            Canvas canvas = null;
            try {
                canvas = mSurfaceHolder.lockCanvas();
                if (canvas == null) {
                    return;
                }

                mScreenBitmap.copyPixelsFromBuffer(mCurrentScreenBuffer);
                if (getScreen() != null) {
                    canvas.drawBitmap(mScreenBitmap, mLcdRect, mScreenRect, mPaint);
                }
            } finally {
                if (canvas != null) {
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // no-op
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (mScreenLock) {
            mSurfaceHolder = null;
        }
    }
}
