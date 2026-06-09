package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.engine.ResolvedFusionData;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
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
    private static final ScreenRect CATALYST_LABEL = new ScreenRect(372, 181, 40, 8);
    private final SynthesisStationLayout layout = new SynthesisStationLayout();
    private final SynthesisSpatialPrototype spatialPrototype = new SynthesisSpatialPrototype();
    private Button synthesizeButton;
    private Button confirmButton;
    private SynthesisResultOverlay pendingResult;
    private int lastMouseX;
    private int lastMouseY;

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
        addButton(Component.literal("◄"), layout.previousButton, SynthesisStationMenu.BUTTON_PREVIOUS);
        addButton(Component.literal("►"), layout.nextButton, SynthesisStationMenu.BUTTON_NEXT);
        ScreenRect synthesize = absolute(layout.synthesizeButton);
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
        if (synthesizeButton != null) {
            boolean canCraft = menu.canSynthesize()
                    || (menu.selectedProfile().isPresent() && minecraft != null
                            && minecraft.player != null && minecraft.player.getAbilities().instabuild);
            synthesizeButton.active = canCraft && !hasResult;
            synthesizeButton.visible = !hasResult;
        }
        if (confirmButton != null) {
            confirmButton.visible = hasResult;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        SynthesisStationDrawing.window(graphics, absolute(layout.root));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.titleBar));
        renderCategoryTabs(graphics);
        SynthesisStationDrawing.panel(graphics, absolute(layout.mainPanel));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.recipePanel));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.detailPanel));
        SynthesisStationDrawing.recessedPanel(graphics, absolute(layout.reagentPanel));
        SynthesisStationDrawing.tiledWood(graphics, absolute(layout.recipePanel).inset(UiMetrics.INSET_MEDIUM));
        SynthesisStationDrawing.tiledWood(graphics, absolute(layout.detailPanel).inset(UiMetrics.INSET_MEDIUM));
        SynthesisStationDrawing.tiledWood(graphics, absolute(layout.reagentPanel).inset(UiMetrics.INSET_MEDIUM));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.recipeSearch));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.reagentSearch));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.reagentFilter));
        SynthesisStationDrawing.searchBox(graphics, absolute(layout.reagentSort));
        SynthesisRecipeGrid.render(graphics, font, menu, layout, origin(), selectedCategory());
        spatialPrototype.render(graphics, font, currentPlan(), origin());
        renderCatalystSlot(graphics, mouseX, mouseY);
        if (DEBUG_LAYOUT) {
            SynthesisStationLayoutDebug.render(graphics, font, layout, origin());
        }
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
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.fill(8, 226, imageWidth - 8, imageHeight - 7, SynthesisScreenTheme.PANEL_DARKEST);

        SynthesisStationText.drawCenteredFit(
                graphics,
                font,
                roomStatusText(),
                new ScreenRect(40, 7, imageWidth - 80, 12),
                menu.hasValidSynthesisRoom() ? SynthesisScreenTheme.TEXT : SynthesisScreenTheme.BAD
        );
        SynthesisStationText.drawFit(graphics, font, Component.literal("[Reagent Vault]"), layout.reagentSearch.inset(UiMetrics.INSET_MEDIUM), vaultLabelColor());
        SynthesisStationText.drawFit(graphics, font, roomStorageSummary(), new ScreenRect(380, 235, 84, 9), SynthesisScreenTheme.GOOD);
        renderCategoryTabLabels(graphics);

        SynthesisStationText.drawFit(graphics, font, Component.literal("Catalyst"),
                CATALYST_LABEL, SynthesisScreenTheme.MUTED);

        Optional<SynthesisProfile> selected = menu.selectedProfile();
        if (selected.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("screen.zen_atelier.synthesis.no_profiles"), imageWidth / 2, 67, SynthesisScreenTheme.BAD);
            return;
        }

        spatialPrototype.renderLabels(graphics, font);
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
        spatialPrototype.renderOverlay(graphics, font, currentPlan(), menu.roomVaultReagents(), playerInventoryReagents(), origin(), mouseX, mouseY);
        if (pendingResult != null) {
            pendingResult.render(graphics, font, origin());
            if (confirmButton != null) {
                confirmButton.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        UiLayer.TOOLTIP.run(graphics, () -> renderSynthesisTooltips(graphics, mouseX, mouseY));
    }

    private void renderSynthesisTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!spatialPrototype.renderTooltip(graphics, font, currentPlan(), menu.roomVaultReagents(), playerInventoryReagents(), origin(), mouseX, mouseY)) {
            renderRoomStatusTooltip(graphics, mouseX, mouseY);
            renderRoomVaultTooltip(graphics, mouseX, mouseY);
            renderCategoryTooltip(graphics, mouseX, mouseY);
            renderCraftReasonTooltip(graphics, mouseX, mouseY);
            renderCatalystTooltip(graphics, mouseX, mouseY);
            SynthesisRecipeGrid.renderTooltip(graphics, font, menu, layout, origin(), selectedCategory(), mouseX, mouseY);
            renderTooltip(graphics, mouseX, mouseY);
        }
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
        if (menu.canSynthesize()) {
            return;
        }
        if (!absolute(layout.synthesizeButton).contains(mouseX, mouseY)) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        Optional<SynthesisPlan> plan = currentPlan();
        if (plan.isEmpty()) {
            lines.add(Component.literal("No recipe selected."));
        } else {
            lines.add(Component.literal("Cannot craft:").withStyle(s -> s.withColor(SynthesisScreenTheme.BAD)));
            for (var status : plan.get().requirements()) {
                if (!status.satisfied()) {
                    lines.add(Component.literal(SynthesisStationText.requirementLine(status))
                            .withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
                    String qualifier = SynthesisStationText.queryQualifier(status.requirement().query());
                    if (!qualifier.isBlank()) {
                        lines.add(Component.literal("  " + qualifier)
                                .withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
                    }
                }
            }
            if (lines.size() == 1) {
                lines.add(Component.literal("Not enough reagents in storage or inventory.").withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
            }
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (pendingResult != null) {
            // Only the confirm button (rendered above the overlay) should receive clicks
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // spatialPrototype runs first so popup overlays (filter drawer) can block clicks
        // from reaching widgets that sit visually behind them.
        if (spatialPrototype.mouseClicked(mouseX, mouseY, button, hasShiftDown(), currentPlan(), menu.roomVaultReagents(), playerInventoryReagents(), origin())) {
            return true;
        }
        Optional<Integer> hoveredCategory = hoveredCategoryIndex((int) mouseX, (int) mouseY);
        if (hoveredCategory.isPresent()) {
            clickMenuButton(SynthesisStationMenu.BUTTON_CATEGORY_BASE + hoveredCategory.get());
            return true;
        }
        Optional<Integer> hovered = SynthesisRecipeGrid.hoveredProfileIndex(menu, layout, origin(), selectedCategory(), (int) mouseX, (int) mouseY);
        if (hovered.isPresent()) {
            menu.selectProfile(hovered.get());
            clickMenuButton(SynthesisStationMenu.BUTTON_PROFILE_BASE + hovered.get());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (spatialPrototype.mouseScrolled(mouseX, mouseY, scrollY, menu.roomVaultReagents(), playerInventoryReagents(), origin())) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
        if (keyCode == GLFW.GLFW_KEY_R && spatialPrototype.rotateCarriedAt(lastMouseX, lastMouseY, currentPlan(), origin())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void addButton(Component label, ScreenRect localBounds, int buttonId) {
        ScreenRect bounds = absolute(localBounds);
        addRenderableWidget(SynthesisStationButton.build(Button.builder(label, button -> clickMenuButton(buttonId))
                .bounds(bounds.x(), bounds.y(), bounds.width(), bounds.height()),
                SynthesisScreenTheme.ACCENT_DIM));
    }

    private void clickMenuButton(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            if (button == SynthesisStationMenu.BUTTON_SYNTHESIZE) {
                PacketDistributor.sendToServer(spatialPrototype.buildFusionPayload(menu.containerId));
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
        boolean reagent = com.sanhiruzu.atelier.synthesis.item.ReagentItem.getReagent(stack) != null;
        SynthesisStationDrawing.frame(graphics, rect.inset(1), reagent ? 0xAA7FD889 : 0x664A4038);
    }

    private List<ReagentStack> playerInventoryReagents() {
        ArrayList<ReagentStack> reagents = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            ReagentStack reagent = ReagentItem.getReagent(stack);
            if (reagent != null) {
                reagents.add(reagent);
            }
        }
        return reagents;
    }

    private Optional<SynthesisPlan> currentPlan() {
        return menu.currentPlan(spatialPrototype.currentFusion());
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

    private String roomStorageSummary() {
        if (menu.roomStorageStacks() <= 0) {
            return "0";
        }
        return menu.roomStorageStacks() + "/" + menu.roomStorageUnits();
    }

    private int vaultLabelColor() {
        if (!menu.hasValidSynthesisRoom()) {
            return SynthesisScreenTheme.MUTED;
        }
        return menu.roomStorageStacks() > 0 ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.ACCENT_DIM;
    }

    private Component roomStatusText() {
        if (menu.hasFineAtelierRoom()) {
            return Component.literal("Room Bonus: Fine Atelier (Tier " + menu.unlockedRecipeTier() + ")");
        }
        if (menu.hasAtelierRoom()) {
            return Component.literal("Room Bonus: Atelier (Tier " + menu.unlockedRecipeTier() + ")");
        }
        if (menu.hasValidSynthesisRoom()) {
            return Component.literal("Room Bonus: Indoor Room (Tier " + menu.unlockedRecipeTier() + ")");
        }
        return Component.literal("No Atelier Room: Synthesis Unstable");
    }

    private void renderRoomStatusTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!absolute(layout.titleBar).contains(mouseX, mouseY)) {
            return;
        }
        if (menu.hasFineAtelierRoom()) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal("High-quality Atelier detected."),
                    Component.literal("Tier " + menu.unlockedRecipeTier() + " synthesis recipes are available."),
                    Component.literal("Success outcomes are strongly favored.")
            ), mouseX, mouseY);
            return;
        }
        if (menu.hasAtelierRoom()) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal("Atelier room detected."),
                    Component.literal("Tier " + menu.unlockedRecipeTier() + " synthesis recipes are available."),
                    Component.literal("Success outcomes are favored.")
            ), mouseX, mouseY);
            return;
        }
        if (menu.hasValidSynthesisRoom()) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal("The station is inside a room."),
                    Component.literal("Tier " + menu.unlockedRecipeTier() + " recipes are available."),
                    Component.literal("Build an Atelier to unlock advanced synthesis.")
            ), mouseX, mouseY);
            return;
        }
        graphics.renderComponentTooltip(font, java.util.List.of(
                Component.literal("Build an enclosed room around the station."),
                Component.literal("Outside synthesis is highly unstable."),
                Component.literal("Room reagent storage will not connect.")
        ), mouseX, mouseY);
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
        if (!menu.hasValidSynthesisRoom()) {
            graphics.renderComponentTooltip(font, java.util.List.of(
                    Component.literal("Room reagent storage is disconnected."),
                    Component.literal("Place the station and reagent storage inside the same enclosed room.")
            ), mouseX, mouseY);
            return;
        }
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
        }
    }

    private void clearResult() {
        pendingResult = null;
    }

    private ScreenRect absolute(ScreenRect rect) {
        return rect.offset(leftPos, topPos);
    }

    private ScreenRect origin() {
        return new ScreenRect(leftPos, topPos, 0, 0);
    }
}
