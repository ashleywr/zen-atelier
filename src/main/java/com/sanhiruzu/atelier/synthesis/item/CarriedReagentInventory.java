package com.sanhiruzu.atelier.synthesis.item;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringBasketItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CarriedReagentInventory {
    private CarriedReagentInventory() {
    }

    public static ReagentContainer snapshot(Container inventory) {
        ReagentContainer container = new ReagentContainer();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ReagentStack reagent = ReagentItem.getReagent(inventory.getItem(slot));
            if (reagent != null) {
                container.insert(reagent);
                continue;
            }
            if (GatheringBasketItem.isBasket(inventory.getItem(slot))) {
                for (ReagentStack entry : GatheringBasketItem.entries(inventory.getItem(slot))) {
                    container.insert(entry);
                }
            }
        }
        return container;
    }

    public static boolean consume(Container inventory, List<ReagentStack> consumed) {
        List<Integer> slots = new ArrayList<>();
        List<ReagentStack> carried = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ReagentStack reagent = ReagentItem.getReagent(inventory.getItem(slot));
            if (reagent != null) {
                slots.add(slot);
                carried.add(reagent);
                continue;
            }
            if (GatheringBasketItem.isBasket(inventory.getItem(slot))) {
                for (ReagentStack entry : GatheringBasketItem.entries(inventory.getItem(slot))) {
                    slots.add(slot);
                    carried.add(entry);
                }
            }
        }

        Optional<List<ReagentStack>> remaining = remainingAfterConsume(carried, consumed);
        if (remaining.isEmpty()) {
            return false;
        }

        List<ReagentStack> remainingStacks = remaining.get();
        java.util.Map<Integer, ReagentContainer> basketRemainders = new java.util.LinkedHashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            ItemStack stack = inventory.getItem(slot);
            ReagentStack reagent = remainingStacks.get(i);
            if (GatheringBasketItem.isBasket(stack)) {
                if (reagent != null) {
                    basketRemainders.computeIfAbsent(slot, ignored -> new ReagentContainer()).insert(reagent);
                }
                continue;
            }
            if (reagent == null) {
                inventory.setItem(slot, ItemStack.EMPTY);
            } else {
                inventory.setItem(slot, ReagentItem.createStack(reagent));
            }
        }
        for (var entry : basketRemainders.entrySet()) {
            GatheringBasketItem.setContents(inventory.getItem(entry.getKey()), entry.getValue());
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (GatheringBasketItem.isBasket(stack) && !basketRemainders.containsKey(slot)) {
                GatheringBasketItem.setContents(stack, new ReagentContainer());
            }
        }
        inventory.setChanged();
        return true;
    }

    static Optional<List<ReagentStack>> remainingAfterConsume(List<ReagentStack> carried, List<ReagentStack> consumed) {
        List<ReagentStack> remaining = new ArrayList<>(carried);
        for (ReagentStack stack : consumed) {
            if (!hasAmount(remaining, stack)) {
                return Optional.empty();
            }
        }

        for (ReagentStack stack : consumed) {
            consumeMatching(remaining, stack);
        }
        return Optional.of(remaining);
    }

    private static boolean hasAmount(List<ReagentStack> carried, ReagentStack target) {
        int available = 0;
        for (ReagentStack reagent : carried) {
            if (sameProfile(reagent, target)) {
                available += reagent.amount();
            }
        }
        return available >= target.amount();
    }

    private static void consumeMatching(List<ReagentStack> carried, ReagentStack target) {
        int remaining = target.amount();
        for (int slot = 0; slot < carried.size() && remaining > 0; slot++) {
            ReagentStack reagent = carried.get(slot);
            if (!sameProfile(reagent, target)) {
                continue;
            }

            int taken = Math.min(reagent.amount(), remaining);
            remaining -= taken;
            int left = reagent.amount() - taken;
            if (left <= 0) {
                carried.set(slot, null);
            } else {
                carried.set(slot, reagent.withAmount(left));
            }
        }
    }

    private static boolean sameProfile(ReagentStack left, ReagentStack right) {
        return left != null
                  && right != null
                  && left.reagentId().equals(right.reagentId())
                  && left.categories().equals(right.categories())
                  && left.tier() == right.tier()
                && left.quality() == right.quality()
                && left.purity() == right.purity()
                && left.instability() == right.instability()
                  && left.elements().equals(right.elements())
                  && left.traits().equals(right.traits())
                  && left.shape().equals(right.shape())
                  && left.sourceHints().equals(right.sourceHints());
    }
}
