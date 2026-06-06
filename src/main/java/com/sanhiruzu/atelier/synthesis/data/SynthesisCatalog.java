package com.sanhiruzu.atelier.synthesis.data;

import com.sanhiruzu.atelier.synthesis.core.SourceKey;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SynthesisCatalog {
    private SynthesisCatalog() {
    }

    public static List<ExtractionProfile> findExtractionProfiles(String itemId, Set<String> itemTags) {
        return ExtractionProfileRegistry.all().stream()
                .filter(profile -> SourceKey.parse(profile.source()).matches(itemId, itemTags))
                .map(ExtractionProfileDefinition::toCore)
                .toList();
    }

    public static Optional<SynthesisProfile> getSynthesisProfile(ResourceLocation id) {
        SynthesisProfileDefinition definition = SynthesisProfileRegistry.get(id);
        return definition == null ? Optional.empty() : Optional.of(definition.toCore());
    }
}
