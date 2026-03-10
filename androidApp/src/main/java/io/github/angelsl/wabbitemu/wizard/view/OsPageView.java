package io.github.angelsl.wabbitemu.wizard.view;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

public class OsPageView extends RelativeLayout {
	private final Spinner mSpinner;
	private final RadioGroup mRadioGroup;

	public OsPageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		LayoutInflater.from(context).inflate(R.layout.os_page, this, true);

		final TextView osTerms = ViewUtils.findViewById(this, R.id.osTerms, TextView.class);
		osTerms.setMovementMethod(LinkMovementMethod.getInstance());

		mSpinner = ViewUtils.findViewById(this, R.id.osVersionSpinner, Spinner.class);
		mRadioGroup = ViewUtils.findViewById(this, R.id.setupOsAcquisistion, RadioGroup.class);
	}

	public Spinner getSpinner() {
		return mSpinner;
	}

	public int getSelectedRadioId() {
		return mRadioGroup.getCheckedRadioButtonId();
	}
}
