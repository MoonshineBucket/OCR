package com.github.dreamsmoke.ocr.configuration;

import com.github.dreamsmoke.api.configuration.IConfiguration;
import com.github.dreamsmoke.ocr.OCR;
import com.github.dreamsmoke.ocr.plugin.JSPlugin;
import com.github.dreamsmoke.ocr.util.Settings;
import com.github.dreamsmoke.ocr.util.io.IOUtil;

import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class OCRConfiguration extends IConfiguration {

    public static final OCRConfiguration INSTANCE = new OCRConfiguration();

    @Override
    public File getFile() {
        return new File(IOUtil.DATA_FOLDER, "config.yml");
    }

    @Override
    protected void loadBody() {
        Settings.ActionType actionType;

        try {
            actionType = Settings.ActionType.values()[configuration.getInt("getSettings.getAction", 2)];
        } catch (Throwable throwable) {
            actionType = Settings.ActionType.BLOCK_PLACE;
        }

        Settings.ACTION_TYPE = actionType;
        Settings.ACTION_REPEAT = configuration.getLong("getSettings.getActionRepeat", 30L);

        Settings.SCROLLING_REPEAT = configuration.getLong("getSettings.getScrollingRepeat", 4000L);
        Settings.SCROLLING_LENGTH = Math.max(1, Math.min(9, configuration.getInt("getSettings.getScrollingLength", 9)));

        stringArrayToList(Settings.FIND_GAME_LINE_LIST,
                configuration.getString("getSettings.getFindLines",
                "Сетевая игра, Настройки, Выйти из игры").split(", "));
        stringArrayToList(Settings.WINDOW_NAME_LIST,
                configuration.getString("getSettings.getWindowNames",
                "MineSweet.ru, Minecraft 1.6.4").split(", "));

        Settings.ACTION_HOTKEY = configuration.getInt("getSettings.getActionHotkey", KeyEvent.VK_F6);
        Settings.SCROLLING_HOTKEY = configuration.getInt("getSettings.getScrollingHotkey", KeyEvent.VK_F7);

        Settings.VOLUME = (float) configuration.getDouble("getSettings.getVolume", 100);
    }

    @Override
    public void saveConfig() {
        configuration.set("getSettings.getAction", Settings.ACTION_TYPE.ordinal());
        configuration.set("getSettings.getActionRepeat", Settings.ACTION_REPEAT);

        configuration.set("getSettings.getScrollingRepeat", Settings.SCROLLING_REPEAT);
        configuration.set("getSettings.getScrollingLength", Settings.SCROLLING_LENGTH);

        configuration.set("getSettings.getFindLines", listToString(Settings.FIND_GAME_LINE_LIST));
        configuration.set("getSettings.getWindowNames", listToString(Settings.WINDOW_NAME_LIST));

        configuration.set("getSettings.getActionHotkey", Settings.ACTION_HOTKEY);
        configuration.set("getSettings.getScrollingHotkey", Settings.SCROLLING_HOTKEY);

        configuration.set("getSettings.getVolume", Settings.VOLUME);

        JSPlugin.INSTANCE.runPlugins("save", OCR.INSTANCE, configuration);
        super.saveConfig();
    }

    String listToString(List<String> stringList) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String string : stringList) {
            if(stringBuilder.length() > 0) {
                stringBuilder.append(", ");
            }

            stringBuilder.append(string);
        }

        return stringBuilder.toString();
    }

    void stringArrayToList(List<String> stringList, String... strings) {
        stringList.clear();
        for(String string : strings) {
            stringList.add(string);
        }
    }

}
