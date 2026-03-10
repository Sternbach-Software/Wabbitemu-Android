package io.github.angelsl.wabbitemu.wizard.controller;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController;
import io.github.angelsl.wabbitemu.wizard.WizardPageController;
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData;
import io.github.angelsl.wabbitemu.wizard.data.OSDownloadData;
import io.github.angelsl.wabbitemu.wizard.view.OsDownloadPageView;

public class OsDownloadPageController implements WizardPageController {
    public static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.97+Safari/537.36";

    private final OsDownloadPageView mView;
    private final WebView mWebView;

    private WizardNavigationController mNavController;
    private CalcModel mCalcModel;
    private String mDownloadCode;
    private String mOsUrl;

    public OsDownloadPageController(@NonNull OsDownloadPageView osDownloadPageView) {
        mView = osDownloadPageView;
        mWebView = mView.getWebView();
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setUserAgentString(USER_AGENT);

        mWebView.setWebViewClient(new OsDownloadWebViewClient());
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "Android");
    }

    @Override
    public void configureButtons(@NonNull WizardNavigationController navController) {
        mNavController = navController;
        navController.hideNextButton();
    }

    @Override
    public boolean hasPreviousPage() {
        return true;
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public boolean isFinalPage() {
        return true;
    }

    @Override
    public int getNextPage() {
        throw new UnsupportedOperationException("No next page");
    }

    @Override
    public int getPreviousPage() {
        return R.id.model_page;
    }

    @Override
    public void onHiding() {
        mWebView.setVisibility(View.GONE);
        mDownloadCode = null;
    }

    @Override
    public void onShowing(Object previousData) {
        final OSDownloadData osDownloadData = (OSDownloadData) previousData;
        mCalcModel = osDownloadData.mCalcModel;
        mOsUrl = osDownloadData.mOsUrl;
        mWebView.setVisibility(View.GONE);
        mDownloadCode = null;

        mView.showProgressBar(true);
        switch (mCalcModel) {
            case TI_73:
                mWebView.loadUrl("https://education.ti.com/en/us/software/details/en/956CE30854A74767893104FCDF195B76/73ti73exploreroperatingsystem");
                break;
            case TI_83P:
            case TI_83PSE:
                mWebView.loadUrl("https://education.ti.com/en/us/software/details/en/C95956E744FB4C0A899F5A63EBEA60DD/83ti83plusoperatingsystemsoftware");
                break;
            case TI_84P:
            case TI_84PSE:
                mWebView.loadUrl("https://education.ti.com/en/us/software/details/en/B7DADA7FD4AA40CE9D7911B004B8C460/ti84plusoperatingsystem");
                break;
            case TI_84PCSE:
                mWebView.loadUrl("https://education.ti.com/en/asia/software/details/en/812E5FCF48C6456CB156A03DE5D07016/singaporeapprovedosapps");
                break;
            default:
                throw new IllegalStateException("Invalid calculator type");
        }
    }

    @Override
    public int getTitleId() {
        return R.string.osDownloadTitle;
    }

    @Override
    public Object getControllerData() {
        return new FinishWizardData(mCalcModel, mOsUrl, mDownloadCode);
    }

    private class OsDownloadWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            final String newStyles = "#layout-default { 	width: 100%; }  #header-site { 	display: none; }  .sublayout-etdownloadbundle { 	min-height: auto; }   .sublayout-etdownloadbundledetails { 	width: auto; }  .sublayout-etdownloadsactivitiesheader { 	display: none; }  .etdownloadbundleheader { 	padding-top: 10px; 	width: auto; }  .etheroimage { 	display: none; }  .column-pdf, .column-size, .column-version, .column-appsspaces { 	display: none; }  .etguidebooks, .etrelatedsoftware { 	display: none; }  ui-dialog-buttonset > button { 	margin: 20px; 	display: block; }  eula-captcha > div { 	width: 100% !important; }  .etmaincontent {  display: none; }  .back-to-results {  display: none; }  .feature-summary {  display: none; }  #footer-site {  display: none; }  .ui-dialog-buttonset {  display: block !important;  margin-left: auto;  margin-right: auto;  width: 300px; }  .dialog-key-eula.ui-dialog .ui-dialog-buttonpane .ui-dialog-buttonset .ui-button {  margin: 20px; }  .dialog-eula {  max-height: 200px !important; } .column-downloaditem.protected-download { opacity: 1 } .column-downloaditem { opacity: 0 }";
            view.loadUrl("javascript:$(document).ajaxComplete(function(e, xhr, settings) { " +
                    "Android.onFoundCode(xhr.responseText); });" +
                    "Dialogs.Init('Eula', function(dialogData) { dialogData.params.width = 400 } );" +
                    " Dialogs['Eula'].Dialog.dialog(Dialogs['Eula'].params); " +
                    "$(\"<style type='text/css'>" +
                    newStyles +
                    "</style>\").appendTo('head')");
            view.setVisibility(View.VISIBLE);
            mView.showProgressBar(false);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.e("Wabbitemu", description + " " + failingUrl);
            mView.showProgressBar(false);
            final AlertDialog dialog = new AlertDialog.Builder(view.getContext())
                    .setMessage(R.string.errorWebPageDownloadError)
                    .setTitle(R.string.errorTitle)
                    .create();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    mNavController.movePreviousPage();
                }
            });
            dialog.show();
        }
    }

    private class JavaScriptInterface {
        @JavascriptInterface
        public void onFoundCode(final String message) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {

                @Override
                public void run() {
                    if (mDownloadCode != null) {
                        return;
                    }

                    mDownloadCode = message.replace("\"", "");
                    mNavController.finishWizard();
                }
            });
        }
    }
}
