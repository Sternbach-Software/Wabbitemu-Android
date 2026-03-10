package io.github.angelsl.wabbitemu.calc.variants

import android.view.KeyEvent

class TI84PSELayout : CalcLayout {
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
    override val keys: List<List<Int?>> = listOf(
        listOf(
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_F3,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_F5,
        ),
        listOf(
            null,
            null,
            null,
            null,
            KeyEvent.KEYCODE_DPAD_UP,
        ),
        listOf(
            KeyEvent.KEYCODE_SHIFT_LEFT,//2nd
            KeyEvent.KEYCODE_ESCAPE,//quit/mode
            KeyEvent.KEYCODE_DEL,//delete/ins
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
        ),
        listOf(
            KeyEvent.KEYCODE_ALT_LEFT,//Alpha
            KeyEvent.KEYCODE_EQUALS,//Default var
            KeyEvent.KEYCODE_SWITCH_CHARSET,//stat, they both start with s, so why not?
            null,
            KeyEvent.KEYCODE_DPAD_DOWN
        ),
        listOf(
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_PAGE_DOWN, //Vars
            KeyEvent.KEYCODE_CLEAR,
        ),
        listOf(
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_G,
            KeyEvent.KEYCODE_H,
        ),
        listOf(
            KeyEvent.KEYCODE_I,
            KeyEvent.KEYCODE_J,
            KeyEvent.KEYCODE_K,
            KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_M,
        ),
        listOf(
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_R,
        ),
        listOf(
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_W,
        ),
        listOf(
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_Z,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_PLUS,
        ),
        listOf(
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_MINUS,
            KeyEvent.KEYCODE_ENTER,
        ),
    )
}