package io.github.angelsl.wabbitemu.calc;

import android.net.Uri;

import java.nio.IntBuffer;

class CalcInterface {

	static {
		System.loadLibrary("Wabbitemu");
	}

	public static final int NO_CALC = -1;
	public static final int TI_81 = 0;
	public static final int TI_82 = 1;
	public static final int TI_83 = 2;
	public static final int TI_85 = 3;
	public static final int TI_86 = 4;
	public static final int TI_73 = 5;
	public static final int TI_83P = 6;
	public static final int TI_83PSE = 7;
	public static final int TI_84P = 8;
	public static final int TI_84PSE = 9;
	public static final int TI_84PCSE = 10;

	public static final int ON_KEY_BIT = 0;
	public static final int ON_KEY_GROUP = 5;

	public static native void Initialize(String filePath);
	public static native int LoadFile(String filePath);
//	public static native int LoadFile(Uri uri);
	public static native boolean SaveCalcState(String filePath);
	public static native int CreateRom(String osPath, String bootPath, String romPath, int model);
	public static native void ResetCalc();
	public static native void RunCalcs();
	public static native void PauseCalc();
	public static native void UnpauseCalc();
	public static native int GetModel();
	public static native long Tstates();
	public static native void SetSpeedCalc(int speed);
	public static native void PressKey(int group, int bit);
	public static native void ReleaseKey(int group, int bit);
	public static native void SetAutoTurnOn(boolean autoTurnOn);
	public static native int GetLCD(IntBuffer buffer);
}
