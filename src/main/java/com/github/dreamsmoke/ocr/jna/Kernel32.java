package com.github.dreamsmoke.ocr.jna;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

public interface Kernel32 extends StdCallLibrary {

    Kernel32 INSTANCE = Native.loadLibrary("kernel32", Kernel32.class, JNA.UNICODE_OPTIONS);

    WinDef.HMODULE GetModuleHandle(String name);

}
