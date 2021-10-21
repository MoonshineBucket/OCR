package com.github.dreamsmoke.api.configuration;

import com.github.dreamsmoke.api.configuration.file.FileConfiguration;
import com.github.dreamsmoke.api.configuration.file.YamlConfiguration;
import com.github.dreamsmoke.ocr.util.io.IOUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public abstract class IConfiguration {

    protected FileConfiguration configuration;

    public abstract File getFile();
    protected abstract void loadBody();

    public void reloadConfig() {
        File file;
        if((file = getFile()).exists()) {
            return;
        }

        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(file.getName());
            if(inputStream == null) {
                file.createNewFile();
            } else {
                IOUtil.writeByteArrayToFile(file, IOUtil.toByteArray(inputStream));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        if((configuration = YamlConfiguration.loadConfiguration(getFile())) != null) {
            loadBody();
        }
    }

    public void saveConfig() {
        try {
            configuration.save(getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateConfig() {
        reloadConfig();
        loadConfig();

        saveConfig();
    }

}
