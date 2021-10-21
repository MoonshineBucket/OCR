package com.github.dreamsmoke.ocr.jna;

import com.github.dreamsmoke.ocr.configuration.OCRConfiguration;
import com.github.dreamsmoke.ocr.gui.OCRController;
import com.github.dreamsmoke.ocr.plugin.JSPlugin;
import com.github.dreamsmoke.ocr.sound.Sound;
import com.github.dreamsmoke.ocr.util.Settings;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class JNA {

    static final Map<String, Object> UNICODE_OPTIONS = new HashMap<>();

    // keyboard
    static final int WH_KEYBOARD_LL = 13, WH_MOUSE_LL = 14,
    // mouse
            WM_MOUSEMOVE = 512, WM_LBUTTONDOWN = 513, WM_LBUTTONUP = 514, WM_RBUTTONDOWN = 516,
            WM_RBUTTONUP = 517, WM_MBUTTONDOWN = 519, WM_MBUTTONUP = 520;

    static {
        UNICODE_OPTIONS.put("type-mapper", W32APITypeMapper.UNICODE);
        UNICODE_OPTIONS.put("function-mapper", W32APIFunctionMapper.UNICODE);
    }

    public static WinUser.HHOOK KEYBOARD_HHOOK, MOUSE_HHOOK;

    public static KeyboardHook KEYBOARD_HOOK;
    public static MouseHook MOUSE_HOOK;

    public static void hook() {
        if(Platform.isWindows()) {
            hookKeyboard();
            hookMouse();

            unHook();
        }
    }

    static void hookKeyboard() {
        System.out.println("Keyboard hooking...");
        WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        System.out.println(hMod);

        KEYBOARD_HOOK = (nCode, wParam, lParam) -> {
            if(wParam.intValue() == WinUser.WM_KEYUP) {
                if(Settings.ACTION_HOTKEY_ENABLE) {
                    // сбрасываем макрос
                    Settings.ACTION_ENABLE = false;
                    Settings.ACTION_HOTKEY_ENABLE = false;

                    // обновляем информацию о кнопке
                    OCRController.INSTANCE.action_hotkey.setText(KeyEvent.getKeyText(Settings.ACTION_HOTKEY = lParam.vkCode));
                    OCRController.INSTANCE.action_hotkey.setFocusTraversable(false);
                    OCRConfiguration.INSTANCE.saveConfig();
                } else if(Settings.SCROLLING_HOTKEY_ENABLE) {
                    // сбрасываем переключение слотов
                    Settings.CURRENT_SLOT = 0;
                    Settings.SCROLLING_ENABLE = false;
                    Settings.SCROLLING_HOTKEY_ENABLE = false;

                    // обновляем информацию о кнопке
                    OCRController.INSTANCE.scrolling_hotkey.setText(KeyEvent.getKeyText(Settings.SCROLLING_HOTKEY = lParam.vkCode));
                    OCRController.INSTANCE.scrolling_hotkey.setFocusTraversable(false);
                    OCRConfiguration.INSTANCE.saveConfig();
                } else {
                    // включение/выключение макроса/переключения слотов
                    if (lParam.vkCode == Settings.ACTION_HOTKEY) {
                        Sound.playSound(String.format("sounds/%s.wav", (Settings.ACTION_ENABLE = !Settings.ACTION_ENABLE) ? "start" : "stop"));
                    } else if (lParam.vkCode == Settings.SCROLLING_HOTKEY) {
                        Settings.CURRENT_SLOT = 0;
                        Sound.playSound(String.format("sounds/%s.wav", (Settings.SCROLLING_ENABLE = !Settings.SCROLLING_ENABLE) ? "start" : "stop"));
                    }
                }
            }

            return User32.INSTANCE.CallNextHookEx(KEYBOARD_HHOOK, nCode, wParam, lParam.getPointer());
        };

        KEYBOARD_HHOOK = User32.INSTANCE.SetWindowsHookEx(WH_KEYBOARD_LL, KEYBOARD_HOOK, hMod, 0);
        System.out.println(KEYBOARD_HHOOK);

        if(KEYBOARD_HHOOK != null) {
            System.out.println("Keyboard hooked!");
        }
    }

    static void hookMouse() {
        System.out.println("Mouse hooking...");
        WinDef.HMODULE hMod = Kernel32.INSTANCE.GetModuleHandle(null);
        System.out.println(hMod);

        MOUSE_HOOK = (nCode, wParam, lParam) -> {
            // нажатие совершено мышью
            if(lParam.dwFlags.intValue() != 1) {
                int clickType = wParam.intValue();

                // зажали левую кнопку мыши
                if (clickType == WM_LBUTTONDOWN) {
                    Settings.MOUSE_ACTIVE = true;
                }
                // отжали левую кнопку мыши
                else if (clickType == WM_LBUTTONUP) {
                    Settings.MOUSE_ACTIVE = false;
                }
            }

            return User32.INSTANCE.CallNextHookEx(MOUSE_HHOOK, nCode, wParam, lParam.getPointer());
        };

        MOUSE_HHOOK = User32.INSTANCE.SetWindowsHookEx(WH_MOUSE_LL, MOUSE_HOOK, hMod, 0);
        System.out.println(MOUSE_HHOOK);

        if(MOUSE_HHOOK != null) {
            System.out.println("Mouse hooked!");
        }
    }

    static void unHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Unhooking...");
            if(KEYBOARD_HHOOK != null) {
                if (User32.INSTANCE.UnhookWindowsHookEx(KEYBOARD_HHOOK)) {
                    System.out.println("Keyboard unhooked!");
                }
            }

            if(MOUSE_HHOOK != null) {
                if(User32.INSTANCE.UnhookWindowsHookEx(MOUSE_HHOOK)) {
                    System.out.println("Mouse unhooked!");
                }
            }

            JSPlugin.INSTANCE.runPlugins("exit");
        }, "JNAHook"));
    }

    static final WinUser.INPUT[] CLICK;

    static {
        CLICK = (WinUser.INPUT[]) new WinUser.INPUT().toArray(2);

        // down
        CLICK[0].type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        CLICK[0].input.setType("mi");
        CLICK[0].input.mi.dwFlags = new WinDef.DWORD(0x0002);
        CLICK[0].input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        CLICK[0].input.mi.time = new WinDef.DWORD(0);
        CLICK[0].input.mi.mouseData = new WinDef.DWORD(0);

        // up
        CLICK[1].type = new WinDef.DWORD(WinUser.INPUT.INPUT_MOUSE);
        CLICK[1].input.setType("mi");
        CLICK[1].input.mi.dwFlags = new WinDef.DWORD(0x0004);
        CLICK[1].input.mi.dwExtraInfo = new BaseTSD.ULONG_PTR(0);
        CLICK[1].input.mi.time = new WinDef.DWORD(0);
        CLICK[1].input.mi.mouseData = new WinDef.DWORD(0);
    }

    // вызываем клик мыши нативно
    public static void nativeMouseClick() {
        User32.INSTANCE.SendInput(new WinDef.DWORD(2), CLICK, CLICK[0].size());
    }

    // хук клавиатуры
    interface KeyboardHook extends StdCallLibrary.StdCallCallback {
        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.KBDLLHOOKSTRUCT lParam);
    }

    // хук мыши
    interface MouseHook extends StdCallLibrary.StdCallCallback {
        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, WinUser.MOUSEINPUT lParam);
    }

}
