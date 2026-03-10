package io.github.angelsl.wabbitemu.calc.variants

import android.view.KeyEvent

interface CalcLayout {
//    @android.hardware.input.VirtualKeyEvent.SupportedKeycode
    /**
     * Each inner list is a row of buttons. If an element in an inner list is null,
     * it indicates that there should be a blank space instead of that element
     * */
    val keys: List<List<Int?>>/*row-column*/
}