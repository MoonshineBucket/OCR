package com.github.dreamsmoke.ocr.update;

import com.github.dreamsmoke.ocr.OCR;
import com.github.dreamsmoke.ocr.gui.UpdateController;
import com.github.dreamsmoke.ocr.gui.notification.Notifications;
import com.github.dreamsmoke.ocr.util.Util;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Arrays;

public class Updater extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.getIcons().add(new Image(getClass().getClassLoader().getResourceAsStream("assets/ocr.png")));
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.setResizable(false);

        Update update;
        if((update = checkUpdates()) != null) {
            Thread.currentThread().setName("OCRUpdater");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("assets/update.fxml"));

            primaryStage.setTitle(String.format("Updater v%s", Util.VERSION));

            Parent parent = fxmlLoader.load();
            UpdateController.INSTANCE.update(update, primaryStage);
            parent.setOnMouseClicked(event -> parent.requestFocus());
            primaryStage.setScene(new Scene(parent));
            primaryStage.show();
            return;
        }

        OCR.INSTANCE.start(primaryStage);
    }

    Update checkUpdates() {
        Version[] versions = Util.getVersions();
        if(versions == null) {
            return null;
        }

        System.out.printf("Program: %s.%n", Util.BINARY_PATH.toString());

        Update update = new Update();
        String[] currentVersion = Util.VERSION.split("\\.");
        for(Version version : versions) {
            System.out.println(version.tag_name);

            String[] strings = version.tag_name
                    .toLowerCase().trim()
                    .replace("version", "")
                    .split("\\.");

            if(Util.canUpdate(strings, currentVersion)) {
                currentVersion = strings;

                update.version = version.tag_name;
                update.remoteFiles = version.assets;
                update.description = version.body;
            }
        }

        if(update.remoteFiles == null) {
            System.out.println("Обновлений не обнаружено!");
            return null;
        }

        System.out.printf("Найдено обновлении до версии %s.%n", update.version);
        Notifications.notify("Найдено обновлении до версии %s.", update.version);
        return update;
    }

    public class Update {
        public String version, description;
        public Asset[] remoteFiles;

        public Update() {

        }
    }

    public class Version {
        int id;
        String node_id;

        public String html_url, tag_name, body;
        public Asset[] assets;

        @Override
        public String toString() {
            return "Version{" +
                    "id=" + id +
                    ", node_id='" + node_id + '\'' +
                    ", html_url='" + html_url + '\'' +
                    ", tag_name='" + tag_name + '\'' +
                    ", body='" + body + '\'' +
                    ", assets=" + Arrays.toString(assets) +
                    '}';
        }
    }

    public class Asset {
        int id;
        String node_id;

        public String name, browser_download_url;
        int size;

        @Override
        public String toString() {
            return "Asset{" +
                    "id=" + id +
                    ", node_id='" + node_id + '\'' +
                    ", name='" + name + '\'' +
                    ", browser_download_url='" + browser_download_url + '\'' +
                    ", size=" + size +
                    '}';
        }
    }

}
