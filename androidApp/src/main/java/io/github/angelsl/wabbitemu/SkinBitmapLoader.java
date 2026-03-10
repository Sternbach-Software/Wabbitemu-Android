package io.github.angelsl.wabbitemu;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import io.github.angelsl.wabbitemu.CalcSkin.CalcSkinChangedListener;
import io.github.angelsl.wabbitemu.calc.CalcModel;
import io.github.angelsl.wabbitemu.calc.CalculatorManager;
import io.github.angelsl.wabbitemu.utils.PreferenceConstants;

public class SkinBitmapLoader {
	private static final int SKIN_WIDTH = 700;
	private static final int SKIN_HEIGHT = 1450;

	private static final Point[] RECT_POINTS = {
			new Point(150, 1350),
			new Point(190, 1366),
			new Point(524, 1364),
			new Point(558, 1350),
			new Point(632, 1070),
			new Point(632, 546),
			new Point(74, 546),
			new Point(74, 1136),
			new Point(SKIN_WIDTH - 74, 1136),
			new Point(SKIN_WIDTH - 74, 546),
			new Point(SKIN_WIDTH - 632, 546),
			new Point(SKIN_WIDTH - 632, 1070),
			new Point(SKIN_WIDTH - 558, 1350),
			new Point(SKIN_WIDTH - 524, 1364),
			new Point(SKIN_WIDTH - 190, 1366),
			new Point(SKIN_WIDTH - 150, 1350) };

	private final Set<CalcSkinChangedListener> mSkinListeners = new HashSet<>();
	private final AtomicBoolean mHasLoadedSkin = new AtomicBoolean(false);
	private final SparseArray<Rect> mButtonRects = new SparseArray<>();

	private Context mContext;
	private Resources mResources;
	private double mRatio;
	private int mFaceplateColor;
	private Path mFaceplatePath;
	private boolean mCorrectRatio;
	private boolean mLargeScreen;
	private boolean mMaximizeSkin;
	private SharedPreferences mSharedPrefs;
	private volatile Bitmap mRenderedSkinImage;
	private Rect mLcdRect;
	private Rect mScreenRect;
	private Rect mSkinRect;
	private int mSkinX, mSkinY;
	private int mKeymapWidth;
	private int mKeymapHeight;
	private double mKeymapWidthScale;
	private double mKeymapHeightScale;
	private double mWidthMaxScale;
	private double mHeightMaxScale;
	private int[] mKeymapPixels;

	private static class SingletonHolder {
		private static final SkinBitmapLoader SINGLETON = new SkinBitmapLoader();
	}

	public static SkinBitmapLoader getInstance() {
		return SingletonHolder.SINGLETON;
	}

	public void initialize(Context context) {
		mContext = context;
		mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		mCorrectRatio = mSharedPrefs.getBoolean(PreferenceConstants.CORRECT_SCREEN_RATIO.toString(), false);
		mLargeScreen = mSharedPrefs.getBoolean(PreferenceConstants.LARGE_SCREEN.toString(), false);
		mFaceplateColor = mSharedPrefs.getInt(PreferenceConstants.FACEPLATE_COLOR.toString(), Color.GRAY);
		mMaximizeSkin = mSharedPrefs.getBoolean(PreferenceConstants.FULL_SKIN_SIZE.toString(), false);
		mResources = context.getResources();
	}

