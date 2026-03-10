package io.github.angelsl.wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

public class LandingPageView extends RelativeLayout {

	private final RadioGroup mRadioGroup;

	public LandingPageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		LayoutInflater.from(context).inflate(R.layout.landing_page, this, true);
		mRadioGroup = ViewUtils.findViewById(this, R.id.setupOptionsRadioGroup, RadioGroup.class);
	}

	public int getSelectedRadioId() { 
		return mRadioGroup.getCheckedRadioButtonId();
	}
}
