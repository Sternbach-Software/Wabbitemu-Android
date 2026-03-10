package io.github.angelsl.wabbitemu.wizard;

import androidx.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

public class WizardNavigationController {

	private final WizardController mWizardController;
	private final Button mNextButton;
	private final Button mBackButton;

	public WizardNavigationController(@NonNull WizardController wizardController,
			@NonNull ViewGroup navContainer)
	{
		mWizardController = wizardController;
		mNextButton = ViewUtils.findViewById(navContainer, R.id.nextButton, Button.class);
		mBackButton = ViewUtils.findViewById(navContainer, R.id.backButton, Button.class);

		mNextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mWizardController.moveNextPage();
			}
		});

		mBackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mWizardController.movePreviousPage();
			}
		});
	}
	
	public void onPageLaunched(@NonNull WizardPageController pageController) {
		setButtonVisibility(mBackButton, View.VISIBLE);
		setButtonVisibility(mNextButton, View.VISIBLE);
		setNextButton();
		pageController.configureButtons(this);
	}

	public void hideNextButton() {
		setButtonVisibility(mNextButton, View.GONE);
	}

	public void hideBackButton() {
		setButtonVisibility(mBackButton, View.GONE);
	}

	public void finishWizard() {
		mWizardController.moveNextPage();
	}

	public void movePreviousPage() {
		mWizardController.movePreviousPage();
	}

	public void setNextButton() {
		mNextButton.setText(R.string.next);
	}

	public void setFinishButton() {
		mNextButton.setText(R.string.finish);
	}

	private void setButtonVisibility(View button, int visibility) {
		button.setVisibility(visibility);
	}
}
