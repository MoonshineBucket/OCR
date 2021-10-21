package com.github.dreamsmoke.ocr.gui;

import com.github.dreamsmoke.ocr.configuration.OCRConfiguration;
import com.github.dreamsmoke.ocr.gui.notification.Notifications;
import com.github.dreamsmoke.ocr.update.Updater;
import com.github.dreamsmoke.ocr.util.Settings;
import com.github.dreamsmoke.ocr.util.Util;
import com.github.dreamsmoke.ocr.util.io.IOUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class OCRController {

    public static OCRController INSTANCE;

    @FXML
    private TextArea game_line_pattern;

    @FXML
    private TextArea window_name_pattern;

    @FXML
    private ComboBox<Settings.ActionType> action_type;

    @FXML
    private TextField action_repeat;

    @FXML
    private Slider scrollable;

    @FXML
    private TextField scrolling_repeat;

    @FXML
    public TextField action_hotkey;

    @FXML
    public TextField scrolling_hotkey;

    @FXML
    private Slider volume;

    @FXML
    private Button download_texture;

    @FXML
    private Hyperlink link_lfg;

    @FXML
    private Hyperlink link_support;

    @FXML
    private Hyperlink link_developer;

    @FXML
    private CheckBox ocr_enable;

    @FXML
    void initialize() {
        INSTANCE = this;

        OCRConfiguration.INSTANCE.updateConfig();
        bindTooltip(game_line_pattern, new Tooltip("Список искомых в игре слов, * - искать все."));
        game_line_pattern.setText(listToString(Settings.FIND_GAME_LINE_LIST));
        game_line_pattern
                .textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    updateListFromString(Settings.FIND_GAME_LINE_LIST, newValue);
                    OCRConfiguration.INSTANCE.saveConfig();

                    // чистим кэши, фильтр поиска был обновлен, мы вынуждены это сделать
                    Util.CACHED_SCREEN_MAP.clear();
                });

        bindTooltip(window_name_pattern, new Tooltip("Список окон для поиска текста."));
        window_name_pattern.setText(listToString(Settings.WINDOW_NAME_LIST));
        window_name_pattern
                .textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    updateListFromString(Settings.WINDOW_NAME_LIST, newValue);
                    OCRConfiguration.INSTANCE.saveConfig();
                });

        action_type.getItems().addAll(Settings.ActionType.values());
        action_type.setValue(Settings.ACTION_TYPE);

        action_type
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    Settings.ACTION_TYPE = newValue;
                    OCRConfiguration.INSTANCE.saveConfig();
                });

        setTextFormatter(action_repeat, "[0-9]*");
        action_repeat.setText(((Long) Math.max(25, Settings.ACTION_REPEAT)).toString());
        action_repeat
                .textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if(newValue.length() > 0) {
                        Settings.ACTION_REPEAT = Long.parseLong(newValue);
                    }
                });

        action_repeat
                .focusedProperty()
                .addListener((observable, oldValue, newValue) -> {
            if(oldValue) {
                long value;

                try {
                    value = Math.max(25, Long.parseLong(action_repeat.getText()));
                } catch (Throwable throwable) {
                    value = 25;
                }

                action_repeat.setText(((Long) value).toString());
                Settings.ACTION_REPEAT = value;

                OCRConfiguration.INSTANCE.saveConfig();
            }
        });

        scrollable.setMin(1);
        scrollable.setMax(9);

        Tooltip tooltip = bindTooltip(scrollable, new Tooltip(String.format("%s слот(ов)", Settings.SCROLLING_LENGTH)));
        scrollable.setValue(Settings.SCROLLING_LENGTH);
        scrollable
                .valueProperty()
                .addListener((observable, oldValue, newValue) -> {
                    scrollable.setValue(Settings.SCROLLING_LENGTH = (int) Math.ceil((Double) newValue));
                    tooltip.setText(String.format("%s слот(ов)", Settings.SCROLLING_LENGTH));
                    OCRConfiguration.INSTANCE.saveConfig();
                });

        setTextFormatter(scrolling_repeat, "[0-9]*");
        scrolling_repeat.setText(((Long) Settings.SCROLLING_REPEAT).toString());
        scrolling_repeat
                .textProperty()
                .addListener((observable, oldValue, newValue) -> {
                    Settings.SCROLLING_REPEAT = Long.parseLong(newValue);
                    OCRConfiguration.INSTANCE.saveConfig();
                });

        action_hotkey.setEditable(false);
        action_hotkey.setText(KeyEvent.getKeyText(Settings.ACTION_HOTKEY));
        action_hotkey.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue) {
                if(Settings.ACTION_HOTKEY_ENABLE && action_hotkey.getText().equals("Press any key...")) {
                    action_hotkey.setText(KeyEvent.getKeyText(Settings.ACTION_HOTKEY));
                    Settings.ACTION_HOTKEY_ENABLE = false;
                }
            } else {
                action_hotkey.setText("Press any key...");
                Settings.ACTION_HOTKEY_ENABLE = true;
            }
        });

        scrolling_hotkey.setEditable(false);
        scrolling_hotkey.setText(KeyEvent.getKeyText(Settings.SCROLLING_HOTKEY));
        scrolling_hotkey.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue) {
                if(Settings.SCROLLING_HOTKEY_ENABLE && scrolling_hotkey.getText().equals("Press any key...")) {
                    scrolling_hotkey.setText(KeyEvent.getKeyText(Settings.SCROLLING_HOTKEY));
                    Settings.SCROLLING_HOTKEY_ENABLE = false;
                }
            } else {
                scrolling_hotkey.setText("Press any key...");
                Settings.SCROLLING_HOTKEY_ENABLE = true;
            }
        });

        volume.setMin(0);
        volume.setMax(100);

        Tooltip volumeTooltip = bindTooltip(volume, new Tooltip(String.format("%s%s", Settings.VOLUME, "%")));
        volume.setValue(Settings.VOLUME);
        volume
                .valueProperty()
                .addListener((observable, oldValue, newValue) -> {
                    volume.setValue(Settings.VOLUME = (int) Math.ceil((Double) newValue));
                    volumeTooltip.setText(String.format("%s%s", Settings.VOLUME, "%"));
                    OCRConfiguration.INSTANCE.saveConfig();
                });

        OCRConfiguration.INSTANCE.saveConfig();

        ocr_enable.setSelected(false);
        bindTooltip(ocr_enable, new Tooltip("Нагружает процессор."));
        ocr_enable
                .selectedProperty()
                .addListener((observable, oldValue, newValue) ->
                        Settings.OCR = newValue);

        download_texture.setOnMouseClicked(event -> {
            download_texture.setDisable(true);
            new Thread(() -> {
                try {
                    Updater.Version[] versions = Util.getVersions();
                    if(versions != null) {
                        String[] currentVersion = Util.VERSION.split("\\.");
                        label: for(Updater.Version version : versions) {
                            System.out.println(version.tag_name);

                            String[] strings = version.tag_name
                                    .toLowerCase().trim()
                                    .replace("version", "")
                                    .split("\\.");

                            if(Util.verifyVersions(strings, currentVersion, true)) {
                                for(Updater.Asset asset : version.assets) {
                                    if(asset.name.startsWith("ResourcePack") || asset.name.endsWith(".zip")) {
                                        Notifications.notify("Началась загрузка текстурпака с репозитория, пожалуйста подождите...");
                                        IOUtil.downloadFile(asset.browser_download_url, new File(String.format("%s/Downloads/%s", System.getProperty("user.home"), asset.name)).toPath());
                                        Notifications.notify("Текстурпак находится в директории загрузок, приятной игры!");
                                        break label;
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    Platform.runLater(() -> download_texture.setDisable(false));
                }
            }, "ResourceDownloader").start();
        });

        // приятности
        link_developer.setOnMouseClicked(event -> viewURL("https://vk.com/moonshinebucket"));
        link_support.setOnMouseClicked(event -> viewURL("https://vk.com/id248005111"));
        link_lfg.setOnMouseClicked(event -> viewURL("https://discord.gg/f5BXKTBgDC"));
    }

    String listToString(List<String> stringList) {
        StringBuilder stringBuilder = new StringBuilder();
        for(String string : stringList) {
            if(stringBuilder.length() > 0) {
                stringBuilder.append("\n");
            }

            stringBuilder.append(string);
        }

        return stringBuilder.toString();
    }

    void updateListFromString(List<String> stringList, String string) {
        stringList.clear();
        for(String s : string.split("\n")) {
            stringList.add(s);
        }
    }

    Tooltip bindTooltip(final Node node, final Tooltip tooltip) {
        node.setOnMouseMoved(event -> tooltip.show(node, event.getScreenX() + 16, event.getScreenY() - 6));
        node.setOnMouseExited(event -> tooltip.hide());
        return tooltip;
    }

    void setTextFormatter(TextField textField, String matches) {
        textField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if(text.matches(matches)) {
                return change;
            }

            return null;
        }));
    }

    void viewURL(String string) {
        if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URL(string).toURI());
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

}