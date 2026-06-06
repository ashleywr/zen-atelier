package com.sanhiruzu.atelier.ui.client;

record ScreenRect(int x, int y, int width, int height) {
    int right() {
        return x + width;
    }

    int bottom() {
        return y + height;
    }

    boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }

    ScreenRect offset(int offsetX, int offsetY) {
        return new ScreenRect(x + offsetX, y + offsetY, width, height);
    }

    ScreenRect inset(int amount) {
        return new ScreenRect(x + amount, y + amount, width - amount * 2, height - amount * 2);
    }
}
