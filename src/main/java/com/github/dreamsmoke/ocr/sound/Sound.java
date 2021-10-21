package com.github.dreamsmoke.ocr.sound;

import com.github.dreamsmoke.ocr.util.Settings;
import com.github.dreamsmoke.ocr.util.io.IOUtil;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// https://habr.com/ru/post/191422/
public class Sound implements AutoCloseable {

    static Map<String, byte[]> SOUND_MAP = new HashMap<>();

    static {
        loadSounds("sounds/start.wav", "sounds/stop.wav");
    }

    boolean released, playing;

    AudioInputStream audioInputStream = null;

    FloatControl volumeControl = null;
    Clip clip = null;

    public Sound(InputStream inputStream) {
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));

            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            released = true;
        } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
            e.printStackTrace();
            released = false;
            close();
        }
    }

    // true если звук успешно загружен, false если произошла ошибка
    public boolean isReleased() {
        return released;
    }

    // проигрывается ли звук в данный момент
    public boolean isPlaying() {
        return playing;
    }

    // Запуск
	/*
	  breakOld определяет поведение, если звук уже играется
	  Если breakOld==true, то звук будет прерван и запущен заново
	  Иначе ничего не произойдёт
	*/
    public void play(boolean breakOld) {
        if (released) {
            if (breakOld) {
                clip.stop();
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            } else if (!isPlaying()) {
                clip.setFramePosition(0);
                clip.start();
                playing = true;
            }
        }
    }

    // То же самое, что и play(true)
    public void play() {
        play(true);
    }

    // Останавливает воспроизведение
    public void stop() {
        if (playing) {
            clip.stop();
        }
    }

    public void close() {
        if (clip != null) {
            clip.close();
        }

        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
    }

    // Установка громкости
	/*
	  x долже быть в пределах от 0 до 1 (от самого тихого к самому громкому)
	*/
    public void setVolume(float x) {
        if (x < 0) x = 0;
        if (x > 1) x = 1;
        float min = volumeControl.getMinimum();
        float max = volumeControl.getMaximum();
        volumeControl.setValue((max-min)*x+min);
    }

    // Возвращает текущую громкость (число от 0 до 1)
    public float getVolume() {
        float v = volumeControl.getValue();
        float min = volumeControl.getMinimum();
        float max = volumeControl.getMaximum();
        return (v-min)/(max-min);
    }

    // Статический метод, для удобства
    public static Sound playSound(String string) {
        if(Settings.VOLUME <= 0) {
            return null;
        }

        byte[] bytes = SOUND_MAP.get(string);
        if(bytes == null) {
            return null;
        }

        Sound sound = new Sound(new ByteArrayInputStream(bytes));
        sound.setVolume(Math.max(0, Math.min(1, Settings.VOLUME / 100F)));
        sound.play();

        return sound;
    }

    static void loadSounds(String... strings) {
        for(String string : strings) {
            InputStream inputStream = Sound.class.getClassLoader().getResourceAsStream(string);
            if(inputStream == null) {
                continue;
            }

            try {
                SOUND_MAP.put(string, IOUtil.toByteArray(inputStream));
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
