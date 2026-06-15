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

import java.util.List;

final class SynthesisResultOverlay {
    static final int FAILURE_IMPACT_TICKS = 20;

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
        int ox = origin.x();
        int oy = origin.y();

        // Darken the whole GUI area
        int screenW = SynthesisStationMetrics.DEFAULT.width();
        int screenH = SynthesisStationMetrics.DEFAULT.height();
        graphics.fill(ox, oy, ox + screenW, oy + screenH, 0xDC0E0B08);

        // Panel: centered horizontally over the main content area
        int pw = 280;
        int affixRows = outputs.stream().mapToInt(o -> o.affixes().isEmpty() ? 0 : 1).sum();
        int ph = Math.max(90, 56 + outputs.size() * 18 + affixRows * 11 + (byproducts.isEmpty() ? 0 : 20 + byproducts.size() * 18));
        int px = ox + (screenW - pw) / 2;
        int py = oy + 55;
        ScreenRect panel = new ScreenRect(px, py, pw, ph);
        SynthesisStationDrawing.panel(graphics, panel);
        SynthesisStationDrawing.frame(graphics, panel, titleColor());

        // Outcome title
        int titleX = px + pw / 2;
        int titleY = py + 12;
        graphics.drawCenteredString(font, outcomeTitle(), titleX, titleY, titleColor());
        if (!outcomeClass.successful()) {
            graphics.drawCenteredString(font, failureDetail(), titleX, titleY + 11, SynthesisScreenTheme.MUTED);
        }

        // Divider
        int dividerY = outcomeClass.successful() ? titleY + 14 : titleY + 24;
        graphics.fill(px + 10, dividerY, px + pw - 10, dividerY + 1, 0x44EFE6D5);

        // Outputs
        int listY = dividerY + 8;
        if (outputs.isEmpty()) {
            graphics.drawCenteredString(font, emptyOutputText(),
                    titleX, listY, SynthesisScreenTheme.MUTED);
        } else {
            for (SynthesisOutput output : outputs) {
                ResourceLocation loc = ResourceLocation.tryParse(output.outputId());
                Item item = (loc != null) ? BuiltInRegistries.ITEM.get(loc) : Items.AIR;
                Component name = (item != Items.AIR) ? item.getDescription()
                        : Component.literal(SynthesisStationText.shortLabel(output.outputId()));
                ChatFormatting qualityFmt = output.quality() >= 100 ? ChatFormatting.GOLD
                        : output.quality() >= 75 ? ChatFormatting.AQUA
                        : output.quality() >= 50 ? ChatFormatting.GRAY
                        : ChatFormatting.DARK_GRAY;
                String qualityTierKey = output.quality() >= 100 ? "tooltip.zen_atelier.quality.masterwork"
                        : output.quality() >= 75 ? "tooltip.zen_atelier.quality.superior"
                        : output.quality() >= 50 ? "tooltip.zen_atelier.quality.fine"
                        : "tooltip.zen_atelier.quality.crude";
                MutableComponent statsComp = Component.literal(output.count() + "x  T" + output.tier() + "  ")
                        .withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED))
                        .append(Component.translatable(qualityTierKey).withStyle(qualityFmt))
                        .append(Component.literal(" " + output.quality()).withStyle(s -> s.withColor(SynthesisScreenTheme.MUTED)));
                if (item != Items.AIR) {
                    graphics.renderFakeItem(item.getDefaultInstance(), px + 12, listY);
                }
                graphics.drawString(font, name, px + 32, listY + 5, SynthesisScreenTheme.TEXT, false);
                graphics.drawString(font, statsComp, px + pw - 10 - font.width(statsComp), listY + 5,
                        SynthesisScreenTheme.TEXT, false);
                listY += 18;
                if (!output.affixes().isEmpty()) {
                    MutableComponent affixLine = Component.empty();
                    for (int i = 0; i < output.affixes().size(); i++) {
                        if (i > 0) {
                            affixLine.append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY));
                        }
                        String affixId = output.affixes().get(i);
                        String affixKey = affixId.contains(":") ? affixId.replace(":", ".affix.") : "zen_atelier.affix." + affixId;
                        affixLine.append(Component.translatable(affixKey)
                                .withStyle(ChatFormatting.GOLD));
                    }
                    graphics.drawString(font, affixLine, px + 32, listY, SynthesisScreenTheme.TEXT, false);
                    listY += 11;
                }
            }
        }
        if (!byproducts.isEmpty()) {
            graphics.fill(px + 10, listY + 1, px + pw - 10, listY + 2, 0x33EFE6D5);
            listY += 9;
            graphics.drawString(font, byproductHeading(), px + 14, listY, SynthesisScreenTheme.MUTED, false);
            listY += 11;
            for (ReagentStack bp : byproducts) {
                ItemStack bpStack = ReagentItem.createStack(bp);
                Component name = Component.literal(SynthesisStationText.shortLabel(bp.reagentId()));
                String stats = bp.amount() + "x  T" + bp.tier() + (bp.quality() > 0 ? "  " + bp.quality() + "Q" : "");
                graphics.renderFakeItem(bpStack, px + 12, listY);
                graphics.drawString(font, name, px + 32, listY + 5, SynthesisScreenTheme.TEXT, false);
                graphics.drawString(font, stats, px + pw - 10 - font.width(stats), listY + 5,
                        SynthesisScreenTheme.MUTED, false);
                listY += 18;
            }
        }
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

}
