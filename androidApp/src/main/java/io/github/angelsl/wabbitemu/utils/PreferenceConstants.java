package io.github.angelsl.wabbitemu.utils;

public enum PreferenceConstants {
	STAY_AWAKE("alwaysAwake"),
	AUTO_TURN_ON("autoTurnOn"),
	ROM_PATH("romPath"),
	ROM_MODEL("romModel"),
	FIRST_RUN("firstRun"),
	FACEPLATE_COLOR("faceplateColor"),
	USE_VIBRATION("useVibration"),
	CORRECT_SCREEN_RATIO("correctScreenRatio"),
	LARGE_SCREEN("largeScreen"),
	IMMERSIVE_MODE("useImmersiveMode"),
	FULL_SKIN_SIZE("maximizeSkin");
	
	private String mKey;
	
	private PreferenceConstants(String key) {
		mKey = key;
	}
	
	@Override
	public String toString() {
		return mKey;
	}
}
