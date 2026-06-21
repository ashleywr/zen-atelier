package com.sanhiruzu.atelier.environment;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentFacetMappingReloadListenerTest {
    @Test
    void parsesFacetToSignalMappingJson() {
        EnvironmentFacetMappings mappings = EnvironmentFacetMappingReloadListener.parse(JsonParser.parseString("""
                {
                  "facets": {
                    "ami:decor.soft": ["sleep_soft_bedding", "wool"],
                    "ami:decor.plant": "plant"
                  }
                }
                """));

        assertThat(mappings.signalsFor(java.util.Set.of("ami:decor.soft")))
                .containsExactlyInAnyOrder("sleep_soft_bedding", "wool");
        assertThat(mappings.signalsFor(java.util.Set.of("ami:decor.plant")))
                .containsExactly("plant");
    }
}
