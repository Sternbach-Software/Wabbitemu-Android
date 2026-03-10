package io.github.angelsl.wabbitemu.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import io.github.angelsl.wabbitemu.R;

public class ErrorUtils {

	public static void showErrorDialog(final Context context, final int errorMessage) {
		showErrorDialog(context, errorMessage, null);
	}

	public static void showErrorDialog(final Context context,
			final int errorMessage,
			final OnClickListener onClickListener)
	{
		final String error = context.getResources().getString(errorMessage);

		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		final AlertDialog dialog = builder.setTitle(R.string.errorTitle)
				.setMessage(error)
				.setPositiveButton(android.R.string.ok, new OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						dialog.dismiss();
						if (onClickListener != null) {
							onClickListener.onClick(dialog, which);
						}
					}
				})
				.create();
		dialog.show();
	}

}
