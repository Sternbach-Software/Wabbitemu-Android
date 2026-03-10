package io.github.angelsl.wabbitemu.calc;

import android.os.SystemClock;
import android.util.Log;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

public class CalcThread extends Thread {

    private static final long FPS = 50L;
    private static final long TPF = TimeUnit.SECONDS.toMillis(1) / FPS;

    private final AtomicBoolean mIsPaused = new AtomicBoolean(false);
    private final List<String> mPauseList;
    private final List<Runnable> mRunnables = Collections.synchronizedList(new ArrayList<Runnable>());

    private CalcScreenUpdateCallback mScreenUpdateCallback;
    private long mPreviousTimerMillis;
    private long mDifference;

    public CalcThread() {
        mPauseList = new ArrayList<>();
    }

    @Override
    public void run() {
        while (true) {
            if (isInterrupted()) {
                break;
            }

            synchronized (mRunnables) {
                for (Runnable runnable : mRunnables) {
                    runnable.run();
                }
                mRunnables.clear();
            }

            if (mIsPaused.get()) {
                try {
                    sleep(100);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue;
            }

            final long newTimeMillis = SystemClock.elapsedRealtime();
            mDifference += ((newTimeMillis - mPreviousTimerMillis) & 0x3F) - TPF;
            mPreviousTimerMillis = newTimeMillis;

            if (mDifference > -TPF) {
                CalcInterface.RunCalcs();

                if (mScreenUpdateCallback != null) {
                    final IntBuffer screenBuffer = mScreenUpdateCallback.getScreenBuffer();
                    if (screenBuffer != null) {
                        screenBuffer.rewind();
                        CalcInterface.GetLCD(screenBuffer);
                        screenBuffer.rewind();
                        mScreenUpdateCallback.onUpdateScreen();
                    }
                }
                while (mDifference >= TPF) {
                    CalcInterface.RunCalcs();
                    mDifference -= TPF;
                }
            } else {
                mDifference += TPF;
                // Log.d("Wabbitemu", "Frame skip");
            }

            // if (framesSkipped == MAX_FRAME_SKIP) {
            // Log.d("", "Frame skip: " + framesSkipped);
            // }
        }
    }

    public void setPaused(@Nonnull String key, boolean shouldBePaused) {
        if (shouldBePaused) {
            if (!mPauseList.contains(key)) {
                mPauseList.add(key);
            }

            mIsPaused.set(true);
        } else {
            mPauseList.remove(key);
            if (mPauseList.size() == 0) {
                mIsPaused.set(false);
            }
        }
    }

    public void resetCalc() {
        mRunnables.add(new Runnable() {
            @Override
            public void run() {
                CalcInterface.ResetCalc();
            }
        });
    }

    public void setScreenUpdateCallback(CalcScreenUpdateCallback callback) {
        mScreenUpdateCallback = callback;
    }

    public void queueRunnable(Runnable runnable) {
        mRunnables.add(runnable);
    }
}
