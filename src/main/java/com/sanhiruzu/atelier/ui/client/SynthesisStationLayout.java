package com.sanhiruzu.atelier.ui.client;

final class SynthesisStationLayout {
    static final int WIDTH = 480;
    static final int HEIGHT = 326;
    static final int RECIPE_ROWS = 6;
    private static final int CATEGORY_TAB_WIDTH = 50;
    private static final int CATEGORY_TAB_HEIGHT = 18;
    private static final int CATEGORY_TAB_STEP = 53;
    private static final int RECIPE_CELL_HEIGHT = 21;
    private static final int SLOT_SIZE = 18;

    final ScreenRect root = new ScreenRect(0, 0, WIDTH, HEIGHT);
    final ScreenRect titleBar = new ScreenRect(6, 5, 468, 16);
    final ScreenRect mainPanel = new ScreenRect(8, 43, 464, 178);
    final ScreenRect recipePanel = new ScreenRect(14, 49, 134, 166);
    final ScreenRect detailPanel = new ScreenRect(152, 49, 315, 166);
    final ScreenRect reagentPanel = new ScreenRect(8, 226, 464, 93);
    final ScreenRect recipeSearch = new ScreenRect(19, 55, 124, 17);
    final ScreenRect reagentSearch = new ScreenRect(14, 231, 138, 15);
    final ScreenRect reagentFilter = new ScreenRect(156, 231, 96, 15);
    final ScreenRect reagentSort = new ScreenRect(256, 231, 119, 15);
    final ScreenRect synthesizeButton = new ScreenRect(397, 187, 58, 24);
    final ScreenRect previousButton = new ScreenRect(2, 29, 10, 15);
    final ScreenRect nextButton = new ScreenRect(468, 29, 10, 15);
    final ScreenRect detailIcon = new ScreenRect(305, 86, 24, 24);
    final ScreenRect detailName = new ScreenRect(20, 143, 103, 9);
    final ScreenRect detailCategory = new ScreenRect(20, 153, 103, 9);
    final ScreenRect requirementsTitle = new ScreenRect(20, 163, 103, 9);
    final ScreenRect requirementsList = new ScreenRect(20, 173, 103, 14);
    final ScreenRect successBar = new ScreenRect(150, 159, 70, 6);
    final ScreenRect successPercent = new ScreenRect(224, 156, 38, 9);
    final ScreenRect outcomeList = new ScreenRect(150, 169, 112, 18);
    final ScreenRect emptyRecipeCategoryLabel = new ScreenRect(19, 112, 124, 9);

    ScreenRect categoryTab(int index) {
        return new ScreenRect(21 + index * CATEGORY_TAB_STEP, 25, CATEGORY_TAB_WIDTH, CATEGORY_TAB_HEIGHT);
    }

    ScreenRect categoryUnderline(int index) {
        ScreenRect tab = categoryTab(index);
        return new ScreenRect(tab.x() + 4, tab.bottom() - 2, tab.width() - 8, 2);
    }

    ScreenRect recipeCell(int index) {
        return new ScreenRect(19, 76 + index * RECIPE_CELL_HEIGHT, 124, 20);
    }

    ScreenRect core() {
        return new ScreenRect(235, 83, 26, 26);
    }

    ScreenRect synthesisNode(int index) {
        return switch (index) {
            case 0 -> new ScreenRect(178, 57, 22, 22);
            case 1 -> new ScreenRect(235, 54, 22, 22);
            case 2 -> new ScreenRect(292, 58, 22, 22);
            case 3 -> new ScreenRect(154, 99, 22, 22);
            case 4 -> new ScreenRect(185, 125, 22, 22);
            case 5 -> new ScreenRect(239, 121, 22, 22);
            default -> new ScreenRect(292, 110, 22, 22);
        };
    }

    ScreenRect inventorySlot(int row, int column) {
        int slot = row * 9 + column;
        return new ScreenRect(16 + (slot % 18) * SLOT_SIZE, 220 + (slot / 18) * SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }

    ScreenRect hotbarSlot(int column) {
        return new ScreenRect(16 + column * SLOT_SIZE, 256, SLOT_SIZE, SLOT_SIZE);
    }

    ScreenRect roomVaultSlot(int column) {
        return new ScreenRect(178 + column * SLOT_SIZE, 238, SLOT_SIZE, SLOT_SIZE);
    }
}
