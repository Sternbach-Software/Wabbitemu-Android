package io.github.angelsl.wabbitemu.utils;

import android.os.Environment;

public class StorageUtils {

	public static boolean hasExternalStorage() {
		final String state = Environment.getExternalStorageState();
		return state.contentEquals(Environment.MEDIA_MOUNTED) || state.contentEquals(Environment.MEDIA_MOUNTED_READ_ONLY);
	}

	public static String getPrimaryStoragePath() {
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}
}
