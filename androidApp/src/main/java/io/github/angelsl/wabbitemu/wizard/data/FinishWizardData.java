package io.github.angelsl.wabbitemu.wizard.data;

import io.github.angelsl.wabbitemu.calc.CalcModel;

import javax.annotation.Nullable;


public class FinishWizardData {

	private final CalcModel mCalcModel;
	private final String mStringValue;
	private final boolean mNeedsDownload;
	private String mOsDownloadUrl;

	public FinishWizardData(CalcModel calcModel) {
		mCalcModel = calcModel;
		mStringValue = null;
		mNeedsDownload = false;
	}

	public FinishWizardData(String filePath) {
		mCalcModel = CalcModel.NO_CALC;
		mStringValue = filePath;
		mNeedsDownload = false;
	}

	public FinishWizardData(CalcModel calcModel, String downloadCode, boolean needsDownload) {
		mCalcModel = calcModel;
		mOsDownloadUrl = null;
		mStringValue = downloadCode;
		mNeedsDownload = needsDownload;
	}

	public FinishWizardData(CalcModel calcModel, String osDownloadUrl, String downloadCode) {
		mCalcModel = calcModel;
		mOsDownloadUrl = osDownloadUrl;
		mStringValue = downloadCode;
		mNeedsDownload = true;
	}

	public boolean shouldDownloadOs() {
		return mNeedsDownload;
	}

	@Nullable
	public String getDownloadCode() {
		if (!mNeedsDownload) {
			throw new IllegalArgumentException("Cannot get download code for non download");
		}

		return mStringValue;
	}

	@Nullable
	public String getOsDownloadUrl() {
		if (!mNeedsDownload) {
			throw new IllegalArgumentException("Cannot get download url for non download");
		}

		return mOsDownloadUrl;
	}


	@Nullable
	public String getFilePath() {
		return mStringValue;
	}

	public CalcModel getCalcModel() {
		return mCalcModel;
	}
}
