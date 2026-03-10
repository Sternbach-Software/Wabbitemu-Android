package io.github.angelsl.wabbitemu;

import android.view.KeyEvent;

import io.github.angelsl.wabbitemu.calc.CalculatorManager;
import io.github.angelsl.wabbitemu.utils.KeyMapping;

import java.util.ArrayList;

import javax.annotation.Nullable;

public class CalcKeyManager {

	public static final KeyMapping[] KEY_MAPPINGS = {
		new KeyMapping(KeyEvent.KEYCODE_DPAD_DOWN, 0, 0),
		new KeyMapping(KeyEvent.KEYCODE_DPAD_LEFT, 0, 1),
		new KeyMapping(KeyEvent.KEYCODE_DPAD_RIGHT, 0, 2),
		new KeyMapping(KeyEvent.KEYCODE_DPAD_UP, 0, 3),
		new KeyMapping(KeyEvent.KEYCODE_ENTER, 1, 0),
		new KeyMapping(KeyEvent.KEYCODE_AT, 5, 0),
		new KeyMapping(KeyEvent.KEYCODE_A, 5, 6),
		new KeyMapping(KeyEvent.KEYCODE_B, 4, 6),
		new KeyMapping(KeyEvent.KEYCODE_C, 3, 6),
		new KeyMapping(KeyEvent.KEYCODE_D, 5, 5),
		new KeyMapping(KeyEvent.KEYCODE_E, 4, 5),
		new KeyMapping(KeyEvent.KEYCODE_F, 3, 5),
		new KeyMapping(KeyEvent.KEYCODE_G, 2, 5),
		new KeyMapping(KeyEvent.KEYCODE_H, 1, 5),
		new KeyMapping(KeyEvent.KEYCODE_I, 5, 4),
		new KeyMapping(KeyEvent.KEYCODE_J, 4, 4),
		new KeyMapping(KeyEvent.KEYCODE_K, 3, 4),
		new KeyMapping(KeyEvent.KEYCODE_L, 2, 4),
		new KeyMapping(KeyEvent.KEYCODE_M, 1, 4),
		new KeyMapping(KeyEvent.KEYCODE_N, 5, 3),
		new KeyMapping(KeyEvent.KEYCODE_O, 4, 3),
		new KeyMapping(KeyEvent.KEYCODE_P, 3, 3),
		new KeyMapping(KeyEvent.KEYCODE_Q, 2, 3),
		new KeyMapping(KeyEvent.KEYCODE_R, 1, 3),
		new KeyMapping(KeyEvent.KEYCODE_S, 5, 2),
		new KeyMapping(KeyEvent.KEYCODE_T, 4, 2),
		new KeyMapping(KeyEvent.KEYCODE_U, 3, 2),
		new KeyMapping(KeyEvent.KEYCODE_V, 2, 2),
		new KeyMapping(KeyEvent.KEYCODE_W, 1, 2),
		new KeyMapping(KeyEvent.KEYCODE_X, 5, 1),
		new KeyMapping(KeyEvent.KEYCODE_Y, 4, 1),
		new KeyMapping(KeyEvent.KEYCODE_Z, 3, 1),
		new KeyMapping(KeyEvent.KEYCODE_SPACE, 4, 0),
		new KeyMapping(KeyEvent.KEYCODE_0, 4, 0),
		new KeyMapping(KeyEvent.KEYCODE_1, 4, 1),
		new KeyMapping(KeyEvent.KEYCODE_2, 3, 1),
		new KeyMapping(KeyEvent.KEYCODE_3, 2, 1),
		new KeyMapping(KeyEvent.KEYCODE_4, 4, 2),
		new KeyMapping(KeyEvent.KEYCODE_5, 3, 2),
		new KeyMapping(KeyEvent.KEYCODE_6, 2, 2),
		new KeyMapping(KeyEvent.KEYCODE_7, 4, 3),
		new KeyMapping(KeyEvent.KEYCODE_8, 3, 3),
		new KeyMapping(KeyEvent.KEYCODE_9, 2, 3),
		new KeyMapping(KeyEvent.KEYCODE_PERIOD, 3, 0),
		new KeyMapping(KeyEvent.KEYCODE_COMMA, 4, 4),
		new KeyMapping(KeyEvent.KEYCODE_PLUS, 1, 1),
		new KeyMapping(KeyEvent.KEYCODE_MINUS, 1, 2),
		new KeyMapping(KeyEvent.KEYCODE_STAR, 2, 3),
		new KeyMapping(KeyEvent.KEYCODE_SLASH, 1, 4),
		new KeyMapping(KeyEvent.KEYCODE_LEFT_BRACKET, 3, 4),
		new KeyMapping(KeyEvent.KEYCODE_RIGHT_BRACKET, 2, 4),
		new KeyMapping(KeyEvent.KEYCODE_SHIFT_LEFT, 6, 5),//2nd
		new KeyMapping(KeyEvent.KEYCODE_SHIFT_RIGHT, 1, 6),
		new KeyMapping(KeyEvent.KEYCODE_ALT_LEFT, 5, 7),
		new KeyMapping(KeyEvent.KEYCODE_EQUALS, 4, 7),
			/*
			{N, 6, 5}, // 2nd
			{TAB, 6, 5}, // 2nd
			{ESCAPE, 6, 6}, // Mode
			{DELETE, 6, 7}, // Delete
			{FORWARD_DELETE, 6, 7}, // Delete
			{A, 5, 7}, // Alpha
			{TILDE, 5, 7}, //Alpha
			{EQUAL, 4, 7}, // Default Var
			{NUMPAD_EQUAL, 4, 7}, // Default Var
			{S, 3, 7}, // Stat
			{M, 5, 6}, // Math
			{HOME, 5, 6}, // Math
			{H, 4, 6}, // Apps
			{END, 4, 6}, // Apps
			{J, 3, 6}, // Prgm
			{PAGE_UP, 3, 6}, // Prgm
			{K, 2, 6}, // Vars
			{PAGE_DOWN, 2, 6}, // Vars
			{C, 1, 6}, // Clear
			{CLEAR, 1, 6}, // Clear
			{V, 5, 5}, // Inverse
			{I, 4, 5}, // Sin
			{O, 3, 5}, // Cos
			{T, 2, 5}, // Tan
			{P, 1, 5}, // Power
			{Q, 5, 4}, // Square
			{COMMA, 4, 4}, // Comma
			{LBRACKET, 3, 4}, // (
			{RBRACKET, 2, 4}, // )
			{DIVIDE, 1, 4}, // Divide
			{BACKSLASH, 1, 4}, // Divide
			{G, 5, 3}, // Log
			{SEVEN, 4, 3}, // 7
			{NUMPAD_SEVEN, 4, 3}, // 7
			{EIGHT, 3, 3}, // 8
			{NUMPAD_EIGHT, 3, 3}, // 8
			{NINE, 2, 3}, // 9
			{NUMPAD_NINE, 2, 3}, // 9
			{U, 1, 3}, // Multiply
			{MULTIPLY, 1, 3}, // Multiply
			{L, 5, 2}, // Ln
			{FOUR, 4, 2}, // 4
			{NUMPAD_FOUR, 4, 2}, // 4
			{FIVE, 3, 2}, // 5
			{NUMPAD_FIVE, 3, 2}, // 5
			{SIX, 2, 2}, // 6
			{NUMPAD_SIX, 2, 2}, // 6
			{MINUS, 1, 2}, // Subtract
			{SUBTRACT, 1, 2}, // Subtract
			{X, 5, 1}, // Sto
			{ONE, 4, 1}, // 1
			{NUMPAD_ONE, 4, 1}, // 1
			{TWO, 3, 1}, // 2
			{NUMPAD_TWO, 3, 1}, // 2
			{THREE, 2, 1}, // 3
			{NUMPAD_THREE, 2, 1}, // 3
			{D, 1, 1}, // Add
			{ADD, 1, 1}, // Add
			{O, 5, 0}, // Power On/Off
			{FUNCTION6, 5, 0}, // Power On/Off
			{ZERO, 4, 0}, // 0
			{NUMPAD_ZERO, 4, 0}, // 0
			{PERIOD, 3, 0}, // Decimal Point
			{DECIMAL, 3, 0}, // Decimal Point
			{E, 2, 0}, // Negate
			{ENTER, 1, 0}, // Enter
			{RETURN, 1, 0}, // Enter
			{FUNCTION1, 6, 4}, // Y=
			{FUNCTION2, 6, 3}, // Window
			{FUNCTION3, 6, 2}, // Zoom
			{FUNCTION4, 6, 1}, // Trace
			{FUNCTION5, 6, 0}, // Graph
			{UP, 0, 3}, // Up Arrow
			{DOWN, 0, 0}, // Down Arrow
			{LEFT, 0, 1}, // Left Arrow
			{RIGHT, 0, 2} // Right Arrow*/
			new KeyMapping(KeyEvent.KEYCODE_F1, 6,4),
			new KeyMapping(KeyEvent.KEYCODE_F2, 6,3),
			new KeyMapping(KeyEvent.KEYCODE_F3, 6,2),
			new KeyMapping(KeyEvent.KEYCODE_F4, 6,1),
			new KeyMapping(KeyEvent.KEYCODE_F5, 6,0),
			new KeyMapping(KeyEvent.KEYCODE_ESCAPE, 6,0),
			new KeyMapping(KeyEvent.KEYCODE_POWER, 5,0),
			new KeyMapping(KeyEvent.KEYCODE_DEL, 6,7),
		/*
		 * { VK_F1 , 6 , 4 }, { VK_F2 , 6 , 3 }, { VK_F3 , 6 , 2 }, { VK_F4
		 * , 6 , 1 }, { VK_F5 , 6 , 0 },
		 */
	};

