package io.github.angelsl.wabbitemu.wizard;

import androidx.annotation.NonNull;


public interface WizardPageController {

	void configureButtons(@NonNull WizardNavigationController navController);

	boolean hasPreviousPage();

	boolean hasNextPage();

	boolean isFinalPage();

	int getNextPage();

	int getPreviousPage();

	void onHiding();

	void onShowing(Object previousData);

	int getTitleId();

	Object getControllerData();
}
