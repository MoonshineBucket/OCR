package com.github.dreamsmoke.ocr.cache;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class CachedImage {

    public BufferedImage bufferedImage;

    public int pixelLength;
    public List<Location> locationList;

    public CachedImage(BufferedImage image) {
        bufferedImage = image;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for (int y = 0; y < bufferedImage.getHeight(); ++y) {
            for (int x = 0; x < bufferedImage.getWidth(); ++x) {
                int rgb = bufferedImage.getRGB(x, y);
                int red =   (rgb & 0x00ff0000) >> 16;
                int green = (rgb & 0x0000ff00) >> 8;
                int blue =   rgb & 0x000000ff;

                // ищем минимальные/максимальные координаты, чтобы сместить картинку в левый верхний угол
                if(red == 170 && green == 0 && blue == 0) {
                    if(minX > x) {
                        minX = x;
                    }

                    if(minY > y) {
                        minY = y;
                    }
                }
            }
        }

        locationList = new ArrayList<>();
        for (int y = 0; y < bufferedImage.getHeight(); ++y) {
            for (int x = 0; x < bufferedImage.getWidth(); ++x) {
                int rgb = bufferedImage.getRGB(x, y);
                int red =   (rgb & 0x00ff0000) >> 16;
                int green = (rgb & 0x0000ff00) >> 8;
                int blue =   rgb & 0x000000ff;

                if(red == 170 && green == 0 && blue == 0) {
                    int minRGB = bufferedImage.getRGB(x - minX, y - minY);
                    if(minRGB != rgb) {
                        // смещаем картинку в левый верхний угол
                        bufferedImage.setRGB(x - minX, y - minY, rgb);
                        bufferedImage.setRGB(x, y, minRGB);
                    }

                    // список координат, в которых находится пиксель
                    locationList.add(new Location(x - minX, y - minY));
                }
            }
        }

        // количество пикселей в букве
        pixelLength = locationList.size();
    }

    // сравниваем картинку с найденными координатами на экране
    public static boolean verifyLocationList(CachedImage cachedImage, List<Location> locationList) {
        // количество пикселей
        if(cachedImage.pixelLength != locationList.size()) {
            return false;
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        for(Location location : locationList) {
            if(minX > location.posX) {
                minX = location.posX;
            }

            if(minY > location.posY) {
                minY = location.posY;
            }
        }

        // сверяем расположение пикселей
        int i = 0;
        for(Location location : cachedImage.locationList) {
            if(locationList.contains(new Location(location.posX + minX, location.posY + minY))) {
                ++i;
            }
        }

        return cachedImage.locationList.size() == i;
    }

}
