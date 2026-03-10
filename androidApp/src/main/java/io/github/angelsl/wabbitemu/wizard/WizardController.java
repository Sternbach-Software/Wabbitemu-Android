package io.github.angelsl.wabbitemu.wizard;

import java.util.Stack;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ViewAnimator;

import io.github.angelsl.wabbitemu.R;

public class WizardController {

	private final Activity mActivity;
	private final ViewAnimator mViewFlipper;
	private final OnWizardFinishedListener mFinishedListener;
	private final SparseArray<WizardPageController> mPageControllers = new SparseArray<WizardPageController>();
	private final SparseIntArray mIdToPositionMap = new SparseIntArray();
	private final WizardNavigationController mWizardNavController;

	private WizardPageController mCurrentController;
	private Stack<Object> mPreviousData = new Stack<Object>();

	public WizardController(@NonNull Activity activity,
			@NonNull ViewAnimator viewFlipper,
			@NonNull ViewGroup navContainer,
			@NonNull OnWizardFinishedListener onFinishListener)
	{
		mActivity = activity;
		mViewFlipper = viewFlipper;
		mFinishedListener = onFinishListener;
		mWizardNavController = new WizardNavigationController(this, navContainer);

		for (int i = 0; i < viewFlipper.getChildCount(); i++) {
			final int pageId= viewFlipper.getChildAt(i).getId();
			if (pageId == View.NO_ID) {
				throw new IllegalStateException("Child at index " + i + " missing id");
			}

			if (mIdToPositionMap.get(pageId) != 0) {
				throw new IllegalStateException("Duplicate page id " + pageId);
			}

			mIdToPositionMap.put(pageId, i);
		}
	}

	public void registerView(int pageId, @NonNull WizardPageController pageController) {
		mPageControllers.put(pageId, pageController);

		if (mIdToPositionMap.get(pageId, -1) == -1) {
			throw new IllegalStateException("View Id must be child of the view animator");
		}

		if (mCurrentController == null) {
			mCurrentController = pageController;
			showPage(pageId, null);
			mWizardNavController.onPageLaunched(mCurrentController);
		}
	}

	public boolean moveNextPage() {
		if (mCurrentController.isFinalPage()) {
			mFinishedListener.onWizardFinishedListener(mCurrentController.getControllerData());
			return true;
		}

		if (!mCurrentController.hasNextPage()) {
			return false;
		}

		setNextAnimation();
		final int nextPageId = mCurrentController.getNextPage();
		final Object previousData = mCurrentController.getControllerData();
		mPreviousData.push(previousData);

		moveToPage(nextPageId, previousData);
		return true;
	}

	public boolean movePreviousPage() {
		if (!mCurrentController.hasPreviousPage()) {
			return false;
		}

		setBackAnimation();
		final int previousPageId = mCurrentController.getPreviousPage();
		final Object previousData = mPreviousData.pop();
		moveToPage(previousPageId, previousData);
		return true;
	}

	private void moveToPage(int nextPageId, Object data) {
		final WizardPageController lastController = mCurrentController;
		mCurrentController = mPageControllers.get(nextPageId);
		if (mCurrentController == null) {
			throw new IllegalStateException("Must have registered next page");
		}

		lastController.onHiding();
		mViewFlipper.getInAnimation().setAnimationListener(mAnimationListener);

		if (mIdToPositionMap.get(nextPageId, -1) == -1) {
			throw new IllegalStateException("Id is not registered " + nextPageId);
		}

		showPage(nextPageId, data);
	}

	private void showPage(int nextPageId, @Nullable Object data) {
		mViewFlipper.setDisplayedChild(mIdToPositionMap.get(nextPageId));
		mCurrentController.onShowing(data);
	}

	private void setNextAnimation() {
		mViewFlipper.setOutAnimation(mActivity, R.anim.out_to_left);
		mViewFlipper.setInAnimation(mActivity, R.anim.in_from_right);
	}

	private void setBackAnimation() {
		mViewFlipper.setOutAnimation(mActivity, R.anim.out_to_right);
		mViewFlipper.setInAnimation(mActivity, R.anim.in_from_left);
	}

	private final AnimationListener mAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(final Animation animation) {
			// no-op
		}

		@Override
		public void onAnimationRepeat(final Animation animation) {
			// no-op
		}

		@Override
		public void onAnimationEnd(final Animation animation) {
			mActivity.setTitle(mCurrentController.getTitleId());
			mWizardNavController.onPageLaunched(mCurrentController);
		}
	};
}
