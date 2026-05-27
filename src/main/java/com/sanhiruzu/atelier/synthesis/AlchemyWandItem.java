package com.sanhiruzu.atelier.synthesis;

import net.minecraft.world.item.Item;

public class AlchemyWandItem extends Item {
    private final AlchemyWandTier tier;

    public AlchemyWandItem(AlchemyWandTier tier, Properties properties) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    public AlchemyWandTier tier() {
        return tier;
    }
}
