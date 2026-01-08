package io.github.angelsl.wabbitemu.wizard.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.utils.ViewUtils

class OsDownloadPageView(context: Context?, attributeSet: AttributeSet?) :
    RelativeLayout(context, attributeSet) {
    val webView: WebView
    private val mLoadingSpinner: ProgressBar

    init {
        LayoutInflater.from(context).inflate(R.layout.os_download_page, this, true)

        webView = ViewUtils.findViewById(this, R.id.webDownloadView, WebView::class.java)
        mLoadingSpinner = ViewUtils.findViewById(this, R.id.loadingSpinner, ProgressBar::class.java)
    }

    fun showProgressBar(shouldShow: Boolean) {
        mLoadingSpinner.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }
}
