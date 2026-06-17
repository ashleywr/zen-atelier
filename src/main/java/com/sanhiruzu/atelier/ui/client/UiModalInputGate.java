package com.sanhiruzu.atelier.ui.client;

final class UiModalInputGate {
    private UiModalInputGate() {
    }

    static boolean blocks(boolean active) {
        return active;
    }

    static boolean allowsClick(boolean active, ScreenRect allowed, double mouseX, double mouseY) {
        return !active || allowed.contains((int) mouseX, (int) mouseY);
    }

    static boolean consumeClickUnlessAllowed(boolean active, ScreenRect allowed, double mouseX, double mouseY) {
        return active && !allowed.contains((int) mouseX, (int) mouseY);
    }

    static boolean consumeScroll(boolean active) {
        return active;
    }

    static boolean blockUnderlyingTooltips(boolean active) {
        return active;
    }
}
