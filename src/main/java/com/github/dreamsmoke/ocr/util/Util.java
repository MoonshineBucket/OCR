package com.github.dreamsmoke.ocr.util;

import com.github.dreamsmoke.api.util.NumberConversions;
import com.github.dreamsmoke.ocr.cache.CachedImage;
import com.github.dreamsmoke.ocr.cache.Location;
import com.github.dreamsmoke.ocr.update.Updater;
import com.github.dreamsmoke.ocr.util.io.IOUtil;
import com.google.gson.Gson;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

public class Util {

    public static final String VERSION;

    public static final Path BINARY_PATH = IOUtil.getCodeSource(Util.class);
    public static final Path C_BINARY_PATH = BINARY_PATH.getParent().resolve(IOUtil.getFileName(BINARY_PATH) + ".tmp");

    public static final Map<Character, CachedImage> CHARACTER_BUFFERED_IMAGE_MAP = new HashMap<>();
    public static final Map<Character, Character> CHARACTER_MAP = new HashMap<>();

    public static final Map<Integer, List<GameLine>> CACHED_SCREEN_MAP = new HashMap<>();
    public static final Robot ROBOT;

    static {
        String version;
        if((version = Util.class
                .getPackage()
                .getImplementationVersion()
                .toLowerCase()
                .trim()) != null &&
                version.startsWith("ocr-")) {
            version = version.substring("ocr-".length());
        }

        if(version.endsWith("-snapshot")) {
            version = version.substring(0, version.lastIndexOf('-'));
        }

        if(version == null) {
            version = "1.0.0";
        }

        System.out.printf("Version: %s.%n", VERSION = version);

        // english to russian
        CHARACTER_MAP.put('A', 'А');
        CHARACTER_MAP.put('a', 'а');
        CHARACTER_MAP.put('B', 'В');
        CHARACTER_MAP.put('C', 'С');
        CHARACTER_MAP.put('c', 'с');
        CHARACTER_MAP.put('E', 'Е');
        CHARACTER_MAP.put('e', 'е');
        CHARACTER_MAP.put('M', 'М');
        CHARACTER_MAP.put('O', 'О');
        CHARACTER_MAP.put('o', 'о');
        CHARACTER_MAP.put('P', 'Р');
        CHARACTER_MAP.put('X', 'Х');
        CHARACTER_MAP.put('x', 'х');

        // russian to english
        CHARACTER_MAP.put('А', 'A');
        CHARACTER_MAP.put('а', 'a');
        CHARACTER_MAP.put('В', 'B');
        CHARACTER_MAP.put('С', 'C');
        CHARACTER_MAP.put('с', 'c');
        CHARACTER_MAP.put('Е', 'E');
        CHARACTER_MAP.put('е', 'e');
        CHARACTER_MAP.put('М', 'M');
        CHARACTER_MAP.put('О', 'O');
        CHARACTER_MAP.put('о', 'o');
        CHARACTER_MAP.put('Р', 'P');
        CHARACTER_MAP.put('Х', 'X');
        CHARACTER_MAP.put('х', 'x');

        Robot robot = null;

        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }

