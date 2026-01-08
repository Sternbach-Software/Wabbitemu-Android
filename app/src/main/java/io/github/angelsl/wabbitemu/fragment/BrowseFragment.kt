package io.github.angelsl.wabbitemu.fragment

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.utils.FileUtils
import io.github.angelsl.wabbitemu.utils.IntentConstants
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected
import io.github.angelsl.wabbitemu.utils.ViewUtils

class BrowseFragment : Fragment() {
    private var mBrowseCallback: OnBrowseItemSelected? = null
    private var mSearchTask: AsyncTask<Void, Void, ArrayAdapter<String>>? = null
    private lateinit var mListView: ListView
    private var mExtensionsRegex: String? = null

    fun setCallback(browseCallback: OnBrowseItemSelected?) {
        mBrowseCallback = browseCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.browse, container, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestReadPermissions()
        }

        if (arguments != null) {
            val arguments = arguments
            mExtensionsRegex = arguments!!.getString(IntentConstants.EXTENSION_EXTRA_REGEX)

            mListView = ViewUtils.findViewById(view, R.id.browseView, ListView::class.java)
            mListView.onItemClickListener =
                AdapterView.OnItemClickListener { _, _, position, _ ->
                    val filePath = mListView.getItemAtPosition(position) as String
                    mBrowseCallback?.onBrowseItemSelected(filePath)
                }

            startSearch(view, mExtensionsRegex)
            Log.d("BrowseFragment", "onCreateView: $mExtensionsRegex")
        }

        return view
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestReadPermissions() {
        if (activity!!.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
            val view = view
            if (view != null) {
                startSearch(view, mExtensionsRegex)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            FileUtils.invalidateFiles()
            val currentView = view
            if (currentView != null) {
                startSearch(currentView, mExtensionsRegex)
            }
        }
    }

    private fun startSearch(view: View, extensionsRegex: String?) {
        mSearchTask = object : AsyncTask<Void, Void, ArrayAdapter<String>>() {
            private var mContext: Context? = null
            private lateinit var mLoadingSpinner: View

            override fun onPreExecute() {
                mContext = activity
                mLoadingSpinner =
                    ViewUtils.findViewById(view, R.id.browseLoadingSpinner, View::class.java)
                mLoadingSpinner.visibility = View.VISIBLE
            }

            override fun doInBackground(vararg params: Void?): ArrayAdapter<String> {
                val files = FileUtils.getValidFiles(extensionsRegex!!)
                Log.d(
                    "BrowseFragment",
                    "doInBackground: " + extensionsRegex + " " + files.size
                )
                return ArrayAdapter(mContext!!, android.R.layout.simple_list_item_1, files)
            }

            override fun onPostExecute(adapter: ArrayAdapter<String>) {
                mLoadingSpinner.visibility = View.GONE
                Log.d(
                    "BrowseFragment",
                    "onPostExecute: " + extensionsRegex + " " + adapter.count
                )
                mListView.adapter = adapter
                mSearchTask = null
            }
        }

        if (extensionsRegex != null) {
            (mSearchTask as AsyncTask<Void, Void, ArrayAdapter<String>>).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mSearchTask != null) {
            mSearchTask!!.cancel(true)
        }
    }

    companion object {
        const val REQUEST_CODE = 20
    }
}
