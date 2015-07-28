package com.pplive.epg.boxcontroller;

import java.util.HashMap;

public class KeyCode {
    // =================================================================
    public final static int SUNING_TV_KEYCODE_DPAD_DOWN = 28;
    public final static int SUNING_TV_KEYCODE_DPAD_UP = 26;
    public final static int SUNING_TV_KEYCODE_DPAD_LEFT = 25;
    public final static int SUNING_TV_KEYCODE_DPAD_RIGHT = 27;
    public final static int SUNING_TV_KEYCODE_DPAD_CENTER = 0x0D;
    public final static int SUNING_TV_KEYCODE_RETURN = 0x1B;
    public final static int SUNING_TV_KEYCODE_HOME = 49;
    public final static int SUNING_TV_KEYCODE_MENU = 48;
    private final static int KEYCODE_0 = 7;
    private final static int KEYCODE_1 = 8;
    private final static int KEYCODE_2 = 9;
    private final static int KEYCODE_3 = 10;
    private final static int KEYCODE_4 = 11;
    private final static int KEYCODE_5 = 12;
    private final static int KEYCODE_6 = 13;
    private final static int KEYCODE_7 = 14;
    private final static int KEYCODE_8 = 15;
    private final static int KEYCODE_9 = 16;
    private final static int KEYCODE_A = 29;
    private final static int KEYCODE_B = 30;
    private final static int KEYCODE_C = 31;
    private final static int KEYCODE_D = 32;
    private final static int KEYCODE_E = 33;
    private final static int KEYCODE_F = 34;
    private final static int KEYCODE_G = 35;
    private final static int KEYCODE_H = 36;
    private final static int KEYCODE_I = 37;
    private final static int KEYCODE_J = 38;
    private final static int KEYCODE_K = 39;
    private final static int KEYCODE_L = 40;
    private final static int KEYCODE_M = 41;
    private final static int KEYCODE_N = 42;
    private final static int KEYCODE_O = 43;
    private final static int KEYCODE_P = 44;
    private final static int KEYCODE_Q = 45;
    private final static int KEYCODE_R = 46;
    private final static int KEYCODE_S = 47;
    private final static int KEYCODE_T = 48;
    private final static int KEYCODE_U = 49;
    private final static int KEYCODE_V = 50;
    private final static int KEYCODE_W = 51;
    private final static int KEYCODE_X = 52;
    private final static int KEYCODE_Y = 53;
    private final static int KEYCODE_Z = 54;
    private final static int KEYCODE_PERIOD = 56;
    private final static int KEYCODE_NUMPAD_DIVIDE = 154;
    private final static int KEYCODE_NUMPAD_MULTIPLY = 155;
    private final static int KEYCODE_NUMPAD_SUBTRACT = 156;
    private final static int KEYCODE_NUMPAD_ADD = 157;
    /**
     * 获取InputKeys
     * 
     * @return HashMap
     */
    public static HashMap<Character, Integer> getInputKeys() {
        HashMap<Character, Integer> keys = new HashMap<Character, Integer>();
        keys.put('0', KEYCODE_0);
        keys.put('1', KEYCODE_1);
        keys.put('2', KEYCODE_2);
        keys.put('3', KEYCODE_3);
        keys.put('4', KEYCODE_4);
        keys.put('5', KEYCODE_5);
        keys.put('6', KEYCODE_6);
        keys.put('7', KEYCODE_7);
        keys.put('8', KEYCODE_8);
        keys.put('9', KEYCODE_9);
        keys.put('/', KEYCODE_NUMPAD_DIVIDE);
        keys.put('*', KEYCODE_NUMPAD_MULTIPLY);
        keys.put('-', KEYCODE_NUMPAD_SUBTRACT);
        keys.put('+', KEYCODE_NUMPAD_ADD);
        keys.put('.', KEYCODE_PERIOD);
        keys.put('A', KEYCODE_A);
        keys.put('B', KEYCODE_B);
        keys.put('C', KEYCODE_C);
        keys.put('D', KEYCODE_D);
        keys.put('E', KEYCODE_E);
        keys.put('F', KEYCODE_F);
        keys.put('G', KEYCODE_G);
        keys.put('H', KEYCODE_H);
        keys.put('I', KEYCODE_I);
        keys.put('J', KEYCODE_J);
        keys.put('K', KEYCODE_K);
        keys.put('L', KEYCODE_L);
        keys.put('M', KEYCODE_M);
        keys.put('N', KEYCODE_N);
        keys.put('O', KEYCODE_O);
        keys.put('P', KEYCODE_P);
        keys.put('Q', KEYCODE_Q);
        keys.put('R', KEYCODE_R);
        keys.put('S', KEYCODE_S);
        keys.put('T', KEYCODE_T);
        keys.put('U', KEYCODE_U);
        keys.put('V', KEYCODE_V);
        keys.put('W', KEYCODE_W);
        keys.put('X', KEYCODE_X);
        keys.put('Y', KEYCODE_Y);
        keys.put('Z', KEYCODE_Z);

        return keys;

    }

}
