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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

public class RoomDiscoveryPageComponent implements ICustomComponent {
    private static final int COLUMNS = 5;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 3;
    private static final int ICON_OFFSET = 1;
    private static final int GRID_TOP = 14;
    private static final Set<String> CUSTOM_ICON_PROFILES = Set.of(
            "atelier",
            "bedroom",
            "church",
            "enchanting_room",
            "farm_pen",
            "fletchery",
            "gardener_shed",
            "greenhouse",
            "kitchen",
            "library",
            "loom_room",
            "map_room",
            "masonry",
            "smithy",
            "storage_room",
            "tannery",
            "terrarium",
            "workshop"
    );

    private int x;
    private int y;

    @Override
    public void build(int componentX, int componentY, int pageNum) {
        this.x = componentX;
        this.y = componentY;
    }

    @Override
    public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        List<RoomCatalogSyncPayload.Entry> entries = ClientRoomCatalogData.all();
        Font font = Minecraft.getInstance().font;

        if (entries.isEmpty()) {
            graphics.drawString(font, "Room catalog not synced yet.", x, y, context.getTextColor(), false);
            return;
        }

        int discovered = ClientRoomCatalogData.discoveredCount();
        graphics.drawString(font, discovered + "/" + entries.size() + " rooms discovered", x, y, context.getHeaderColor(), false);

        for (int i = 0; i < entries.size(); i++) {
            RoomCatalogSyncPayload.Entry entry = entries.get(i);
            int slotX = x + (i % COLUMNS) * (SLOT_SIZE + SLOT_GAP);
            int slotY = y + GRID_TOP + (i / COLUMNS) * (SLOT_SIZE + SLOT_GAP);
            renderSlot(graphics, context, entry, slotX, slotY, mouseX, mouseY);
        }
    }

    private void renderSlot(GuiGraphics graphics, IComponentRenderContext context, RoomCatalogSyncPayload.Entry entry,
                            int slotX, int slotY, int mouseX, int mouseY) {
        boolean discovered = ClientDiscoveryData.isDiscovered(entry.profileId());
        int border = discovered ? 0xFF70B55B : 0xFF555555;
        int fill = discovered ? 0xFF182A18 : 0xFF202020;

        graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, border);
        graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, fill);

        ResourceLocation customIcon = customIcon(entry.profileId());
        if (customIcon != null) {
            graphics.blit(customIcon, slotX + ICON_OFFSET, slotY + ICON_OFFSET, 0, 0, 16, 16, 16, 16);
        } else {
            ItemStack icon = iconStack(entry.iconItemId());
            context.renderItemStack(graphics, slotX + ICON_OFFSET, slotY + ICON_OFFSET, mouseX, mouseY, icon);
        }

        if (!discovered) {
            graphics.fill(slotX + 1, slotY + 1, slotX + SLOT_SIZE - 1, slotY + SLOT_SIZE - 1, 0xAA111111);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, "?", slotX + 7, slotY + 5, 0xFFD0D0D0, false);
        }

        if (context.isAreaHovered(slotX, slotY, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
            context.setHoverTooltipComponents(tooltipFor(entry, discovered).stream()
                    .map(text -> (Component) Component.literal(text))
                    .toList());
        }
    }

    private List<String> tooltipFor(RoomCatalogSyncPayload.Entry entry, boolean discovered) {
        List<String> tooltip = new ArrayList<>();
        String name = displayName(entry);

        if (discovered) {
            int score = ClientDiscoveryData.getBestScore(entry.profileId());
            tooltip.add(name);
            tooltip.add("Score: " + formatScore(score) + " " + tierLabel(score));
            tooltip.add(getStarRating(score));
        } else {
            tooltip.add("Undiscovered: " + name);
            tooltip.add("");
            if (!entry.hints().isEmpty()) {
                tooltip.add("Hints:");
                for (String hint : entry.hints()) {
                    tooltip.add("  • " + hint);
                }
            }
        }

        return tooltip;
    }

    private String formatScore(int score) {
        if (score < 0) return "N/A";
        return String.format("%3d%%", score);
    }

    private String getStarRating(int score) {
        if (score >= 90) return "⭐⭐⭐⭐⭐";
        if (score >= 80) return "⭐⭐⭐⭐";
        if (score >= 70) return "⭐⭐⭐";
        if (score >= 50) return "⭐⭐";
        if (score >= 30) return "⭐";
        return "☆";
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
            if (word.isEmpty()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return label.isEmpty() ? profileId : label.toString();
    }

    private String tierLabel(int score) {
        if (score >= 80) {
            return "Excellent";
        }
        if (score >= 50) {
            return "Good";
        }
        return "Fair";
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
