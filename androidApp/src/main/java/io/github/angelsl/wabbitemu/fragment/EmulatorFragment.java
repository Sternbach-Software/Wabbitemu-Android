package io.github.angelsl.wabbitemu.fragment;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import io.github.angelsl.wabbitemu.CalcSkin;
import io.github.angelsl.wabbitemu.CalcSkin.CalcSkinChangedListener;
import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.SkinBitmapLoader;
import io.github.angelsl.wabbitemu.WabbitLCD;
import io.github.angelsl.wabbitemu.calc.CalculatorManager;
import io.github.angelsl.wabbitemu.calc.FileLoadedCallback;
import io.github.angelsl.wabbitemu.utils.PreferenceConstants;
import io.github.angelsl.wabbitemu.utils.ProgressTask;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

public class EmulatorFragment extends Fragment {
	private static final String ACTIVITY_PAUSE_KEY = "EmulatorFragment";
	public static final int REQUEST_CODE = 21;

	private final Handler mHandler = new Handler(Looper.getMainLooper());
	private final CalculatorManager mCalculatorManager = CalculatorManager.getInstance();
	private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
	private final SkinUpdateListener mSkinUpdateListener = new SkinUpdateListener();
	private final ImmersiveModeListener mImmersiveModeListener = new ImmersiveModeListener();

	private Context mContext;
	private SharedPreferences mSharedPrefs;
	private CalcSkin mCalcSkin;
	private ProgressTask mSendFileTask;

	private WabbitLCD mSurfaceView;
	private boolean mIsInitialized;
	private File mFileToHandle;
	private Runnable mRunnableToHandle;

	public void handleFile(final File f, final Runnable runnable) {
		if (!mIsInitialized) {
			mFileToHandle = f;
			mRunnableToHandle = runnable;
			return;
		}

		final String name = f.getName();
		final boolean isRom = name.endsWith(".rom") || name.endsWith(".sav");
		final int stringId = isRom ? R.string.sendingRom : R.string.sendingFile;
		final String description = mContext.getResources().getString(stringId);

		mSendFileTask = new LoadFileAsyncTask(mContext, description, false, runnable, f, isRom);
		mSendFileTask.execute();
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.emulator, container);
		mSurfaceView = ViewUtils.findViewById(view, R.id.textureView, WabbitLCD.class);
		mCalcSkin = ViewUtils.findViewById(view, R.id.skinView, CalcSkin.class);
		mCalculatorManager.setScreenCallback(mSurfaceView);
		mCalculatorManager.setCalcSkin(mCalcSkin);

		mSkinLoader.registerSkinChangedListener(mSkinUpdateListener);

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();

		mSkinLoader.unregisterSkinChangedListener(mSkinUpdateListener);
		mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mImmersiveModeListener);
	}

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mContext = getActivity();
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mSharedPrefs.registerOnSharedPreferenceChangeListener(mImmersiveModeListener);
		if (VERSION.SDK_INT >= VERSION_CODES.M) {
			requestWritePermissions();
		}
	}

	@TargetApi(VERSION_CODES.M)
	private void requestWritePermissions() {
		if (getActivity().checkSelfPermission(permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE);
		}
	}

	@Override
	public void onResume() {
		mCalculatorManager.setCalcSkin(mCalcSkin);
		mCalculatorManager.unPauseCalc(ACTIVITY_PAUSE_KEY);
		mSurfaceView.updateSkin(mSkinLoader.getLcdRect(), mSkinLoader.getLcdSkinRect());
		mCalcSkin.invalidate();

		mIsInitialized = true;
		if (mFileToHandle != null) {
			handleFile(mFileToHandle, mRunnableToHandle);
			mFileToHandle = null;
			mRunnableToHandle = null;
		}
		super.onResume();

		updateSettings();
	}

	@Override
	public void onPause() {
		mCalculatorManager.pauseCalc(ACTIVITY_PAUSE_KEY);
		super.onPause();

		mCalculatorManager.saveCurrentRom();
		mIsInitialized = false;

		if (mSendFileTask != null) {
			mSendFileTask.cancel(false);
		}
	}

	@Nullable
	public Bitmap getScreenshot() {
		return mSurfaceView.getScreen();
	}

	public void resetCalc() {
		mCalculatorManager.resetCalc();
	}

	private void updateSettings() {
		if (mSharedPrefs.getBoolean(PreferenceConstants.STAY_AWAKE.toString(), false)) {
			getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	private class ImmersiveModeListener implements OnSharedPreferenceChangeListener {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if (PreferenceConstants.IMMERSIVE_MODE.toString().equals(key)) {
				mSkinLoader.destroySkin();
				mSkinLoader.loadSkinAndKeymap(CalculatorManager.getInstance().getModel());
			}
		}
	}

	private class SkinUpdateListener implements CalcSkinChangedListener {
		@Override
		public void onCalcSkinChanged(final Rect lcdRect, final Rect lcdSkinRect) {
			mHandler.post(new Runnable() {

				@Override
				public void run() {
					mSurfaceView.updateSkin(lcdRect, lcdSkinRect);
					Log.d("View", "Request update");
					mCalcSkin.invalidate();
				}

			});
		}
	}

	private class LoadFileAsyncTask extends ProgressTask {
		private final Runnable mRunnable;
		private final File mFile;
		private final boolean mIsRom;

		private LoadFileAsyncTask(final Context context,
				final String descriptionString,
				final boolean isCancelable,
				final Runnable runnable,
				final File f,
				final boolean isRom)
		{
			super(context, descriptionString, isCancelable);
			mRunnable = runnable;
			mFile = f;
			mIsRom = isRom;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			if (mIsRom) {
				mCalcSkin.destroySkin();
				mCalcSkin.invalidate();
			}
		}

		private volatile Boolean mSuccess;

		@Override
		protected Boolean doInBackground(final Void... params) {
			final CountDownLatch latch = new CountDownLatch(1);
			mSuccess = Boolean.FALSE;
			final FileLoadedCallback callback = new FileLoadedCallback() {

				@Override
				public void onFileLoaded(int errorCode) {
					mSuccess = errorCode == 0;
					latch.countDown();
				}

			};

			if (mIsRom) {
				mCalculatorManager.loadRomFile(mFile, callback);
			} else {
				mCalculatorManager.loadFile(mFile, callback);
			}

			try {
				latch.await();
			} catch (InterruptedException e) {
				return Boolean.FALSE;
			}

			return mSuccess;
		}

		@Override
		protected void onPostExecute(final Boolean wasSuccessful) {
			if (!wasSuccessful && mRunnable != null) {
				mRunnable.run();
			}

			super.onPostExecute(wasSuccessful);
		}
	}
}
