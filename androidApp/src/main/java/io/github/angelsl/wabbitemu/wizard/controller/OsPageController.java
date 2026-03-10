package io.github.angelsl.wabbitemu.wizard.controller;

import androidx.annotation.NonNull;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.utils.SpinnerDropDownAdapter;
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController;
import io.github.angelsl.wabbitemu.wizard.WizardPageController;
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData;
import io.github.angelsl.wabbitemu.wizard.data.OSDownloadData;
import io.github.angelsl.wabbitemu.wizard.view.OsPageView;

import java.util.ArrayList;
import java.util.List;

public class OsPageController implements WizardPageController {

	private final OsPageView mView;

	private CalcModel mCalcModel;

	public OsPageController(@NonNull OsPageView osPageView) {
		mView = osPageView;
	}

	@Override
	public void configureButtons(@NonNull WizardNavigationController navController) {
		if (isFinalPage()) {
			navController.setFinishButton();
		} else {
			navController.setNextButton();
		}
	}

	@Override
	public boolean hasPreviousPage() {
		return true;
	}

	@Override
	public boolean hasNextPage() {
		return !isFinalPage();
	}

	@Override
	public boolean isFinalPage() {
		return mView.getSelectedRadioId() == R.id.downloadOsRadio && mCalcModel != CalcModel.TI_84PCSE;
	}

	@Override
	public int getNextPage() {
		if (isFinalPage()) {
			throw new IllegalStateException("No next page");
		}
		return mCalcModel == CalcModel.TI_84PCSE ? R.id.os_download_page : R.id.browse_os_page;
	}

	@Override
	public int getPreviousPage() {
		return R.id.model_page;
	}

	@Override
	public void onHiding() {
		// no-op
	}

	@Override
	public void onShowing(Object previousData) {
		final List<String> items = new ArrayList<>();

		final OSDownloadData downloadData = (OSDownloadData) previousData;
		mCalcModel = downloadData.mCalcModel;
		switch (mCalcModel) {
		case TI_73:
			items.add("1.91");
			break;
		case TI_83P:
		case TI_83PSE:
			items.add("1.19");
			break;
		case TI_84P:
		case TI_84PSE:
			items.add("2.55 MP");
			items.add("2.43");
			break;
		case TI_84PCSE:
			//items.add("4.2");
			items.add("4.0");
			break;
		default:
			throw new IllegalStateException("Invalid calc model");
		}

		final Spinner spinner = mView.getSpinner();
		final SpinnerAdapter adapter = new SpinnerDropDownAdapter(spinner.getContext(), items);
		spinner.setAdapter(adapter);
		spinner.setSelection(0);
	}

	@Override
	public int getTitleId() {
		return R.string.osSelectionTitle;
	}

	@Override
	public Object getControllerData() {
		return isFinalPage() ? new FinishWizardData(mCalcModel, null, true) : mCalcModel;
	}
}
