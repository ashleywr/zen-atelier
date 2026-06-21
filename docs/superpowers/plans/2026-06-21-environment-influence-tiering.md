# Environment Influence Tiering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable environment influence resolver so tiered zone effects reward varied contributors and apply diminishing returns to repeated sources.

**Architecture:** Add focused API records for influence categories, profiles, results, and a resolver. Migrate sleep comfort to a code-defined profile, preserving existing reward behavior while exposing explanation data for later inspection/highlight UI.

**Tech Stack:** Java 21, NeoForge 1.21.1, JUnit 5, AssertJ, Gradle.

---

## File Structure

- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceCategory.java`: category id, display name key, cap, and match prefixes.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceProfile.java`: target effect type, categories, uncategorized fallback, variety bonus, and thresholds.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceResult.java`: normalized score, tier, category breakdown, capped/diminished state, and contributing effect ids.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceResolver.java`: pure resolver over `List<EnvironmentEffect>` and a profile.
- Create `src/test/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceResolverTest.java`: resolver behavior and edge cases.
- Modify `src/main/java/com/sanhiruzu/atelier/synthesis/world/SleepComfortEffects.java`: use the shared resolver and expose sleep comfort result.
- Modify `src/test/java/com/sanhiruzu/atelier/synthesis/world/SleepComfortEffectsTest.java`: update sleep tests to assert variety beats duplicates.
- Modify `src/main/resources/assets/zen_atelier/lang/en_us.json`: add category display names for explanation text.

## Task 1: Add Resolver API

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceCategory.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceProfile.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceResult.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceResolver.java`
- Test: `src/test/java/com/sanhiruzu/atelier/api/EnvironmentInfluenceResolverTest.java`

- [ ] **Step 1: Write failing resolver tests**

```java
package com.sanhiruzu.atelier.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentInfluenceResolverTest {
    private static final EnvironmentInfluenceProfile PROFILE = new EnvironmentInfluenceProfile(
            EnvironmentEffectType.COMFORT_MODIFIER,
            List.of(
                    new EnvironmentInfluenceCategory("shelter", "category.shelter", 0.35f, List.of("sleep_shelter")),
                    new EnvironmentInfluenceCategory("bedding", "category.bedding", 0.35f, List.of("sleep_bedding")),
                    new EnvironmentInfluenceCategory("decor", "category.decor", 0.35f, List.of("sleep_decor"))
            ),
            new EnvironmentInfluenceCategory("other", "category.other", 0.20f, List.of()),
            0.10f,
            List.of(0.45f, 0.90f, 1.25f)
    );

    @Test
    void repeatedSourcesAreCappedPerCategory() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(List.of(
                EnvironmentEffects.modifyComfort("sleep_bedding_wool", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_bedding_carpet", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_bedding_blanket", 0.30f)
        ), PROFILE);

        assertThat(result.tier()).isEqualTo(0);
        assertThat(result.category("bedding").contribution()).isEqualTo(0.35f);
        assertThat(result.category("bedding").capped()).isTrue();
    }

    @Test
    void variedCategoriesReachHigherTierThanDuplicateCategory() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(List.of(
                EnvironmentEffects.modifyComfort("sleep_shelter_roof", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_bedding_wool", 0.30f),
                EnvironmentEffects.modifyComfort("sleep_decor_books", 0.30f)
        ), PROFILE);

        assertThat(result.tier()).isEqualTo(2);
        assertThat(result.score()).isEqualTo(1.20f);
    }

    @Test
    void uncategorizedEffectsUseFallbackCategory() {
        EnvironmentInfluenceResult result = EnvironmentInfluenceResolver.resolve(List.of(
                EnvironmentEffects.modifyComfort("modded_comfort", 0.50f)
        ), PROFILE);

        assertThat(result.category("other").contribution()).isEqualTo(0.20f);
        assertThat(result.category("other").capped()).isTrue();
    }
}
```

- [ ] **Step 2: Run tests to verify failure**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.api.EnvironmentInfluenceResolverTest`

Expected: compilation fails because resolver types do not exist.

- [ ] **Step 3: Implement resolver records and algorithm**

Create immutable records with constructor validation. Resolver should ignore null effects, ignore mismatched effect types, clamp negative amounts to zero, group by category prefix, cap each category, add `varietyBonus` once per active category after the first, and calculate tier from ascending thresholds.

- [ ] **Step 4: Run resolver tests**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.api.EnvironmentInfluenceResolverTest`

Expected: PASS.

## Task 2: Migrate Sleep Comfort

**Files:**
- Modify: `src/main/java/com/sanhiruzu/atelier/synthesis/world/SleepComfortEffects.java`
- Modify: `src/test/java/com/sanhiruzu/atelier/synthesis/world/SleepComfortEffectsTest.java`

- [ ] **Step 1: Update sleep tests**

Change repeated-source expectations to assert the shared result explains caps, and add a varied-category expectation that reaches a higher tier.

- [ ] **Step 2: Run sleep tests to verify failure**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.synthesis.world.SleepComfortEffectsTest`

Expected: compilation or assertion failure until sleep uses the resolver.

- [ ] **Step 3: Replace local sleep scoring**

Add a `SLEEP_COMFORT_PROFILE` with categories `shelter`, `bedding`, `decor`, `lighting`, and `nature`. Change built-in effect ids to category-prefixed ids and make `rewardFor` call `comfortResultFor(effects).tier()`.

- [ ] **Step 4: Run sleep tests**

Run: `.\gradlew.bat test --tests com.sanhiruzu.atelier.synthesis.world.SleepComfortEffectsTest`

Expected: PASS.

## Task 3: Add Explanation Labels And Full Verification

**Files:**
- Modify: `src/main/resources/assets/zen_atelier/lang/en_us.json`

- [ ] **Step 1: Add category translation keys**

Add translation keys for sleep comfort category names so later UI can show player-readable category summaries.

- [ ] **Step 2: Run full test/build/sync**

Run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat syncSynesthesia
```

Expected: all three commands finish with `BUILD SUCCESSFUL`.
