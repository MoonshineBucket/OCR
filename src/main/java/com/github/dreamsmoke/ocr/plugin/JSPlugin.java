package com.github.dreamsmoke.ocr.plugin;

import com.github.dreamsmoke.ocr.OCR;
import com.github.dreamsmoke.ocr.util.Util;
import com.github.dreamsmoke.ocr.util.io.IOUtil;
import org.mozilla.javascript.*;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

// function initialize(ocr) {} // функция инициализации программы
// срабатывает при запуске программы в методе JSPlugin.loadPlugins();

// function reload(ocr) {} // функция перезагрузки плагина
// срабатывает при перезагрузке плагина в методе JSPlugin.loadPlugins();

// function tick() {} // функция макроса
// срабатывает в потоке MouseRobot

// function run(gameLineList) {} // функция обработки найденного списка
// срабатывает после завершения обработки скриншота экрана в классе OCR, поток OCR

// function save(ocr, configuration) {} // функция сохранения конфигурации
// срабатывает при сохранении конфигурации в методе OCRConfiguration.saveConfig();

// function exit() {} // функция выключения программы
// срабатывает при выключении программы в специальном потоке добавленном
// через Runtime.getRuntime().addShutdownHook(...);
public class JSPlugin {

    public static final JSPlugin INSTANCE = new JSPlugin();

    static final File DATA_FOLDER = new File(IOUtil.DATA_FOLDER, "plugins");
    static final Map<String, Script> STRING_SCRIPT_MAP = new HashMap<>();

    static {
        DATA_FOLDER.mkdirs();
    }

    public void initialize() {
        // загружаем плагины из директории
        // онлайн без смс и регистрации (yatoreno ^_^)
        loadPlugins(true);

        // поток обновления плагинов, если файл был удален/изменен/создан, происходит перезагрузка всех плагинов
        new Thread(() -> {
            try {
                WatchService watchService = FileSystems.getDefault().newWatchService();
                DATA_FOLDER.toPath().register(watchService, new WatchEvent.Kind[] { StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE });

                label: while(true) {
                    WatchKey watchKey;

                    try {
                        watchKey = watchService.take();
                    } catch (InterruptedException e) {
                        continue;
                    }

                    for(WatchEvent watchEvent : watchKey.pollEvents()) {
                        WatchEvent.Kind<Path> pathKind = watchEvent.kind();
                        System.out.println(pathKind);

                        if(pathKind.equals(StandardWatchEventKinds.OVERFLOW)) {
                            break label;
                        }

                        File file = new File(((Path) watchKey.watchable()).toFile(),
                                ((Path) watchEvent.context()).toFile().getName());
                        System.out.println(file.getAbsolutePath());

                        String fileName = file.getName();
                        if(fileName.endsWith(".js")) {
                            loadPlugins(false);
                        }
                    }

                    watchKey.reset();
                }

                throw new IOException("Overflow");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "PluginListener").start();
    }

    void loadPlugins(boolean initialize) {
        STRING_SCRIPT_MAP.clear();

        DATA_FOLDER.mkdirs();
        File[] files = DATA_FOLDER.listFiles((dir, name) -> name.endsWith(".js"));
        for(int length = files.length, i = 0; i < length; ++i) {
            File file = files[i];

            try {
                String fileName = file.getName();
                PluginScript pluginScript = createScript(new FileInputStream(file), fileName.substring(0, fileName.lastIndexOf('.')));
                if(pluginScript == null || pluginScript.script == null) {
                    continue;
                }

                String functionName = initialize ? "initialize" : "reload";
                if(!initialize && !STRING_SCRIPT_MAP.containsKey(pluginScript.name)) {
                    functionName = "initialize";
                }

                STRING_SCRIPT_MAP.put(pluginScript.name, pluginScript.script);
                System.out.printf("Loaded %s plugin.%n", pluginScript.name);

                runPlugins(functionName, OCR.INSTANCE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(STRING_SCRIPT_MAP.isEmpty()) {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("plugins/auth.js");
            if(inputStream == null) {
                System.out.println("No plugins found!");
                return;
            }

            try {
                File file = new File(DATA_FOLDER, "auth.js");
                file.createNewFile();

                Files.write(file.toPath(), IOUtil.toByteArray(inputStream));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void runPlugins(String functionName, Object... objects) {
        for(Map.Entry<String, Script> entry : STRING_SCRIPT_MAP.entrySet()) {
            /*System.out.printf("Running %s plugin %s function.%n", entry.getKey(), functionName);
            long startTime = System.currentTimeMillis();*/

            try {
                Context context = Context.enter();

                try {
                    Scriptable scriptable = getScriptable(context);
                    entry.getValue().exec(context, scriptable);

                    Object object = scriptable.get(functionName, scriptable);
                    if(object != Scriptable.NOT_FOUND && object instanceof Function) {
                        ((Function) object).call(context, scriptable, scriptable, objects);
                    }
                } finally {
                    Context.exit();
                }
            } catch (RhinoException e) {
                e.printStackTrace();
            }

            /*System.out.printf("Finished %s plugin %s function on %sms.%n", entry.getKey(),
                    functionName, System.currentTimeMillis() - startTime);*/
        }
    }

    ScriptableObject scriptableObject = Context.enter().initStandardObjects();

    public Scriptable getScriptable(Context context) {
        Scriptable scriptable = context.newObject(scriptableObject);
        scriptable.setPrototype(scriptableObject);
        scriptable.setParentScope(null);

        ScriptableObject.putProperty(scriptable, "plugin", this);
        ScriptableObject.putProperty(scriptable, "robot", Util.ROBOT);
        return scriptable;
    }

    public PluginScript createScript(InputStream inputStream, String fileName) throws Exception {
        Context context = Context.enter();

        Script script;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            script = context.compileReader(bufferedReader, fileName, 0, null);
            bufferedReader.close();
        } finally {
            Context.exit();
        }

        return new PluginScript(fileName, script);
    }

    class PluginScript {
        String name;
        Script script;

        public PluginScript(String string, Script s) {
            name = string; script = s;
        }
    }

}
