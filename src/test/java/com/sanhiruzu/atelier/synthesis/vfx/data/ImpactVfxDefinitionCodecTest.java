package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ImpactVfxDefinitionCodecTest {
    private static final String ICE = """
        {
          "id": "zen_atelier:ice",
          "sound": "minecraft:block.glass.break",
          "emitters": [
            { "particle": "zen_atelier:ice_crystal", "shape": "ring",
              "count": [4,7,11,16], "radius": [1.2,1.8,2.5,3.2],
              "size": [1.0,1.4,1.9,2.6], "lifetime": [12,14,16,20],
              "grow_ticks": 6, "fade_ticks": 4, "anchor": "ground" }
          ]
        }
        """;

    @Test
    void parsesEmittersAndPerTierValues() {
        ImpactVfxDefinition def = ImpactVfxDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(ICE)).getOrThrow();
        assertThat(def.emitters()).hasSize(1);
        EmitterDefinition e = def.emitters().get(0);
        assertThat(e.shape()).isEqualTo(EmitterShape.RING);
        assertThat(e.anchor()).isEqualTo(Anchor.GROUND);
        assertThat(e.count().intAt(0)).isEqualTo(4);
        assertThat(e.count().intAt(3)).isEqualTo(16);
        assertThat(e.size().floatAt(2)).isEqualTo(1.9f);
    }

    @Test
    void emptyEmittersIsRejected() {
        String bad = "{ \"id\": \"zen_atelier:x\", \"emitters\": [] }";
        assertThat(ImpactVfxDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(bad)).error()).isPresent();
    }
}
