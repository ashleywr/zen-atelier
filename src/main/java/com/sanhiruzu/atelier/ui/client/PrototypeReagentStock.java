package com.sanhiruzu.atelier.ui.client;

import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionAttempt;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionEngine;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

final class PrototypeReagentStock {
    private static final int ERA_ONE_TIER_CAP = 1;
    private static final int ROLLS_PER_PROFILE = 2;
    private static final long SEED_SALT = 0x5EED5A7E11L;

    private static String cachedSignature = "";
    private static List<ReagentStack> cachedEraOneStock = List.of();

    private PrototypeReagentStock() {
    }

    static List<ReagentStack> eraOneStorageStock() {
        String signature = registrySignature();
        if (signature.equals(cachedSignature)) {
            return cachedEraOneStock;
        }

        ExtractionEngine engine = new ExtractionEngine();
        ArrayList<ReagentStack> stock = new ArrayList<>();
        List<ExtractionProfileDefinition> definitions = ExtractionProfileRegistry.all().stream()
                .sorted(Comparator.comparing(definition -> definition.id().toString()))
                .toList();

        for (ExtractionProfileDefinition definition : definitions) {
            ExtractionProfile profile = definition.toCore();
            for (int roll = 0; roll < ROLLS_PER_PROFILE; roll++) {
                long seed = SEED_SALT ^ ((long) profile.id().hashCode() << 32) ^ roll;
                ExtractionResult result = engine.roll(new ExtractionAttempt(
                        profile,
                        1,
                        ERA_ONE_TIER_CAP,
                        ERA_ONE_TIER_CAP,
                        ERA_ONE_TIER_CAP,
                        seed
                ));
                stock.addAll(result.reagents());
                stock.addAll(result.byproducts());
            }
        }

        cachedSignature = signature;
        cachedEraOneStock = List.copyOf(stock);
        return cachedEraOneStock;
    }

    private static String registrySignature() {
        return ExtractionProfileRegistry.all().stream()
                .map(definition -> definition.id().toString())
                .sorted()
                .collect(Collectors.joining("|"));
    }
}
