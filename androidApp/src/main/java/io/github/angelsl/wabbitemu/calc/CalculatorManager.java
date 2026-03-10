package io.github.angelsl.wabbitemu.calc;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import io.github.angelsl.wabbitemu.CalcSkin;
import io.github.angelsl.wabbitemu.SkinBitmapLoader;
import io.github.angelsl.wabbitemu.utils.PreferenceConstants;

import java.io.File;
import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CalculatorManager {
    private static final String PAUSE_KEY = "pauseKey";

    private static class SingletonHolder {
        private static final CalculatorManager SINGLETON = new CalculatorManager();
    }

    public static CalculatorManager getInstance() {
        return SingletonHolder.SINGLETON;
    }

    private static final long MIN_TSTATE_KEY = 600;
    private static final long MIN_TSTATE_ON_KEY = 25000;

    private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
    private final AtomicBoolean mHasLoadedRom = new AtomicBoolean();
    private final CalcThread mCalcThread = new CalcThread();
    private final long[][] mKeyTimePressed = new long[8][8];
    private final boolean[][] mIsKeyPressPending = new boolean[8][8];
    private final ScheduledExecutorService mRepressExecutor = new ScheduledThreadPoolExecutor(1);


    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private String mCurrentRomFile;
    private CalcModel mCurrentModel;
    private CalcSkin mCalcSkin;

    private CalculatorManager() {
        // disallow instantiation
        pauseCalc(PAUSE_KEY);
        mCalcThread.start();
    }

    public void initialize(Context context, final String cacheDir) {
        mContext = context;
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mCalcThread.queueRunnable(new InitializeRunnable(cacheDir));
    }

    public void loadRomFile(final File file, FileLoadedCallback callback) {
        if (mHasLoadedRom.get() && mCurrentRomFile.equals(file.getPath())) {
            handleRomLoaded(callback, 0);
            return;
        }

        mHasLoadedRom.set(false);
        mCurrentRomFile = file.getPath();

        mCalcThread.queueRunnable(new LoadRomRunnable(callback));
    }

    public void loadFile(final File file, final FileLoadedCallback callback) {
        mCalcThread.queueRunnable(new LoadFileRunnable(file, callback));
    }

    public void setScreenCallback(CalcScreenUpdateCallback callback) {
        mCalcThread.setScreenUpdateCallback(callback);
    }

    public void setCalcSkin(CalcSkin calcSkin) {
        mCalcSkin = calcSkin;
    }

    public void pauseCalc(String pauseKey) {
        mCalcThread.setPaused(pauseKey, true);
    }

    public void unPauseCalc(String pauseKey) {
        mCalcThread.setPaused(pauseKey, false);
    }

    public void pressKey(final int group, final int bit) {
        synchronized (mKeyTimePressed) {
            mIsKeyPressPending[group][bit] = true;
        }
        mCalcThread.queueRunnable(new Runnable() {
            @Override
            public void run() {
                CalcInterface.PressKey(group, bit);
                synchronized (mKeyTimePressed) {
                    mKeyTimePressed[group][bit] = CalcInterface.Tstates();
                    mIsKeyPressPending[group][bit] = false;
                }
            }
        });
    }

    public void releaseKey(final int group, final int bit) {
        if (shouldDelayRelease(group, bit)) {
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    releaseKey(group, bit);
                }
            };
            mRepressExecutor.schedule(task, 40, TimeUnit.MILLISECONDS);
        } else {
            mCalcThread.queueRunnable(new Runnable() {
                @Override
                public void run() {
                    CalcInterface.ReleaseKey(group, bit);
                }
            });
        }
    }


    private boolean shouldDelayRelease(final int group, final int bit) {
        synchronized (mKeyTimePressed) {
            if (mIsKeyPressPending[group][bit]) {
                return true;
            }

            final long tstates = CalcInterface.Tstates();
            final long timePressed = mKeyTimePressed[group][bit];
            final long elapsed = tstates - timePressed;

            if (group == CalcInterface.ON_KEY_GROUP && bit == CalcInterface.ON_KEY_BIT) {
                return elapsed < MIN_TSTATE_ON_KEY;
            } else {
                return elapsed < MIN_TSTATE_KEY;
            }
        }
    }

    public void resetCalc() {
        mCalcThread.resetCalc();
    }

    public void saveCurrentRom() {
        if (mCurrentRomFile == null || mCurrentRomFile.equals("") && mHasLoadedRom.get()) {
            return;
        }

        final SaveCurrentRomRunnable command = new SaveCurrentRomRunnable();
        mCalcThread.queueRunnable(command);
    }

    public void createRom(final String osFilePath,
            final String bootPagePath,
            final String createdFilePath,
            final CalcModel calcModel,
            final FileLoadedCallback callback)
    {
        mCalcThread.queueRunnable(new Runnable() {
            @Override
            public void run() {
                final int errorCode = CalcInterface.CreateRom(osFilePath, bootPagePath, createdFilePath, calcModel.getModelInt());
                callback.onFileLoaded(errorCode);
            }
        });
    }

    public CalcModel getModel() {
        return mCurrentModel;
    }

    public void testLoadLib() {
        CalcInterface.GetModel();
    }

    private void updateCurrentRomSetting() {
        mSharedPrefs.edit()
                .putString(PreferenceConstants.ROM_PATH.toString(), mCurrentRomFile)
                .putInt(PreferenceConstants.ROM_MODEL.toString(), CalcInterface.GetModel())
                .commit();
    }

    private void handleRomLoaded(FileLoadedCallback callback, int errorCode) {
        final CalcModel model = getModel();
        final boolean wasSuccessful = errorCode == 0;
        if (mCalcSkin != null && wasSuccessful) {
            mSkinLoader.loadSkinAndKeymap(model);
        }
        mHasLoadedRom.set(wasSuccessful);
        callback.onFileLoaded(errorCode);
    }

    private static class InitializeRunnable implements Runnable {
        private final String mBestCacheDir;

        public InitializeRunnable(String bestCacheDir) {
            mBestCacheDir = bestCacheDir;
        }

        @Override
        public void run() {
            CalcInterface.Initialize(mBestCacheDir);
        }
    }

    private class SaveCurrentRomRunnable implements Runnable {
        @Override
        public void run() {
            final File tempDir = mContext.getFilesDir();
            mCurrentRomFile = tempDir.getAbsoluteFile() + "/Wabbitemu.sav";
            final boolean wasSuccessful = CalcInterface.SaveCalcState(mCurrentRomFile);
            if (wasSuccessful) {
                updateCurrentRomSetting();
            }

            Log.e("CalculatorManager", "Finished writing ROM");
        }
    }

    private class LoadRomRunnable implements Runnable {

        private final FileLoadedCallback mCallback;

        public LoadRomRunnable(FileLoadedCallback callback) {
            mCallback = callback;
        }

        @Override
        public void run() {
            CalcInterface.SetAutoTurnOn(mSharedPrefs.getBoolean(PreferenceConstants.AUTO_TURN_ON.toString(), true));
            final int errorCode = CalcInterface.LoadFile(mCurrentRomFile);
            final boolean wasSuccess = errorCode == 0;
            mCurrentModel = CalcModel.fromModel(CalcInterface.GetModel());
            if (wasSuccess) {
                unPauseCalc(PAUSE_KEY);
            }
            handleRomLoaded(mCallback, errorCode);
        }
    }

    private static class LoadFileRunnable implements Runnable {
        private final File file;
        private final FileLoadedCallback callback;

        private LoadFileRunnable(File file, FileLoadedCallback callback) {
            this.file = file;
            this.callback = callback;
        }

        @Override
        public void run() {
            final int linkResult = CalcInterface.LoadFile(file.getPath());
            callback.onFileLoaded(linkResult);
        }
    }
}
