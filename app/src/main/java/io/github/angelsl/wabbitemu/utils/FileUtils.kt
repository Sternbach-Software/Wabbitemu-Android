package io.github.angelsl.wabbitemu.utils

import android.os.AsyncTask
import android.os.FileObserver
import java.io.File
import java.io.FileFilter
import java.util.concurrent.CountDownLatch
import java.util.regex.Pattern

object FileUtils {

    private var mFiles: MutableSet<String> = HashSet()
    private var mSearchLatch: CountDownLatch? = null
    private val mObservers: MutableList<FileObserver>

    init {
        mObservers = ArrayList()
        startFileSearch()
    }

    fun invalidateFiles() {
        startFileSearch()
    }
    private fun startFileSearch() {
        val asyncTask: AsyncTask<Void?, Void?, Void?> = object : AsyncTask<Void?, Void?, Void?>() {
            override fun onPreExecute() {
                mSearchLatch = CountDownLatch(1)
            }

            override fun doInBackground(vararg params: Void?): Void? {
                val path: String
                if (StorageUtils.hasExternalStorage()) {
                    path = StorageUtils.getPrimaryStoragePath()
                    println("Primary storage path: $path")
                    mFiles = findValidFiles(path)
                    val extraStorage = System.getenv("SECONDARY_STORAGE")
                    val extraStorageMore = System.getenv("EMULATED_STORAGE_TARGET")
                    if (extraStorage != null && "" != extraStorage && extraStorageMore != null && "" != extraStorageMore) {
                        for (dir in extraStorage.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()) {
                            mFiles.addAll(findValidFiles(dir))
                        }
                    }
                } else {
                    mFiles = HashSet()
                }
                mSearchLatch!!.countDown()
                return null
            }

            override fun onPostExecute(arg: Void?) {}
        }
        asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun getValidFiles(extensionsRegex: String?): List<String> {
        try {
            mSearchLatch!!.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            return ArrayList()
        }
        val validFiles: MutableList<String> = ArrayList()
        val pattern = Pattern.compile(extensionsRegex, Pattern.CASE_INSENSITIVE)
        for (file in mFiles) {
            println("Checking file: $file")
            val matcher = pattern.matcher(file)
            if (matcher.find()) {
                println("File matches: $file")
                validFiles.add(file)
            } else {
                println("File does not match: $file")
            }
        }
        return validFiles
    }

    private fun findValidFiles(dir: String): MutableSet<String> {
        val validFiles: MutableSet<String> = HashSet()
        File(dir).walk().forEach {
            println("Checking file: ${it.path}")
            if (it.isFile) {
                val isValid = isValidFile(it.path)
                if (isValid) {
                    validFiles.add(it.absolutePath)
                }
            } else if (it.isDirectory) {
                val observer: FileObserver = SingleFileObserver(
                    it.path,
                    FileObserver.CREATE or FileObserver.DELETE
                )
                mObservers.add(observer)
                observer.startWatching()
            }
        }
        return validFiles
    }

    private fun isValidFile(file: String): Boolean {
        var i = file.lastIndexOf('.')
        if (i <= 0) {
            return false
        }
        if (file.length != i + 4) {
            return false
        }
        val ext1 = file[++i].lowercaseChar()
        val ext2 = file[++i].lowercaseChar()
        val ext3 = file[++i].lowercaseChar()
        // Regex for reference
        // ".+\\.(rom|sav|([87][xc3265][bcdgiklmnpqstuvwyz]))$";
        if (ext1 == 'r' && ext2 == 'o' && ext3 == 'm' || ext1 == 's' && ext2 == 'a' && ext3 == 'v') {
            return true
        }
        if (ext1 != '8' && ext1 != '7') {
            return false
        }
        when (ext2) {
            'x', 'c', '2', '6', '5' -> {}
            else -> return false
        }
        when (ext3) {
            'b', 'c', 'd', 'g', 'i', 'k', 'l', 'm', 'n', 'p', 'q', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' -> {}
            else -> return false
        }
        return true
    }

    private fun handleFileEvent(event: Int, path: String) {
        when (event) {
            FileObserver.CREATE -> {
                val file = File(path)
                if (file.isFile) {
                    mFiles.add(file.absolutePath)
                }
            }

            FileObserver.DELETE -> mFiles.remove(path)
        }
    }

    private class WabbitFileFilter : FileFilter {
        override fun accept(pathname: File): Boolean {
            return pathname.isDirectory || isValidFile(pathname.path)
        }
    }

    private class SingleFileObserver(private val mPath: String, mask: Int) :
        FileObserver(mPath, mask) {
        override fun onEvent(event: Int, path: String?) {
            val newPath = "$mPath/$path"
            handleFileEvent(event, newPath)
        }
    }
}
