//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dorkbox.notify;

import dorkbox.util.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class Notify {

    public static final String DIALOG_CONFIRM = "dialog-confirm.png";
    public static final String DIALOG_INFORMATION = "dialog-information.png";
    public static final String DIALOG_WARNING = "dialog-warning.png";
    public static final String DIALOG_ERROR = "dialog-error.png";
    @Property
    public static String TITLE_TEXT_FONT = "Source Code Pro BOLD 16";
    @Property
    public static String MAIN_TEXT_FONT = "Source Code Pro BOLD 12";
    @Property
    public static float MOVE_DURATION = 1.0F;
    @Property
    public static String IMAGE_PATH = "resources";
    protected static Map<String, SoftReference<ImageIcon>> imageCache = new HashMap(4);
    String title;
    String text;
    Theme theme;
    Pos position;
    int hideAfterDurationInMillis;
    boolean hideCloseButton;
    boolean isDark;
    int screenNumber;
    protected ImageIcon icon;
    ActionHandler<Notify> onGeneralAreaClickAction;
    private INotify notifyPopup;
    protected String name;
    private int shakeDurationInMillis;
    private int shakeAmplitude;
    private JFrame appWindow;

    public static String getVersion() {
        return "3.6";
    }

    public static Notify create() {
        return new Notify();
    }

    public static int getImageSize() {
        return 48;
    }

    public static void overrideDefaultImage(String imageName, BufferedImage image) {
        if (imageCache.containsKey(imageName)) {
            throw new RuntimeException("Unable to set an image that already has been set. This action must be done as soon as possible.");
        } else {
            Image imageImmediate = ImageUtil.getImageImmediate(image);
            int width = imageImmediate.getWidth((ImageObserver)null);
            int height = imageImmediate.getHeight((ImageObserver)null);
            BufferedImage bufferedImage;
            if (width > height) {
                bufferedImage = ImageUtil.resizeImage(image, getImageSize(), -1);
            } else {
                bufferedImage = ImageUtil.resizeImage(image, -1, getImageSize());
            }

            imageCache.put(imageName, new SoftReference(new ImageIcon(bufferedImage)));
        }
    }

    private static ImageIcon getImage(String imageName) {
        ImageIcon image = null;
        InputStream resourceAsStream = null;

        try {
            SoftReference<ImageIcon> reference = (SoftReference)imageCache.get(imageName);
            if (reference != null) {
                image = (ImageIcon)reference.get();
            }

            if (image == null) {
                String name = IMAGE_PATH + File.separatorChar + imageName;
                resourceAsStream = LocationResolver.getResourceAsStream(name);
                image = new ImageIcon(ImageUtil.getImageImmediate(ImageIO.read(resourceAsStream)));
                imageCache.put(imageName, new SoftReference(image));
            }
        } catch (IOException var13) {
            var13.printStackTrace();
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException var12) {
                    var12.printStackTrace();
                }
            }

        }

        return image;
    }

    protected Notify() {
        this.position = Pos.BOTTOM_RIGHT;
        this.hideAfterDurationInMillis = 0;
        this.isDark = false;
        this.screenNumber = -32768;
        this.shakeDurationInMillis = 0;
        this.shakeAmplitude = 0;
    }

    public Notify text(String text) {
        this.text = text;
        return this;
    }

    public Notify title(String title) {
        this.title = title;
        return this;
    }

    public Notify image(Image image) {
        int width = image.getWidth((ImageObserver)null);
        int height = image.getHeight((ImageObserver)null);
        BufferedImage bufferedImage = ImageUtil.getBufferedImage(image);
        if (width > height) {
            bufferedImage = ImageUtil.resizeImage(bufferedImage, 48, -1);
        } else {
            bufferedImage = ImageUtil.resizeImage(bufferedImage, -1, 48);
        }

        bufferedImage = ImageUtil.clampMaxImageSize(bufferedImage, 48);
        bufferedImage = ImageUtil.getSquareBufferedImage(bufferedImage);
        this.icon = new ImageIcon(bufferedImage);
        return this;
    }

    public Notify position(Pos position) {
        this.position = position;
        return this;
    }

    public Notify hideAfter(int durationInMillis) {
        if (durationInMillis < 0) {
            durationInMillis = 0;
        }

        this.hideAfterDurationInMillis = durationInMillis;
        return this;
    }

    public Notify onAction(ActionHandler<Notify> onAction) {
        this.onGeneralAreaClickAction = onAction;
        return this;
    }

    public Notify darkStyle() {
        this.isDark = true;
        return this;
    }

    public Notify text(Theme theme) {
        this.theme = theme;
        return this;
    }

    public Notify hideCloseButton() {
        this.hideCloseButton = true;
        return this;
    }

    public void showWarning() {
        this.name = "dialog-warning.png";
        this.icon = getImage(this.name);
        this.show();
    }

    public void showInformation() {
        this.name = "dialog-information.png";
        this.icon = getImage(this.name);
        this.show();
    }

    public void showError() {
        this.name = "dialog-error.png";
        this.icon = getImage(this.name);
        this.show();
    }

    public void showConfirm() {
        this.name = "dialog-confirm.png";
        this.icon = getImage(this.name);
        this.show();
    }

    public void show() {
        SwingUtil.invokeAndWaitQuietly(new Runnable() {
            public void run() {
                Notify notify = Notify.this;
                ImageIcon image = notify.icon;
                Theme theme;
                if (notify.theme != null) {
                    theme = notify.theme;
                } else {
                    theme = new Theme(Notify.TITLE_TEXT_FONT, Notify.MAIN_TEXT_FONT, notify.isDark);
                }

                if (Notify.this.appWindow == null) {
                    Notify.this.notifyPopup = new AsDesktop(notify, image, theme);
                } else {
                    Notify.this.notifyPopup = new AsApplication(notify, image, Notify.this.appWindow, theme);
                }

                Notify.this.notifyPopup.setVisible(true);
                if (Notify.this.shakeDurationInMillis > 0) {
                    Notify.this.notifyPopup.shake(notify.shakeDurationInMillis, notify.shakeAmplitude);
                }

            }
        });
        this.icon = null;
    }

    public Notify shake(final int durationInMillis, final int amplitude) {
        this.shakeDurationInMillis = durationInMillis;
        this.shakeAmplitude = amplitude;
        if (this.notifyPopup != null) {
            SwingUtil.invokeLater(new Runnable() {
                public void run() {
                    Notify.this.notifyPopup.shake(durationInMillis, amplitude);
                }
            });
        }

        return this;
    }

    public void close() {
        if (this.notifyPopup == null) {
            throw new NullPointerException("NotifyPopup");
        } else {
            SwingUtil.invokeLater(new Runnable() {
                public void run() {
                    Notify.this.notifyPopup.close();
                }
            });
        }
    }

    public Notify setScreen(int screenNumber) {
        this.screenNumber = screenNumber;
        return this;
    }

    public Notify attach(JFrame frame) {
        this.appWindow = frame;
        return this;
    }

    void onClose() {
        this.notifyPopup = null;
    }
}
