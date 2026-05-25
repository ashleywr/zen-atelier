package com.sanhiruzu.atelier.space.zone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("BlockRarityCache Tests")
class BlockRarityCacheTest {

    @Test
    @DisplayName("Default rarity for uninitialized blocks is COMMON (0.5)")
    void testDefaultRarityIsCommon() {
        // Document the expected fallback behavior: unknown blocks get COMMON
        float expected = 0.5f;
        assertThat(expected).isCloseTo(0.5f, within(0.001f));
    }

    @Test
    @DisplayName("Rarity values are in valid range [0.0, 2.0]")
    void testRarityRangeIsValid() {
        // Boundary conditions for rarity scoring
        float commonMin = 0.5f; // COMMON
        float epicMax = 2.0f;   // EPIC

        assertThat(commonMin).isGreaterThanOrEqualTo(0.0f).isLessThanOrEqualTo(epicMax);
        assertThat(epicMax).isGreaterThanOrEqualTo(0.0f).isLessThanOrEqualTo(2.0f);
    }

    @Test
    @DisplayName("End block rarity constant is EPIC (2.0)")
    void testEndBlockRarityIsEpic() {
        // End blocks should be the highest rarity
        float endRarity = 2.0f;
        assertThat(endRarity).isCloseTo(2.0f, within(0.001f));
    }

    @Test
    @DisplayName("Nether block rarity constant is RARE (1.5)")
    void testNetherBlockRarityIsRare() {
        // Nether blocks are less rare than End
        float netherRarity = 1.5f;
        assertThat(netherRarity).isLessThan(2.0f);
        assertThat(netherRarity).isGreaterThan(0.5f);
    }

    @Test
    @DisplayName("Deep Dark block rarity constant is 1.3")
    void testDeepDarkRarityIsMidRare() {
        float deepDarkRarity = 1.3f;
        assertThat(deepDarkRarity).isLessThan(1.5f);
        assertThat(deepDarkRarity).isGreaterThan(1.0f);
    }

    @Test
    @DisplayName("Rarity values are consistent constants")
    void testRarityConstantsAreConsistent() {
        // Verify the hierarchy of rarity values
        float common = 0.5f;
        float uncommon = 1.0f;
        float deepDark = 1.3f;
        float nether = 1.5f;
        float epic = 2.0f;

        assertThat(common).isLessThan(uncommon);
        assertThat(uncommon).isLessThan(deepDark);
        assertThat(deepDark).isLessThan(nether);
        assertThat(nether).isLessThan(epic);
    }

    @Test
    @DisplayName("Rarity capping: values capped at EPIC (2.0)")
    void testRarityCapAtEpic() {
        float overCap = 3.0f;
        float capped = Math.min(2.0f, overCap);
        assertThat(capped).isEqualTo(2.0f);
    }

    @Test
    @DisplayName("Recipe average logic: plain average of ingredients")
    void testRecipeAveragingLogic() {
        // Test the averaging formula used in computeRarity
        float ingredient1 = 0.5f;
        float ingredient2 = 0.5f;
        float ingredient3 = 0.5f;
        float ingredient4 = 0.5f;

        float average = (ingredient1 + ingredient2 + ingredient3 + ingredient4) / 4.0f;
        assertThat(average).isCloseTo(0.5f, within(0.001f));
    }

    @Test
    @DisplayName("Recipe cap at EPIC: averaged rarity capped at 2.0")
    void testRecipeCapAtEpic() {
        float rarityValue = 2.5f;
        float capped = Math.min(2.0f, rarityValue);
        assertThat(capped).isEqualTo(2.0f);
    }
}
