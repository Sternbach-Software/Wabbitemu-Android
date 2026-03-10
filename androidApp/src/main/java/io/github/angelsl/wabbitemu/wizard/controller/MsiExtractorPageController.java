package io.github.angelsl.wabbitemu.wizard.controller;

import androidx.annotation.NonNull;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController;
import io.github.angelsl.wabbitemu.wizard.WizardPageController;
import io.github.angelsl.wabbitemu.wizard.view.LandingPageView;

public class MsiExtractorPageController implements WizardPageController {

	private final LandingPageView mView;

	public MsiExtractorPageController(@NonNull LandingPageView view) {
		mView = view;
	}

	@Override
	public void configureButtons(@NonNull WizardNavigationController navController) {
		navController.hideBackButton();
	}

	@Override
	public boolean hasPreviousPage() {
		return false;
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
		switch (mView.getSelectedRadioId()) {
		case R.id.browseRomRadio:
			return R.id.browse_rom_page;
		case R.id.createWizardRadio:
			return R.id.model_page;
		default:
			throw new IllegalStateException("Invalid radio id");
		}
	}

	@Override
	public int getPreviousPage() {
		throw new IllegalStateException("No previous page");
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
		return R.string.gettingStartedTitle;
	}

	@Override
	public Object getControllerData() {
		return null;
	}
}
