package io.github.angelsl.wabbitemu.wizard.controller;

import androidx.annotation.NonNull;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController;
import io.github.angelsl.wabbitemu.wizard.WizardPageController;
import io.github.angelsl.wabbitemu.wizard.view.ModelPageView;

public class CalcModelPageController implements WizardPageController {

	private final ModelPageView mView;

	public CalcModelPageController(ModelPageView view) {
		mView = view;
	}

	@Override
	public void configureButtons(@NonNull WizardNavigationController navController) {
		// no-op
	}

	@Override
	public boolean hasPreviousPage() {
		return true;
	}

	@Override
	public boolean hasNextPage() {
		return true;
	}

	@Override
	public boolean isFinalPage() {
		return false;
	}

	@Override
	public int getNextPage() {
		return R.id.choose_os_page;
	}

	@Override
	public int getPreviousPage() {
		return R.id.landing_page;
	}

	@Override
	public void onHiding() {
		// no-op
	}

	@Override
	public void onShowing(Object previousData) {
		// no-op
	}

	@Override
	public int getTitleId() {
		return R.string.calculatorTypeTitle;
	}

	@Override
	public Object getControllerData() {
		switch (mView.getSelectedRadioId()) {
		case R.id.ti73Radio:
			return CalcModel.TI_73;
		case R.id.ti83pRadio:
			return CalcModel.TI_83P;
		case R.id.ti83pseRadio:
			return CalcModel.TI_83PSE;
		case R.id.ti84pRadio:
			return CalcModel.TI_84P;
		case R.id.ti84pseRadio:
			return CalcModel.TI_84PSE;
		case R.id.ti84pcseRadio:
			return CalcModel.TI_84PCSE;
		default:
			throw new IllegalStateException("Invalid radio id");
		}
	}
}
