package com.sanhiruzu.atelier.ui.patchouli;

import com.sanhiruzu.atelier.ui.client.ClientDiscoveryData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class RoomDiscoveryPageComponent implements ICustomComponent {
    private int x;
    private int y;

    @Override
    public void build(int componentX, int componentY, int pageNum) {
        this.x = componentX;
        this.y = componentY;
    }

    @Override
    public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        List<String> discovered = getDiscoveredRooms();
        int count = discovered.size();

        if (count == 0) {
            graphics.fill(x, y, x + 100, y + 20, 0xFF333333);
            graphics.fill(x, y, x + 100, y + 1, 0xFF666666);
            graphics.fill(x, y, x + 1, y + 20, 0xFF666666);
            return;
        }

        for (int i = 0; i < count; i++) {
            int boxX = x + (i % 4) * 45;
            int boxY = y + (i / 4) * 25;
            graphics.fill(boxX, boxY, boxX + 40, boxY + 20, 0xFF22AA22);
            graphics.fill(boxX, boxY, boxX + 40, boxY + 1, 0xFF44DD44);
            graphics.fill(boxX, boxY, boxX + 1, boxY + 20, 0xFF44DD44);
        }
    }

    private List<String> getDiscoveredRooms() {
        List<String> rooms = new ArrayList<>();
        String[][] roomData = {
                {"zen_atelier:bedroom", "Bedroom"},
                {"zen_atelier:storage_room", "Storage"},
                {"zen_atelier:kitchen", "Kitchen"},
                {"zen_atelier:library", "Library"},
                {"zen_atelier:workshop", "Workshop"},
                {"zen_atelier:atelier", "Atelier"},
                {"zen_atelier:smithy", "Smithy"},
                {"zen_atelier:enchanting_room", "Enchanting"},
                {"zen_atelier:masonry", "Masonry"},
                {"zen_atelier:map_room", "Map Room"},
                {"zen_atelier:loom_room", "Loom"},
                {"zen_atelier:fletchery", "Fletchery"},
                {"zen_atelier:gardener_shed", "Gardener"},
                {"zen_atelier:greenhouse", "Greenhouse"},
                {"zen_atelier:tannery", "Tannery"},
                {"zen_atelier:church", "Church"}
        };

        for (String[] room : roomData) {
            String profileId = room[0];
            if (ClientDiscoveryData.isDiscovered(profileId)) {
                int score = ClientDiscoveryData.getBestScore(profileId);
                rooms.add(room[1] + " [" + score + "%] " + getTierLabel(score));
            }
        }

        return rooms;
    }

    private String getTierLabel(int score) {
        if (score >= 80) {
            return "Excellent";
        }
        if (score >= 50) {
            return "Good";
        }
        return "Fair";
    }

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
    }
}
