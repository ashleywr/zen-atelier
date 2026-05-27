package com.sanhiruzu.atelier.ui.patchouli;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.ui.client.ClientDiscoveryData;
import com.sanhiruzu.atelier.ui.client.ClientRoomCatalogData;
import com.sanhiruzu.atelier.ui.network.RoomCatalogSyncPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Pokédex-style stat card for room types
 * Shows room name, score, tier, and key properties
 */
public class RoomStatCardComponent implements ICustomComponent {
    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 160;
    private static final int ICON_SIZE = 32;
    private static final Set<String> CUSTOM_ICON_PROFILES = Set.of(
            "atelier", "bedroom", "church", "enchanting_room", "farm_pen",
            "fletchery", "gardener_shed", "greenhouse", "kitchen", "library",
            "loom_room", "map_room", "masonry", "smithy", "storage_room",
            "tannery", "terrarium", "workshop"
    );

    private String roomProfileId = "";
    private int x;
    private int y;

    @Override
    public void build(int componentX, int componentY, int pageNum) {
        this.x = componentX;
        this.y = componentY;
    }

    public void setRoomProfile(String profileId) {
        this.roomProfileId = profileId;
    }

    @Override
    public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        // If no specific room was set, show the first discovered room
        if (roomProfileId.isEmpty()) {
            Optional<RoomCatalogSyncPayload.Entry> optFirstDiscovered = ClientRoomCatalogData.all().stream()
                    .filter(e -> ClientDiscoveryData.isDiscovered(e.profileId()))
                    .findFirst();

            if (optFirstDiscovered.isPresent()) {
                roomProfileId = optFirstDiscovered.get().profileId();
            } else {
                // Show message if no rooms discovered yet
                graphics.drawString(Minecraft.getInstance().font, "No rooms discovered yet!", x + 4, y + 10, context.getTextColor(), false);
                graphics.drawString(Minecraft.getInstance().font, "Explore to find rooms.", x + 4, y + 22, context.getTextColor(), false);
                return;
            }
        }

        Optional<RoomCatalogSyncPayload.Entry> optEntry = ClientRoomCatalogData.all().stream()
                .filter(e -> e.profileId().equals(roomProfileId))
                .findFirst();

        if (optEntry.isEmpty()) {
            graphics.drawString(Minecraft.getInstance().font, "Room not found", x, y, context.getTextColor(), false);
            return;
        }

        RoomCatalogSyncPayload.Entry entry = optEntry.get();
        boolean discovered = ClientDiscoveryData.isDiscovered(roomProfileId);

        // Draw card background
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF182A18); // Dark background
        graphics.fill(x + 1, y + 1, x + CARD_WIDTH - 1, y + CARD_HEIGHT - 1, 0xFF0F1A10); // Darker inner

        if (discovered) {
            renderDiscoveredCard(graphics, context, entry);
        } else {
            renderUndiscoveredCard(graphics, context, entry);
        }
    }

    private void renderDiscoveredCard(GuiGraphics graphics, IComponentRenderContext context, RoomCatalogSyncPayload.Entry entry) {
        Font font = Minecraft.getInstance().font;
        int score = ClientDiscoveryData.getBestScore(entry.profileId());

        // Icon
        int iconX = x + (CARD_WIDTH - ICON_SIZE) / 2;
        int iconY = y + 8;
        renderRoomIcon(graphics, context, entry, iconX, iconY, ICON_SIZE, ICON_SIZE);

        // Name
        String name = displayName(entry);
        int nameY = iconY + ICON_SIZE + 4;
        graphics.drawString(font, name, x + 4, nameY, 0xFF70B55B, true); // Green for discovered

        // Score
        int scoreY = nameY + 12;
        String scoreStr = String.format("%d%%", Math.max(0, score));
        graphics.drawString(font, "Score:", x + 4, scoreY, 0xFFD8B46A, false); // Gold
        graphics.drawString(font, scoreStr, x + CARD_WIDTH - font.width(scoreStr) - 4, scoreY, 0xFF70B55B, false); // Green

        // Tier
        int tierY = scoreY + 10;
        String tier = getTierLabel(score);
        graphics.drawString(font, tier, x + 4, tierY, 0xFF416F8F, false); // Blue
        graphics.drawString(font, getStarRating(score), x + CARD_WIDTH - 24, tierY, 0xFFD8B46A, false); // Gold stars
    }

    private void renderUndiscoveredCard(GuiGraphics graphics, IComponentRenderContext context, RoomCatalogSyncPayload.Entry entry) {
        Font font = Minecraft.getInstance().font;

        // Silhouette/shadow icon
        int iconX = x + (CARD_WIDTH - ICON_SIZE) / 2;
        int iconY = y + 8;
        graphics.fill(iconX, iconY, iconX + ICON_SIZE, iconY + ICON_SIZE, 0xFF333333); // Gray shadow
        graphics.drawString(font, "?", iconX + 12, iconY + 10, 0xFF999999, true); // Gray question mark

        // Undiscovered label
        int nameY = iconY + ICON_SIZE + 4;
        graphics.drawString(font, "Undiscovered", x + 4, nameY, 0xFF888888, true);

        // Hints
        int hintY = nameY + 12;
        graphics.drawString(font, "Hints:", x + 4, hintY, 0xFF999999, false);

        int displayedHints = 0;
        for (String hint : entry.hints()) {
            if (displayedHints >= 2) break; // Only show first 2 hints
            hintY += 10;
            graphics.drawString(font, "• " + hint, x + 6, hintY, 0xFF666666, false);
            displayedHints++;
        }
    }

    private void renderRoomIcon(GuiGraphics graphics, IComponentRenderContext context, RoomCatalogSyncPayload.Entry entry,
                                int x, int y, int width, int height) {
        ResourceLocation customIcon = customIcon(entry.profileId());
        if (customIcon != null) {
            graphics.blit(customIcon, x, y, 0, 0, width, height, width, height);
        } else {
            ItemStack icon = iconStack(entry.iconItemId());
            context.renderItemStack(graphics, x, y, 0, 0, icon); // Use 0,0 for mouse to avoid tooltips
        }
    }

    private String displayName(RoomCatalogSyncPayload.Entry entry) {
        String key = entry.displayName();
        return I18n.exists(key) ? I18n.get(key) : fallbackName(entry.profileId());
    }

    private String fallbackName(String profileId) {
        int separator = profileId.indexOf(':');
        String path = separator >= 0 ? profileId.substring(separator + 1) : profileId;
        String[] words = path.split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!label.isEmpty()) label.append(' ');
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.isEmpty() ? profileId : label.toString();
    }

    private String getTierLabel(int score) {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Good";
        if (score >= 40) return "Fair";
        if (score >= 20) return "Poor";
        return "Minimal";
    }

    private String getStarRating(int score) {
        if (score >= 90) return "⭐⭐⭐⭐⭐";
        if (score >= 80) return "⭐⭐⭐⭐";
        if (score >= 70) return "⭐⭐⭐";
        if (score >= 50) return "⭐⭐";
        if (score >= 30) return "⭐";
        return "☆";
    }

    private ItemStack iconStack(String itemId) {
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        } catch (RuntimeException ignored) {
        }
        return new ItemStack(Items.PAPER);
    }

    private ResourceLocation customIcon(String profileId) {
        ResourceLocation id = ResourceLocation.tryParse(profileId);
        if (id == null || !ZenAtelier.MODID.equals(id.getNamespace()) || !CUSTOM_ICON_PROFILES.contains(id.getPath())) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(
                ZenAtelier.MODID,
                "textures/gui/room_icons/" + id.getPath() + ".png"
        );
    }

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
    }
}
