package com.Revsoft.Wabbitemu.activity;

import com.Revsoft.Wabbitemu.R;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstance) {
		super.onCreate(savedInstance);		
		setContentView(R.layout.about);
		setTitle(R.string.about);
		
		final TextView colorPickerLink = (TextView) findViewById(R.id.colorPickerLink); 
		colorPickerLink.setMovementMethod(LinkMovementMethod.getInstance());
		
		final TextView bootFreeLink = (TextView) findViewById(R.id.bootFreeLink); 
		bootFreeLink.setMovementMethod(LinkMovementMethod.getInstance());
		
		try {
			final TextView view = (TextView) findViewById(R.id.aboutVersion);
			final String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			view.setText(version);
		} catch (NameNotFoundException e) {
			Log.e("About", "Version exception", e);
		}
	}
}
