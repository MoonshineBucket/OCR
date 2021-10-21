package com.github.dreamsmoke.ocr;

import com.github.dreamsmoke.ocr.cache.Location;
import com.github.dreamsmoke.ocr.jna.JNA;
import com.github.dreamsmoke.ocr.jna.User32;
import com.github.dreamsmoke.ocr.plugin.JSPlugin;
import com.github.dreamsmoke.ocr.util.GameLine;
import com.github.dreamsmoke.ocr.util.Settings;
import com.github.dreamsmoke.ocr.util.Util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OCR {

    public static final OCR INSTANCE = new OCR();

    static {
        new Thread(() -> {
            synchronized (OCR.class) {
                try {
                    OCR.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            INSTANCE.initialize();
        }, "Main").start();
    }

    // загрузка ui части приложения
    public void start(Stage primaryStage) throws Exception {
        Thread.currentThread().setName("FXMain");
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getClassLoader().getResource("assets/ocr.fxml"));

        primaryStage.setTitle(String.format("OCR v%s", Util.VERSION));

        Parent parent = fxmlLoader.load();
        parent.setOnMouseClicked(event -> parent.requestFocus());
        primaryStage.setScene(new Scene(parent));
        primaryStage.show();

        Settings.WIDTH = (int) primaryStage.getWidth();
        Settings.HEIGHT = (int) primaryStage.getHeight();

        // хукаем процессы
        JNA.hook();

        synchronized (OCR.class) {
            OCR.class.notify();
        }
    }

    // загрузка приложения
    void initialize() {
        // загружаем буквы
        Util.initialize();

        // загружаем плагины
        JSPlugin.INSTANCE.initialize();

        // делаем скриншоты
        new Thread(() -> {
            Rectangle rectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            while (true) {
                try {
                    if(Settings.OCR) {
                        // название активного окна
                        String windowName = User32.getWindowName();
                        // убеждаемся, что открыто нужное нам окно
                        if (windowName != null && Settings.WINDOW_NAME_LIST.contains(windowName)) {
                            /*WinDef.RECT rect = User32.getWindowRect();
                            if(rect.left <= -30000 ||
                                    rect.right <= -30000 ||
                                    rect.top <= -30000 ||
                                    rect.bottom <= -30000) {
                                continue;
                            }

                            Rectangle rectangle = rect.toRectangle();
                            System.out.printf("rect: minX: %s, minY: %s, maxX: %s, maxY: %s.%n",
                                    rect.left, rect.top, rect.right, rect.bottom);*/

                            // создаем скриншот
                            //long startTime = System.currentTimeMillis();
                            BufferedImage screenCapture = Util.ROBOT.createScreenCapture(rectangle);
                            //System.out.printf("screen: %s.%n", System.currentTimeMillis() - startTime);

                            // переводим в черно-белое полотно
                            //startTime = System.currentTimeMillis();
                            java.util.List<Location> locationList = Util.blackAndWhiteCoordinateList(screenCapture);
                            //System.out.printf("black-white: %s.%n", System.currentTimeMillis() - startTime);

                            //int cache = locationList.size();
                            java.util.List<GameLine> gameLineList;
                            //if (Util.CACHED_SCREEN_MAP.containsKey(cache)) {
                                // достаем из кеша, потому что можно
                                //gameLineList = Util.CACHED_SCREEN_MAP.get(cache);
                            //} else {
                                List<Location> ignoreLocationList = new ArrayList<>();

                                gameLineList = new ArrayList<>();
                                for (int size = locationList.size(), i = 0; i < size; ++i) {
                                    Location location = locationList.get(i);
                                    if (ignoreLocationList.contains(location)) {
                                        continue;
                                    }

                                    // ищем строку на экране
                                    GameLine gameLine = Util.findInGameLine(i, location, locationList, ignoreLocationList);
                                    if (gameLine == null || gameLine.line == null || gameLine.line.isEmpty()) {
                                        continue;
                                    }

                                    gameLineList.add(gameLine);
                                }

                                // кешируем, потому что умеем
                                //Util.CACHED_SCREEN_MAP.put(cache, gameLineList);

                                ignoreLocationList.clear();
                                locationList.clear();
                            //}

                            for (GameLine gameLine : gameLineList) {
                                System.out.println(gameLine.line);
                            }

                            // выполняем плагины
                            JSPlugin.INSTANCE.runPlugins("run", gameLineList);
                            gameLineList.clear();
                        }

                        continue;
                    }

                    TimeUnit.MILLISECONDS.sleep(5L);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }, "OCR").start();

        // макросим :/
        new Thread(() -> {
            boolean activeLeftMouse = false, activeRightMouse = false;

            while (true) {
                try {
                    if(Settings.ACTION_ENABLE) {
                        if(Settings.ACTION_TYPE == Settings.ActionType.BOW) {
                            Util.ROBOT.mousePress(InputEvent.BUTTON3_MASK);

                            // ожидаем задержку
                            TimeUnit.MILLISECONDS.sleep(Settings.ACTION_REPEAT);
                            Util.ROBOT.mouseRelease(InputEvent.BUTTON3_MASK);

                            // bow fix
                            TimeUnit.MILLISECONDS.sleep(5L);
                        } else {
                            switch (Settings.ACTION_TYPE) {
                                case SHOVEL: {
                                    Util.ROBOT.mousePress(InputEvent.BUTTON1_MASK);
                                    Util.ROBOT.mouseRelease(InputEvent.BUTTON1_MASK);
                                    break;
                                }
                                case BLOCK_PLACE: {
                                    activeRightMouse = true;
                                    Util.ROBOT.mousePress(InputEvent.BUTTON3_MASK);
                                    break;
                                }
                                case BLOCK_BREAK: {
                                    activeLeftMouse = true;
                                    Util.ROBOT.mousePress(InputEvent.BUTTON1_MASK);
                                    break;
                                }
                                case LEFT_CLICK: {
                                    if (Settings.MOUSE_ACTIVE) {
                                        // фейковый клик
                                        JNA.nativeMouseClick();
                                    }

                                    break;
                                }
                            }

                            // ожидаем задержку
                            TimeUnit.MILLISECONDS.sleep(Settings.ACTION_REPEAT);
                        }
                    } else {
                        // отжимаем кнопку
                        if(activeLeftMouse) {
                            activeLeftMouse = false;
                            Util.ROBOT.mouseRelease(InputEvent.BUTTON1_MASK);
                        } else if(activeRightMouse) {
                            activeRightMouse = false;
                            Util.ROBOT.mouseRelease(InputEvent.BUTTON3_MASK);
                        }

                        TimeUnit.MILLISECONDS.sleep(5L);
                    }

                    JSPlugin.INSTANCE.runPlugins("tick");
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }, "MouseRobot").start();

        // крутим слоты :/
        new Thread(() -> {
            while (true) {
                try {
                    if(Settings.SCROLLING_ENABLE) {
                        if(++Settings.CURRENT_SLOT > Settings.SCROLLING_LENGTH) {
                            Settings.CURRENT_SLOT = 1;
                        }

                        Util.ROBOT.keyPress(KeyEvent.VK_0 + Settings.CURRENT_SLOT);
                        TimeUnit.MILLISECONDS.sleep(Settings.SCROLLING_REPEAT);
                        continue;
                    }

                    TimeUnit.MILLISECONDS.sleep(5L);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }, "ScrollRobot").start();
    }

}