	private static final CalcKeyManager INSTANCE = new CalcKeyManager();

	public static CalcKeyManager getInstance() {
		return INSTANCE;
	}

	private final ArrayList<KeyMapping> mKeysDown = new ArrayList<>();

	/**
	 * @param id doesn't matter what it is, just that it is unique at any given time. It can be
	 *              the pointer id of the ActionEvent, or the id of a view.
	 * */
	public void doKeyDown(final int id, final int group, final int bit) {
		CalculatorManager.getInstance().pressKey(group, bit);

		mKeysDown.add(new KeyMapping(id, group, bit));
	}

	public boolean doKeyDownKeyCode(int keyCode) {
		final KeyMapping mapping = CalcKeyManager.getKeyMapping(keyCode);
		if (mapping == null) {
			return false;
		}

		doKeyDown(keyCode, mapping.getGroup(), mapping.getBit());
		return true;
	}

	public void doKeyUp(final int id) {
		KeyMapping mapping = null;
		for (int i = 0; i < mKeysDown.size(); i++) {
			final KeyMapping possibleMapping = mKeysDown.get(i);
			if (possibleMapping != null && possibleMapping.getKey() == id) {
				mapping = mKeysDown.get(i);
			}
		}

		if (mapping == null) {
			return;
		}

		final int group = mapping.getGroup();
		final int bit = mapping.getBit();
		CalculatorManager.getInstance().releaseKey(group, bit);
		mKeysDown.remove(mapping);
	}

	public boolean doKeyUpKeyCode(final int keyCode) {
		final KeyMapping mapping = CalcKeyManager.getKeyMapping(keyCode);
		if (mapping == null) {
			return false;
		}

		doKeyUp(keyCode);
		return true;
	}

	@Nullable
	private static KeyMapping getKeyMapping(int keyCode) {
		for (final KeyMapping mapping : KEY_MAPPINGS) {
			if (mapping.getKey() == keyCode) {
				return mapping;
			}
		}

		return null;
	}
}
