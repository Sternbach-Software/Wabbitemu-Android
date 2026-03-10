package io.github.angelsl.wabbitemu.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import io.github.angelsl.wabbitemu.R;
import io.github.angelsl.wabbitemu.SkinBitmapLoader;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.calc.CalculatorManager;
import io.github.angelsl.wabbitemu.calc.FileLoadedCallback;
import io.github.angelsl.wabbitemu.fragment.EmulatorFragment;
import io.github.angelsl.wabbitemu.utils.ErrorUtils;
import io.github.angelsl.wabbitemu.utils.IntentConstants;
import io.github.angelsl.wabbitemu.utils.PreferenceConstants;
import io.github.angelsl.wabbitemu.utils.StorageUtils;
import io.github.angelsl.wabbitemu.utils.ViewUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;

public class WabbitemuActivityJava extends AppCompatActivity {
	private static final int LOAD_FILE_CODE = 1;
	private static final int SETUP_WIZARD = 2;
	private static final String DEFAULT_FILE_REGEX = "\\.(rom|sav|[7|8][2|3|x|c|5|6][b|c|d|g|i|k|l|m|n|p|q|s|t|u|v|w|y|z])$";
	public static String sBestCacheDir;

	private enum MainMenuItem {
		LOAD_FILE_MENU_ITEM(0),
		WIZARD_MENU_ITEM(1),
		RESET_MENU_ITEM(2),
		SCREENSHOT_MENU_ITEM(3),
		SETTINGS_MENU_ITEM(4),
		ABOUT_MENU_ITEM(5);

		private final int mPosition;

		MainMenuItem(final int position) {
			mPosition = position;
		}

		public static MainMenuItem fromPosition(final int position) {
			for (final MainMenuItem item : values()) {
				if (item.mPosition == position) {
					return item;
				}
			}

			return null;
		}
	}

	private final CalculatorManager mCalcManager = CalculatorManager.getInstance();
	private final SkinBitmapLoader mSkinLoader = SkinBitmapLoader.getInstance();
	private final VisibilityChangeListener mVisibilityListener = new VisibilityChangeListener();

	private SharedPreferences mSharedPrefs;
	private EmulatorFragment mEmulatorFragment;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private boolean mWasUserLaunched;

	private void handleFile(File f, Runnable runnable) {
		mEmulatorFragment.handleFile(f, runnable);
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		HttpURLConnection.setFollowRedirects(true);

		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		workaroundAsyncTaskIssue();
		if (!testNativeLibraryLoad()) {
			ErrorUtils.showErrorDialog(this,
                    R.string.error_failed_load_native_lib,
                    new FinishActivityClickListener());
			return;
		}

		sBestCacheDir = findBestCacheDir();
		mCalcManager.initialize(this, sBestCacheDir);
		mSkinLoader.initialize(this);

		final String fileName = getLastRomSetting();
		final Runnable runnable = getLaunchRunnable();
		if (fileName != null) {
			final File file = new File(fileName);
			mCalcManager.loadRomFile(file, new FileLoadedCallback() {

				@Override
				public void onFileLoaded(int errorCode) {
					if (errorCode != 0) {
						Log.e("Wabbitemu", String.format("Loading last ROM '%s' failed with error code %d", fileName, errorCode));
						runnable.run();
					}
				}
			});
		}

		setContentView(R.layout.main);
		mEmulatorFragment = (EmulatorFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
		attachMenu();

		if (isFirstRun()) {
			mWasUserLaunched = false;
			final Intent wizardIntent = new Intent(this, WizardActivity.class);
			startActivityForResult(wizardIntent, SETUP_WIZARD);
			return;
		}

		// we expect an absolute filename
		final CalcModel lastRomModel = CalcModel.fromModel(getLastRomModel());
		if (lastRomModel != CalcModel.NO_CALC) {
			mSkinLoader.loadSkinAndKeymap(lastRomModel);
		} else if (fileName == null || fileName.equals("")) {
			runnable.run();
		}
	}

	private String findBestCacheDir() {
		final File cacheDir = getApplicationContext().getCacheDir();
		if (cacheDir != null) {
			return cacheDir.getAbsolutePath();
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			for (File file : getApplicationContext().getExternalCacheDirs()) {
				if (file != null) {
					return file.getAbsolutePath();
				}
			}
		}
		return null;
	}

	private void workaroundAsyncTaskIssue() {
		try {
			Class.forName("android.os.AsyncTask");
		} catch (Throwable ignore) {
			// ignored
		}
	}

	private boolean testNativeLibraryLoad() {
		try {
			mCalcManager.testLoadLib();
		} catch (UnsatisfiedLinkError ex) {
			return false;
		}

		return true;
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
			getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(mVisibilityListener);
			setImmersiveMode(true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true)) {
			setImmersiveMode(false);
			getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
		}
	}

