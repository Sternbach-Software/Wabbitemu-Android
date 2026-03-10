package io.github.angelsl.wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.github.angelsl.wabbitemu.R;

import javax.annotation.Nonnull;

public class ChooseOsPageView extends RelativeLayout {

	private final TextView mMessage;
	private final ProgressBar mLoadingSpinner;

	public ChooseOsPageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		LayoutInflater.from(context).inflate(R.layout.choose_os_page, this, true);
		mMessage = (TextView) findViewById(R.id.message);
		mLoadingSpinner = (ProgressBar) findViewById(R.id.loadingSpinner);
	}

	@Nonnull
	public TextView getMessage() {
		return mMessage;
	}

	public ProgressBar getLoadingSpinner() {
		return mLoadingSpinner;
	}
}
