package com.github.dreamsmoke.ocr.util.io;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.DecimalFormat;

// многие методы взяты с проекта https://github.com/GravitLauncher/Launcher, версия hotfix/5.0.11,
// а так же с библиотеки commons-io, версия 2.6
public class IOUtil {

    public static final File DATA_FOLDER;

    static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    static {
        String appData = System.getenv("AppData");

        File dataFolder;
        if(appData == null || appData.isEmpty()) {
            dataFolder = new File(System.getProperty("user.home", "."), ".ocr");
        } else {
            dataFolder = new File(appData, ".ocr");
        }

        DATA_FOLDER = dataFolder;
    }

    public static void writeByteArrayToFile(File file, byte[] bytes) throws IOException {
        File parentFile;
        if((parentFile = file.getParentFile()) != null) {
            parentFile.mkdirs();
        }

        if(!file.exists()) {
            file.createNewFile();
        }

        writeByteArrayToFile(file, bytes, false);
    }

    public static void writeByteArrayToFile(File file, byte[] bytes, boolean append) throws IOException {
        OutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(file, append);
            outputStream.write(bytes);
            outputStream.close();
        } finally {
            closeQuietly(outputStream);
        }
    }

    static void closeQuietly(Closeable closeable) {
        try {
            if(closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {

        }
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        copy(inputStream, byteArrayOutputStream);
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    public static int copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        long copyLarge = copyLarge(inputStream, outputStream);
        return copyLarge > 2147483647L ? -1 : (int) copyLarge;
    }

    public static long copyLarge(InputStream inputStream, OutputStream outputStream) throws IOException {
        return copyLarge(inputStream, outputStream, new byte[4096]);
    }

    public static long copyLarge(InputStream inputStream, OutputStream outputStream, byte[] bytes) throws IOException {
        long length = 0L;

        int i;
        while((i = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, i);
            length += i;
        }

        return length;
    }

    public static Path resolveJavaBin() {
        // Get Java binaries path
        Path javaBinDir = Paths.get(System.getProperty("java.home")).resolve("bin");

        // Verify has "javaw.exe" file
        Path javawExe = javaBinDir.resolve("javaw.exe");
        if (isFile(javawExe))
            return javawExe;

        // Verify has "java.exe" file
        Path javaExe = javaBinDir.resolve("java.exe");
        if (isFile(javaExe))
            return javaExe;

        // Verify has "java" file
        Path java = javaBinDir.resolve("java");
        if (isFile(java))
            return java;

        // Throw exception as no runnable found
        throw new RuntimeException("Java binary wasn't found");
    }

    public static Path getCodeSource(Class<?> clazz) {
        return Paths.get(toURI(clazz.getProtectionDomain().getCodeSource().getLocation()));
    }

    public static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getFileName(Path path) {
        return path.getFileName().toString();
    }

    public static boolean hasExtension(Path file, String extension) {
        return getFileName(file).endsWith(extension);
    }

    public static InputStream newInput(Path file) throws IOException {
        return Files.newInputStream(file, StandardOpenOption.READ);
    }

    static OutputStream newOutput(Path file) throws IOException {
        return newOutput(file, false);
    }

    static OutputStream newOutput(Path file, boolean append) throws IOException {
        createParentDirs(file);
        return Files.newOutputStream(file, append ?
                new OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND} :
                new OpenOption[] {StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING});
    }

    static void createParentDirs(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !isDir(parent))
            Files.createDirectories(parent);
    }

    static boolean isFile(Path path) {
        return Files.isRegularFile(path);
    }

    static boolean isDir(Path path) {
        return Files.isDirectory(path);
    }

    static byte[] newBuffer() {
        return new byte[4096];
    }

    public static long transfer(InputStream input, Path file) throws IOException {
        return transfer(input, file, false);
    }

    static long transfer(InputStream input, Path file, boolean append) throws IOException {
        try (OutputStream output = newOutput(file, append)) {
            return transfer(input, output);
        }
    }

    static long transfer(InputStream input, OutputStream output) throws IOException {
        long transferred = 0;
        byte[] buffer = newBuffer();
        for (int length = input.read(buffer); length >= 0; length = input.read(buffer)) {
            output.write(buffer, 0, length);
            transferred += length;
        }

        return transferred;
    }

    public static void downloadFile(String downloadURL, Path path) {
        URL url;

        try {
            url = new URL(downloadURL);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }

        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            int DOWNLOADED = 0, TOTAL_SIZE = httpURLConnection.getContentLength();

            Files.deleteIfExists(path);
            long startTime = System.currentTimeMillis();

            try(InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                OutputStream outputStream = newOutput(path)) {

                byte[] bytes = newBuffer();

                int bytesRead;
                while((bytesRead = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, bytesRead);
                    DOWNLOADED += bytesRead;

                    double speedInKBps = 0.0D;
                    try {
                        long timeInSecs = (System.currentTimeMillis() - startTime);
                        speedInKBps = (DOWNLOADED / timeInSecs);
                    } catch (ArithmeticException e) {

                    }

                    String speed = DECIMAL_FORMAT.format(speedInKBps);
                    int percentage = (int) (((double) DOWNLOADED / (double) TOTAL_SIZE) * 100d);
                    System.out.printf("Percentage: %s%s, speed: %s/kbps.%n",
                            percentage, "%", speed);
                }
            }

            httpURLConnection.disconnect();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
