package io.github.angelsl.wabbitemu.wizard.view

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.utils.ViewUtils

class OsPageView(context: Context?, attributeSet: AttributeSet?) :
    RelativeLayout(context, attributeSet) {
    val spinner: Spinner
    private val mRadioGroup: RadioGroup

    init {
        LayoutInflater.from(context).inflate(R.layout.os_page, this, true)

        val osTerms = ViewUtils.findViewById(this, R.id.osTerms, TextView::class.java)
        osTerms.movementMethod = LinkMovementMethod.getInstance()

        spinner = ViewUtils.findViewById(this, R.id.osVersionSpinner, Spinner::class.java)
        mRadioGroup = ViewUtils.findViewById(this, R.id.setupOsAcquisistion, RadioGroup::class.java)
    }

    val selectedRadioId: Int
        get() = mRadioGroup.checkedRadioButtonId
}
