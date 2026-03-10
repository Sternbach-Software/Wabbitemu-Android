package io.github.angelsl.wabbitemu.fragment;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.utils.FileUtils;
import io.github.angelsl.wabbitemu.utils.IntentConstants;
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

import java.util.List;

public class BrowseFragment extends Fragment {

    public static final int REQUEST_CODE = 20;
    private OnBrowseItemSelected mBrowseCallback;

    private AsyncTask<Void, Void, ArrayAdapter<String>> mSearchTask;
    private ListView mListView;
    private String mExtensionsRegex;

    public void setCallback(@Nullable OnBrowseItemSelected browseCallback) {
        mBrowseCallback = browseCallback;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.browse, container, false);
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            requestReadPermissions();
        }

        if (getArguments() != null) {
            final Bundle arguments = getArguments();
            mExtensionsRegex = arguments.getString(IntentConstants.EXTENSION_EXTRA_REGEX);

            mListView = ViewUtils.findViewById(view, R.id.browseView, ListView.class);
            mListView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final String filePath = (String) mListView.getItemAtPosition(position);
                    if (mBrowseCallback != null) {
                        mBrowseCallback.onBrowseItemSelected(filePath);
                    }
                }
            });

            startSearch(view, mExtensionsRegex);
            Log.d("BrowseFragment", "onCreateView: " + mExtensionsRegex);
        }

        return view;
    }

    @TargetApi(VERSION_CODES.M)
    private void requestReadPermissions() {
        if (getActivity().checkSelfPermission(permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
            final View view = getView();
            if (view != null) {
                startSearch(view, mExtensionsRegex);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            FileUtils.INSTANCE.invalidateFiles();
            if (getView() != null) {
                startSearch(getView(), mExtensionsRegex);
            }
        }
    }

    private void startSearch(final View view, final String extensionsRegex) {
        mSearchTask = new AsyncTask<Void, Void, ArrayAdapter<String>>() {
            private Context mContext;
            private View mLoadingSpinner;

            @Override
            protected void onPreExecute() {
                mContext = getActivity();
                mLoadingSpinner = ViewUtils.findViewById(view, R.id.browseLoadingSpinner, View.class);
                mLoadingSpinner.setVisibility(View.VISIBLE);
            }

            @Override
            protected ArrayAdapter<String> doInBackground(final Void... params) {
                final List<String> files = FileUtils.INSTANCE.getValidFiles(extensionsRegex);
                Log.d("BrowseFragment", "doInBackground: " + extensionsRegex + " " + files.size());
                return new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, files);
            }

            @Override
            protected void onPostExecute(final ArrayAdapter<String> adapter) {
                mLoadingSpinner.setVisibility(View.GONE);
                Log.d("BrowseFragment", "onPostExecute: " + extensionsRegex + " " + adapter.getCount());
                mListView.setAdapter(adapter);
                mSearchTask = null;
            }
        };

        if (extensionsRegex != null) {
            mSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSearchTask != null) {
            mSearchTask.cancel(true);
        }
    }
}
