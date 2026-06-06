package com.sanhiruzu.atelier.synthesis.storage;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReagentContainer {
    private static final Comparator<ReagentStack> BEST_MATCH_ORDER = Comparator
            .comparingInt(ReagentStack::tier).reversed()
            .thenComparing(Comparator.comparingInt(ReagentStack::purity).reversed())
            .thenComparing(Comparator.comparingInt(ReagentStack::quality).reversed())
            .thenComparing(ReagentStack::reagentId);

    private final List<ReagentStack> entries = new ArrayList<>();

    public List<ReagentStack> entries() {
        return List.copyOf(entries);
    }

    public void insert(ReagentStack stack) {
        for (int i = 0; i < entries.size(); i++) {
            ReagentStack existing = entries.get(i);
            if (sameProfile(existing, stack)) {
                entries.set(i, existing.withAmount(existing.amount() + stack.amount()));
                return;
            }
        }
        entries.add(stack);
    }

    public int totalAmount(ReagentQuery query) {
        return entries.stream()
                .filter(query::matches)
                .mapToInt(ReagentStack::amount)
                .sum();
    }

    public List<ReagentStack> search(ReagentQuery query) {
        return entries.stream()
                .filter(query::matches)
                .sorted(BEST_MATCH_ORDER)
                .toList();
    }

    public List<ReagentStack> extract(ReagentQuery query, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (totalAmount(query) < amount) {
            return List.of();
        }

        int remaining = amount;
        List<ReagentStack> extracted = new ArrayList<>();
        for (ReagentStack stack : search(query)) {
            if (remaining <= 0) {
                break;
            }

            int taken = Math.min(stack.amount(), remaining);
            extracted.add(stack.withAmount(taken));
            remaining -= taken;

            int i = indexOfSameProfile(stack);
            if (i < 0) {
                throw new IllegalStateException("ranked reagent disappeared during extraction");
            }
            if (taken == stack.amount()) {
                entries.remove(i);
            } else {
                entries.set(i, stack.withAmount(stack.amount() - taken));
            }
        }
        return List.copyOf(extracted);
    }

    private static boolean sameProfile(ReagentStack left, ReagentStack right) {
        return left.reagentId().equals(right.reagentId())
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

    private int indexOfSameProfile(ReagentStack stack) {
        for (int i = 0; i < entries.size(); i++) {
            if (sameProfile(entries.get(i), stack)) {
                return i;
            }
        }
        return -1;
    }
}