	private void attachMenu() {
		mDrawerLayout = ViewUtils.findViewById(this, R.id.drawer_layout, DrawerLayout.class);
		mDrawerList = ViewUtils.findViewById(this, R.id.left_drawer, ListView.class);
		final String[] menuItems = getResources().getStringArray(R.array.menu_array);

		mDrawerList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, menuItems));
		mDrawerLayout.setScrimColor(Color.parseColor("#DD000000"));
		mDrawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
				handleMenuItem(MainMenuItem.fromPosition(position));
			}
		});
		mDrawerLayout.setDrawerListener(new DrawerListener() {

			@Override
			public void onDrawerStateChanged(final int arg0) {
				// no-op
			}

			@Override
			public void onDrawerSlide(final View drawerView, final float slideOffset) {
				mDrawerLayout.bringChildToFront(drawerView);
				mDrawerLayout.requestLayout();
			}

			@Override
			public void onDrawerOpened(final View arg0) {}

			@Override
			public void onDrawerClosed(final View arg0) {}
		});
	}

	private Runnable getLaunchRunnable() {
		return new Runnable() {

			@Override
			public void run() {
				final Handler handler = new Handler(Looper.getMainLooper());
				handler.post(new Runnable() {
					@Override
					public void run() {
						ErrorUtils.showErrorDialog(WabbitemuActivityJava.this, R.string.errorLink);
					}
				});
				final Intent wizardIntent = new Intent(WabbitemuActivityJava.this, WizardActivity.class);
				startActivityForResult(wizardIntent, SETUP_WIZARD);
			}
		};
	}

	private String getLastRomSetting() {
		return mSharedPrefs.getString(PreferenceConstants.ROM_PATH.toString(), null);
	}

	private int getLastRomModel() {
		return mSharedPrefs.getInt(PreferenceConstants.ROM_MODEL.toString(), -1);
	}

	private boolean isFirstRun() {
		return mSharedPrefs.getBoolean(PreferenceConstants.FIRST_RUN.toString(), true);
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case LOAD_FILE_CODE:
				if (resultCode == RESULT_OK && data != null) {
					final Uri uri = data.getData();
					if (uri != null) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								final File file = copyFileFromUri(uri);
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										if (file != null) {
											handleFile(file, new Runnable() {
												@Override
												public void run() {
													ErrorUtils.showErrorDialog(WabbitemuActivityJava.this, R.string.errorLink);
												}
											});
										} else {
											ErrorUtils.showErrorDialog(WabbitemuActivityJava.this, R.string.errorLink);
										}
									}
								});
							}
						}).start();
					}
				}
				break;
			case SETUP_WIZARD:
				if (resultCode == RESULT_OK) {
					final String fileName = data.getStringExtra(IntentConstants.FILENAME_EXTRA_STRING);
					handleFile(new File(fileName), getLaunchRunnable());

					if (isFirstRun()) {
						final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
						final SharedPreferences.Editor editor = sharedPrefs.edit();
						editor.putBoolean(PreferenceConstants.FIRST_RUN.toString(), false);
						editor.apply();

						mDrawerLayout.openDrawer(mDrawerList);
					}
				} else if (!mWasUserLaunched) {
					finish();
				}
				break;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mDrawerLayout.closeDrawer(mDrawerList);
		} else {
			mDrawerLayout.openDrawer(mDrawerList);
		}

		return false;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		final MainMenuItem position;
		switch (item.getItemId()) {
		case R.id.aboutMenuItem:
			position = MainMenuItem.ABOUT_MENU_ITEM;
			break;
		case R.id.settingsMenuItem:
			position = MainMenuItem.SETTINGS_MENU_ITEM;
			break;
		case R.id.resetMenuItem:
			position = MainMenuItem.RESET_MENU_ITEM;
			break;
		case R.id.rerunWizardMenuItem:
			position = MainMenuItem.WIZARD_MENU_ITEM;
			break;
		case R.id.loadFileMenuItem:
			position = MainMenuItem.LOAD_FILE_MENU_ITEM;
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return handleMenuItem(position);
	}

	private boolean handleMenuItem(final MainMenuItem position) {
		mDrawerLayout.closeDrawer(mDrawerList);

		switch (position) {
		case SETTINGS_MENU_ITEM:
			launchSettings();
			return true;
		case RESET_MENU_ITEM:
			resetCalc();
			return true;
		case SCREENSHOT_MENU_ITEM:
			screenshotCalc();
			return true;
		case WIZARD_MENU_ITEM:
			launchWizard();
			return true;
		case LOAD_FILE_MENU_ITEM:
			launchBrowse();
			return true;
		case ABOUT_MENU_ITEM:
			launchAbout();
			return true;
		default:
			throw new IllegalStateException("Invalid menu item");
		}
	}

	private void screenshotCalc() {

		final Bitmap screenshot = mEmulatorFragment.getScreenshot();
		if (screenshot == null) {
			ErrorUtils.showErrorDialog(this, R.string.errorScreenshot);
			return;
		}
		final Bitmap scaledScreenshot = Bitmap.createScaledBitmap(screenshot, screenshot.getWidth() * 2,
				screenshot.getHeight() * 2, true);
		final File outputDir;
		final File outputFile;
		if (StorageUtils.hasExternalStorage()) {
			outputDir = new File(new File(StorageUtils.getPrimaryStoragePath(), "Wabbitemu"), "Screenshots");
			if (!outputDir.exists()) {
				outputDir.mkdirs();
			}

			final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
			final String now = sdf.format(new Date());

			final String fileName = "screenshot" + now + ".png";
			outputFile = new File(outputDir, fileName);
		} else {
			ErrorUtils.showErrorDialog(this, R.string.errorMissingSdCard);
			return;
		}

		try {
			final FileOutputStream out = new FileOutputStream(outputFile);
			scaledScreenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
			out.close();
		} catch (final Exception e) {
			ErrorUtils.showErrorDialog(this, R.string.errorScreenshot);
			return;
		}

		final String formatString = getResources().getString(R.string.screenshotSuccess);
		final String successString = String.format(formatString, outputFile);
		final Toast toast = Toast.makeText(this, successString, Toast.LENGTH_LONG);
		toast.show();
	}

	private void launchAbout() {
		final Intent aboutIntent = new Intent(this, AboutActivity.class);
		startActivity(aboutIntent);
	}

	private void launchBrowse() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("*/*");
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		try {
			startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.browseFileDescription)), LOAD_FILE_CODE);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, "Please install a file manager.", Toast.LENGTH_SHORT).show();
		}
	}

	private void launchWizard() {
		mWasUserLaunched = true;
		final Intent wizardIntent = new Intent(this, WizardActivity.class);
		startActivityForResult(wizardIntent, SETUP_WIZARD);
	}

	private void resetCalc() {
		mEmulatorFragment.resetCalc();
	}

	private void launchSettings() {
		final Intent intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void setImmersiveMode(boolean isImmersive) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
			return;
		}

		// The UI options currently enabled are represented by a bitfield.
		// getSystemUiVisibility() gives us that bitfield.
		final View decorView = getWindow().getDecorView();
		final int uiOptions = decorView.getSystemUiVisibility();
		int newUiOptions;

		// Immersive mode: Backward compatible to KitKat.
		// Note that this flag doesn't do anything by itself, it only augments
		// the behavior of HIDE_NAVIGATION and FLAG_FULLSCREEN. For the purposes
		// of this sample all three flags are being toggled together.
		// Note that there are two immersive mode UI flags, one of which is
		// referred to as "sticky".
		// Sticky immersive mode differs in that it makes the navigation and
		// status bars semi-transparent, and the UI flag does not get cleared
		// when the user interacts with the screen.
		if (isImmersive) {
			newUiOptions = uiOptions | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
			newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		} else {
			newUiOptions = uiOptions & (~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
			newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}

		decorView.setSystemUiVisibility(newUiOptions);
	}

	private class VisibilityChangeListener implements OnSystemUiVisibilityChangeListener {
		@Override
		public void onSystemUiVisibilityChange(int visibility) {
			// If someone tries to to change the visibility after have set it,
			// we basically want to ignore it. Usually this is caused by
			// something like the loading dialog.
			setImmersiveMode(true);
		}
	}

	private class FinishActivityClickListener implements OnClickListener {
		@Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            finish();
        }
	}

	private File copyFileFromUri(Uri uri) {
		String fileName = getFileName(uri);
		if (fileName == null) {
			fileName = "temp_file.8xp";
		}
		File cacheDir = getCacheDir();
		File file = new File(cacheDir, fileName);
		try (InputStream inputStream = getContentResolver().openInputStream(uri);
			 FileOutputStream outputStream = new FileOutputStream(file)) {
			byte[] buffer = new byte[4 * 1024];
			int read;
			while ((read = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, read);
			}
			outputStream.flush();
			return file;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private String getFileName(Uri uri) {
		String result = null;
		if (uri.getScheme().equals("content")) {
			try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
				if (cursor != null && cursor.moveToFirst()) {
					int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					if (index >= 0) {
						result = cursor.getString(index);
					}
				}
			}
		}
		if (result == null) {
			result = uri.getPath();
			int cut = result.lastIndexOf('/');
			if (cut != -1) {
				result = result.substring(cut + 1);
			}
		}
		return result;
	}
}
