package io.github.angelsl.wabbitemu.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

public abstract class ProgressTask extends AsyncTask<Void, Void, Boolean> {

	private final String mDescriptionString;
	private final Context mContext;
	private final boolean mIsCancelable;
	private final Handler mHandler = new Handler(Looper.getMainLooper());

	private Runnable mProgressRunnable;
	private ProgressDialog mProgress;

	public ProgressTask(final Context context, final String descriptionString, final boolean isCancelable) {
		mContext = context;
		mDescriptionString = descriptionString;
		mIsCancelable = isCancelable;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		mProgressRunnable = new Runnable() {

			@Override
			public void run() {
				mProgress = new ProgressDialog(mContext);
				mProgress.setTitle("Loading");
				mProgress.setMessage(mDescriptionString);
				mProgress.setCancelable(mIsCancelable);
				mProgress.show();
			}
		};
		mHandler.postDelayed(mProgressRunnable, 500);
	}

	@Override
	protected void onPostExecute(final Boolean arg) {
		super.onPostExecute(arg);

		mHandler.removeCallbacks(mProgressRunnable);

		if (mProgress != null && mProgress.isShowing()) {
			mProgress.dismiss();
		}
	}

	protected Context getContext() {
		return mContext;
	}
}
