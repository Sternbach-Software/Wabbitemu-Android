package io.github.angelsl.wabbitemu.utils

import android.os.Environment

object StorageUtils {
    @JvmStatic
    fun hasExternalStorage(): Boolean {
        val state = Environment.getExternalStorageState()
        return state.contentEquals(Environment.MEDIA_MOUNTED) || state.contentEquals(Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    @JvmStatic
    val primaryStoragePath: String
        get() = Environment.getExternalStorageDirectory().absolutePath
}