	private final OnSharedPreferenceChangeListener mPrefListener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, final String key) {
			if (key.equals(PreferenceConstants.FACEPLATE_COLOR.toString())) {
				// the theory here is that its better memory wise
				// to throw away our scaled skin and to reload it
				// when the faceplate changes
				mFaceplateColor = sharedPreferences.getInt(key, Color.GRAY);
				loadSkinThread();
			} else if (key.equals(PreferenceConstants.LARGE_SCREEN.toString())) {
				mLargeScreen = sharedPreferences.getBoolean(key, false);
				loadSkinThread();
			} else if (key.equals(PreferenceConstants.CORRECT_SCREEN_RATIO.toString())) {
				mCorrectRatio = sharedPreferences.getBoolean(key, false);
				loadSkinThread();
			} else if (key.equals(PreferenceConstants.FULL_SKIN_SIZE.toString())) {
				mMaximizeSkin = mSharedPrefs.getBoolean(PreferenceConstants.FULL_SKIN_SIZE.toString(), false);
				loadSkinThread();
			}
		}

		private void loadSkinThread() {
			final Thread thread = new Thread(new Runnable() {

				@Override
				public void run() {
					mHasLoadedSkin.set(false);
					loadSkinAndKeymap(CalculatorManager.getInstance().getModel());
				}
			});
			thread.start();
		}
	};

	public void registerSkinChangedListener(final CalcSkinChangedListener listener) {
		mSkinListeners.add(listener);
	}

	public void unregisterSkinChangedListener(final CalcSkinChangedListener listener) {
		mSkinListeners.remove(listener);
	}

	public void destroySkin() {
		mRenderedSkinImage = null;
		mScreenRect = null;
		mSkinRect = null;
		mLcdRect = null;
		mKeymapPixels = null;
		mHasLoadedSkin.set(false);
	}

	public void loadSkinAndKeymap(CalcModel model) {
		if (mHasLoadedSkin.getAndSet(true)) {
			return;
		}

		String skinImageFile;
		String keymapImageFile;
		switch (model) {
		case TI_73:
			skinImageFile = "ti73";
			keymapImageFile = "ti83pkeymap";
			break;
		case TI_81:
			skinImageFile = "ti81";
			keymapImageFile = "ti81keymap";
			break;
		case TI_82:
			skinImageFile = "ti82";
			keymapImageFile = "ti82keymap";
			break;
		case TI_83:
			skinImageFile = "ti83";
			keymapImageFile = "ti83keymap";
			break;
		case TI_83P:
			skinImageFile = "ti83p";
			keymapImageFile = "ti83pkeymap";
			break;
		case TI_83PSE:
			skinImageFile = "ti83pse";
			keymapImageFile = "ti83pkeymap";
			break;
		case TI_84P:
			skinImageFile = "ti84p";
			keymapImageFile = "ti84psekeymap";
			break;
		case TI_84PSE:
			skinImageFile = "ti84pse";
			keymapImageFile = "ti84psekeymap";
			break;
		case TI_84PCSE:
			skinImageFile = "ti84pcse";
			keymapImageFile = "ti84pcsekeymap";
			break;
		case TI_85:
			skinImageFile = "ti85";
			keymapImageFile = "ti85keymap";
			break;
		case TI_86:
			skinImageFile = "ti86";
			keymapImageFile = "ti86keymap";
			break;
		default:
			return;
		}

		if (mLargeScreen) {
			keymapImageFile = keymapImageFile + "large";
		}

		if (mResources.getConfiguration().smallestScreenWidthDp >= 600) {
			keymapImageFile = "tablet/" + keymapImageFile;
			skinImageFile = "tablet/" + skinImageFile;
		} else {
			keymapImageFile = "phone/" + keymapImageFile;
			skinImageFile = "phone/" + skinImageFile;
		}

		skinImageFile = skinImageFile + ".png";
		keymapImageFile = keymapImageFile + ".png";

		setSurfaceSize(skinImageFile, keymapImageFile, model);
		notifySkinChanged();
	}

	private void notifySkinChanged() {
		for (final CalcSkinChangedListener listener : mSkinListeners) {
			listener.onCalcSkinChanged(mLcdRect, mScreenRect);
		}
	}

	private synchronized void setSurfaceSize(final String skinImageId, final String keymapImageId, CalcModel model) {
		final Point displaySize = getDisplaySize();
		mSkinX = 0;
		mSkinY = 0;
		mRatio = 1.0;

		final int skinWidth;
		final int skinHeight;

		final int smallestScreenWidthDp = mResources.getConfiguration().smallestScreenWidthDp;
		final boolean isTablet = smallestScreenWidthDp >= 600;
		if (isTablet) {
			mRatio = Math.min((double) displaySize.x / SKIN_WIDTH, (double) displaySize.y / SKIN_HEIGHT);
			skinWidth = (int) (SKIN_WIDTH * mRatio);
			skinHeight = (int) (SKIN_HEIGHT * mRatio);
			mSkinX = (displaySize.x - skinWidth) / 2;
			mSkinY = (displaySize.y - skinHeight) / 2;
		} else {
			skinWidth = displaySize.x;
			skinHeight = displaySize.y;
		}

		final Bitmap skinImage = getScaledSkinImage(skinImageId, mResources, skinWidth, skinHeight);
		final Bitmap keymapImage = getKeymapImage(keymapImageId, mResources);
		mKeymapWidth = keymapImage.getWidth();
		mKeymapHeight = keymapImage.getHeight();
		mKeymapWidthScale = (double) displaySize.x / mKeymapWidth;
		mKeymapHeightScale = (double) displaySize.y / mKeymapHeight;

		if (smallestScreenWidthDp >= 600) {
			if (mKeymapHeightScale > mKeymapWidthScale) {
				mKeymapHeightScale = mKeymapWidthScale;
			} else {
				mKeymapWidthScale = mKeymapHeightScale;
			}
		}

		mKeymapPixels = new int[mKeymapWidth * mKeymapHeight];
		keymapImage.getPixels(mKeymapPixels, 0, mKeymapWidth, 0, 0, mKeymapWidth, mKeymapHeight);
		keymapImage.recycle();
		final Rect lcdRect = parseKeymap();
		if (lcdRect == null) {
			mLcdRect = new Rect(0, 0, 1, 1);
			return;
		}

		final double extraPadding = mContext.getResources().getDimension(R.dimen.maxSkinPadding);
		if (mMaximizeSkin && !isTablet) {
			mSkinRect = new Rect((int) (mSkinRect.left * mKeymapWidthScale - extraPadding),
					(int) (mSkinRect.top * mKeymapHeightScale - extraPadding),
					(int) (mSkinRect.right * mKeymapWidthScale + extraPadding),
					(int) (mSkinRect.bottom * mKeymapHeightScale + extraPadding));
			mWidthMaxScale = (double) skinWidth / (mSkinRect.right - mSkinRect.left);
			mHeightMaxScale = (double) skinHeight / (mSkinRect.bottom - mSkinRect.top);
		} else {
			if (isTablet) {
				mSkinRect = new Rect(0, 0, displaySize.x, displaySize.y);
			} else {
				mSkinRect = new Rect(0, 0, skinImage.getWidth(), skinImage.getHeight());
			}
			mWidthMaxScale = 1.0;
			mHeightMaxScale = 1.0;
		}

		final int lcdWidth, lcdHeight;
		switch (model) {
		case TI_85:
		case TI_86:
			lcdWidth = 128;
			lcdHeight = 64;
			break;
		case TI_84PCSE:
			lcdWidth = 320;
			lcdHeight = 240;
			break;
		default:
			lcdWidth = 96;
			lcdHeight = 64;
			break;
		}

		mLcdRect = new Rect(0, 0, lcdWidth, lcdHeight);
		mScreenRect = new Rect((int) (lcdRect.left * mKeymapWidthScale * mWidthMaxScale) + mSkinX - mSkinRect.left,
				(int) (lcdRect.top * mKeymapHeightScale * mHeightMaxScale) - mSkinRect.top,
				(int) (lcdRect.right * mKeymapWidthScale * mWidthMaxScale) + mSkinX - mSkinRect.left,
				(int) (lcdRect.bottom * mKeymapHeightScale * mHeightMaxScale) - mSkinRect.top);

		if (mCorrectRatio) {
			final int screenWidth, screenHeight;
			final double screenRatio = (double) mLcdRect.width() / mLcdRect.height();
			final double realRatio = (double) mScreenRect.width() / mScreenRect.height();
			if (realRatio > screenRatio) {
				// assuming all calc screens width > height
				final int oldWidth = mScreenRect.width();
				screenHeight = mScreenRect.height();
				screenWidth = (int) (screenHeight * screenRatio);
				mScreenRect.right = mScreenRect.left + screenWidth;
				final int shift = (oldWidth - screenWidth) / 2;
				mScreenRect.left += shift;
				mScreenRect.right += shift;
			} else {
				final int oldHeight = mScreenRect.height();
				screenWidth = mScreenRect.width();
				screenHeight = (int) (screenWidth * screenRatio);
				mScreenRect.bottom = mScreenRect.top + screenHeight;
				final int shift = (oldHeight - screenHeight) / 2;
				mScreenRect.top += shift;
				mScreenRect.bottom += shift;
			}
		}

		mFaceplateColor = mSharedPrefs.getInt(PreferenceConstants.FACEPLATE_COLOR.toString(), Color.GRAY);
		mSharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);
		createRenderSkin(displaySize.x, displaySize.y, skinImage, model);
	}

	private Bitmap getScaledSkinImage(final String skinImageId,
			final Resources resources,
			final int skinWidth,
			final int skinHeight)
	{
		final Bitmap skinImage = getSkinImage(skinImageId, resources, null);
		final Bitmap scaledBitmap = Bitmap.createScaledBitmap(skinImage, skinWidth, skinHeight, true);
		skinImage.recycle();
		return scaledBitmap;
	}

	private Bitmap getSkinImage(String skinImageId, Resources resources, BitmapFactory.Options options) {
		Bitmap bitmap = null;
		InputStream inputStream = null;
		try {
			inputStream = resources.getAssets().open(skinImageId);
			bitmap = BitmapFactory.decodeStream(inputStream, null, null);
		} catch (IOException ex) {
			Log.w("CalcSkin", "Exception reading input stream" + ex.toString());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException ex) {
				Log.w("CalcSkin", "Exception closing input stream" + ex.toString());
			}
		}
		return bitmap;
	}

	private Bitmap getKeymapImage(final String keymapImageId, final Resources resources) {
		final Bitmap keymapImage = getSkinImage(keymapImageId, resources, null);
		return keymapImage;
	}

	private Point getDisplaySize() {
		// TODO: use getWidth() / getHeight()
		final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		final Display display = wm.getDefaultDisplay();
		final Point displaySize = new Point();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
				mSharedPrefs.getBoolean(PreferenceConstants.IMMERSIVE_MODE.toString(), true))
		{
			display.getRealSize(displaySize);
		} else {
			display.getSize(displaySize);
		}

		return displaySize;
	}

	private Rect parseKeymap() {
		mButtonRects.clear();
		Rect lcdRect = null;
		mSkinRect = null;

		for (int pixelOffset = 0; pixelOffset < mKeymapWidth * mKeymapHeight; pixelOffset++) {
			final int pixelData = mKeymapPixels[pixelOffset];
			if (pixelData == Color.RED) {
				if (lcdRect == null) {
					final int x = pixelOffset % mKeymapWidth;
					final int y = pixelOffset / mKeymapWidth;
					lcdRect = new Rect(x, y, x, y);
					// This assumes screen is always above the first button
					// Fix if custom skins are added
					mSkinRect = new Rect(lcdRect);
				} else {
					updateRect(lcdRect, pixelOffset);
					updateRect(mSkinRect, pixelOffset);
				}
			}

			if (pixelData != Color.WHITE) {
				final Rect rect = mButtonRects.get(pixelData);
				if (rect == null) {
					final int x = pixelOffset % mKeymapWidth;
					final int y = pixelOffset / mKeymapWidth;
					mButtonRects.put(pixelData, new Rect(x, y, x, y));
				} else {
					updateRect(rect, pixelOffset);
					updateRect(mSkinRect, pixelOffset);
				}
			}
		}

		if (lcdRect == null) {
			Log.d("Keymap", "Keymap fail");
			return null;
		}

		return lcdRect;
	}

	private void updateRect(Rect rect, int offset) {
		final int x = offset % mKeymapWidth;
		final int y = offset / mKeymapWidth;
		if (rect.left > x) {
			rect.left = x;
		} else if (rect.right < x) {
			rect.right = x;
		}

		if (rect.top > y) {
			rect.top = y;
		} else if (rect.bottom < y) {
			rect.bottom = y;
		}
	}

	private void createRenderSkin(final int width, final int height, final Bitmap skinImage, CalcModel model) {
		mRenderedSkinImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		final Canvas skinCanvas = new Canvas(mRenderedSkinImage);
		skinCanvas.setDensity(Bitmap.DENSITY_NONE);
		mFaceplatePath = getSkinPath();
		drawFaceplate(skinCanvas, model);
		skinCanvas.drawBitmap(skinImage, mSkinX, mSkinY, null);
	}

	private Path getSkinPath() {
		final Path path = new Path();
		path.moveTo(RECT_POINTS[0].x, RECT_POINTS[0].y);
		for (int i = 1; i < RECT_POINTS.length; i++) {
			path.lineTo(RECT_POINTS[i].x, RECT_POINTS[i].y);
		}

		final Matrix scaleMatrix = new Matrix();
		final RectF rectF = new RectF();
		path.computeBounds(rectF, true);
		scaleMatrix.setScale((float) mRatio, (float) mRatio, mSkinX, mSkinY);
		path.offset(mSkinX, mSkinY);
		path.transform(scaleMatrix);

		return path;
	}

	public int getKeymapPixel(int x, int y) {
		x = (int) ((x + mSkinRect.left) / (mKeymapWidthScale * mWidthMaxScale));
		y = (int) ((y + mSkinRect.top) / (mKeymapHeightScale * mHeightMaxScale));
		final int index = y * mKeymapWidth + x;
		if (index > mKeymapPixels.length) {
			// LG seems to have issues being able to go beyond bounds. Not sure
			// why, so gonna track this and see if it goes away with immersive
			// mode
			return mKeymapPixels[mKeymapPixels.length - (index % mKeymapWidth)];
		} else if (index < 0) {
			return mKeymapPixels[-(index % mKeymapWidth)];
		}

		return mKeymapPixels[index];
	}

	private void drawFaceplate(final Canvas canvas, CalcModel model) {
		if (model != CalcModel.TI_84PSE && model != CalcModel.TI_84PCSE) {
			return;
		}

		mFaceplateColor = mSharedPrefs.getInt(PreferenceConstants.FACEPLATE_COLOR.toString(), Color.GRAY);

		if (mSkinX > 0) {
			final Paint faceplatePaint = new Paint();
			faceplatePaint.setStyle(Paint.Style.FILL);
			faceplatePaint.setColor(mFaceplateColor);
			canvas.drawPath(mFaceplatePath, faceplatePaint);
		} else {
			canvas.drawColor(mFaceplateColor);
		}
	}

	public Bitmap getRenderedSkin() {
		return mRenderedSkinImage;
	}

	public Rect getLcdRect() {
		return mLcdRect;
	}

	public Rect getLcdSkinRect() {
		return mScreenRect;
	}

	public Rect getSkinRect() {
		return mSkinRect;
	}

	public int getSkinX() {
		return mSkinX;
	}

	public int getSkinY() {
		return mSkinY;
	}

	public boolean isOutsideKeymap(int x, int y) {
		return (x >= (mKeymapWidth * mKeymapWidthScale)) ||
				(y >= (mKeymapHeight * mKeymapHeightScale)) ||
				(x < 0) ||
				(y < 0) ||
				mKeymapPixels == null;
	}

	@Nullable
	public Rect getKeymapRect(int x, int y) {
		final int pixelData = getKeymapPixel(x, y);
		final Rect buttonRect = mButtonRects.get(pixelData);
		if (buttonRect == null) {
			return null;
		}

		return new Rect(
				(int) (buttonRect.left * mKeymapWidthScale * mWidthMaxScale) + mSkinX - mSkinRect.left,
				(int) (buttonRect.top * mKeymapHeightScale * mHeightMaxScale) + mSkinY - mSkinRect.top,
				(int) (buttonRect.right * mKeymapWidthScale * mWidthMaxScale) + mSkinX - mSkinRect.left,
				(int) (buttonRect.bottom * mKeymapHeightScale * mHeightMaxScale) + mSkinY - mSkinRect.top);
	}
}
