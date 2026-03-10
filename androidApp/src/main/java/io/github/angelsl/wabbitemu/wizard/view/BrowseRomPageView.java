package io.github.angelsl.wabbitemu.wizard.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;

import io.github.angelsl.wabbitemu.R;

public class BrowseRomPageView extends RelativeLayout {

	public BrowseRomPageView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);

		LayoutInflater.from(context).inflate(R.layout.browse_wizard_rom_page, this, true);
	}
}
