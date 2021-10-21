package com.github.dreamsmoke.ocr.util;

public class GameLine {

    public String line;
    public int posX, posY, width, height;

    public GameLine(String string, int x, int y, int w, int h) {
        line = string; posX = x; posY = y; width = w; height = h;
    }

}
