package io.github.angelsl.wabbitemu.wizard.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RadioGroup
import android.widget.RelativeLayout
import io.github.angelsl.wabbitemu.R
import io.github.angelsl.wabbitemu.utils.ViewUtils

class ModelPageView(context: Context?, attributeSet: AttributeSet?) :
    RelativeLayout(context, attributeSet) {
    private val mRadioGroup: RadioGroup

    init {
        LayoutInflater.from(context).inflate(R.layout.model_page, this, true)
        mRadioGroup = ViewUtils.findViewById(this, R.id.setupModelRadioGroup, RadioGroup::class.java)
    }

    val selectedRadioId: Int
        get() = mRadioGroup.checkedRadioButtonId
}
