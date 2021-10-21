package com.github.dreamsmoke.ocr.gui.notification;

import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.notify.Theme;
import dorkbox.util.ActionHandler;
import dorkbox.util.ImageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.IOException;

public class Notifications {

    public static void notify(String s, Object... objects) {
        String string = String.format(s, objects);

        OCRNotify.create()
                .title("OCR")
                .text(string)
                .position(Pos.TOP_LEFT)
                /*.onAction(value -> {

                })*/
                .hideAfter(10000)
                .shake(250, 5)
                .darkStyle()      // There are two default themes darkStyle() and default.
                .show();   // You can use warnings and error as well.
    }

    public static final class OCRNotify extends Notify {

        static final ImageIcon IMAGE_ICON;

        static {
            ImageIcon imageIcon;

            try {
                imageIcon = new ImageIcon(ImageUtil
                        .getImageImmediate(ImageIO
                        .read(OCRNotify.class.getClassLoader().getResourceAsStream("assets/ocr.png"))));
            } catch (IOException e) {
                e.printStackTrace();
                imageIcon = null;
            }

            IMAGE_ICON = imageIcon;
        }

        public static OCRNotify create() {
            return new OCRNotify();
        }

        @Override
        public OCRNotify text(String text) {
            return (OCRNotify) super.text(text);
        }

        @Override
        public OCRNotify title(String title) {
            return (OCRNotify) super.title(title);
        }

        @Override
        public OCRNotify image(java.awt.Image image) {
            return (OCRNotify) super.image(image);
        }

        @Override
        public OCRNotify position(Pos position) {
            return (OCRNotify) super.position(position);
        }

        @Override
        public OCRNotify hideAfter(int durationInMillis) {
            return (OCRNotify) super.hideAfter(durationInMillis);
        }

        @Override
        public OCRNotify onAction(ActionHandler<Notify> onAction) {
            return (OCRNotify) super.onAction(onAction);
        }

        @Override
        public OCRNotify darkStyle() {
            return (OCRNotify) super.darkStyle();
        }

        @Override
        public OCRNotify text(Theme theme) {
            return (OCRNotify) super.text(theme);
        }

        @Override
        public OCRNotify hideCloseButton() {
            return (OCRNotify) super.hideCloseButton();
        }

        @Override
        public OCRNotify shake(int durationInMillis, int amplitude) {
            return (OCRNotify) super.shake(durationInMillis, amplitude);
        }

        @Override
        public OCRNotify setScreen(int screenNumber) {
            return (OCRNotify) super.setScreen(screenNumber);
        }

        @Override
        public OCRNotify attach(JFrame frame) {
            return (OCRNotify) super.attach(frame);
        }

        @Override
        public void show() {
            icon = IMAGE_ICON;
            super.show();
        }
    }

}
