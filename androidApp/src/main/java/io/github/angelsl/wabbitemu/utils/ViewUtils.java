package io.github.angelsl.wabbitemu.utils;

import android.app.Activity;
import android.view.View;

public class ViewUtils {

	public static <T extends View> T findViewById(Activity activity, int resId, Class<T> clazz) {
		final View foundView = activity.findViewById(resId);
		if (foundView == null) {
			throw new IllegalStateException("Unable to find view " + resId);
		}

		if (!clazz.isInstance(foundView)) {
			throw new IllegalStateException("Cannot cast view to " + clazz.getSimpleName());
		}

		return clazz.cast(foundView);
	}

	public static <T extends View> T findViewById(View view, int resId, Class<T> clazz) {
		final View foundView = view.findViewById(resId);
		if (foundView == null) {
			throw new IllegalStateException("Unable to find view " + resId);
		}

		if (!clazz.isInstance(foundView)) {
			throw new IllegalStateException("Cannot cast view to " + clazz.getSimpleName());
		}
		
		return clazz.cast(foundView);
	}
}
