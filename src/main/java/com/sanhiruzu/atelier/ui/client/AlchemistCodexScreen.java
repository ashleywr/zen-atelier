package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileRegistry;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisRequirement;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public final class AlchemistCodexScreen extends Screen {
    private static final CodexMetrics METRICS = CodexMetrics.DEFAULT;

    private Mode mode = Mode.GOALS;
    private Filter filter = Filter.ALL;
    private String selectedSourceId;
    private String selectedRecipeId;
    private int sourceScroll;
    private int goalScroll;

    public AlchemistCodexScreen() {
        super(Component.translatable("screen.zen_atelier.alchemist_codex"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        ScreenRect root = root();
        SynthesisStationDrawing.window(graphics, root);
        graphics.drawString(font, title, root.x() + 14, root.y() + 10, SynthesisScreenTheme.TEXT, false);
        renderModeTabs(graphics, root, mouseX, mouseY);

        if (mode == Mode.SOURCES) {
            renderSources(graphics, root, mouseX, mouseY);
        } else {
            renderGoals(graphics, root, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ScreenRect root = root();
        if (handleModeClick(root, mouseX, mouseY)) {
            return true;
        }

        if (mode == Mode.SOURCES) {
            return handleSourceClick(root, mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
        }
        return handleGoalClick(root, mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ScreenRect root = root();
        if (mode == Mode.SOURCES) {
            ScreenRect grid = sourceGrid(root);
            if (grid.contains((int) mouseX, (int) mouseY)) {
                List<CodexEntry> sourceEntries = entries();
                sourceScroll = clampScroll(sourceScroll - (int) Math.signum(scrollY) * gridColumns(grid), sourceEntries.size(), visibleCellCount(grid));
                return true;
            }
        } else {
            ScreenRect grid = goalGrid(root);
            if (grid.contains((int) mouseX, (int) mouseY)) {
                List<GoalEntry> goals = goalEntries();
                goalScroll = clampScroll(goalScroll - (int) Math.signum(scrollY) * gridColumns(grid), goals.size(), visibleCellCount(grid));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void renderSources(GuiGraphics graphics, ScreenRect root, int mouseX, int mouseY) {
        List<CodexEntry> entries = entries();
        ensureSelection(entries);

        renderFilters(graphics, root, mouseX, mouseY);
        renderPinnedGoal(graphics, root, mouseX, mouseY);
        ScreenRect grid = sourceGrid(root);
        ScreenRect details = sourceDetails(root, grid);
        SynthesisStationDrawing.recessedPanel(graphics, grid);
        SynthesisStationDrawing.panel(graphics, details);

        renderSourceGrid(graphics, grid, entries, mouseX, mouseY);
        renderSourceDetails(graphics, details, selected(entries), mouseX, mouseY);
    }

    private void renderGoals(GuiGraphics graphics, ScreenRect root, int mouseX, int mouseY) {
        List<GoalEntry> goals = goalEntries();
        ensureGoalSelection(goals);

        ScreenRect grid = goalGrid(root);
        ScreenRect details = goalDetails(root, grid);
        SynthesisStationDrawing.recessedPanel(graphics, grid);
        SynthesisStationDrawing.panel(graphics, details);

        renderGoalGrid(graphics, grid, goals, mouseX, mouseY);
        renderGoalDetails(graphics, details, selectedGoal(goals), mouseX, mouseY);
    }

    private void renderModeTabs(GuiGraphics graphics, ScreenRect root, int mouseX, int mouseY) {
        int x = root.right() - 14 - Mode.values().length * METRICS.buttonSize() - (Mode.values().length - 1) * METRICS.cellGap();
        int y = root.y() + 7;
        for (Mode next : Mode.values()) {
            ScreenRect rect = new ScreenRect(x, y, METRICS.buttonSize(), METRICS.buttonSize());
            SynthesisStationDrawing.recipeCell(graphics, rect, next == mode);
            renderCenteredItem(graphics, AlchemyDiscoveryIcons.modeIcon(next), rect);
            if (rect.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.translatable(next.tooltipKey()), mouseX, mouseY);
            }
            x += METRICS.buttonSize() + METRICS.cellGap();
        }
    }

    private void renderFilters(GuiGraphics graphics, ScreenRect root, int mouseX, int mouseY) {
        int x = root.x() + 14;
        int y = root.y() + 28;
        for (Filter next : Filter.values()) {
            ScreenRect rect = new ScreenRect(x, y, METRICS.buttonSize(), METRICS.buttonSize());
            SynthesisStationDrawing.recipeCell(graphics, rect, next == filter);
            renderCenteredItem(graphics, AlchemyDiscoveryIcons.filterIcon(next), rect);
            if (rect.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.translatable(next.tooltipKey()), mouseX, mouseY);
            }
            x += METRICS.buttonSize() + METRICS.cellGap();
        }
    }

    private void renderPinnedGoal(GuiGraphics graphics, ScreenRect root, int mouseX, int mouseY) {
        if (!ClientExtractionKnowledgeData.hasPinnedReagentGoal()) {
            return;
        }

        List<String> pinned = ClientExtractionKnowledgeData.pinnedReagentGoal().stream()
                .sorted()
                .toList();
        int x = pinnedGoalX(root);
        int y = root.y() + 30;
        for (int i = 0; i < Math.min(pinned.size(), 4); i++) {
            String reagentId = pinned.get(i);
            ScreenRect rect = new ScreenRect(x + i * 20, y, 18, 18);
            SynthesisStationDrawing.slotFrame(graphics, rect);
            renderCenteredItem(graphics, AlchemyDiscoveryIcons.reagentIcon(reagentId), rect);
            if (rect.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.translatable(
                        "screen.zen_atelier.codex.pinned_goal",
                        AlchemyDiscoveryIcons.readableId(reagentId)
                ), mouseX, mouseY);
            }
        }

        ScreenRect clear = clearPinnedGoalRect(root);
        SynthesisStationDrawing.recipeCell(graphics, clear, false);
        renderCenteredItem(graphics, AlchemyDiscoveryIcons.clearGoalIcon(), clear);
        if (clear.contains(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("screen.zen_atelier.codex.clear_pinned_goal"), mouseX, mouseY);
        }
    }

    private void renderSourceGrid(GuiGraphics graphics, ScreenRect grid, List<CodexEntry> entries, int mouseX, int mouseY) {
        if (entries.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.zen_atelier.codex.empty"), grid.x() + 12, grid.y() + 12, SynthesisScreenTheme.MUTED, false);
            return;
        }

        sourceScroll = clampScroll(sourceScroll, entries.size(), visibleCellCount(grid));
        int visible = Math.min(entries.size() - sourceScroll, visibleCellCount(grid));
        for (int i = 0; i < visible; i++) {
            CodexEntry entry = entries.get(sourceScroll + i);
            ScreenRect cell = sourceCellRect(grid, i);
            SynthesisStationDrawing.recipeCell(graphics, cell, entry.sourceId().equals(selectedSourceId));
            renderCenteredItem(graphics, AlchemyDiscoveryIcons.sourceIcon(entry.sourceId()), cell);
            renderStateMark(graphics, cell, entry.state());

            if (cell.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, List.of(
                        Component.literal(AlchemyDiscoveryIcons.readableId(entry.sourceId())),
                        Component.translatable(entry.state().tooltipKey()).withStyle(entry.state().style())
                ), Optional.empty(), mouseX, mouseY);
            }
        }
        renderScrollBar(graphics, grid, sourceScroll, entries.size(), visibleCellCount(grid));
    }

    private void renderGoalGrid(GuiGraphics graphics, ScreenRect grid, List<GoalEntry> goals, int mouseX, int mouseY) {
        if (goals.isEmpty()) {
            graphics.drawString(font, Component.translatable("screen.zen_atelier.codex.no_goals"), grid.x() + 12, grid.y() + 12, SynthesisScreenTheme.MUTED, false);
            return;
        }

        goalScroll = clampScroll(goalScroll, goals.size(), visibleCellCount(grid));
        int visible = Math.min(goals.size() - goalScroll, visibleCellCount(grid));
        for (int i = 0; i < visible; i++) {
            GoalEntry goal = goals.get(goalScroll + i);
            ScreenRect cell = goalCellRect(grid, i);
            SynthesisStationDrawing.recipeCell(graphics, cell, goal.profileId().equals(selectedRecipeId));
            ItemStack output = SynthesisOutputItemFactory.previewStack(goal.profile());
            renderCenteredItem(graphics, output, cell);
            renderGoalMark(graphics, cell, goal);
            if (cell.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, output, mouseX, mouseY);
            }
        }
        renderScrollBar(graphics, grid, goalScroll, goals.size(), visibleCellCount(grid));
    }

    private void renderSourceDetails(GuiGraphics graphics, ScreenRect details, CodexEntry selected, int mouseX, int mouseY) {
        if (selected == null) {
            graphics.drawString(font, Component.translatable("screen.zen_atelier.codex.select_source"), details.x() + 12, details.y() + 12, SynthesisScreenTheme.MUTED, false);
            return;
        }

        ScreenRect icon = new ScreenRect(details.x() + 12, details.y() + 12, 28, 28);
        SynthesisStationDrawing.slotFrame(graphics, icon);
        renderCenteredItem(graphics, AlchemyDiscoveryIcons.sourceIcon(selected.sourceId()), icon);
        graphics.drawString(font, Component.literal(AlchemyDiscoveryIcons.readableId(selected.sourceId())), icon.right() + 8, details.y() + 13, SynthesisScreenTheme.TEXT, false);
        graphics.drawString(font, Component.translatable(selected.state().tooltipKey()), icon.right() + 8, details.y() + 24, selected.state().color(), false);
        if (selected.state() == EntryState.KNOWN) {
            renderAttemptPips(graphics, ClientExtractionKnowledgeData.knownSourceDetails(selected.sourceId()).attempts(), icon.right() + 8, details.y() + 37);
        }

        List<String> reagents = selected.reagents();
        int y = details.y() + 62;
        graphics.drawString(font, Component.translatable("screen.zen_atelier.codex.result_icons"), details.x() + 12, y, SynthesisScreenTheme.MUTED, false);
        y += 13;

        if (reagents.isEmpty()) {
            renderEmptySockets(graphics, details.x() + 12, y);
            return;
        }

        for (int i = 0; i < Math.min(reagents.size(), 12); i++) {
            String reagentId = reagents.get(i);
            ScreenRect cell = new ScreenRect(details.x() + 12 + (i % 6) * 25, y + (i / 6) * 25, 22, 22);
            SynthesisStationDrawing.slotFrame(graphics, cell);
            renderCenteredItem(graphics, AlchemyDiscoveryIcons.reagentIcon(reagentId), cell);
            if (cell.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.literal(AlchemyDiscoveryIcons.readableId(reagentId)), mouseX, mouseY);
            }
        }

        if (selected.state() == EntryState.KNOWN) {
            int rows = Math.min(2, (reagents.size() + 5) / 6);
            renderDiscoveredAttributes(graphics, selected.sourceId(), details, y + rows * 25 + 8, mouseX, mouseY);
        }
    }

    private void renderGoalDetails(GuiGraphics graphics, ScreenRect details, GoalEntry goal, int mouseX, int mouseY) {
        if (goal == null) {
            graphics.drawString(font, Component.translatable("screen.zen_atelier.codex.select_goal"), details.x() + 12, details.y() + 12, SynthesisScreenTheme.MUTED, false);
            return;
        }

        ItemStack output = SynthesisOutputItemFactory.previewStack(goal.profile());
        ScreenRect icon = new ScreenRect(details.x() + 12, details.y() + 12, 28, 28);
        SynthesisStationDrawing.slotFrame(graphics, icon);
        renderCenteredItem(graphics, output, icon);
        graphics.drawString(font, Component.literal(AlchemyDiscoveryIcons.readableId(goal.profileId())), icon.right() + 8, details.y() + 13, SynthesisScreenTheme.TEXT, false);
        graphics.drawString(font, Component.translatable("screen.zen_atelier.codex.goal_progress", goal.knownRequirementCount(), goal.profile().requirements().size()), icon.right() + 8, details.y() + 24, goal.complete() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.ACCENT, false);

        int y = details.y() + 50;
        List<SynthesisRequirement> requirements = goal.profile().requirements();
        for (int i = 0; i < requirements.size() && y + 38 < details.bottom(); i++) {
            SynthesisRequirement requirement = requirements.get(i);
            renderRequirementRow(graphics, details, requirement, y, mouseX, mouseY);
            y += 43;
        }
    }

    private void renderRequirementRow(GuiGraphics graphics, ScreenRect details, SynthesisRequirement requirement, int y, int mouseX, int mouseY) {
        int rowX = details.x() + 10;
        int rowWidth = details.width() - 20;
        graphics.fill(rowX, y, rowX + rowWidth, y + 38, 0x33000000);
        if (ClientExtractionKnowledgeData.matchesPinnedReagentGoal(requirement.query().reagentIds())) {
            graphics.fill(rowX, y, rowX + 3, y + 38, SynthesisScreenTheme.GOOD);
        }
        graphics.drawString(font, Component.literal("x" + requirement.amount()), rowX + 7, y + 15, SynthesisScreenTheme.TEXT, false);

        List<IconToken> queryIcons = queryIcons(requirement.query());
        renderIconTokens(graphics, queryIcons, rowX + 35, y + 8, 3, mouseX, mouseY);
        ScreenRect reagentArea = new ScreenRect(rowX, y, 104, 38);
        if (!requirement.query().reagentIds().isEmpty() && reagentArea.contains(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("screen.zen_atelier.codex.pin_goal")
                    .withStyle(ChatFormatting.GOLD), mouseX, mouseY);
        }

        List<SourceCandidate> known = knownCandidates(requirement.query());
        List<SourceCandidate> suspected = suspectedCandidates(requirement.query(), known.stream().map(SourceCandidate::sourceId).collect(java.util.stream.Collectors.toSet()));
        renderSourceCandidates(graphics, known, rowX + 114, y + 7, 3, mouseX, mouseY);
        renderSourceCandidates(graphics, suspected, rowX + 185, y + 7, 3, mouseX, mouseY);

        if (known.isEmpty() && suspected.isEmpty()) {
            renderTinyUnknowns(graphics, rowX + 114, y + 9);
        }
    }

    private void renderIconTokens(GuiGraphics graphics, List<IconToken> icons, int x, int y, int max, int mouseX, int mouseY) {
        for (int i = 0; i < Math.min(icons.size(), max); i++) {
            IconToken token = icons.get(i);
            ScreenRect rect = new ScreenRect(x + i * 21, y, 18, 18);
            SynthesisStationDrawing.slotFrame(graphics, rect);
            renderCenteredItem(graphics, token.stack(), rect);
            if (rect.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.literal(AlchemyDiscoveryIcons.readableId(token.id())), mouseX, mouseY);
            }
        }
    }

    private void renderSourceCandidates(GuiGraphics graphics, List<SourceCandidate> candidates, int x, int y, int max, int mouseX, int mouseY) {
        for (int i = 0; i < Math.min(candidates.size(), max); i++) {
            SourceCandidate candidate = candidates.get(i);
            ScreenRect rect = new ScreenRect(x + i * 21, y, 18, 18);
            SynthesisStationDrawing.recipeCell(graphics, rect, false);
            renderCenteredItem(graphics, AlchemyDiscoveryIcons.sourceIcon(candidate.sourceId()), rect);
            renderStateMark(graphics, rect, candidate.state());
            if (rect.contains(mouseX, mouseY)) {
                graphics.renderTooltip(font, List.of(
                        Component.literal(AlchemyDiscoveryIcons.readableId(candidate.sourceId())),
                        Component.translatable(candidate.state().tooltipKey()).withStyle(candidate.state().style())
                ), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    private void renderEmptySockets(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < 6; i++) {
            ScreenRect cell = new ScreenRect(x + i * 25, y, 22, 22);
            SynthesisStationDrawing.slotFrame(graphics, cell);
            SynthesisStationDrawing.unknownRecipeOutline(graphics, cell.x() + 3, cell.y() + 3, SynthesisScreenTheme.ACCENT_DIM);
        }
    }

    private void renderTinyUnknowns(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < 3; i++) {
            SynthesisStationDrawing.unknownRecipeOutline(graphics, x + i * 21 + 2, y + 2, SynthesisScreenTheme.ACCENT_DIM);
        }
    }

    private void renderAttemptPips(GuiGraphics graphics, int attempts, int x, int y) {
        int filled = Math.min(5, Math.max(0, attempts));
        for (int i = 0; i < 5; i++) {
            int color = i < filled ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.ACCENT_DIM;
            graphics.fill(x + i * 7, y, x + i * 7 + 4, y + 4, color);
        }
    }

    private void renderDiscoveredAttributes(GuiGraphics graphics, String sourceId, ScreenRect details, int y, int mouseX, int mouseY) {
        var knownDetails = ClientExtractionKnowledgeData.knownSourceDetails(sourceId);
        List<IconToken> traits = knownDetails.traits().stream()
                .sorted()
                .map(id -> new IconToken(id, AlchemyDiscoveryIcons.elementIcon(id)))
                .toList();
        List<IconToken> elements = knownDetails.elements().keySet().stream()
                .sorted()
                .map(id -> new IconToken(id, AlchemyDiscoveryIcons.elementIcon(id)))
                .toList();

        if (!traits.isEmpty()) {
            y = renderAttributeRow(graphics, Component.translatable("screen.zen_atelier.codex.traits"), traits, details, y, mouseX, mouseY);
        }
        if (!elements.isEmpty() && y + 18 <= details.bottom() - 8) {
            renderAttributeRow(graphics, Component.translatable("screen.zen_atelier.codex.elements"), elements, details, y, mouseX, mouseY);
        }
    }

    private int renderAttributeRow(GuiGraphics graphics, Component label, List<IconToken> icons, ScreenRect details, int y, int mouseX, int mouseY) {
        int x = details.x() + 12;
        int tokenX = x + 49;
        int maxTokens = Math.max(1, (details.right() - tokenX - 8) / 21);
        graphics.drawString(font, label, x, y + 5, SynthesisScreenTheme.MUTED, false);
        renderIconTokens(graphics, icons, tokenX, y, maxTokens, mouseX, mouseY);
        return y + 23;
    }

    private void renderStateMark(GuiGraphics graphics, ScreenRect cell, EntryState state) {
        int color = state.color();
        switch (state) {
            case KNOWN -> graphics.fill(cell.right() - 6, cell.y() + 3, cell.right() - 3, cell.y() + 6, color);
            case SUSPECTED -> SynthesisStationDrawing.unknownRecipeOutline(graphics, cell.x() + 5, cell.y() + 5, color);
            case EMPTY -> {
                graphics.fill(cell.x() + 5, cell.y() + 5, cell.right() - 5, cell.y() + 7, color);
                graphics.fill(cell.x() + 5, cell.bottom() - 7, cell.right() - 5, cell.bottom() - 5, color);
            }
        }
    }

    private void renderGoalMark(GuiGraphics graphics, ScreenRect cell, GoalEntry goal) {
        int color = goal.complete() ? SynthesisScreenTheme.GOOD : SynthesisScreenTheme.ACCENT;
        int filled = Math.max(1, Math.min(cell.width() - 6, Math.round((cell.width() - 6) * goal.progress())));
        graphics.fill(cell.x() + 3, cell.bottom() - 5, cell.x() + 3 + filled, cell.bottom() - 3, color);
    }

    private boolean handleModeClick(ScreenRect root, double mouseX, double mouseY) {
        int x = root.right() - 14 - Mode.values().length * METRICS.buttonSize() - (Mode.values().length - 1) * METRICS.cellGap();
        int y = root.y() + 7;
        for (Mode next : Mode.values()) {
            ScreenRect rect = new ScreenRect(x, y, METRICS.buttonSize(), METRICS.buttonSize());
            if (rect.contains((int) mouseX, (int) mouseY)) {
                mode = next;
                return true;
            }
            x += METRICS.buttonSize() + METRICS.cellGap();
        }
        return false;
    }

    private boolean handleSourceClick(ScreenRect root, double mouseX, double mouseY) {
        int filterX = root.x() + 14;
        int filterY = root.y() + 28;
        for (Filter next : Filter.values()) {
            ScreenRect rect = new ScreenRect(filterX, filterY, METRICS.buttonSize(), METRICS.buttonSize());
            if (rect.contains((int) mouseX, (int) mouseY)) {
                filter = next;
                selectedSourceId = null;
                sourceScroll = 0;
                return true;
            }
            filterX += METRICS.buttonSize() + METRICS.cellGap();
        }

        if (ClientExtractionKnowledgeData.hasPinnedReagentGoal()
                && clearPinnedGoalRect(root).contains((int) mouseX, (int) mouseY)) {
            ClientExtractionKnowledgeData.clearPinnedReagentGoal();
            selectedSourceId = null;
            return true;
        }

        ScreenRect grid = sourceGrid(root);
        List<CodexEntry> sourceEntries = entries();
        sourceScroll = clampScroll(sourceScroll, sourceEntries.size(), visibleCellCount(grid));
        int visible = Math.min(sourceEntries.size() - sourceScroll, visibleCellCount(grid));
        for (int i = 0; i < visible; i++) {
            ScreenRect cell = sourceCellRect(grid, i);
            if (cell.contains((int) mouseX, (int) mouseY)) {
                selectedSourceId = sourceEntries.get(sourceScroll + i).sourceId();
                return true;
            }
        }
        return false;
    }

    private boolean handleGoalClick(ScreenRect root, double mouseX, double mouseY) {
        ScreenRect grid = goalGrid(root);
        List<GoalEntry> goals = goalEntries();
        goalScroll = clampScroll(goalScroll, goals.size(), visibleCellCount(grid));
        int visible = Math.min(goals.size() - goalScroll, visibleCellCount(grid));
        for (int i = 0; i < visible; i++) {
            ScreenRect cell = goalCellRect(grid, i);
            if (cell.contains((int) mouseX, (int) mouseY)) {
                selectedRecipeId = goals.get(goalScroll + i).profileId();
                return true;
            }
        }

        GoalEntry goal = selectedGoal(goals);
        if (goal == null) {
            return false;
        }

        ScreenRect details = goalDetails(root, grid);
        int y = details.y() + 50;
        for (SynthesisRequirement requirement : goal.profile().requirements()) {
            if (clickedRequirementGoal(details, requirement, y, mouseX, mouseY)) {
                ClientExtractionKnowledgeData.pinReagentGoal(requirement.query().reagentIds());
                mode = Mode.SOURCES;
                filter = Filter.ALL;
                selectedSourceId = null;
                return true;
            }

            List<SourceCandidate> known = knownCandidates(requirement.query());
            List<SourceCandidate> suspected = suspectedCandidates(requirement.query(), known.stream().map(SourceCandidate::sourceId).collect(java.util.stream.Collectors.toSet()));
            SourceCandidate clicked = clickedCandidate(known, details.x() + 124, y + 7, mouseX, mouseY).orElse(null);
            if (clicked == null) {
                clicked = clickedCandidate(suspected, details.x() + 195, y + 7, mouseX, mouseY).orElse(null);
            }
            if (clicked != null) {
                mode = Mode.SOURCES;
                filter = clicked.state() == EntryState.KNOWN ? Filter.KNOWN : Filter.SUSPECTED;
                selectedSourceId = clicked.sourceId();
                return true;
            }
            y += 43;
        }
        return false;
    }

    private boolean clickedRequirementGoal(ScreenRect details, SynthesisRequirement requirement, int y, double mouseX, double mouseY) {
        if (requirement.query().reagentIds().isEmpty()) {
            return false;
        }
        ScreenRect reagentArea = new ScreenRect(details.x() + 10, y, 104, 38);
        return reagentArea.contains((int) mouseX, (int) mouseY);
    }

    private Optional<SourceCandidate> clickedCandidate(List<SourceCandidate> candidates, int x, int y, double mouseX, double mouseY) {
        for (int i = 0; i < Math.min(candidates.size(), 3); i++) {
            ScreenRect rect = new ScreenRect(x + i * 21, y, 18, 18);
            if (rect.contains((int) mouseX, (int) mouseY)) {
                return Optional.of(candidates.get(i));
            }
        }
        return Optional.empty();
    }

    private List<CodexEntry> entries() {
        Map<String, List<String>> known = ClientExtractionKnowledgeData.allKnownSourceReagents();
        Set<String> empty = ClientExtractionKnowledgeData.allTestedEmptySources();
        List<CodexEntry> result = new ArrayList<>();

        known.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> result.add(new CodexEntry(entry.getKey(), EntryState.KNOWN, entry.getValue())));

        if (filter == Filter.ALL || filter == Filter.SUSPECTED) {
            suspectedEntries(known.keySet(), empty).forEach(result::add);
        }
        if (filter == Filter.ALL || filter == Filter.EMPTY) {
            empty.stream()
                    .sorted()
                    .map(source -> new CodexEntry(source, EntryState.EMPTY, List.of()))
                    .forEach(result::add);
        }

        return result.stream()
                .filter(entry -> filter.matches(entry.state()))
                .filter(this::matchesPinnedGoal)
                .sorted(Comparator.comparing(CodexEntry::state).thenComparing(CodexEntry::sourceId))
                .toList();
    }

    private boolean matchesPinnedGoal(CodexEntry entry) {
        if (!ClientExtractionKnowledgeData.hasPinnedReagentGoal()) {
            return true;
        }
        if (entry.state() == EntryState.EMPTY) {
            return false;
        }
        return ClientExtractionKnowledgeData.matchesPinnedReagentGoal(entry.reagents());
    }

    private List<GoalEntry> goalEntries() {
        return SynthesisProfileRegistry.all().stream()
                .map(SynthesisProfileDefinition::toCore)
                .map(GoalEntry::new)
                .sorted(Comparator.comparing(GoalEntry::profileId))
                .toList();
    }

    private List<IconToken> queryIcons(ReagentQuery query) {
        List<IconToken> icons = new ArrayList<>();
        query.reagentIds().stream()
                .sorted()
                .map(id -> new IconToken(id, AlchemyDiscoveryIcons.reagentIcon(id)))
                .forEach(icons::add);
        if (icons.isEmpty()) {
            query.minElements().keySet().stream()
                    .sorted()
                    .map(id -> new IconToken(id, AlchemyDiscoveryIcons.elementIcon(id)))
                    .forEach(icons::add);
        }
        return icons;
    }

    private List<SourceCandidate> knownCandidates(ReagentQuery query) {
        Set<String> reagentIds = query.reagentIds();
        if (reagentIds.isEmpty()) {
            return List.of();
        }
        Map<String, List<String>> allKnown = ClientExtractionKnowledgeData.allKnownSourceReagents();
        return ClientExtractionKnowledgeData.knownSourcesForAny(reagentIds).stream()
                .map(source -> new SourceCandidate(source, EntryState.KNOWN, allKnown.getOrDefault(source, List.of())))
                .toList();
    }

    private List<SourceCandidate> suspectedCandidates(ReagentQuery query, Set<String> excludedSources) {
        Map<String, List<String>> known = ClientExtractionKnowledgeData.allKnownSourceReagents();
        Set<String> empty = ClientExtractionKnowledgeData.allTestedEmptySources();
        Set<String> seen = new LinkedHashSet<>();
        return ExtractionProfileRegistry.all().stream()
                .map(ExtractionProfileDefinition::toCore)
                .filter(profile -> !known.containsKey(sourceId(profile)))
                .filter(profile -> !empty.contains(sourceId(profile)))
                .filter(profile -> !excludedSources.contains(sourceId(profile)))
                .filter(profile -> profileProduces(profile, query))
                .filter(profile -> seen.add(sourceId(profile)))
                .map(profile -> new SourceCandidate(sourceId(profile), EntryState.SUSPECTED, previewReagents(profile)))
                .sorted(Comparator.comparing(SourceCandidate::sourceId))
                .toList();
    }

    private boolean profileProduces(ExtractionProfile profile, ReagentQuery query) {
        return profile.outcomes().stream()
                .flatMap(outcome -> Stream.concat(outcome.reagents().stream(), outcome.byproducts().stream()))
                .anyMatch(query::matches);
    }

    private static List<CodexEntry> suspectedEntries(Set<String> known, Set<String> empty) {
        return ExtractionProfileRegistry.all().stream()
                .map(ExtractionProfileDefinition::toCore)
                .filter(profile -> !known.contains(sourceId(profile)) && !empty.contains(sourceId(profile)))
                .map(profile -> new CodexEntry(sourceId(profile), EntryState.SUSPECTED, previewReagents(profile)))
                .toList();
    }

    private static String sourceId(ExtractionProfile profile) {
        return profile.sourceKey();
    }

    private static List<String> previewReagents(ExtractionProfile profile) {
        return profile.outcomes().stream()
                .flatMap(outcome -> Stream.concat(outcome.reagents().stream(), outcome.byproducts().stream()))
                .map(ReagentStack::reagentId)
                .distinct()
                .sorted()
                .limit(12)
                .toList();
    }

    private void ensureSelection(List<CodexEntry> sourceEntries) {
        if (sourceEntries.isEmpty()) {
            selectedSourceId = null;
            return;
        }
        if (selectedSourceId == null || sourceEntries.stream().noneMatch(entry -> entry.sourceId().equals(selectedSourceId))) {
            selectedSourceId = sourceEntries.getFirst().sourceId();
        }
    }

    private void ensureGoalSelection(List<GoalEntry> goals) {
        if (goals.isEmpty()) {
            selectedRecipeId = null;
            return;
        }
        if (selectedRecipeId == null || goals.stream().noneMatch(goal -> goal.profileId().equals(selectedRecipeId))) {
            selectedRecipeId = goals.getFirst().profileId();
        }
    }

    private CodexEntry selected(List<CodexEntry> sourceEntries) {
        if (selectedSourceId == null) {
            return null;
        }
        return sourceEntries.stream()
                .filter(entry -> entry.sourceId().equals(selectedSourceId))
                .findFirst()
                .orElse(null);
    }

    private GoalEntry selectedGoal(List<GoalEntry> goals) {
        if (selectedRecipeId == null) {
            return null;
        }
        return goals.stream()
                .filter(goal -> goal.profileId().equals(selectedRecipeId))
                .findFirst()
                .orElse(null);
    }

    private ScreenRect sourceGrid(ScreenRect root) {
        return new ScreenRect(root.x() + 14, root.y() + 56, 206, root.height() - 72);
    }

    private ScreenRect sourceDetails(ScreenRect root, ScreenRect grid) {
        return new ScreenRect(grid.right() + METRICS.sectionGap(), grid.y(), root.right() - grid.right() - 24, grid.height());
    }

    private ScreenRect goalGrid(ScreenRect root) {
        return new ScreenRect(root.x() + 14, root.y() + 42, 128, root.height() - 58);
    }

    private ScreenRect goalDetails(ScreenRect root, ScreenRect grid) {
        return new ScreenRect(grid.right() + METRICS.sectionGap(), grid.y(), root.right() - grid.right() - 24, grid.height());
    }

    private int pinnedGoalX(ScreenRect root) {
        return root.x() + 14 + Filter.values().length * (METRICS.buttonSize() + METRICS.cellGap()) + UiMetrics.INSET_MEDIUM;
    }

    private ScreenRect clearPinnedGoalRect(ScreenRect root) {
        int maxPinned = Math.min(4, ClientExtractionKnowledgeData.pinnedReagentGoal().size());
        return new ScreenRect(pinnedGoalX(root) + maxPinned * 20 + 3, root.y() + 28, METRICS.buttonSize(), METRICS.buttonSize());
    }

    private ScreenRect sourceCellRect(ScreenRect grid, int index) {
        int columns = gridColumns(grid);
        int x = grid.x() + METRICS.gridInset() + (index % columns) * (METRICS.cellSize() + METRICS.cellGap());
        int y = grid.y() + METRICS.gridInset() + (index / columns) * (METRICS.cellSize() + METRICS.cellGap());
        return new ScreenRect(x, y, METRICS.cellSize(), METRICS.cellSize());
    }

    private ScreenRect goalCellRect(ScreenRect grid, int index) {
        int columns = gridColumns(grid);
        int x = grid.x() + METRICS.gridInset() + (index % columns) * (METRICS.cellSize() + METRICS.cellGap());
        int y = grid.y() + METRICS.gridInset() + (index / columns) * (METRICS.cellSize() + METRICS.cellGap());
        return new ScreenRect(x, y, METRICS.cellSize(), METRICS.cellSize());
    }

    private int visibleCellCount(ScreenRect grid) {
        int rows = Math.max(1, (grid.height() - 12) / (METRICS.cellSize() + METRICS.cellGap()));
        return rows * gridColumns(grid);
    }

    private int gridColumns(ScreenRect grid) {
        return Math.max(1, (grid.width() - 12) / (METRICS.cellSize() + METRICS.cellGap()));
    }

    private int clampScroll(int scroll, int total, int visible) {
        return Math.clamp(scroll, 0, Math.max(0, total - visible));
    }

    private void renderScrollBar(GuiGraphics graphics, ScreenRect grid, int scroll, int total, int visible) {
        if (total <= visible) {
            return;
        }
        int trackX = grid.right() - METRICS.scrollbarInset();
        int trackY = grid.y() + METRICS.gridInset();
        int trackHeight = grid.height() - 16;
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, SynthesisScreenTheme.PANEL_LIGHT);
        int thumbHeight = Math.max(12, trackHeight * visible / total);
        int thumbY = trackY + (trackHeight - thumbHeight) * scroll / Math.max(1, total - visible);
        graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, SynthesisScreenTheme.ACCENT);
    }

    private void renderCenteredItem(GuiGraphics graphics, ItemStack stack, ScreenRect rect) {
        graphics.renderItem(stack, rect.x() + (rect.width() - 16) / 2, rect.y() + (rect.height() - 16) / 2);
    }

    private ScreenRect root() {
        int width = Math.min(METRICS.rootMaxWidth(), this.width - METRICS.rootMargin());
        int height = Math.min(METRICS.rootMaxHeight(), this.height - METRICS.rootMargin());
        return new ScreenRect((this.width - width) / 2, (this.height - height) / 2, width, height);
    }

    enum Mode {
        SOURCES("screen.zen_atelier.codex.mode.sources"),
        GOALS("screen.zen_atelier.codex.mode.goals");

        private final String tooltipKey;

        Mode(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }

        private String tooltipKey() {
            return tooltipKey;
        }
    }

    enum Filter {
        ALL("screen.zen_atelier.codex.filter.all"),
        KNOWN("screen.zen_atelier.codex.filter.known"),
        SUSPECTED("screen.zen_atelier.codex.filter.suspected"),
        EMPTY("screen.zen_atelier.codex.filter.empty");

        private final String tooltipKey;

        Filter(String tooltipKey) {
            this.tooltipKey = tooltipKey;
        }

        private boolean matches(EntryState state) {
            return this == ALL
                    || this == KNOWN && state == EntryState.KNOWN
                    || this == SUSPECTED && state == EntryState.SUSPECTED
                    || this == EMPTY && state == EntryState.EMPTY;
        }

        private String tooltipKey() {
            return tooltipKey;
        }
    }

    private enum EntryState {
        KNOWN("screen.zen_atelier.codex.state.known", SynthesisScreenTheme.GOOD, ChatFormatting.GREEN),
        SUSPECTED("screen.zen_atelier.codex.state.suspected", SynthesisScreenTheme.ACCENT, ChatFormatting.GOLD),
        EMPTY("screen.zen_atelier.codex.state.empty", SynthesisScreenTheme.BAD, ChatFormatting.RED);

        private final String tooltipKey;
        private final int color;
        private final ChatFormatting style;

        EntryState(String tooltipKey, int color, ChatFormatting style) {
            this.tooltipKey = tooltipKey;
            this.color = color;
            this.style = style;
        }

        private String tooltipKey() {
            return tooltipKey;
        }

        private int color() {
            return color;
        }

        private ChatFormatting style() {
            return style;
        }
    }

    private record GoalEntry(SynthesisProfile profile) {
        private String profileId() {
            return profile.id();
        }

        private int knownRequirementCount() {
            int known = 0;
            for (SynthesisRequirement requirement : profile.requirements()) {
                if (!ClientExtractionKnowledgeData.knownSourcesForAny(requirement.query().reagentIds()).isEmpty()) {
                    known++;
                }
            }
            return known;
        }

        private float progress() {
            return profile.requirements().isEmpty() ? 0.0f : knownRequirementCount() / (float) profile.requirements().size();
        }

        private boolean complete() {
            return knownRequirementCount() >= profile.requirements().size();
        }
    }

    private record CodexEntry(String sourceId, EntryState state, List<String> reagents) {
        private CodexEntry {
            reagents = List.copyOf(reagents);
        }
    }

    private record SourceCandidate(String sourceId, EntryState state, List<String> reagents) {
        private SourceCandidate {
            reagents = List.copyOf(reagents);
        }
    }

    private record IconToken(String id, ItemStack stack) {
    }
}
