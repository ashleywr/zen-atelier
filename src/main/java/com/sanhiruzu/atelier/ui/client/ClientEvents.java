package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.data.SynthesisCatalog;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputData;
import com.sanhiruzu.atelier.synthesis.world.ItemSourceSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;
import java.util.Locale;

public final class ClientEvents {
    private ClientEvents() {
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide()) return;
        ClientDiscoveryData.clear();
        ClientExtractionKnowledgeData.clear();
        ClientRoomCatalogData.clear();
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        SynthesisOutputData outputData = stack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
        if (outputData != null) {
            addSynthesisOutputTooltip(event.getToolTip(), outputData);
            return;
        }

        if (player == null || isDiscoveryTool(stack) || !hasDiscoveryTool(player)) {
            return;
        }

        ItemSourceSnapshot source = ItemSourceSnapshot.fromStack(stack);
        if (ClientExtractionKnowledgeData.hasKnownSource(source.itemId())) {
            List<String> matchingPinned = matchingPinnedReagents(ClientExtractionKnowledgeData.knownReagents(source.itemId()));
            if (!matchingPinned.isEmpty()) {
                event.getToolTip().add(Component.translatable(
                        "tooltip.zen_atelier.extraction.pinned_known",
                        formatIds(matchingPinned)
                ).withStyle(ChatFormatting.GOLD));
            }
            event.getToolTip().add(Component.translatable(
                    "tooltip.zen_atelier.extraction.known",
                    formatIds(ClientExtractionKnowledgeData.knownReagents(source.itemId()))
            ).withStyle(ChatFormatting.DARK_AQUA));
            return;
        }

        if (ClientExtractionKnowledgeData.isTestedEmpty(source.itemId())) {
            event.getToolTip().add(Component.translatable("tooltip.zen_atelier.extraction.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        List<ExtractionProfile> profiles = SynthesisCatalog.findExtractionProfiles(source.itemId(), source.tags());
        if (profiles.isEmpty()) {
            event.getToolTip().add(Component.translatable("tooltip.zen_atelier.extraction.no_response")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        List<String> suspectedReagents = profiles.stream()
                .flatMap(profile -> profile.outcomes().stream())
                .flatMap(outcome -> java.util.stream.Stream.concat(outcome.reagents().stream(), outcome.byproducts().stream()))
                .map(reagent -> reagent.reagentId())
                .distinct()
                .sorted()
                .limit(4)
                .toList();
        if (suspectedReagents.isEmpty()) {
            event.getToolTip().add(Component.translatable("tooltip.zen_atelier.extraction.reactive")
                    .withStyle(ChatFormatting.DARK_GREEN));
            return;
        }
        List<String> matchingPinned = matchingPinnedReagents(suspectedReagents);
        if (!matchingPinned.isEmpty()) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.zen_atelier.extraction.pinned_suspected",
                    formatIds(matchingPinned)
            ).withStyle(ChatFormatting.GOLD));
        }
        event.getToolTip().add(Component.translatable(
                "tooltip.zen_atelier.extraction.suspected",
                formatIds(suspectedReagents)
        ).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static boolean hasDiscoveryTool(Player player) {
        return isDiscoveryTool(player.getMainHandItem()) || isDiscoveryTool(player.getOffhandItem());
    }

    private static boolean isDiscoveryTool(ItemStack stack) {
        return stack.is(ZenAtelier.ALCHEMIST_LENS.get()) || stack.is(ZenAtelier.ALCHEMIST_CODEX.get());
    }

    private static void addSynthesisOutputTooltip(List<Component> tooltip, SynthesisOutputData data) {
        boolean expanded = Screen.hasShiftDown();

        // Stat line: tier label (+ raw tier in shift) · color-coded quality grade
        MutableComponent statLine;
        if (expanded) {
            statLine = Component.literal(tierLabel(data.tier()) + " (T" + data.tier() + ")")
                    .withStyle(ChatFormatting.GRAY);
        } else {
            statLine = Component.literal(tierLabel(data.tier()))
                    .withStyle(ChatFormatting.GRAY);
        }
        statLine.append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY))
                .append(qualityGrade(data.quality()));
        tooltip.add(statLine);

        if (data.affixes().isEmpty()) {
            return;
        }

        if (expanded) {
            tooltip.add(Component.literal("──────────────────────").withStyle(ChatFormatting.DARK_GRAY));
        }

        for (String affix : data.affixes()) {
            String affixPath = affix.contains(":") ? affix.substring(affix.indexOf(':') + 1) : affix;
            MutableComponent name = Component.translatable("zen_atelier.affix." + affixPath)
                    .withStyle(ChatFormatting.GOLD);
            tooltip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.DARK_GRAY).append(name));
            if (expanded) {
                tooltip.add(Component.literal("   ")
                        .append(Component.translatable("zen_atelier.affix." + affixPath + ".desc")
                                .withStyle(ChatFormatting.GRAY)));
            }
        }

        if (!expanded) {
            tooltip.add(Component.literal(" Hold Shift for details")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    private static String tierLabel(int tier) {
        return switch (tier) {
            case 1 -> "Crude";
            case 2 -> "Common";
            case 3 -> "Refined";
            case 4 -> "Superior";
            case 5 -> "Masterwork";
            case 6 -> "Transcendent";
            default -> "T" + tier;
        };
    }

    private static MutableComponent qualityGrade(int quality) {
        String label;
        ChatFormatting color;
        if (quality >= 81) {
            label = "Prime";
            color = ChatFormatting.AQUA;
        } else if (quality >= 61) {
            label = "Excellent";
            color = ChatFormatting.GREEN;
        } else if (quality >= 41) {
            label = "Good";
            color = ChatFormatting.YELLOW;
        } else if (quality >= 21) {
            label = "Fair";
            color = ChatFormatting.GOLD;
        } else {
            label = "Poor";
            color = ChatFormatting.RED;
        }
        return Component.literal(label + " (" + quality + ")")
                .withStyle(color);
    }

    private static String formatIds(List<String> ids) {
        return ids.stream()
                .map(ClientEvents::readableId)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static List<String> matchingPinnedReagents(List<String> reagentIds) {
        if (!ClientExtractionKnowledgeData.hasPinnedReagentGoal()) {
            return List.of();
        }
        return reagentIds.stream()
                .filter(ClientExtractionKnowledgeData.pinnedReagentGoal()::contains)
                .sorted()
                .toList();
    }

    private static String readableId(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.replace('_', ' ').split(" ");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isBlank()) {
                parts[i] = parts[i].substring(0, 1).toUpperCase(Locale.ROOT) + parts[i].substring(1);
            }
        }
        return String.join(" ", parts);
    }

}
