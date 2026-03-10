package io.github.angelsl.wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

public class ModelPageView extends RelativeLayout {

	private final RadioGroup mRadioGroup;

	public ModelPageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		LayoutInflater.from(context).inflate(R.layout.model_page, this, true);
		mRadioGroup = ViewUtils.findViewById(this, R.id.setupModelRadioGroup, RadioGroup.class);
	}

	public int getSelectedRadioId() {
		return mRadioGroup.getCheckedRadioButtonId();
	}
}
