package com.sanhiruzu.atelier.ui.client;

final class UiSkins {
    private static final UiSkin ACTIVE = select();

    private UiSkins() {
    }

    static UiSkin active() {
        return ACTIVE;
    }

    private static UiSkin select() {
        String value = System.getProperty("zen_atelier.uiSkin",
                System.getenv().getOrDefault("ZEN_ATELIER_UI_SKIN", "classic")).trim().toLowerCase();
        return switch (value) {
            case "nine", "nineslice", "nine_slice" -> AtelierNineSliceSkin.INSTANCE;
            default -> AtelierSkin.INSTANCE;
        };
    }
}