        ROBOT = robot;
    }

    // загружаем ascii картинки из nbt файла
    public static void initialize() {
        InputStream inputStream = Util.class.getClassLoader().getResourceAsStream("ascii.dat");
        NBTTagCompound nbtTagCompound;
        if(inputStream == null) {
            nbtTagCompound = new NBTTagCompound();
        } else {
            try {
                nbtTagCompound = CompressedStreamTools.readCompressed(inputStream);
                inputStream.close();
            } catch (IOException e) {
                nbtTagCompound = new NBTTagCompound();
                e.printStackTrace();
            }
        }

        NBTTagList nbtTagList = nbtTagCompound.getTagList("getAsciiImageList");
        for(int count = nbtTagList.tagCount(), i = 0; i < count; ++i) {
            NBTTagCompound tagCompound = (NBTTagCompound) nbtTagList.tagAt(i);

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(tagCompound.getByteArray("getImage"));
                CachedImage cachedImage = new CachedImage(ImageIO.read(bis));
                bis.close();

                char character = tagCompound.getString("getName").replace("<space>", " ").charAt(0);
                Util.CHARACTER_BUFFERED_IMAGE_MAP.put(character, cachedImage);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    // список координат, в которых находится пиксель с подходящим цветом
    public static List<Location> blackAndWhiteCoordinateList(BufferedImage bufferedImage) {
        List<Location> locationList = new ArrayList<>();

        for (int x = 0; x < bufferedImage.getWidth(); x++) {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                // ищем пиксель с цветом, который поддерживается в игре
                if(ChatColor.hasColor(bufferedImage.getRGB(x, y))) {
                    locationList.add(new Location(x, y));
                    //bufferedImage.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    //bufferedImage.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }

        //writeImageToFile(bufferedImage, "black_and_white");

        // сортируем по порядку, сначала по координате y (от меньшего к большему), затем по x (от меньшего к большему)
        Collections.sort(locationList, (a, b) -> (a.posY + a.posX) - (b.posY + b.posX));
        return locationList;
    }

    // ищем текст в игре
    public static GameLine findInGameLine(int index, Location location, List<Location> list, List<Location> ignoreLocationList) {
        StringBuilder stringBuilder = new StringBuilder();

        List<Location> lineLocationList = new ArrayList<>();
        boolean isSpaceDetected = false;
        label: while (true) {
            if(location == null || ignoreLocationList.contains(location)) {
                break;
            }

            String toString = stringBuilder.toString();
            if(toString.length() > 0) {
                for (String string : Settings.FIND_GAME_LINE_LIST) {
                    // нашли совпадающий текст, дальше нет смысла
                    if (string.equals(toString)) {
                        break label;
                    }
                }
            }

            // ищем символ на скриншоте экрана
            Character character = null;
            List<Location> locationList = createLocationList(location, list, new ArrayList<>());
            for (Map.Entry<Character, CachedImage> entry : CHARACTER_BUFFERED_IMAGE_MAP.entrySet()) {
                // картинка с найденными пикселями существует???
                if(CachedImage.verifyLocationList(entry.getValue(), locationList)) {
                    character = entry.getKey();
                    break;
                }
            }

            // список пройденных координат (оптимизация???)
            ignoreLocationList.addAll(locationList);
            if(character == null) {
                // мы нашли странные пиксели, которые не являются буквой???
                while(true) {
                    // будем искать следующие координаты, которые нам еще не удалось проверить
                    if(++index == list.size()) {
                        break;
                    }

                    if(ignoreLocationList.contains(location = list.get(index))) {
                        continue;
                    }

                    break;
                }

                continue;
            }

            if(isSpaceDetected) {
                toString = stringBuilder.append(" ").toString();
                isSpaceDetected = false;
            }

            // убеждаемся, что символ, который мы нашли нам нужен
            if((character = findCharacter(toString, character)) == null) {
                break;
            }

            // символ мы нашли, что же дальше?
            lineLocationList.addAll(locationList);
            stringBuilder.append(character);

            // дальше мы будем пытаться найти слово с этим символом
            int maxX = -1, minY = Integer.MAX_VALUE, maxY = -1;
            for(Location l : locationList) {
                if(maxX < l.posX) {
                    maxX = l.posX;
                }

                if(maxY < l.posY) {
                    maxY = l.posY;
                }

                if(minY > l.posY) {
                    minY = l.posY;
                }
            }

            if(maxY - minY < 15) {
                minY -= 4;
            }

            // сканируем соседнии координаты
            Location l = null;
            LOCATION_LABEL: while(minY < maxY) {
                for(int x = maxX + 1; x < maxX + 10; ++x) {
                    Location search = new Location(x, minY);
                    if(list.contains(search) && !ignoreLocationList.contains(search)) {
                        if(l == null || (l.posX - maxX >= x - maxX)) {
                            isSpaceDetected = false;
                            if (x - maxX >= 6) {
                                isSpaceDetected = true;
                            }

                            l = search;
                        }
                    }
                }

                ++minY;
            }

            index = list.indexOf(location = l);
        }

        int posX = Integer.MAX_VALUE, posY = Integer.MAX_VALUE,
                width = -1, height = -1;
        for(Location l : lineLocationList) {
            if(posX > l.posX) {
                posX = l.posX;
            }

            if(posY > l.posY) {
                posY = l.posY;
            }

            if(width < l.posX) {
                width = l.posX;
            }

            if(height < l.posY) {
                height = l.posY;
            }
        }

        // найденное слово
        return new GameLine(stringBuilder.toString(), posX, posY, width - posX, height - posY);
    }

    // анализируем соседнии пиксели для сбора в букву
    static List<Location> createLocationList(Location location, List<Location> list, List<Location> locationList) {
        locationList.add(location);
        for(int posY = location.posY - 6; posY < location.posY + 4; ++posY) {
            for(int posX = location.posX - 2; posX < location.posX + 2; ++posX) {
                Location l = new Location(posX, posY);
                if(list.contains(l) && !locationList.contains(l)) {
                    createLocationList(l, list, locationList);
                }
            }
        }

        return locationList;
    }

    // убеждаемся, что символ нам нужен (находится в списке исключений)
    static Character findCharacter(String toString, Character character) {
        if(Settings.FIND_GAME_LINE_LIST.isEmpty() || Settings.FIND_GAME_LINE_LIST.get(0).equals("*")) {
            return character;
        }

        List<Character> characterList = new ArrayList<>();
        characterList.add(character);

        // переводим совпадающие символы в разных языках, например Русская и Англиская буквы 'А/а'
        if(CHARACTER_MAP.containsKey(character)) {
            characterList.add(CHARACTER_MAP.get(character));
        }

        for (String string : Settings.FIND_GAME_LINE_LIST) {
            for(Character c : characterList) {
                if (string.startsWith(toString + c)) {
                    return c;
                }
            }
        }

        return null;
    }

    // создать картинку с текстом из символов
    public static void printMessage(String string, int imageType) throws IOException {
        char[] chars = string.toCharArray();
        BufferedImage bufferedImage = new BufferedImage(chars.length * 16, 16, imageType);
        for(int length = chars.length, i = 0; i < length; ++i) {
            char character = chars[i];

            int currentXPos = i * 16;
            if(Util.CHARACTER_BUFFERED_IMAGE_MAP.containsKey(character)) {
                CachedImage cacheImage = Util.CHARACTER_BUFFERED_IMAGE_MAP.get(character);
                for (int y = 0; y < 16; ++y) {
                    for (int x = 0; x < 16; ++x) {
                        bufferedImage.setRGB(currentXPos + x, y, cacheImage.bufferedImage.getRGB(x, y));
                    }
                }
            }
        }

        writeImageToFile(bufferedImage, string);
    }

    // нарезать файл с символами на мелкие файлы 16x16
    public static void generateAscii() throws IOException {
        InputStream inputStream = Util.class.getClassLoader().getResourceAsStream("assets/ascii.png");
        if (inputStream == null) {
            return;
        }

        BufferedImage asciiBufferedImage = ImageIO.read(inputStream);
        for (int height = asciiBufferedImage.getHeight(), width = asciiBufferedImage.getWidth(), asciiY = 0; asciiY < height; asciiY += 16) {
            for (int asciiX = 0; asciiX < width; asciiX += 16) {
                BufferedImage bufferedImage = new BufferedImage(16, 16, asciiBufferedImage.getType());
                for (int y = 0; y < 16; ++y) {
                    for (int x = 0; x < 16; ++x) {
                        bufferedImage.setRGB(x, y, asciiBufferedImage.getRGB(asciiX + x, asciiY + y));
                    }
                }

                int index = asciiX / 16 + asciiY;
                writeImageToFile(bufferedImage, index);
            }
        }
    }

    // создание nbt конфигурации программы
    public static void generateNBT() throws IOException {
        System.out.println("Send to me file name and him tag name.");
        System.out.println("Example: 210 A");

        Scanner scanner = new Scanner(System.in, "Cp866");
        while (scanner.hasNext()) {
            String string = scanner.nextLine();
            if(string.equalsIgnoreCase("stop")) {
                scanner.close();
                break;
            }

            String[] strings = string.split(" ");
            if(strings.length < 2) {
                System.out.println("Missing arguments!");
                continue;
            }

            File file = new File(String.format("%s.png", strings[0]));
            if(file.exists()) {
                System.out.println("Saving...");
                File nbtFile = new File("nbt.dat");

                NBTTagCompound nbtTagCompound;
                if(nbtFile.exists()) {
                    FileInputStream fis = new FileInputStream(nbtFile);
                    nbtTagCompound = CompressedStreamTools.readCompressed(fis);
                    fis.close();
                } else {
                    nbtTagCompound = new NBTTagCompound();
                    nbtFile.createNewFile();
                }

                NBTTagList nbtTagList = nbtTagCompound.getTagList("getAsciiImageList");

                NBTTagCompound tagCompound = new NBTTagCompound();
                tagCompound.setString("getName", strings[1]);
                tagCompound.setByteArray("getImage", Files.readAllBytes(file.toPath()));
                nbtTagList.appendTag(tagCompound);

                nbtTagCompound.setTag("getAsciiImageList", nbtTagList);

                FileOutputStream fos = new FileOutputStream(nbtFile);
                CompressedStreamTools.writeCompressed(nbtTagCompound, fos);
                fos.close();

                file.delete();
                System.out.println("Saved!");
            }
        }

        System.out.println("Goodbye!");
    }

    // записать изображение в файл
    public static void writeImageToFile(BufferedImage bufferedImage, Object object) {
        File file = new File(String.format("%s.png", object));
        if(file.exists()) {
            file.delete();
        }

        try {
            file.createNewFile();
            ImageIO.write(bufferedImage, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Updater.Version[] getVersions() {
        try {
            Reader reader = new InputStreamReader(new BufferedInputStream(new URL("https://api.github.com/repos/MoonshineBucket/OCR/releases").openStream()), "UTF-8");
            Updater.Version[] versions = new Gson().fromJson(reader, Updater.Version[].class);
            System.out.printf("Founded remote repository versions: %s.%n", Arrays.toString(versions));
            reader.close();

            return versions;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean canUpdate(String[] strings, String[] currentVersion) {
        return verifyVersions(strings, currentVersion, false);
    }

    public static boolean verifyVersions(String[] strings, String[] currentVersion, boolean isCurrentVersion) {
        for(int length = strings.length, i = 0; i < length; ++i) {
            int currentValue = 0, newValue = NumberConversions.toInt(strings[i]);
            if(currentVersion.length > i) {
                currentValue = NumberConversions.toInt(currentVersion[i]);
            }

            if(isCurrentVersion) {
                if(currentValue != newValue) {
                    return false;
                }
            } else {
                if(currentValue < newValue) {
                    return true;
                }
            }
        }

        return isCurrentVersion;
    }

}
