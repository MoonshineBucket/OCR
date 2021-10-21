package com.github.dreamsmoke.ocr.util;

import java.util.ArrayList;
import java.util.List;

public class ChatColor {

    static final List<Integer> INTEGER_LIST = new ArrayList<>();

    static {
        for(Color color : Color.values()) {
            for(String string : color.hexArray) {
                java.awt.Color awtColor = hexToColor(string);
                if (awtColor == null) {
                    System.out.printf("Error while converting %s:%s color to rgb.%n",
                            color.name(), color.hexArray);
                    continue;
                }

                INTEGER_LIST.add(awtColor.getRGB());
            }

            continue;
        }
    }

    // https://stackoverflow.com/questions/4129666/how-to-convert-hex-to-rgb-using-java
    static java.awt.Color hexToColor(String hex) {
        switch (hex.length()) {
            case 6: {
                return new java.awt.Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16));
            }
            case 8: {
                return new java.awt.Color(
                        Integer.valueOf(hex.substring(0, 2), 16),
                        Integer.valueOf(hex.substring(2, 4), 16),
                        Integer.valueOf(hex.substring(4, 6), 16),
                        Integer.valueOf(hex.substring(6, 8), 16));
            }
        }

        return null;
    }

    public static boolean hasColor(int rgb) {
        return INTEGER_LIST.contains(rgb);
    }

    enum Color {
        WHITE("E0E0E0"/*, "FFFFFF"*/),
        GRAY("A0A0A0", "808080"),
        DARK_GRAY("303030"),
        DARK_RED("AA0000"),
        DARK_GREEN("00AA00"),
        DARK_PURPLE("AA00AA"),
        YELLOW("FFFFA0"),
        GOLD("FFAA00");

        String[] hexArray;

        Color(String... strings) {
            hexArray = strings;
        }
    }

}
