package com.sanhiruzu.atelier.ui.patchouli;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.function.UnaryOperator;

/**
 * Room stat card — inert pending the atmosphere substrate.
 */
public class RoomStatCardComponent implements ICustomComponent {
    /** Optional: set in page JSON as {@code "room": "namespace:profile_id"} to pin the card to a specific room. */
    public String room = "";

    private int x;
    private int y;

    @Override
    public void build(int componentX, int componentY, int pageNum) {
        this.x = componentX;
        this.y = componentY;
    }

    public void setRoomProfile(String profileId) {
        // Room bonuses disabled pending the atmosphere substrate.
    }

    @Override
    public void render(GuiGraphics graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        // Room bonuses disabled pending the atmosphere substrate.
        graphics.drawString(Minecraft.getInstance().font, "Room info coming soon.", x, y, context.getTextColor(), false);
    }

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
    }
}
