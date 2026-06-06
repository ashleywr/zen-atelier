package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.synthesis.core.SourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.stream.Collectors;

public record ItemSourceSnapshot(String itemId, Set<String> tags) {
    public ItemSourceSnapshot {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        tags = Set.copyOf(tags);
    }

    public static ItemSourceSnapshot fromStack(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("stack must not be empty");
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        Set<String> tags = stack.getTags()
                .map(tag -> tag.location().toString())
                .collect(Collectors.toUnmodifiableSet());
        return new ItemSourceSnapshot(itemId, tags);
    }

    public boolean matches(SourceKey sourceKey) {
        return sourceKey.matches(itemId, tags);
    }
}
