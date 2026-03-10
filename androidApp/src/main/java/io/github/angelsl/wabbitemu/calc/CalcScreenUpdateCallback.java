package io.github.angelsl.wabbitemu.calc;

import java.nio.IntBuffer;

public interface CalcScreenUpdateCallback {

	void onUpdateScreen();

	IntBuffer getScreenBuffer();
}
