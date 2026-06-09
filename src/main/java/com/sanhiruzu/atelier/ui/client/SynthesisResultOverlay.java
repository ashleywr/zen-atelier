package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

final class SynthesisResultOverlay {
    private final OutcomeClass outcomeClass;
    private final List<SynthesisOutput> outputs;
    private final List<ReagentStack> byproducts;

    SynthesisResultOverlay(OutcomeClass outcomeClass, List<SynthesisOutput> outputs, List<ReagentStack> byproducts) {
        this.outcomeClass = outcomeClass;
        this.outputs = List.copyOf(outputs);
        this.byproducts = List.copyOf(byproducts);
    }

    void render(GuiGraphics graphics, Font font, ScreenRect origin) {
        int ox = origin.x();
        int oy = origin.y();

        // Darken the whole GUI area
        int screenW = SynthesisStationMetrics.DEFAULT.width();
        int screenH = SynthesisStationMetrics.DEFAULT.height();
        graphics.fill(ox, oy, ox + screenW, oy + screenH, 0xDC0E0B08);

        // Panel: centered horizontally over the main content area
        int pw = 280;
        int ph = Math.max(90, 56 + outputs.size() * 14 + (byproducts.isEmpty() ? 0 : 20 + byproducts.size() * 14));
        int px = ox + (screenW - pw) / 2;
        int py = oy + 55;
        ScreenRect panel = new ScreenRect(px, py, pw, ph);
        SynthesisStationDrawing.panel(graphics, panel);
        SynthesisStationDrawing.frame(graphics, panel, titleColor());

        // Outcome title
        int titleX = px + pw / 2;
        int titleY = py + 12;
        graphics.drawCenteredString(font, outcomeTitle(), titleX, titleY, titleColor());

        // Divider
        graphics.fill(px + 10, titleY + 14, px + pw - 10, titleY + 15, 0x44EFE6D5);

        // Outputs
        int listY = titleY + 22;
        if (outputs.isEmpty()) {
            graphics.drawCenteredString(font, Component.literal("Nothing produced."),
                    titleX, listY, SynthesisScreenTheme.MUTED);
        } else {
            for (SynthesisOutput output : outputs) {
                Component name = resolveItemName(output.outputId());
                String line = output.count() + "x  T" + output.tier() + "  " + output.quality() + "Q";
                graphics.drawString(font, name, px + 14, listY, SynthesisScreenTheme.TEXT, false);
                graphics.drawString(font, line, px + pw - 10 - font.width(line), listY,
                        SynthesisScreenTheme.MUTED, false);
                listY += 14;
            }
        }
        if (!byproducts.isEmpty()) {
            graphics.fill(px + 10, listY + 1, px + pw - 10, listY + 2, 0x33EFE6D5);
            listY += 9;
            graphics.drawString(font, Component.literal("Byproducts"), px + 14, listY, SynthesisScreenTheme.MUTED, false);
            listY += 11;
            for (ReagentStack bp : byproducts) {
                Component name = Component.literal(SynthesisStationText.shortLabel(bp.reagentId()));
                String line = bp.amount() + "x  T" + bp.tier() + (bp.quality() > 0 ? "  " + bp.quality() + "Q" : "");
                graphics.drawString(font, name, px + 14, listY, SynthesisScreenTheme.TEXT, false);
                graphics.drawString(font, line, px + pw - 10 - font.width(line), listY,
                        SynthesisScreenTheme.MUTED, false);
                listY += 14;
            }
        }
    }

    private Component outcomeTitle() {
        return switch (outcomeClass) {
            case PERFECT_SUCCESS -> Component.literal("Perfect Success!");
            case SUCCESS -> Component.literal("Success");
            case UNSTABLE_SUCCESS -> Component.literal("Unstable Success");
            case PARTIAL_SUCCESS -> Component.literal("Partial Success");
            case MUTATED_SUCCESS -> Component.literal("Mutated Result");
            case DUD -> Component.literal("Dud");
            case RECOVERABLE_FAILURE -> Component.literal("Recoverable Failure");
            case MESSY_FAILURE -> Component.literal("Messy Failure");
            case CATASTROPHIC_FAILURE -> Component.literal("Catastrophic Failure");
        };
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

    private static Component resolveItemName(String outputId) {
        ResourceLocation loc = ResourceLocation.tryParse(outputId);
        if (loc != null) {
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item != Items.AIR) {
                return item.getDescription();
            }
        }
        return Component.literal(SynthesisStationText.shortLabel(outputId));
    }
}
