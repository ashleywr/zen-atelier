package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.ResolvedFusionData;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringBasketItem;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import com.sanhiruzu.atelier.ui.network.ReagentVaultSyncPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisResultPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SynthesisStationScreen extends AbstractContainerScreen<SynthesisStationMenu> {
    private static final boolean DEBUG_LAYOUT = Boolean.getBoolean("zen_atelier.debugSynthesisLayout");
    private static final ScreenRect CATALYST_SLOT = new ScreenRect(372, 190, 18, 18);
    private static final ScreenRect CATALYST_LABEL = new ScreenRect(336, 181, 40, 8);
    private final SynthesisStationLayout layout = new SynthesisStationLayout();
    private final SynthesisSpatialPrototype spatialPrototype = new SynthesisSpatialPrototype();
    private Button synthesizeButton;
    private Button confirmButton;
    private Button previousButton;
    private Button nextButton;
    private SynthesisResultOverlay pendingResult;
    private int failureImpactTicks;
    private int lastMouseX;
    private int lastMouseY;
    private ModeState modeState = ModeState.initial();

    public SynthesisStationScreen(SynthesisStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = SynthesisStationLayout.WIDTH;
        imageHeight = SynthesisStationLayout.HEIGHT;
        inventoryLabelX = 14;
        inventoryLabelY = 231;
    }

    @Override
    protected void init() {
        super.init();
        previousButton = addButton(Component.literal("◄"), layout.previousButton, SynthesisStationMenu.BUTTON_PREVIOUS);
        nextButton = addButton(Component.literal("►"), layout.nextButton, SynthesisStationMenu.BUTTON_NEXT);
        ScreenRect synthesize = absolute(layout.boardModeSynthesizeButton());
        synthesizeButton = addRenderableWidget(SynthesisStationButton.build(Button.builder(
                        Component.literal("Craft"),
                        button -> clickMenuButton(SynthesisStationMenu.BUTTON_SYNTHESIZE)
                )
                .bounds(synthesize.x(), synthesize.y(), synthesize.width(), synthesize.height()),
                SynthesisScreenTheme.ACCENT));
        confirmButton = addRenderableWidget(SynthesisStationButton.build(Button.builder(
                        Component.literal("Confirm"),
                        button -> clearResult()
                )
                .bounds(synthesize.x(), synthesize.y(), synthesize.width(), synthesize.height()),
                SynthesisScreenTheme.GOOD));
        confirmButton.visible = false;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean hasResult = pendingResult != null;
        if (failureImpactTicks > 0) {
            failureImpactTicks--;
        }
        if (synthesizeButton != null) {
            boolean canCraft = spatialPrototype.projection(currentPlan(), menu.roomVaultReagents(), playerInventoryReagents()).canSynthesize()
                    || (menu.selectedProfile().isPresent() && minecraft != null
                            && minecraft.player != null && minecraft.player.getAbilities().instabuild);
            boolean boardMode = modeState.mode() == ScreenMode.BOARD;
            ScreenRect synthesizeBounds = absolute(layout.boardModeSynthesizeButton());
            synthesizeButton.setX(synthesizeBounds.x());
            synthesizeButton.setY(synthesizeBounds.y());
            synthesizeButton.setWidth(synthesizeBounds.width());
            synthesizeButton.setHeight(synthesizeBounds.height());
            synthesizeButton.active = boardMode && canCraft && !hasResult;
            synthesizeButton.visible = boardMode && !hasResult;
        }
        boolean recipeBookMode = modeState.mode() == ScreenMode.RECIPE_BOOK;
        if (previousButton != null) {
            previousButton.visible = recipeBookMode;
            previousButton.active = recipeBookMode;
        }
        if (nextButton != null) {
            nextButton.visible = recipeBookMode;
            nextButton.active = recipeBookMode;
        }
        if (confirmButton != null) {
            confirmButton.visible = hasResult && failureImpactTicks <= 0;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        SynthesisStationDrawing.window(graphics, absolute(layout.root));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.titleBar));
        if (modeState.mode() == ScreenMode.RECIPE_BOOK) {
            renderRecipeBookBackground(graphics);
        } else {
            renderBoardModeBackground(graphics, mouseX, mouseY);
        }
        if (DEBUG_LAYOUT) {
            SynthesisStationLayoutDebug.render(graphics, font, layout, origin());
        }
    }

    private void renderRecipeBookBackground(GuiGraphics graphics) {
        renderCategoryTabs(graphics);
        SynthesisStationDrawing.panel(graphics, absolute(layout.mainPanel));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.recipeBookLeftPage()));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.recipeBookRightPage()));
        SynthesisStationDrawing.tiledWood(graphics, absolute(layout.recipeBookLeftPage()).inset(UiMetrics.INSET_MEDIUM));
        SynthesisStationDrawing.tiledWood(graphics, absolute(layout.recipeBookRightPage()).inset(UiMetrics.INSET_MEDIUM));
        SynthesisRecipeGrid.render(graphics, font, menu, layout, origin(), selectedCategory());
    }

    private void renderBoardModeBackground(GuiGraphics graphics, int mouseX, int mouseY) {
        SynthesisStationDrawing.panel(graphics, absolute(layout.mainPanel));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.boardModePalettePanel()));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.boardModeProgressPanel()));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.boardModeBackButton()));
        ScreenRect paletteFill = absolute(layout.boardModePalettePanel()).inset(UiMetrics.INSET_MEDIUM);
        ScreenRect progressFill = absolute(layout.boardModeProgressPanel()).inset(UiMetrics.INSET_MEDIUM);
        ScreenRect boardFill = absolute(layout.boardModeBoardArea()).inset(2);
        graphics.fill(paletteFill.x(), paletteFill.y(), paletteFill.right(), paletteFill.bottom(), 0xCC18130F);
        graphics.fill(progressFill.x(), progressFill.y(), progressFill.right(), progressFill.bottom(), 0xCC171411);
        graphics.fill(boardFill.x(), boardFill.y(), boardFill.right(), boardFill.bottom(), 0xCC14171B);
        spatialPrototype.renderBoardMode(
                graphics,
                font,
                currentPlan(),
                menu.roomVaultReagents(),
                playerInventoryReagents(),
                origin(),
                layout.boardModeBoardArea(),
                layout.boardModePalettePanel(),
                layout.boardModeProgressPanel()
        );
        renderCatalystSlot(graphics, mouseX, mouseY);
    }

    private void renderCatalystSlot(GuiGraphics graphics, int mouseX, int mouseY) {
        ScreenRect rect = absolute(CATALYST_SLOT);
        SynthesisStationDrawing.slotFrame(graphics, rect);
        if (rect.contains(mouseX, mouseY)) {
            graphics.fill(rect.x() + 1, rect.y() + 1, rect.right() - 1, rect.bottom() - 1, 0x80FFFFFF);
        }
        ItemStack stack = menu.getSlot(SynthesisStationMenu.CATALYST_SLOT_INDEX).getItem();
        if (!stack.isEmpty()) {
            graphics.renderFakeItem(stack, rect.x() + 1, rect.y() + 1);
            graphics.renderItemDecorations(font, stack, rect.x() + 1, rect.y() + 1);
            SynthesisStationDrawing.frame(graphics, rect.inset(1), 0xAAB3E5FF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (modeState.mode() == ScreenMode.RECIPE_BOOK) {
            graphics.fill(8, 226, imageWidth - 8, imageHeight - 7, SynthesisScreenTheme.PANEL_DARKEST);
            renderRecipeBookLabels(graphics);
            return;
        }

        boolean catalystActive = !menu.getSlot(SynthesisStationMenu.CATALYST_SLOT_INDEX).getItem().isEmpty();
        SynthesisStationText.drawCenteredFit(graphics, font, Component.literal("Back"),
                layout.boardModeBackButton().inset(3), SynthesisScreenTheme.TEXT);
        menu.selectedProfile().ifPresent(profile ->
                graphics.drawCenteredString(font, SynthesisStationText.fitWidth(font, SynthesisStationText.profileName(profile), 180), imageWidth / 2, 28, SynthesisScreenTheme.ACCENT));
        SynthesisStationDrawing.smallIcon(graphics, CATALYST_LABEL.x() - 10, CATALYST_LABEL.y() - 1, catalystActive ? 0xFFA5E9FF : 0xFF7C736B);
        SynthesisStationText.drawFit(graphics, font, Component.literal("Cat."),
                CATALYST_LABEL, catalystActive ? SynthesisScreenTheme.ACCENT_DIM : SynthesisScreenTheme.MUTED);

        Optional<SynthesisProfile> selected = menu.selectedProfile();
        if (selected.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.zen_atelier.synthesis.no_profiles"), imageWidth / 2, 67, SynthesisScreenTheme.BAD);
            return;
        }

        spatialPrototype.renderLabels(graphics, font);
    }

    private void renderRecipeBookLabels(GuiGraphics graphics) {
        renderCategoryTabLabels(graphics);
        menu.selectedProfile().ifPresent(profile ->
                SynthesisRecipeDetails.render(graphics, font, menu, layout, profile));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        // Bypass AbstractContainerScreen.render() to avoid its slot-item and slot-highlight
        // rendering, which bleeds non-reagent inventory items through the custom palette layer.
        // We replicate the parts we need: background, bg, widgets, labels, then our overlay.
        renderBackground(graphics, mouseX, mouseY, partialTick);
        renderBg(graphics, partialTick, mouseX, mouseY);
        for (net.minecraft.client.gui.components.Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos, topPos, 0.0F);
        renderLabels(graphics, mouseX, mouseY);
        graphics.pose().popPose();
        if (modeState.mode() == ScreenMode.BOARD) {
            spatialPrototype.renderBoardModeOverlay(
                    graphics,
                    font,
                    currentPlan(),
                    origin(),
                    layout.boardModeBoardArea(),
                    mouseX,
                    mouseY
            );
        }
        if (pendingResult != null && failureImpactTicks > 0) {
            UiLayer.POPUP.run(graphics, () -> renderFailureImpact(graphics, partialTick));
        } else if (pendingResult != null) {
            UiLayer.POPUP.run(graphics, () -> pendingResult.render(graphics, font, origin()));
            if (confirmButton != null) {
                UiLayer.POPUP_CONTENT.run(graphics, () -> confirmButton.render(graphics, mouseX, mouseY, partialTick));
            }
        }
        UiLayer.TOOLTIP.run(graphics, () -> renderSynthesisTooltips(graphics, mouseX, mouseY));
    }

    private void renderSynthesisTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (UiModalInputGate.blockUnderlyingTooltips(pendingResult != null)) {
            return;
        }
        if (modeState.mode() == ScreenMode.BOARD) {
            boolean renderedBoardTooltip = spatialPrototype.renderBoardModeTooltip(
                    graphics,
                    font,
                    currentPlan(),
                    menu.roomVaultReagents(),
                    playerInventoryReagents(),
                    origin(),
                    layout.boardModeBoardArea(),
                    layout.boardModePalettePanel(),
                    mouseX,
                    mouseY
            );
            if (renderedBoardTooltip) {
                return;
            }
            renderCraftReasonTooltip(graphics, mouseX, mouseY);
            renderCatalystTooltip(graphics, mouseX, mouseY);
            return;
        } else if (modeState.mode() == ScreenMode.RECIPE_BOOK) {
            renderCategoryTooltip(graphics, mouseX, mouseY);
            SynthesisRecipeGrid.renderTooltip(graphics, font, menu, layout, origin(), selectedCategory(), mouseX, mouseY);
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderFailureImpact(GuiGraphics graphics, float partialTick) {
        ScreenRect root = absolute(layout.root);
        float elapsed = SynthesisResultOverlay.FAILURE_IMPACT_TICKS - failureImpactTicks + partialTick;
        float progress = Math.clamp(elapsed / SynthesisResultOverlay.FAILURE_IMPACT_TICKS, 0.0F, 1.0F);
        int centerX = root.x() + root.width() / 2;
        int centerY = root.y() + root.height() / 2 - 12;
        int jitter = failureImpactTicks > 13 ? ((failureImpactTicks & 1) == 0 ? 3 : -3) : 0;

        graphics.fill(root.x(), root.y(), root.right(), root.bottom(), 0xE6100D0A);
        renderSmokePuff(graphics, centerX - 82 + jitter, centerY - 28, 92 + (int) (progress * 36), 0x805D5750);
        renderSmokePuff(graphics, centerX - 18 - jitter, centerY - 42, 118 + (int) (progress * 44), 0x966B655C);
        renderSmokePuff(graphics, centerX + 58 + jitter, centerY - 20, 88 + (int) (progress * 32), 0x78504C47);
        renderSmokePuff(graphics, centerX - 52 - jitter, centerY + 34, 108 + (int) (progress * 28), 0x70534B42);
        renderSmokePuff(graphics, centerX + 42 + jitter, centerY + 32, 96 + (int) (progress * 30), 0x7A625A50);

        graphics.fill(root.x(), root.y(), root.right(), root.bottom(), 0x65000000);
        graphics.drawCenteredString(font, Component.literal("SYNTHESIS FAILED"), centerX + jitter, centerY - 3, SynthesisScreenTheme.BAD);
        graphics.drawCenteredString(font, Component.literal("Smoke floods the apparatus..."), centerX, centerY + 13, SynthesisScreenTheme.MUTED);
    }

    private static void renderSmokePuff(GuiGraphics graphics, int centerX, int centerY, int size, int color) {
        int half = size / 2;
        int inset = Math.max(8, size / 5);
        graphics.fill(centerX - half + inset, centerY - half, centerX + half - inset, centerY + half, color);
        graphics.fill(centerX - half, centerY - half + inset, centerX + half, centerY + half - inset, color);
    }

    private void renderCatalystTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!absolute(CATALYST_SLOT).contains(mouseX, mouseY)) return;
        ItemStack stack = menu.getSlot(SynthesisStationMenu.CATALYST_SLOT_INDEX).getItem();
        if (!stack.isEmpty()) {
            graphics.renderTooltip(font, stack, mouseX, mouseY);
        } else {
            graphics.renderTooltip(font,
                    Component.translatable("tooltip.zen_atelier.catalyst.empty"), mouseX, mouseY);
        }
    }

    private void renderCraftReasonTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        SynthesisBoardProjection projection = spatialPrototype.projection(
                currentPlan(),
                menu.roomVaultReagents(),
                playerInventoryReagents()
        );
        if (projection.canSynthesize()) {
            return;
        }
        if (!absolute(layout.boardModeSynthesizeButton()).contains(mouseX, mouseY)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        if (projection.placedPlan().isEmpty()) {
            lines.add(Component.literal("No recipe selected."));
        } else {
            SynthesisPlan placedPlan = projection.placedPlan().get();
            lines.addAll(craftBlockerText(SynthesisDisplayModel.from(
                    placedPlan,
                    projection.placedReagents().entries(),
                    List.of(),
                    List.of()
            )));
            if (lines.size() == 1) {
                lines.add(Component.literal("Not enough reagents in storage or inventory.").withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    static List<Component> craftBlockerText(SynthesisDisplayModel display) {
        ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Cannot craft:").withStyle(s -> s.withColor(SynthesisScreenTheme.BAD)));
        List<SynthesisDisplayModel.Line> missingEssences = display.essences().stream()
                .filter(line -> !line.satisfied())
                .toList();
        if (!missingEssences.isEmpty()) {
            lines.add(Component.literal("Essences").withStyle(s -> s.withColor(SynthesisScreenTheme.ACCENT)));
            for (SynthesisDisplayModel.Line line : missingEssences) {
                lines.add(Component.literal(craftBlockerLine(line)).withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
            }
        }
        List<SynthesisDisplayModel.Line> missingElements = display.elements().stream()
                .filter(line -> !line.satisfied())
                .toList();
        if (!missingElements.isEmpty()) {
            lines.add(Component.literal("Elements").withStyle(s -> s.withColor(SynthesisScreenTheme.ACCENT)));
            for (SynthesisDisplayModel.Line line : missingElements) {
                lines.add(Component.literal(craftBlockerLine(line)).withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
            }
        }
        return lines;
    }

    private static String craftBlockerLine(SynthesisDisplayModel.Line line) {
        return line.available() + "/" + line.required() + " " + line.label();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingResult != null) {
            Optional<ScreenRect> confirmBounds = resultConfirmBounds();
            if (confirmBounds.isPresent()
                    && UiModalInputGate.allowsClick(true, confirmBounds.get(), mouseX, mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            return true;
        }
        if (modeState.mode() == ScreenMode.BOARD) {
            if (absolute(layout.boardModeBackButton()).contains((int) mouseX, (int) mouseY)) {
                spatialPrototype.resetAfterSynthesis();
                modeState = modeState.backToRecipeBook();
                return true;
            }
            if (spatialPrototype.mouseClickedBoardMode(
                    mouseX,
                    mouseY,
                    button,
                    hasShiftDown(),
                    currentPlan(),
                    menu.roomVaultReagents(),
                    playerInventoryReagents(),
                    origin(),
                    layout.boardModeBoardArea(),
                    layout.boardModePalettePanel())) {
                return true;
            }
            if (allowsBoardModeSuperClick(absolute(layout.boardModeSynthesizeButton()), absolute(CATALYST_SLOT), mouseX, mouseY)) {
                return super.mouseClicked(mouseX, mouseY, button);
            }
            if (absolute(layout.root).contains((int) mouseX, (int) mouseY)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Optional<Integer> hoveredCategory = hoveredCategoryIndex((int) mouseX, (int) mouseY);
        if (hoveredCategory.isPresent()) {
            clickMenuButton(SynthesisStationMenu.BUTTON_CATEGORY_BASE + hoveredCategory.get());
            return true;
        }
        Optional<Integer> hovered = SynthesisRecipeGrid.hoveredProfileIndex(menu, layout, origin(), selectedCategory(), (int) mouseX, (int) mouseY);
        if (hovered.isPresent()) {
            ModeState previous = modeState;
            menu.selectProfile(hovered.get());
            clickMenuButton(SynthesisStationMenu.BUTTON_PROFILE_BASE + hovered.get());
            modeState = modeState.enterBoard(hovered.get());
            if (modeState.selectedProfileChangedFrom(previous)) {
                spatialPrototype.resetAfterSynthesis();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (UiModalInputGate.consumeScroll(pendingResult != null)) {
            return true;
        }
        if (modeState.mode() == ScreenMode.BOARD && spatialPrototype.mouseScrolledBoardMode(
                mouseX,
                mouseY,
                scrollY,
                currentPlan(),
                menu.roomVaultReagents(),
                playerInventoryReagents(),
                origin(),
                layout.boardModePalettePanel())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Optional<ScreenRect> resultConfirmBounds() {
        if (confirmButton == null || !confirmButton.visible) {
            return Optional.empty();
        }
        return Optional.of(new ScreenRect(confirmButton.getX(), confirmButton.getY(), confirmButton.getWidth(), confirmButton.getHeight()));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (pendingResult != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE) {
                clearResult();
                return true;
            }
            return false;
        }
        if (modeState.mode() == ScreenMode.BOARD && keyCode == GLFW.GLFW_KEY_R && spatialPrototype.rotateCarriedAt(lastMouseX, lastMouseY, currentPlan(), origin())) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_J) {
            copySynthesisDebugState();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void copySynthesisDebugState() {
        if (minecraft == null) {
            return;
        }
        String json = spatialPrototype.buildState(
                currentPlan(),
                menu.roomVaultReagents(),
                playerInventoryReagents()
        ).toDebugJson();
        minecraft.keyboardHandler.setClipboard(json);
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("message.zen_atelier.synthesis.debug_copied"),
                    true
            );
        }
    }

    private Button addButton(Component label, ScreenRect localBounds, int buttonId) {
        ScreenRect bounds = absolute(localBounds);
        return addRenderableWidget(SynthesisStationButton.build(Button.builder(label, button -> clickMenuButton(buttonId))
                .bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height()),
                SynthesisScreenTheme.ACCENT_DIM));
    }

    private void clickMenuButton(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            if (button == SynthesisStationMenu.BUTTON_SYNTHESIZE) {
                PacketDistributor.sendToServer(spatialPrototype.buildFusionPayload(
                        menu.containerId,
                        currentPlan(),
                        menu.roomVaultReagents(),
                        playerInventoryReagents()
                ));
            } else if (button == SynthesisStationMenu.BUTTON_PREVIOUS) {
                menu.moveSelection(-1);
            } else if (button == SynthesisStationMenu.BUTTON_NEXT) {
                menu.moveSelection(1);
            } else if (button >= SynthesisStationMenu.BUTTON_PROFILE_BASE) {
                menu.selectProfile(button - SynthesisStationMenu.BUTTON_PROFILE_BASE);
            } else if (button >= SynthesisStationMenu.BUTTON_CATEGORY_BASE) {
                menu.selectCategory(button - SynthesisStationMenu.BUTTON_CATEGORY_BASE);
            }
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
        }
    }

    private void renderCategoryTabs(GuiGraphics graphics) {
        String selected = selectedCategory();
        for (int i = 0; i < SynthesisRecipeCategory.orderedIds().size(); i++) {
            String category = SynthesisRecipeCategory.orderedIds().get(i);
            boolean active = menu.profileCountInCategory(category) > 0;
            boolean isSelected = category.equals(selected);
            SynthesisStationDrawing.tab(graphics, absolute(layout.categoryTab(i)), SynthesisRecipeCategory.color(category), isSelected, active);
        }
    }

    private void renderCategoryTabLabels(GuiGraphics graphics) {
        String selected = selectedCategory();
        for (int i = 0; i < SynthesisRecipeCategory.orderedIds().size(); i++) {
            String category = SynthesisRecipeCategory.orderedIds().get(i);
            boolean active = menu.profileCountInCategory(category) > 0;
            int color = active ? SynthesisRecipeCategory.color(category) : SynthesisScreenTheme.MUTED;
            if (category.equals(selected)) {
                color = SynthesisScreenTheme.TEXT;
            }
            SynthesisStationText.drawCenteredFit(
                    graphics,
                    font,
                    Component.translatable(SynthesisRecipeCategory.tabTranslationKey(category)),
                    layout.categoryTab(i).inset(4),
                    color
            );
        }
    }

    private void renderCategoryTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Optional<Integer> hovered = hoveredCategoryIndex(mouseX, mouseY);
        if (hovered.isEmpty()) {
            return;
        }
        String category = SynthesisRecipeCategory.orderedIds().get(hovered.get());
        graphics.renderTooltip(font, Component.translatable(SynthesisRecipeCategory.translationKey(category)), mouseX, mouseY);
    }

    private void renderSlots(GuiGraphics graphics) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                int slot = row * 9 + column + 9;
                renderVaultSlotFrame(graphics, layout.inventorySlot(row, column), slot);
            }
        }
        for (int column = 0; column < 9; ++column) {
            renderRoomVaultSlot(graphics, column);
        }
        for (int column = 0; column < 9; ++column) {
            renderVaultSlotFrame(graphics, layout.hotbarSlot(column), column);
        }
    }

    private void renderRoomVaultSlot(GuiGraphics graphics, int column) {
        ScreenRect rect = absolute(layout.roomVaultSlot(column));
        SynthesisStationDrawing.slotFrame(graphics, rect);
        java.util.List<ReagentStack> reagents = menu.roomVaultReagents();
        if (column >= reagents.size()) {
            return;
        }
        ReagentStack reagent = reagents.get(column);
        graphics.renderFakeItem(ReagentItem.createStack(reagent), rect.x() + 1, rect.y() + 1);
        SynthesisStationDrawing.frame(graphics, rect.inset(1), 0xAA7FD889);
    }

    private void renderVaultSlotFrame(GuiGraphics graphics, ScreenRect localRect, int inventorySlot) {
        ScreenRect rect = absolute(localRect);
        SynthesisStationDrawing.slotFrame(graphics, rect);
        ItemStack stack = menu.getSlot(inventorySlotToMenuSlot(inventorySlot)).getItem();
        if (stack.isEmpty()) {
            return;
        }
        boolean reagent = ReagentItem.getReagent(stack) != null;
        if (!reagent && GatheringBasketItem.isBasket(stack) && !GatheringBasketItem.entries(stack).isEmpty()) {
            reagent = true;
        }
        SynthesisStationDrawing.frame(graphics, rect.inset(1), reagent ? 0xAA7FD889 : 0x664A4038);
    }

    private List<ReagentStack> playerInventoryReagents() {
        ArrayList<ReagentStack> reagents = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            ReagentStack reagent = ReagentItem.getReagent(stack);
            if (reagent == null && GatheringBasketItem.isBasket(stack)) {
                reagents.addAll(GatheringBasketItem.entries(stack));
            } else if (reagent != null) {
                reagents.add(reagent);
            }
        }
        return reagents;
    }

    private Optional<SynthesisPlan> currentPlan() {
        ResolvedFusionData fusion = spatialPrototype.currentFusion();
        ResolvedFusionData catalyst = menu.catalystFusion();
        if (!catalyst.fusedAffixes().isEmpty() || catalyst.qualityBonus() > 0) {
            fusion = fusion.withCatalyst(catalyst.fusedAffixes(), catalyst.qualityBonus());
        }
        return menu.currentPlan(fusion);
    }

    private String selectedCategory() {
        return menu.selectedProfile()
                .map(SynthesisProfile::category)
                .orElse(SynthesisRecipeCategory.MATERIALS);
    }

    private Optional<Integer> hoveredCategoryIndex(int mouseX, int mouseY) {
        for (int i = 0; i < SynthesisRecipeCategory.orderedIds().size(); i++) {
            if (absolute(layout.categoryTab(i)).contains(mouseX, mouseY)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private static int inventorySlotToMenuSlot(int inventorySlot) {
        if (inventorySlot >= 9) {
            return inventorySlot - 9;
        }
        return 27 + inventorySlot;
    }

    static boolean allowsBoardModeSuperClick(ScreenRect synthesizeButton, ScreenRect catalystSlot, double mouseX, double mouseY) {
        return synthesizeButton.contains((int) mouseX, (int) mouseY)
                || catalystSlot.contains((int) mouseX, (int) mouseY);
    }

    private String roomStorageSummary() {
        if (menu.roomStorageStacks() <= 0) {
            return "0";
        }
        return menu.roomStorageStacks() + "/" + menu.roomStorageUnits();
    }

    private int vaultLabelColor() {
        return menu.roomStorageStacks() > 0 ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.ACCENT_DIM;
    }

    private void renderRoomVaultTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Optional<Integer> hovered = hoveredRoomVaultIndex(mouseX, mouseY);
        if (hovered.isPresent()) {
            java.util.List<ReagentStack> reagents = menu.roomVaultReagents();
            if (hovered.get() < reagents.size()) {
                ReagentStack reagent = reagents.get(hovered.get());
                graphics.renderComponentTooltip(font, reagentTooltip(reagent), mouseX, mouseY);
                return;
            }
        }

        if (!absolute(layout.reagentSearch).contains(mouseX, mouseY)
                && !absolute(new ScreenRect(380, 231, 84, 15)).contains(mouseX, mouseY)) {
            return;
        }
        java.util.List<ReagentStack> reagents = menu.roomVaultReagents();
        if (reagents.isEmpty()) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal("No room reagent storage entries found."),
                    Component.literal("Deposit reagent vials into storage in this room.")
            ), mouseX, mouseY);
            return;
        }

        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.literal("Connected room vault: " + menu.roomStorageUnits() + " units"));
        int limit = Math.min(8, reagents.size());
        for (int i = 0; i < limit; i++) {
            lines.add(Component.literal(compactReagentLine(reagents.get(i))));
        }
        if (reagents.size() > limit) {
            lines.add(Component.literal("+" + (reagents.size() - limit) + " more"));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private Optional<Integer> hoveredRoomVaultIndex(int mouseX, int mouseY) {
        for (int i = 0; i < 9; i++) {
            if (absolute(layout.roomVaultSlot(i)).contains(mouseX, mouseY)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private java.util.List<Component> reagentTooltip(ReagentStack reagent) {
        java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.translatable("zen_atelier.reagent." + reagentPath(reagent.reagentId())));
        lines.add(Component.literal("Amount: " + reagent.amount()));
        lines.add(Component.literal("Tier " + reagent.tier() + "  Quality " + reagent.quality() + "  Purity " + reagent.purity()));
        if (!reagent.elements().isEmpty()) {
            lines.add(nounValueLine("Elements: ", reagent.elements()));
        }
        if (!reagent.traits().isEmpty()) {
            lines.add(nounListLine("Traits: ", reagent.traits()));
        }
        return lines;
    }

    private static Component nounValueLine(String prefix, java.util.Map<String, Integer> values) {
        MutableComponent line = Component.literal(prefix);
        int index = 0;
        for (java.util.Map.Entry<String, Integer> entry : values.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .toList()) {
            if (index++ > 0) {
                line.append(Component.literal(", "));
            }
            line.append(SynthesisNoun.component(entry.getKey()));
            line.append(Component.literal(" " + entry.getValue()));
        }
        return line;
    }

    private static Component nounListLine(String prefix, java.util.List<String> ids) {
        MutableComponent line = Component.literal(prefix);
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                line.append(Component.literal(", "));
            }
            line.append(SynthesisNoun.component(ids.get(i)));
        }
        return line;
    }

    private static String compactReagentLine(ReagentStack reagent) {
        return reagent.amount() + "x " + SynthesisStationText.shortLabel(reagent.reagentId()) + " T" + reagent.tier();
    }

    private static String reagentPath(String id) {
        return id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
    }

    public void handleReagentVaultSync(ReagentVaultSyncPayload payload) {
        if (payload.containerId() == menu.containerId) {
            menu.setClientRoomVaultReagents(payload.decodeEntries());
        }
    }

    public void handleSynthesisResult(SynthesisResultPayload payload) {
        if (payload.containerId() == menu.containerId) {
            pendingResult = new SynthesisResultOverlay(payload.outcomeClass(), payload.outputs(), payload.byproducts());
            failureImpactTicks = SynthesisResultOverlay.impactTicksFor(payload.outcomeClass());
            spatialPrototype.resetAfterSynthesis();
        }
    }

    private void clearResult() {
        pendingResult = null;
        failureImpactTicks = 0;
    }

    private ScreenRect absolute(ScreenRect rect) {
        return rect.offset(leftPos, topPos);
    }

    private ScreenRect origin() {
        return new ScreenRect(leftPos, topPos, 0, 0);
    }

    enum ScreenMode {
        RECIPE_BOOK,
        BOARD
    }

    record ModeState(ScreenMode mode, int selectedProfileIndex) {
        static ModeState initial() {
            return new ModeState(ScreenMode.RECIPE_BOOK, 0);
        }

        ModeState enterBoard(int profileIndex) {
            return new ModeState(ScreenMode.BOARD, Math.max(0, profileIndex));
        }

        ModeState backToRecipeBook() {
            return new ModeState(ScreenMode.RECIPE_BOOK, selectedProfileIndex);
        }

        boolean selectedProfileChangedFrom(ModeState previous) {
            return previous != null && selectedProfileIndex != previous.selectedProfileIndex;
        }
    }
}
