package io.github.angelsl.wabbitemu.wizard.controller

import androidx.fragment.app.FragmentManager
import android.content.Context
import io.github.angelsl.wabbitemu.wizard.view.BrowseRomPageView
import io.github.angelsl.wabbitemu.wizard.WizardPageController
import io.github.angelsl.wabbitemu.utils.OnBrowseItemSelected
import io.github.angelsl.wabbitemu.wizard.WizardNavigationController
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.wizard.data.FinishWizardData
import android.os.Bundle
import android.util.Log
import io.github.angelsl.wabbitemu.utils.IntentConstants
import io.github.angelsl.wabbitemu.fragment.BrowseFragment
import java.lang.IllegalStateException

class BrowseRomPageController(
    view: BrowseRomPageView,
    fragmentManager: FragmentManager
) : WizardPageController {
    private val mContext: Context
    private val mFragmentManager: FragmentManager
    private val mBrowseCallback = OnBrowseItemSelected { fileName ->
        if (mNavController == null) {
            return@OnBrowseItemSelected
        }
        mSelectedFileName = fileName
        mNavController!!.finishWizard()
    }
    private var mNavController: WizardNavigationController? = null
    private var mSelectedFileName: String? = null

    init {
        mContext = view.context
        mFragmentManager = fragmentManager
    }

    override fun configureButtons(navController: WizardNavigationController) {
        mNavController = navController
        navController.hideNextButton()
    }

    override fun hasPreviousPage(): Boolean {
        return true
    }

    override fun hasNextPage(): Boolean {
        return false
    }

    override fun isFinalPage(): Boolean {
        return true
    }

    override fun getNextPage(): Int {
        throw IllegalStateException("No next page")
    }

    override fun getPreviousPage(): Int {
        return R.id.landing_page
    }

    override fun onHiding() {
        // no-op
    }

    override fun onShowing(previousData: Any?) {
        launchBrowseRom()
    }

    override fun getTitleId(): Int {
        return R.string.browseRomTitle
    }

    override fun getControllerData(): Any? {
        return if (mSelectedFileName == null) {
            null
        } else FinishWizardData(mSelectedFileName)
    }

    private fun launchBrowseRom() {
        val setupBundle = Bundle()
        setupBundle.putString(IntentConstants.EXTENSION_EXTRA_REGEX, "\\.(rom|sav)")
        setupBundle.putString(
            IntentConstants.BROWSE_DESCRIPTION_EXTRA_STRING,
            mContext.resources.getString(R.string.browseRomDescription)
        )
        val fragInfo = BrowseFragment()
        fragInfo.setCallback(mBrowseCallback)
        fragInfo.arguments = setupBundle
        val transaction = mFragmentManager.beginTransaction()
        transaction.replace(R.id.browse_rom_page, fragInfo)
        transaction.commit()
        Log.d("BrowseRomPageController", "Launched browse rom page")
    }
}