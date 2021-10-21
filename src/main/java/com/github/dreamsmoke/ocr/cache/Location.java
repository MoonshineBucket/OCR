package com.github.dreamsmoke.ocr.cache;

import java.util.Objects;

public class Location {

    public int posX, posY;

    public Location(int x, int y) {
        posX = x; posY = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return posX == location.posX && posY == location.posY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(posX, posY);
    }

}
