package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class SynthesisResultOverlay {
    static final int FAILURE_IMPACT_TICKS = 20;
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_MARGIN_X = 24;
    private static final int PANEL_MARGIN_BOTTOM = 24;
    private static final int PANEL_TOP = 46;
    private static final int HEADER_HEIGHT = 40;
    private static final int ROW_HEIGHT = 20;
    private static final int SECTION_GAP = 8;

    private final OutcomeClass outcomeClass;
    private final List<SynthesisOutput> outputs;
    private final List<ReagentStack> byproducts;

    SynthesisResultOverlay(OutcomeClass outcomeClass, List<SynthesisOutput> outputs, List<ReagentStack> byproducts) {
        this.outcomeClass = outcomeClass;
        this.outputs = List.copyOf(outputs);
        this.byproducts = List.copyOf(byproducts);
    }

    static int impactTicksFor(OutcomeClass outcomeClass) {
        return outcomeClass.successful() ? 0 : FAILURE_IMPACT_TICKS;
    }

    void render(GuiGraphics graphics, Font font, ScreenRect origin) {
        Layout layout = layoutFor(outcomeClass, outputs, byproducts, origin);
        int screenW = SynthesisStationMetrics.DEFAULT.width();
        int screenH = SynthesisStationMetrics.DEFAULT.height();
        graphics.fill(origin.x(), origin.y(), origin.x() + screenW, origin.y() + screenH, 0xE20C0A08);

        ScreenRect panel = layout.panel();
        SynthesisStationDrawing.panel(graphics, panel);
        graphics.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + 4, titleColor());
        SynthesisStationDrawing.frame(graphics, panel, titleColor());

        ScreenRect title = layout.title();
        SynthesisStationText.drawCenteredFit(graphics, font, outcomeTitle(), title, titleColor());
        if (!outcomeClass.successful()) {
            SynthesisStationText.drawCenteredFit(graphics, font, failureDetail(), layout.detail().orElseThrow(), SynthesisScreenTheme.MUTED);
        }

        graphics.fill(panel.x() + 12, layout.dividerY(), panel.right() - 12, layout.dividerY() + 1, 0x55EFE6D5);

        if (outputs.isEmpty()) {
            graphics.drawCenteredString(font, emptyOutputText(),
                    panel.x() + panel.width() / 2, layout.emptyTextY(), SynthesisScreenTheme.MUTED);
        } else {
            graphics.drawString(font, Component.literal("Produced"), panel.x() + 14, layout.outputHeaderY(), SynthesisScreenTheme.ACCENT, false);
            for (int i = 0; i < outputs.size(); i++) {
                renderOutputRow(graphics, font, outputs.get(i), layout.outputRows().get(i));
            }
        }
        if (!byproducts.isEmpty()) {
            ScreenRect header = layout.byproductHeader().orElseThrow();
            graphics.fill(panel.x() + 12, header.y() - 5, panel.right() - 12, header.y() - 4, 0x33EFE6D5);
            graphics.drawString(font, byproductHeading(), header.x(), header.y(), SynthesisScreenTheme.MUTED, false);
            for (int i = 0; i < byproducts.size(); i++) {
                renderByproductRow(graphics, font, byproducts.get(i), layout.byproductRows().get(i));
            }
        }
    }

    static Layout layoutFor(OutcomeClass outcomeClass, List<SynthesisOutput> outputs, List<ReagentStack> byproducts, ScreenRect origin) {
        int screenW = SynthesisStationMetrics.DEFAULT.width();
        int screenH = SynthesisStationMetrics.DEFAULT.height();
        int panelWidth = Math.min(PANEL_WIDTH, screenW - PANEL_MARGIN_X * 2);
        int contentRows = outputs.isEmpty()
                ? ROW_HEIGHT
                : outputs.stream().mapToInt(SynthesisResultOverlay::outputRowHeight).sum();
        if (!byproducts.isEmpty()) {
            contentRows += SECTION_GAP + 10 + byproducts.size() * ROW_HEIGHT;
        }
        int panelHeight = Math.min(screenH - PANEL_TOP - PANEL_MARGIN_BOTTOM, Math.max(112, HEADER_HEIGHT + 18 + contentRows + 18));
        int panelX = origin.x() + (screenW - panelWidth) / 2;
        int panelY = origin.y() + PANEL_TOP;
        ScreenRect panel = new ScreenRect(panelX, panelY, panelWidth, panelHeight);
        ScreenRect title = new ScreenRect(panel.x() + 14, panel.y() + 12, panel.width() - 28, 11);
        Optional<ScreenRect> detail = outcomeClass.successful()
                ? Optional.empty()
                : Optional.of(new ScreenRect(panel.x() + 14, panel.y() + 24, panel.width() - 28, 11));
        int dividerY = outcomeClass.successful() ? panel.y() + 31 : panel.y() + 42;
        int y = dividerY + 9;
        int outputHeaderY = y;
        y += outputs.isEmpty() ? 0 : 12;

        ArrayList<ScreenRect> outputRows = new ArrayList<>();
        if (outputs.isEmpty()) {
            y += ROW_HEIGHT;
        } else {
            for (SynthesisOutput output : outputs) {
                int rowHeight = outputRowHeight(output);
                outputRows.add(new ScreenRect(panel.x() + 12, y, panel.width() - 24, rowHeight));
                y += rowHeight;
            }
        }

        Optional<ScreenRect> byproductHeader = Optional.empty();
        ArrayList<ScreenRect> byproductRows = new ArrayList<>();
        if (!byproducts.isEmpty()) {
            y += SECTION_GAP;
            byproductHeader = Optional.of(new ScreenRect(panel.x() + 14, y, panel.width() - 28, 9));
            y += 11;
            for (int i = 0; i < byproducts.size(); i++) {
                byproductRows.add(new ScreenRect(panel.x() + 12, y, panel.width() - 24, ROW_HEIGHT));
                y += ROW_HEIGHT;
            }
        }

        return new Layout(panel, title, detail, dividerY, outputHeaderY, dividerY + 9, List.copyOf(outputRows), byproductHeader, List.copyOf(byproductRows));
    }

    private void renderOutputRow(GuiGraphics graphics, Font font, SynthesisOutput output, ScreenRect row) {
        ResourceLocation loc = ResourceLocation.tryParse(output.outputId());
        Item item = (loc != null) ? BuiltInRegistries.ITEM.get(loc) : Items.AIR;
        Component name = (item != Items.AIR) ? item.getDescription()
                : Component.literal(SynthesisStationText.shortLabel(output.outputId()));
        MutableComponent statsComp = outputStats(output);
        if (item != Items.AIR) {
            graphics.renderFakeItem(item.getDefaultInstance(), row.x(), row.y() + 2);
        }
        int statsWidth = font.width(statsComp);
        SynthesisStationText.drawFit(graphics, font, name, new ScreenRect(row.x() + 21, row.y() + 6, row.width() - statsWidth - 30, 9), SynthesisScreenTheme.TEXT);
        graphics.drawString(font, statsComp, row.right() - statsWidth, row.y() + 6, SynthesisScreenTheme.TEXT, false);
        if (!output.affixes().isEmpty()) {
            SynthesisStationText.drawRichFit(graphics, font, affixLine(output), new ScreenRect(row.x() + 21, row.y() + 17, row.width() - 21, 9), SynthesisScreenTheme.ACCENT);
        }
    }

    private static int outputRowHeight(SynthesisOutput output) {
        return output.affixes().isEmpty() ? ROW_HEIGHT : ROW_HEIGHT + 10;
    }

    private void renderByproductRow(GuiGraphics graphics, Font font, ReagentStack byproduct, ScreenRect row) {
        ItemStack bpStack = ReagentItem.createStack(byproduct);
        Component name = Component.literal(SynthesisStationText.shortLabel(byproduct.reagentId()));
        String stats = byproduct.amount() + "x  T" + byproduct.tier() + (byproduct.quality() > 0 ? "  " + byproduct.quality() + "Q" : "");
        graphics.renderFakeItem(bpStack, row.x(), row.y() + 2);
        SynthesisStationText.drawFit(graphics, font, name, new ScreenRect(row.x() + 21, row.y() + 6, row.width() - font.width(stats) - 30, 9), SynthesisScreenTheme.TEXT);
        graphics.drawString(font, stats, row.right() - font.width(stats), row.y() + 6, SynthesisScreenTheme.MUTED, false);
    }

    private static MutableComponent outputStats(SynthesisOutput output) {
        ChatFormatting qualityFmt = output.quality() >= 100 ? ChatFormatting.GOLD
                : output.quality() >= 75 ? ChatFormatting.AQUA
                : output.quality() >= 50 ? ChatFormatting.GRAY
                : ChatFormatting.DARK_GRAY;
        String qualityTierKey = output.quality() >= 100 ? "tooltip.zen_atelier.quality.masterwork"
                : output.quality() >= 75 ? "tooltip.zen_atelier.quality.superior"
                : output.quality() >= 50 ? "tooltip.zen_atelier.quality.fine"
                : "tooltip.zen_atelier.quality.crude";
        return Component.literal(output.count() + "x  T" + output.tier() + "  ")
                .withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED))
                .append(Component.translatable(qualityTierKey).withStyle(qualityFmt))
                .append(Component.literal(" " + output.quality()).withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
    }

    private static MutableComponent affixLine(SynthesisOutput output) {
        MutableComponent affixLine = Component.empty();
        for (int i = 0; i < output.affixes().size(); i++) {
            if (i > 0) {
                affixLine.append(Component.literal("  /  ").withStyle(ChatFormatting.DARK_GRAY));
            }
            String affixId = output.affixes().get(i);
            String affixKey = affixId.contains(":") ? affixId.replace(":", ".affix.") : "zen_atelier.affix." + affixId;
            affixLine.append(Component.translatable(affixKey).withStyle(ChatFormatting.GOLD));
        }
        return affixLine;
    }

    private Component outcomeTitle() {
        if (!outcomeClass.successful()) {
            return Component.literal("Synthesis Failed");
        }
        return switch (outcomeClass) {
            case PERFECT_SUCCESS -> Component.literal("Perfect Success!");
            case SUCCESS -> Component.literal("Success");
            case UNSTABLE_SUCCESS -> Component.literal("Unstable Success");
            case PARTIAL_SUCCESS -> Component.literal("Partial Success");
            case MUTATED_SUCCESS -> Component.literal("Mutated Result");
            case DUD, RECOVERABLE_FAILURE, MESSY_FAILURE, CATASTROPHIC_FAILURE -> Component.literal("Synthesis Failed");
        };
    }

    private Component failureDetail() {
        return switch (outcomeClass) {
            case DUD -> Component.literal("The mixture went inert.");
            case RECOVERABLE_FAILURE -> Component.literal("The reaction collapsed, but some residue survived.");
            case MESSY_FAILURE -> Component.literal("The reaction fouled the apparatus.");
            case CATASTROPHIC_FAILURE -> Component.literal("The reaction burned out violently.");
            default -> Component.empty();
        };
    }

    private Component emptyOutputText() {
        if (!outcomeClass.successful()) {
            return Component.literal("Nothing usable survived.");
        }
        return Component.literal("Nothing produced.");
    }

    private Component byproductHeading() {
        if (!outcomeClass.successful()) {
            return Component.literal("Recovered byproducts");
        }
        return Component.literal("Byproducts");
    }

    private int titleColor() {
        return switch (outcomeClass) {
            case PERFECT_SUCCESS -> SynthesisScreenTheme.ACCENT;
            case SUCCESS -> SynthesisScreenTheme.GOOD;
            case UNSTABLE_SUCCESS, PARTIAL_SUCCESS, MUTATED_SUCCESS -> SynthesisScreenTheme.ACCENT_DIM;
            case DUD, RECOVERABLE_FAILURE -> SynthesisScreenTheme.MUTED;
            case MESSY_FAILURE, CATASTROPHIC_FAILURE -> SynthesisScreenTheme.BAD;
        };
    }

    record Layout(
            ScreenRect panel,
            ScreenRect title,
            Optional<ScreenRect> detail,
            int dividerY,
            int outputHeaderY,
            int emptyTextY,
            List<ScreenRect> outputRows,
            Optional<ScreenRect> byproductHeader,
            List<ScreenRect> byproductRows
    ) {
    }

}
