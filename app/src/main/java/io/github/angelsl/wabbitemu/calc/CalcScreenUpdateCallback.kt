package io.github.angelsl.wabbitemu.calc

import java.nio.IntBuffer

interface CalcScreenUpdateCallback {
    fun onUpdateScreen()
    val screenBuffer: IntBuffer?
}
