package com.github.dreamsmoke.ocr.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;

public interface User32 extends StdCallLibrary {

    User32 INSTANCE = Native.loadLibrary("user32", User32.class, JNA.UNICODE_OPTIONS);

    // получаем окно
    WinDef.HWND GetForegroundWindow();
    // название окна
    void GetWindowTextW(WinDef.HWND hWnd, char[] lpString, int nMaxCount);

    // узнаем, видимо окно или же нет
    boolean IsWindowVisible(WinDef.HWND hWnd);
    // получаем координаты окна
    boolean GetWindowRect(WinDef.HWND hWnd, WinDef.RECT rect);

    // прокидываем хук в процесс
    WinUser.HHOOK SetWindowsHookEx(int idHook, StdCallCallback lpfn, WinDef.HMODULE hMod, int dwThreadId);
    // отсылаем следующий хук
    WinDef.LRESULT CallNextHookEx(WinUser.HHOOK idHook, int nCode, WinDef.WPARAM wParam, WinDef.LPARAM lParam);
    WinDef.LRESULT CallNextHookEx(WinUser.HHOOK idHook, int nCode, WinDef.WPARAM wParam, Pointer lParam);

    // удаляем хук
    boolean UnhookWindowsHookEx(WinUser.HHOOK idHook);

    // отправляем действие
    WinDef.DWORD SendInput(WinDef.DWORD nInputs, WinUser.INPUT[] pInputs, int cbSize);

    // статичная реализация методов
    static String getWindowName() {
        WinDef.HWND window = INSTANCE.GetForegroundWindow();
        if(window == null) {
            return null;
        }

        if(INSTANCE.IsWindowVisible(window)) {
            char[] buffer = new char[1024 * 2];
            INSTANCE.GetWindowTextW(window, buffer, 1024);
            return Native.toString(buffer);
        }

        return null;
    }

    static WinDef.RECT getWindowRect() {
        WinDef.RECT rect = new WinDef.RECT();
        INSTANCE.GetWindowRect(INSTANCE.GetForegroundWindow(), rect);
        return rect;
    }

}
