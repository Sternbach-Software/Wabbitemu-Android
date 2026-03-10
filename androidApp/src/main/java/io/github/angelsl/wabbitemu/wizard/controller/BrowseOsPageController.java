package io.github.angelsl.wabbitemu.wizard.controller;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.FragmentManager;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.fragment.BrowseFragment;
import io.github.angelsl.wabbitemu.utils.IntentConstants;
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected;
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController;
import io.github.angelsl.wabbitemu.wizard.WizardPageController;
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData;
import io.github.angelsl.wabbitemu.wizard.view.BrowseOsPageView;

public class BrowseOsPageController implements WizardPageController {

	private final Context mContext;
	private final FragmentManager mFragmentManager;
	private final OnBrowseItemSelected mBrowseCallback = new OnBrowseItemSelected() {

		@Override
		public void onBrowseItemSelected(String fileName) {
			if (mNavController == null) {
				return;
			}

			mOsPath = fileName;
			mNavController.finishWizard();
		}
	};

	private CalcModel mCalcModel;
	private String mOsPath;
	private WizardNavigationController mNavController;

	public BrowseOsPageController(@NonNull BrowseOsPageView view,
			@NonNull FragmentManager fragmentManager)
	{
		mContext = view.getContext();
		mFragmentManager = fragmentManager;
	}

	@Override
	public void configureButtons(@NonNull final WizardNavigationController navController) {
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
		throw new IllegalStateException("No next page");
	}

	@Override
	public int getPreviousPage() {
		return R.id.os_page;
	}

	@Override
	public void onHiding() {
		// no-op
	}

	@Override
	public void onShowing(Object previousData) {
		mCalcModel = (CalcModel) previousData;
		launchBrowseOs();
	}

	@Override
	public int getTitleId() {
		return R.string.osSelectionTitle;
	}

	@Override
	public Object getControllerData() {
		return new FinishWizardData(mCalcModel, mOsPath, false);
	}

	private void launchBrowseOs() {
		final String extensions;
		switch (mCalcModel) {
		case TI_73:
			extensions = "\\.73u";
			break;
		case TI_84PCSE:
			extensions = "\\.8cu";
			break;
		case TI_83P:
		case TI_83PSE:
		case TI_84P:
		case TI_84PSE:
			extensions = "\\.8xu";
			break;
		default:
			throw new IllegalStateException("Invalid calc model");
		}

		final Bundle setupBundle = new Bundle();
		setupBundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, extensions);
		setupBundle.putString(IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING,
				mContext.getResources().getString(R.string.browseOSDescription));

		final BrowseFragment fragInfo = new BrowseFragment();
		fragInfo.setCallback(mBrowseCallback);
		fragInfo.setArguments(setupBundle);

		final FragmentTransaction transaction = mFragmentManager.beginTransaction();
		transaction.replace(R.id.browse_os_page, fragInfo);
		transaction.commit();
	}
}
