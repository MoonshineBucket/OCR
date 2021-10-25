package com.github.dreamsmoke.ocr.util;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    public static final List<String> FIND_GAME_LINE_LIST = new ArrayList<>(),
            WINDOW_NAME_LIST = new ArrayList<>();

    public static int WIDTH, HEIGHT;

    public static ActionType ACTION_TYPE;

    public static long ACTION_REPEAT;
    public static boolean ACTION_ENABLE;

    public static long SCROLLING_REPEAT;
    public static int SCROLLING_LENGTH, CURRENT_SLOT = 0;
    public static boolean SCROLLING_ENABLE;

    public static int ACTION_HOTKEY, SCROLLING_HOTKEY;
    public static boolean ACTION_HOTKEY_ENABLE, SCROLLING_HOTKEY_ENABLE;

    public static float VOLUME;

    public static boolean MOUSE_ACTIVE, OCR;

    public enum ActionType {
        BLOCK_BREAK("Ломать (зажим)"),
        SHOVEL("Ломать (быстро)"),
        BLOCK_PLACE("Ставить блок"),
        BOW("Стрелять луком"),
        LEFT_CLICK("ЛКМ");

        String title;

        ActionType(String string) {
            title = string;
        }

        @Override
        public String toString() {
            return title;
        }

    }

}
