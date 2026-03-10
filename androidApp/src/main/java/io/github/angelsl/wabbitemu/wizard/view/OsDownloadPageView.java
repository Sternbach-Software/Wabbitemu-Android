package io.github.angelsl.wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

public class OsDownloadPageView extends RelativeLayout {
	private final WebView mWebView;
	private final ProgressBar mLoadingSpinner;

	public OsDownloadPageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		LayoutInflater.from(context).inflate(R.layout.os_download_page, this, true);

		mWebView = ViewUtils.findViewById(this, R.id.webDownloadView, WebView.class);
		mLoadingSpinner = ViewUtils.findViewById(this, R.id.loadingSpinner, ProgressBar.class);
	}

	public WebView getWebView() {
		return mWebView;
	}

	public void showProgressBar(boolean shouldShow) {
		mLoadingSpinner.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
	}
}
