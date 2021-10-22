package com.github.dreamsmoke.ocr.gui;

import com.github.dreamsmoke.ocr.OCR;
import com.github.dreamsmoke.ocr.gui.notification.Notifications;
import com.github.dreamsmoke.ocr.update.Updater;
import com.github.dreamsmoke.ocr.util.Util;
import com.github.dreamsmoke.ocr.util.io.IOUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class UpdateController {

    public static UpdateController INSTANCE;

    Updater.Update update;

    @FXML
    private Label version;

    @FXML
    private TextArea description;

    @FXML
    private Button confirm;

    @FXML
    private Button close;

    @FXML
    void initialize() {
        INSTANCE = this;
    }

    public void update(Updater.Update u, Stage primaryStage) {
        update = u;

        version.setText(update.version);

        description.setText(update.description);
        description.setEditable(false);

        confirm.setOnMouseClicked(event -> {
            try {
                boolean isJarFile = IOUtil.hasExtension(Util.BINARY_PATH, ".jar");
                if(isJarFile) {
                    Notifications.notify("Началось обновление приложения, пожалуйста подождите...");
                    update();
                } else {
                    Util.viewURL(update.url);
                    shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();

                open(primaryStage);
            }
        });

        close.setOnMouseClicked(event -> open(primaryStage));
    }

    void update() throws IOException {
        List<String> args = new ArrayList<>(3);
        args.add(IOUtil.resolveJavaBin().toString());
        args.add("-jar");
        args.add(Util.BINARY_PATH.toString());

        ProcessBuilder builder = new ProcessBuilder(args.toArray(new String[args.size()]));
        builder.inheritIO();

        for(Updater.Asset asset : update.remoteFiles) {
            System.out.println(asset.name);

            if(!asset.name.endsWith(".jar")) {
                continue;
            }

            IOUtil.downloadFile(asset.browser_download_url, Util.C_BINARY_PATH);
            try (InputStream inputStream = IOUtil.newInput(Util.C_BINARY_PATH)) {
                IOUtil.transfer(inputStream, Util.BINARY_PATH);
            } finally {
                Files.deleteIfExists(Util.C_BINARY_PATH);
            }
        }

        Notifications.notify("Приложение успешно обновлено, перезагружаем его!");
        builder.start(); shutdown();
    }

    void shutdown() {
        // Kill current instance
        Runtime.getRuntime().exit(255);
        throw new AssertionError("Why OCR wasn't restarted?!");
    }

    void open(Stage primaryStage) {
        try {
            primaryStage.hide();
            OCR.INSTANCE.start(primaryStage);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
