# Environment Query Effects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a cache-backed environment snapshot and declarative effect-rule API beside the dormant room/zone scoring system.

**Architecture:** Add small public API value types under `com.sanhiruzu.atelier.api`, keep scan/cache internals under `com.sanhiruzu.atelier.environment`, and route the existing `ZoneAPI` facade through the new scanner. The first pass exposes reusable environment facts, rule matching, and dirty cache invalidation without wiring old room journal or MineColonies room scoring back in.

**Tech Stack:** Java 21, NeoForge 1.21.1 APIs, Minecraft `Level`/`BlockPos`/`AABB`, JUnit 5, AssertJ, Gradle.

---

## File Structure

- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentAirHazard.java`: public enum for derived air hazards.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentTemperatureBand.java`: public enum for cold-to-hot bands.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectType.java`: public enum for typed effect outputs.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffect.java`: immutable effect value.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentSnapshot.java`: immutable snapshot exposed to callers.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentCondition.java`: functional condition interface.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentConditions.java`: built-in declarative condition factories.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffects.java`: built-in effect factories.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectContext.java`: context passed to callbacks/rule evaluation.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRule.java`: immutable target/condition/effect rule.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistrar.java`: fluent Java registration builder.
- Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistry.java`: public registry and evaluator.
- Modify `src/main/java/com/sanhiruzu/atelier/api/ZoneAPI.java`: add `environmentAt`, cache invalidation, and effect registration facade methods.
- Modify `src/main/java/com/sanhiruzu/atelier/data/Signals.java`: add environment-oriented signal names used by the scanner and examples.
- Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanProfile.java`: internal scan profile enum.
- Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCache.java`: internal cache keyed by level identity, position, radius, and scan profile.
- Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanner.java`: internal bounded world scanner that produces `EnvironmentSnapshot`.
- Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentCacheInvalidation.java`: internal static invalidation bridge.
- Modify `src/main/java/com/sanhiruzu/atelier/event/AtelierEvents.java`: mark nearby environment cache entries dirty on block mutations and clear unloaded levels.
- Create `src/test/java/com/sanhiruzu/atelier/api/EnvironmentSnapshotTest.java`: pure snapshot behavior tests.
- Create `src/test/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistryTest.java`: condition/rule/effect tests.
- Create `src/test/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCacheTest.java`: pure cache reuse and invalidation tests.
- Create `src/test/java/com/sanhiruzu/atelier/resources/EnvironmentSignalRegistryTest.java`: verifies required signal names exist.

## Task 1: Public Snapshot Value Types

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentAirHazard.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentTemperatureBand.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentSnapshot.java`
- Test: `src/test/java/com/sanhiruzu/atelier/api/EnvironmentSnapshotTest.java`

- [ ] **Step 1: Write the failing snapshot test**

Create `src/test/java/com/sanhiruzu/atelier/api/EnvironmentSnapshotTest.java`:

```java
package com.sanhiruzu.atelier.api;

import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentSnapshotTest {
    @Test
    void exposesSignalEntityAndDerivedFacts() {
        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                Map.of("bookshelf", 3, "carpet", 1),
                true,
                true,
                false,
                EnvironmentTemperatureBand.PLEASANT,
                Set.of(EnvironmentAirHazard.SMOKY),
                Map.of(EntityType.CAT, 2)
        );

        assertThat(snapshot.countSignal("bookshelf")).isEqualTo(3);
        assertThat(snapshot.hasSignal("carpet")).isTrue();
        assertThat(snapshot.hasSignalAtLeast("bookshelf", 2)).isTrue();
        assertThat(snapshot.hasSignalAtLeast("bookshelf", 4)).isFalse();
        assertThat(snapshot.isCovered()).isTrue();
        assertThat(snapshot.isIndoorsLike()).isTrue();
        assertThat(snapshot.isEnclosed()).isFalse();
        assertThat(snapshot.temperatureBand()).isEqualTo(EnvironmentTemperatureBand.PLEASANT);
        assertThat(snapshot.hasAirHazard(EnvironmentAirHazard.SMOKY)).isTrue();
        assertThat(snapshot.nearEntity(EntityType.CAT)).isTrue();
        assertThat(snapshot.nearEntityAtLeast(EntityType.CAT, 2)).isTrue();
        assertThat(snapshot.nearEntityAtLeast(EntityType.FROG, 1)).isFalse();
    }

    @Test
    void clampsNegativeCountsAndRejectsInvalidArguments() {
        EnvironmentSnapshot snapshot = new EnvironmentSnapshot(
                Map.of("dirt_like", -4),
                false,
                false,
                false,
                EnvironmentTemperatureBand.COOL,
                Set.of(),
                Map.of(EntityType.FROG, -2)
        );

        assertThat(snapshot.countSignal("dirt_like")).isZero();
        assertThat(snapshot.nearEntityAtLeast(EntityType.FROG, 1)).isFalse();
        assertThatThrownBy(() -> snapshot.countSignal(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.hasSignalAtLeast("carpet", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> snapshot.nearEntityAtLeast(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests EnvironmentSnapshotTest`

Expected: FAIL with compiler errors for missing `EnvironmentSnapshot`, `EnvironmentTemperatureBand`, and `EnvironmentAirHazard`.

- [ ] **Step 3: Implement snapshot value types**

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentAirHazard.java`:

```java
package com.sanhiruzu.atelier.api;

public enum EnvironmentAirHazard {
    SMOKY,
    LAVA_HEAT,
    FIRE_PARTICLES,
    DAMP,
    DUSTY
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentTemperatureBand.java`:

```java
package com.sanhiruzu.atelier.api;

public enum EnvironmentTemperatureBand {
    COLD,
    COOL,
    PLEASANT,
    WARM,
    HOT
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentSnapshot.java`:

```java
package com.sanhiruzu.atelier.api;

import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Set;

public record EnvironmentSnapshot(
        Map<String, Integer> signalCounts,
        boolean isCovered,
        boolean isIndoorsLike,
        boolean isEnclosed,
        EnvironmentTemperatureBand temperatureBand,
        Set<EnvironmentAirHazard> airHazards,
        Map<EntityType<?>, Integer> nearbyEntityCounts
) {
    public static final EnvironmentSnapshot AMBIENT = new EnvironmentSnapshot(
            Map.of(),
            false,
            false,
            false,
            EnvironmentTemperatureBand.PLEASANT,
            Set.of(),
            Map.of()
    );

    public EnvironmentSnapshot {
        signalCounts = sanitizeSignalCounts(signalCounts);
        airHazards = Set.copyOf(airHazards == null ? Set.of() : airHazards);
        nearbyEntityCounts = sanitizeEntityCounts(nearbyEntityCounts);
        if (temperatureBand == null) {
            temperatureBand = EnvironmentTemperatureBand.PLEASANT;
        }
    }

    public int countSignal(String signal) {
        requireSignal(signal);
        return signalCounts.getOrDefault(signal, 0);
    }

    public boolean hasSignal(String signal) {
        return countSignal(signal) > 0;
    }

    public boolean hasSignalAtLeast(String signal, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        return countSignal(signal) >= count;
    }

    public boolean hasAirHazard(EnvironmentAirHazard hazard) {
        if (hazard == null) {
            throw new IllegalArgumentException("hazard must not be null");
        }
        return airHazards.contains(hazard);
    }

    public boolean nearEntity(EntityType<?> type) {
        return nearEntityAtLeast(type, 1);
    }

    public boolean nearEntityAtLeast(EntityType<?> type, int count) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        return nearbyEntityCounts.getOrDefault(type, 0) >= count;
    }

    private static Map<String, Integer> sanitizeSignalCounts(Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Integer> sanitized = new java.util.LinkedHashMap<>();
        source.forEach((signal, count) -> {
            requireSignal(signal);
            sanitized.put(signal, Math.max(0, count == null ? 0 : count));
        });
        return Map.copyOf(sanitized);
    }

    private static Map<EntityType<?>, Integer> sanitizeEntityCounts(Map<EntityType<?>, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<EntityType<?>, Integer> sanitized = new java.util.LinkedHashMap<>();
        source.forEach((type, count) -> {
            if (type == null) {
                throw new IllegalArgumentException("entity type must not be null");
            }
            sanitized.put(type, Math.max(0, count == null ? 0 : count));
        });
        return Map.copyOf(sanitized);
    }

    private static void requireSignal(String signal) {
        if (signal == null || signal.isBlank()) {
            throw new IllegalArgumentException("signal must not be blank");
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests EnvironmentSnapshotTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/api/EnvironmentAirHazard.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentTemperatureBand.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentSnapshot.java src/test/java/com/sanhiruzu/atelier/api/EnvironmentSnapshotTest.java
git commit -m "feat: add environment snapshot api"
```

## Task 2: Declarative Conditions and Effects

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectType.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffect.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentCondition.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentConditions.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffects.java`
- Test: `src/test/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistryTest.java`

- [ ] **Step 1: Write failing condition/effect tests**

Create `src/test/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistryTest.java` with these first tests:

```java
package com.sanhiruzu.atelier.api;

import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentEffectRegistryTest {
    private static final EnvironmentSnapshot SNAPSHOT = new EnvironmentSnapshot(
            Map.of("bookshelf", 3, "carpet", 1, "industrial_blocks", 0),
            true,
            true,
            true,
            EnvironmentTemperatureBand.PLEASANT,
            Set.of(),
            Map.of(EntityType.CAT, 1)
    );

    @Test
    void builtInConditionsMatchSnapshotFacts() {
        EnvironmentCondition condition = EnvironmentConditions.allOf(
                EnvironmentConditions.covered(),
                EnvironmentConditions.indoorsLike(),
                EnvironmentConditions.enclosed(),
                EnvironmentConditions.signalAtLeast("bookshelf", 2),
                EnvironmentConditions.signalAbsent("industrial_blocks"),
                EnvironmentConditions.temperature(EnvironmentTemperatureBand.PLEASANT),
                EnvironmentConditions.nearEntityAtLeast(EntityType.CAT, 1)
        );

        assertThat(condition.matches(SNAPSHOT)).isTrue();
        assertThat(EnvironmentConditions.signalAtLeast("bookshelf", 4).matches(SNAPSHOT)).isFalse();
    }

    @Test
    void effectFactoriesCreateTypedOutputs() {
        EnvironmentEffect speed = EnvironmentEffects.modifySpeed("metallurgy", 0.15f);
        EnvironmentEffect veto = EnvironmentEffects.veto("too_damp");

        assertThat(speed.type()).isEqualTo(EnvironmentEffectType.SPEED_MODIFIER);
        assertThat(speed.id()).isEqualTo("metallurgy");
        assertThat(speed.amount()).isEqualTo(0.15f);
        assertThat(veto.type()).isEqualTo(EnvironmentEffectType.VETO);
        assertThat(veto.id()).isEqualTo("too_damp");
        assertThat(veto.amount()).isZero();
    }

    @Test
    void rejectsBlankEffectIdsAndNegativeSignalThresholds() {
        assertThatThrownBy(() -> EnvironmentEffects.modifyYield("", 0.1f))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAtLeast("bookshelf", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.temperature(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests EnvironmentEffectRegistryTest`

Expected: FAIL with compiler errors for missing condition/effect classes.

- [ ] **Step 3: Implement condition and effect classes**

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectType.java`:

```java
package com.sanhiruzu.atelier.api;

public enum EnvironmentEffectType {
    SPEED_MODIFIER,
    YIELD_MODIFIER,
    RISK_MODIFIER,
    STABILITY_MODIFIER,
    COMFORT_MODIFIER,
    MOB_EFFECT_REQUEST,
    VETO,
    LABEL,
    CUSTOM
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffect.java`:

```java
package com.sanhiruzu.atelier.api;

public record EnvironmentEffect(EnvironmentEffectType type, String id, float amount) {
    public EnvironmentEffect {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentCondition.java`:

```java
package com.sanhiruzu.atelier.api;

@FunctionalInterface
public interface EnvironmentCondition {
    boolean matches(EnvironmentSnapshot snapshot);
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentConditions.java`:

```java
package com.sanhiruzu.atelier.api;

import net.minecraft.world.entity.EntityType;

import java.util.Arrays;
import java.util.Objects;

public final class EnvironmentConditions {
    private EnvironmentConditions() {
    }

    public static EnvironmentCondition always() {
        return snapshot -> true;
    }

    public static EnvironmentCondition allOf(EnvironmentCondition... conditions) {
        EnvironmentCondition[] copy = copyConditions(conditions);
        return snapshot -> Arrays.stream(copy).allMatch(condition -> condition.matches(snapshot));
    }

    public static EnvironmentCondition anyOf(EnvironmentCondition... conditions) {
        EnvironmentCondition[] copy = copyConditions(conditions);
        return snapshot -> Arrays.stream(copy).anyMatch(condition -> condition.matches(snapshot));
    }

    public static EnvironmentCondition not(EnvironmentCondition condition) {
        Objects.requireNonNull(condition, "condition");
        return snapshot -> !condition.matches(snapshot);
    }

    public static EnvironmentCondition covered() {
        return EnvironmentSnapshot::isCovered;
    }

    public static EnvironmentCondition indoorsLike() {
        return EnvironmentSnapshot::isIndoorsLike;
    }

    public static EnvironmentCondition enclosed() {
        return EnvironmentSnapshot::isEnclosed;
    }

    public static EnvironmentCondition signalAtLeast(String signal, int count) {
        requireCount(count);
        return snapshot -> snapshot.hasSignalAtLeast(signal, count);
    }

    public static EnvironmentCondition signalAbsent(String signal) {
        return snapshot -> !snapshot.hasSignal(signal);
    }

    public static EnvironmentCondition temperature(EnvironmentTemperatureBand band) {
        Objects.requireNonNull(band, "band");
        return snapshot -> snapshot.temperatureBand() == band;
    }

    public static EnvironmentCondition airHazard(EnvironmentAirHazard hazard) {
        Objects.requireNonNull(hazard, "hazard");
        return snapshot -> snapshot.hasAirHazard(hazard);
    }

    public static EnvironmentCondition nearEntityAtLeast(EntityType<?> type, int count) {
        requireCount(count);
        return snapshot -> snapshot.nearEntityAtLeast(type, count);
    }

    private static EnvironmentCondition[] copyConditions(EnvironmentCondition[] conditions) {
        if (conditions == null || conditions.length == 0) {
            return new EnvironmentCondition[] { always() };
        }
        EnvironmentCondition[] copy = Arrays.copyOf(conditions, conditions.length);
        for (EnvironmentCondition condition : copy) {
            Objects.requireNonNull(condition, "condition");
        }
        return copy;
    }

    private static void requireCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
    }
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffects.java`:

```java
package com.sanhiruzu.atelier.api;

public final class EnvironmentEffects {
    private EnvironmentEffects() {
    }

    public static EnvironmentEffect modifySpeed(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.SPEED_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyYield(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.YIELD_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyRisk(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.RISK_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyStability(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.STABILITY_MODIFIER, id, amount);
    }

    public static EnvironmentEffect modifyComfort(String id, float amount) {
        return new EnvironmentEffect(EnvironmentEffectType.COMFORT_MODIFIER, id, amount);
    }

    public static EnvironmentEffect veto(String id) {
        return new EnvironmentEffect(EnvironmentEffectType.VETO, id, 0f);
    }

    public static EnvironmentEffect label(String id) {
        return new EnvironmentEffect(EnvironmentEffectType.LABEL, id, 0f);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests EnvironmentEffectRegistryTest --tests EnvironmentSnapshotTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectType.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffect.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentCondition.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentConditions.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffects.java src/test/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistryTest.java
git commit -m "feat: add environment conditions and effects"
```

## Task 3: Effect Registry and Fluent Registration

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectContext.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRule.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistrar.java`
- Create: `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistry.java`
- Modify: `src/test/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistryTest.java`

- [ ] **Step 1: Extend the registry test**

Append these tests to `EnvironmentEffectRegistryTest`:

```java
    @Test
    void registryEvaluatesMatchingActionRules() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(registrar -> registrar
                .forAction("zen_atelier:smelt_ingot")
                .when(EnvironmentConditions.signalAtLeast("bookshelf", 2))
                .then(EnvironmentEffects.modifySpeed("library_heat", 0.10f)));
        registry.register(registrar -> registrar
                .forAction("zen_atelier:smelt_ingot")
                .when(EnvironmentConditions.signalAtLeast("bookshelf", 4))
                .then(EnvironmentEffects.modifyYield("too_many_books", -0.20f)));

        EnvironmentEffectContext context = EnvironmentEffectContext.action("zen_atelier:smelt_ingot");
        List<EnvironmentEffect> effects = registry.evaluate(context, SNAPSHOT);

        assertThat(effects).containsExactly(EnvironmentEffects.modifySpeed("library_heat", 0.10f));
    }

    @Test
    void callbackRulesCanReturnCustomEffects() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(registrar -> registrar
                .forAction("zen_atelier:sleep")
                .when(EnvironmentConditions.covered())
                .then((context, snapshot) -> List.of(EnvironmentEffects.modifyComfort("covered_bed", 0.25f))));

        List<EnvironmentEffect> effects = registry.evaluate(
                EnvironmentEffectContext.action("zen_atelier:sleep"),
                SNAPSHOT
        );

        assertThat(effects).containsExactly(EnvironmentEffects.modifyComfort("covered_bed", 0.25f));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests EnvironmentEffectRegistryTest`

Expected: FAIL with compiler errors for registry and context classes.

- [ ] **Step 3: Implement registry classes**

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectContext.java`:

```java
package com.sanhiruzu.atelier.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record EnvironmentEffectContext(String kind, String id, BlockState blockState) {
    public static EnvironmentEffectContext action(String id) {
        return new EnvironmentEffectContext("action", id, null);
    }

    public static EnvironmentEffectContext block(BlockState state) {
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return new EnvironmentEffectContext("block", blockId.toString(), state);
    }

    public EnvironmentEffectContext {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind must not be blank");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    public boolean matchesAction(String actionId) {
        return "action".equals(kind) && id.equals(actionId);
    }

    public boolean matchesBlock(Block block) {
        return "block".equals(kind) && blockState != null && blockState.is(block);
    }
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRule.java`:

```java
package com.sanhiruzu.atelier.api;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public record EnvironmentEffectRule(
        Predicate<EnvironmentEffectContext> target,
        EnvironmentCondition condition,
        BiFunction<EnvironmentEffectContext, EnvironmentSnapshot, List<EnvironmentEffect>> effectFactory
) {
    public EnvironmentEffectRule {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(effectFactory, "effectFactory");
    }

    public List<EnvironmentEffect> evaluate(EnvironmentEffectContext context, EnvironmentSnapshot snapshot) {
        if (!target.test(context) || !condition.matches(snapshot)) {
            return List.of();
        }
        List<EnvironmentEffect> effects = effectFactory.apply(context, snapshot);
        return effects == null ? List.of() : List.copyOf(effects);
    }
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistrar.java`:

```java
package com.sanhiruzu.atelier.api;

import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class EnvironmentEffectRegistrar {
    private final List<EnvironmentEffectRule> rules = new ArrayList<>();

    public RuleBuilder forAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw new IllegalArgumentException("actionId must not be blank");
        }
        return new RuleBuilder(context -> context.matchesAction(actionId));
    }

    public RuleBuilder forBlock(Block block) {
        Objects.requireNonNull(block, "block");
        return new RuleBuilder(context -> context.matchesBlock(block));
    }

    List<EnvironmentEffectRule> rules() {
        return List.copyOf(rules);
    }

    public final class RuleBuilder {
        private final Predicate<EnvironmentEffectContext> target;
        private EnvironmentCondition condition = EnvironmentConditions.always();

        private RuleBuilder(Predicate<EnvironmentEffectContext> target) {
            this.target = target;
        }

        public RuleBuilder when(EnvironmentCondition condition) {
            this.condition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public EnvironmentEffectRegistrar then(EnvironmentEffect effect) {
            Objects.requireNonNull(effect, "effect");
            rules.add(new EnvironmentEffectRule(target, condition, (context, snapshot) -> List.of(effect)));
            return EnvironmentEffectRegistrar.this;
        }

        public EnvironmentEffectRegistrar then(BiFunction<EnvironmentEffectContext, EnvironmentSnapshot, List<EnvironmentEffect>> factory) {
            rules.add(new EnvironmentEffectRule(target, condition, factory));
            return EnvironmentEffectRegistrar.this;
        }
    }
}
```

Create `src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistry.java`:

```java
package com.sanhiruzu.atelier.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class EnvironmentEffectRegistry {
    private static final EnvironmentEffectRegistry GLOBAL = new EnvironmentEffectRegistry();

    private final List<EnvironmentEffectRule> rules = new ArrayList<>();

    public static EnvironmentEffectRegistry global() {
        return GLOBAL;
    }

    public synchronized void register(Consumer<EnvironmentEffectRegistrar> registration) {
        Objects.requireNonNull(registration, "registration");
        EnvironmentEffectRegistrar registrar = new EnvironmentEffectRegistrar();
        registration.accept(registrar);
        rules.addAll(registrar.rules());
    }

    public synchronized List<EnvironmentEffect> evaluate(EnvironmentEffectContext context, EnvironmentSnapshot snapshot) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(snapshot, "snapshot");
        List<EnvironmentEffect> effects = new ArrayList<>();
        for (EnvironmentEffectRule rule : rules) {
            effects.addAll(rule.evaluate(context, snapshot));
        }
        return List.copyOf(effects);
    }

    public synchronized void clear() {
        rules.clear();
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests EnvironmentEffectRegistryTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectContext.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRule.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistrar.java src/main/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistry.java src/test/java/com/sanhiruzu/atelier/api/EnvironmentEffectRegistryTest.java
git commit -m "feat: add environment effect registry"
```

## Task 4: Snapshot Cache and Dirty Regions

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanProfile.java`
- Create: `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCache.java`
- Create: `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentCacheInvalidation.java`
- Test: `src/test/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCacheTest.java`

- [ ] **Step 1: Write failing cache tests**

Create `src/test/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCacheTest.java`:

```java
package com.sanhiruzu.atelier.environment;

import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentSnapshotCacheTest {
    @Test
    void reusesSnapshotUntilNearbyDirtyMark() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelKey = new Object();
        AtomicInteger scans = new AtomicInteger();

        EnvironmentSnapshot first = cache.getOrCompute(levelKey, 10, 64, 10, 6, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        EnvironmentSnapshot second = cache.getOrCompute(levelKey, 10, 64, 10, 6, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(second).isSameAs(first);
        assertThat(scans).hasValue(1);

        cache.markDirty(levelKey, 12, 64, 10, 3);
        cache.getOrCompute(levelKey, 10, 64, 10, 6, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(scans).hasValue(2);
    }

    @Test
    void separatesProfilesAndLevels() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelA = new Object();
        Object levelB = new Object();
        AtomicInteger scans = new AtomicInteger();

        cache.getOrCompute(levelA, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelA, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_AND_ENTITIES, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 0, 0, 0, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(scans).hasValue(3);
    }

    @Test
    void clearsOneLevelWithoutClearingOthers() {
        EnvironmentSnapshotCache cache = new EnvironmentSnapshotCache();
        Object levelA = new Object();
        Object levelB = new Object();
        AtomicInteger scans = new AtomicInteger();

        cache.getOrCompute(levelA, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        cache.clearLevel(levelA);
        cache.getOrCompute(levelA, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });
        cache.getOrCompute(levelB, 1, 2, 3, 4, EnvironmentScanProfile.BLOCKS_ONLY, () -> {
            scans.incrementAndGet();
            return EnvironmentSnapshot.AMBIENT;
        });

        assertThat(scans).hasValue(3);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests EnvironmentSnapshotCacheTest`

Expected: FAIL with compiler errors for missing cache classes.

- [ ] **Step 3: Implement cache**

Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanProfile.java`:

```java
package com.sanhiruzu.atelier.environment;

public enum EnvironmentScanProfile {
    BLOCKS_ONLY,
    BLOCKS_AND_ENTITIES
}
```

Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCache.java`:

```java
package com.sanhiruzu.atelier.environment;

import com.sanhiruzu.atelier.api.EnvironmentSnapshot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class EnvironmentSnapshotCache {
    private final Map<Key, Entry> entries = new HashMap<>();

    public synchronized EnvironmentSnapshot getOrCompute(
            Object levelKey,
            int x,
            int y,
            int z,
            int radius,
            EnvironmentScanProfile profile,
            Supplier<EnvironmentSnapshot> scanner
    ) {
        Key key = new Key(levelKey, x, y, z, radius, profile);
        Entry entry = entries.get(key);
        if (entry != null && !entry.dirty) {
            return entry.snapshot;
        }
        EnvironmentSnapshot snapshot = Objects.requireNonNull(scanner.get(), "scanner returned null");
        entries.put(key, new Entry(snapshot, false));
        return snapshot;
    }

    public synchronized void markDirty(Object levelKey, int x, int y, int z, int dirtyRadius) {
        for (Map.Entry<Key, Entry> cacheEntry : entries.entrySet()) {
            Key key = cacheEntry.getKey();
            if (key.levelKey != levelKey) {
                continue;
            }
            int reach = key.radius + Math.max(0, dirtyRadius);
            if (Math.abs(key.x - x) <= reach && Math.abs(key.y - y) <= reach && Math.abs(key.z - z) <= reach) {
                cacheEntry.getValue().dirty = true;
            }
        }
    }

    public synchronized void clearLevel(Object levelKey) {
        Iterator<Key> iterator = entries.keySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().levelKey == levelKey) {
                iterator.remove();
            }
        }
    }

    public synchronized void clearAll() {
        entries.clear();
    }

    private record Key(Object levelKey, int x, int y, int z, int radius, EnvironmentScanProfile profile) {
        private Key {
            Objects.requireNonNull(levelKey, "levelKey");
            Objects.requireNonNull(profile, "profile");
            if (radius < 0) {
                throw new IllegalArgumentException("radius must be non-negative");
            }
        }
    }

    private static final class Entry {
        private final EnvironmentSnapshot snapshot;
        private boolean dirty;

        private Entry(EnvironmentSnapshot snapshot, boolean dirty) {
            this.snapshot = snapshot;
            this.dirty = dirty;
        }
    }
}
```

Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentCacheInvalidation.java`:

```java
package com.sanhiruzu.atelier.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class EnvironmentCacheInvalidation {
    public static final int DEFAULT_DIRTY_RADIUS = 16;

    private EnvironmentCacheInvalidation() {
    }

    public static Object levelKey(Level level) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        return level;
    }

    public static void markDirty(EnvironmentSnapshotCache cache, Level level, BlockPos pos) {
        if (cache == null || level == null || pos == null) {
            return;
        }
        cache.markDirty(levelKey(level), pos.getX(), pos.getY(), pos.getZ(), DEFAULT_DIRTY_RADIUS);
    }

    public static void clearLevel(EnvironmentSnapshotCache cache, Level level) {
        if (cache != null && level != null) {
            cache.clearLevel(levelKey(level));
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests EnvironmentSnapshotCacheTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanProfile.java src/main/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCache.java src/main/java/com/sanhiruzu/atelier/environment/EnvironmentCacheInvalidation.java src/test/java/com/sanhiruzu/atelier/environment/EnvironmentSnapshotCacheTest.java
git commit -m "feat: add environment snapshot cache"
```

## Task 5: Environment Signals

**Files:**
- Modify: `src/main/java/com/sanhiruzu/atelier/data/Signals.java`
- Test: `src/test/java/com/sanhiruzu/atelier/resources/EnvironmentSignalRegistryTest.java`

- [ ] **Step 1: Write failing signal registry test**

Create `src/test/java/com/sanhiruzu/atelier/resources/EnvironmentSignalRegistryTest.java`:

```java
package com.sanhiruzu.atelier.resources;

import com.sanhiruzu.atelier.data.Signals;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentSignalRegistryTest {
    @Test
    void environmentSignalsAreRegistered() {
        assertThat(Signals.predicates().keySet()).containsAll(List.of(
                "dirt_like",
                "mossy",
                "rough_stone",
                "metallurgy",
                "kitchen",
                "lava_heat",
                "smoke_source",
                "damp_source",
                "dust_source"
        ));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests EnvironmentSignalRegistryTest`

Expected: FAIL because the listed signal names are not all registered.

- [ ] **Step 3: Add environment-oriented signals**

Modify the static initializer in `src/main/java/com/sanhiruzu/atelier/data/Signals.java` after the existing atmosphere signals:

```java
        PREDICATES.put("dirt_like", s ->
                s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT)
                        || s.is(Blocks.MUD) || s.is(Blocks.CLAY));
        PREDICATES.put("mossy", s ->
                s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET)
                        || s.is(Blocks.MOSSY_COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE_SLAB)
                        || s.is(Blocks.MOSSY_COBBLESTONE_STAIRS) || s.is(Blocks.MOSSY_COBBLESTONE_WALL)
                        || s.is(Blocks.MOSSY_STONE_BRICKS) || s.is(Blocks.MOSSY_STONE_BRICK_SLAB)
                        || s.is(Blocks.MOSSY_STONE_BRICK_STAIRS) || s.is(Blocks.MOSSY_STONE_BRICK_WALL));
        PREDICATES.put("rough_stone", s ->
                s.is(Tags.Blocks.COBBLESTONES) || s.is(Blocks.COBBLESTONE)
                        || s.is(Blocks.COBBLED_DEEPSLATE) || s.is(Blocks.BLACKSTONE));
        PREDICATES.put("metallurgy", s ->
                Signals.matches("smithing_or_repair_block", s)
                        || Signals.matches("stone_or_metal_materials", s));
        PREDICATES.put("kitchen", s ->
                Signals.matches("cooking_block", s)
                        || s.is(Blocks.CAKE) || s.is(Blocks.COMPOSTER));
        PREDICATES.put("lava_heat", s ->
                s.is(Blocks.LAVA_CAULDRON) || s.getFluidState().is(FluidTags.LAVA));
        PREDICATES.put("smoke_source", s ->
                s.is(CAMPFIRES) || s.is(Blocks.CAMPFIRE) || s.is(Blocks.SOUL_CAMPFIRE));
        PREDICATES.put("damp_source", s ->
                s.getFluidState().is(FluidTags.WATER) || s.is(Blocks.WET_SPONGE) || s.is(Blocks.MUD));
        PREDICATES.put("dust_source", s ->
                s.is(Blocks.SAND) || s.is(Blocks.RED_SAND) || s.is(Blocks.GRAVEL)
                        || s.is(Blocks.SUSPICIOUS_SAND) || s.is(Blocks.SUSPICIOUS_GRAVEL));
```

- [ ] **Step 4: Run signal test**

Run: `./gradlew test --tests EnvironmentSignalRegistryTest`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/data/Signals.java src/test/java/com/sanhiruzu/atelier/resources/EnvironmentSignalRegistryTest.java
git commit -m "feat: add environment signal predicates"
```

## Task 6: World Scanner and ZoneAPI Facade

**Files:**
- Create: `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanner.java`
- Modify: `src/main/java/com/sanhiruzu/atelier/api/ZoneAPI.java`
- Modify: `src/test/java/com/sanhiruzu/atelier/api/EnvironmentSnapshotTest.java`

- [ ] **Step 1: Add facade validation tests**

Append this test to `EnvironmentSnapshotTest`:

```java
    @Test
    void ambientSnapshotRemainsNeutral() {
        assertThat(EnvironmentSnapshot.AMBIENT.temperatureBand()).isEqualTo(EnvironmentTemperatureBand.PLEASANT);
        assertThat(EnvironmentSnapshot.AMBIENT.isCovered()).isFalse();
        assertThat(EnvironmentSnapshot.AMBIENT.signalCounts()).isEmpty();
        assertThat(EnvironmentSnapshot.AMBIENT.airHazards()).isEmpty();
    }
```

- [ ] **Step 2: Run existing tests before scanner work**

Run: `./gradlew test --tests EnvironmentSnapshotTest --tests EnvironmentEffectRegistryTest --tests EnvironmentSnapshotCacheTest --tests EnvironmentSignalRegistryTest`

Expected: PASS.

- [ ] **Step 3: Implement scanner**

Create `src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanner.java`:

```java
package com.sanhiruzu.atelier.environment;

import com.sanhiruzu.atelier.api.EnvironmentAirHazard;
import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import com.sanhiruzu.atelier.api.EnvironmentTemperatureBand;
import com.sanhiruzu.atelier.data.Signals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class EnvironmentScanner {
    private static final int MAX_RADIUS = 16;

    private EnvironmentScanner() {
    }

    public static EnvironmentSnapshot scan(Level level, BlockPos center, int radius, EnvironmentScanProfile profile) {
        if (level == null) {
            throw new IllegalArgumentException("level must not be null");
        }
        if (center == null) {
            throw new IllegalArgumentException("center must not be null");
        }
        int clampedRadius = Math.clamp(radius, 0, MAX_RADIUS);
        Map<String, Integer> counts = countSignals(level, center, clampedRadius);
        boolean covered = isCovered(level, center, clampedRadius);
        boolean indoorsLike = covered && boundaryMass(counts) >= Math.max(8, clampedRadius * 3);
        boolean enclosed = indoorsLike && hasSideMass(level, center, clampedRadius);
        EnvironmentTemperatureBand band = temperatureBand(counts);
        Set<EnvironmentAirHazard> hazards = hazards(counts);
        Map<EntityType<?>, Integer> entities = profile == EnvironmentScanProfile.BLOCKS_AND_ENTITIES
                ? countEntities(level, center, clampedRadius)
                : Map.of();
        return new EnvironmentSnapshot(counts, covered, indoorsLike, enclosed, band, hazards, entities);
    }

    private static Map<String, Integer> countSignals(Level level, BlockPos center, int radius) {
        Map<String, Integer> counts = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            BlockState state = level.getBlockState(pos);
            Signals.predicates().forEach((signal, predicate) -> {
                if (predicate.test(state)) {
                    counts.merge(signal, 1, Integer::sum);
                }
            });
        }
        return counts;
    }

    private static boolean isCovered(Level level, BlockPos center, int radius) {
        int height = Math.max(2, Math.min(8, radius + 2));
        BlockPos.MutableBlockPos cursor = center.mutable();
        for (int y = 1; y <= height; y++) {
            cursor.set(center.getX(), center.getY() + y, center.getZ());
            if (level.getBlockState(cursor).isSolidRender(level, cursor)) {
                return true;
            }
        }
        return false;
    }

    private static int boundaryMass(Map<String, Integer> counts) {
        return counts.getOrDefault("wood_materials", 0)
                + counts.getOrDefault("stone_or_metal_materials", 0)
                + counts.getOrDefault("rough_stone", 0)
                + counts.getOrDefault("urban_block", 0)
                + counts.getOrDefault("glass", 0);
    }

    private static boolean hasSideMass(Level level, BlockPos center, int radius) {
        if (radius <= 0) {
            return false;
        }
        return sideHasSolid(level, center, radius, 1, 0)
                && sideHasSolid(level, center, radius, -1, 0)
                && sideHasSolid(level, center, radius, 0, 1)
                && sideHasSolid(level, center, radius, 0, -1);
    }

    private static boolean sideHasSolid(Level level, BlockPos center, int radius, int dx, int dz) {
        BlockPos.MutableBlockPos cursor = center.mutable();
        int sideX = center.getX() + dx * radius;
        int sideZ = center.getZ() + dz * radius;
        for (int y = -1; y <= 2; y++) {
            for (int offset = -radius; offset <= radius; offset++) {
                int x = dx == 0 ? center.getX() + offset : sideX;
                int z = dz == 0 ? center.getZ() + offset : sideZ;
                cursor.set(x, center.getY() + y, z);
                if (level.getBlockState(cursor).isSolidRender(level, cursor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static EnvironmentTemperatureBand temperatureBand(Map<String, Integer> counts) {
        int heat = counts.getOrDefault("heat_source", 0) + counts.getOrDefault("lava_heat", 0) * 3;
        int cold = counts.getOrDefault("ice_block", 0);
        int score = heat - cold;
        if (score <= -6) return EnvironmentTemperatureBand.COLD;
        if (score <= -2) return EnvironmentTemperatureBand.COOL;
        if (score >= 8) return EnvironmentTemperatureBand.HOT;
        if (score >= 3) return EnvironmentTemperatureBand.WARM;
        return EnvironmentTemperatureBand.PLEASANT;
    }

    private static Set<EnvironmentAirHazard> hazards(Map<String, Integer> counts) {
        EnumSet<EnvironmentAirHazard> hazards = EnumSet.noneOf(EnvironmentAirHazard.class);
        if (counts.getOrDefault("smoke_source", 0) > 0) hazards.add(EnvironmentAirHazard.SMOKY);
        if (counts.getOrDefault("lava_heat", 0) > 0) hazards.add(EnvironmentAirHazard.LAVA_HEAT);
        if (counts.getOrDefault("heat_source", 0) > 0) hazards.add(EnvironmentAirHazard.FIRE_PARTICLES);
        if (counts.getOrDefault("damp_source", 0) > 0) hazards.add(EnvironmentAirHazard.DAMP);
        if (counts.getOrDefault("dust_source", 0) > 0) hazards.add(EnvironmentAirHazard.DUSTY);
        return hazards;
    }

    private static Map<EntityType<?>, Integer> countEntities(Level level, BlockPos center, int radius) {
        Map<EntityType<?>, Integer> counts = new HashMap<>();
        AABB bounds = new AABB(center).inflate(radius);
        for (Entity entity : level.getEntities(null, bounds)) {
            counts.merge(entity.getType(), 1, Integer::sum);
        }
        return counts;
    }
}
```

- [ ] **Step 4: Modify `ZoneAPI`**

Replace `ZoneAPI` with this implementation:

```java
package com.sanhiruzu.atelier.api;

import com.sanhiruzu.atelier.environment.EnvironmentCacheInvalidation;
import com.sanhiruzu.atelier.environment.EnvironmentScanProfile;
import com.sanhiruzu.atelier.environment.EnvironmentScanner;
import com.sanhiruzu.atelier.environment.EnvironmentSnapshotCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Point-of-use query facade for local environment facts and declarative effects.
 * There are no room/zone objects. Callers sample a location when an event fires.
 */
public final class ZoneAPI {
    private static final EnvironmentSnapshotCache SNAPSHOT_CACHE = new EnvironmentSnapshotCache();

    private ZoneAPI() {
    }

    public static EnvironmentSnapshot environmentAt(Level level, BlockPos pos, int radius) {
        EnvironmentScanProfile profile = EnvironmentScanProfile.BLOCKS_AND_ENTITIES;
        return SNAPSHOT_CACHE.getOrCompute(
                EnvironmentCacheInvalidation.levelKey(level),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                Math.max(0, radius),
                profile,
                () -> EnvironmentScanner.scan(level, pos, radius, profile)
        );
    }

    public static List<EnvironmentEffect> evaluateEnvironmentEffects(
            EnvironmentEffectContext context,
            Level level,
            BlockPos pos,
            int radius
    ) {
        return EnvironmentEffectRegistry.global().evaluate(context, environmentAt(level, pos, radius));
    }

    public static void registerEnvironmentEffects(Consumer<EnvironmentEffectRegistrar> registration) {
        EnvironmentEffectRegistry.global().register(registration);
    }

    public static void markEnvironmentDirty(Level level, BlockPos pos) {
        EnvironmentCacheInvalidation.markDirty(SNAPSHOT_CACHE, level, pos);
    }

    public static void clearEnvironmentCache(Level level) {
        EnvironmentCacheInvalidation.clearLevel(SNAPSHOT_CACHE, level);
    }

    /** True if any block within {@code radius} of {@code pos} matches {@code predicate}. Bounded, server-or-client safe. */
    public static boolean hasNearby(Level level, BlockPos pos, Predicate<BlockState> predicate, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(
                pos.offset(-radius, -radius, -radius),
                pos.offset(radius, radius, radius))) {
            if (predicate.test(level.getBlockState(p))) {
                return true;
            }
        }
        return false;
    }

    /** Neutral/ambient atmosphere reading retained for compatibility with existing callers. */
    public static AtmosphereReading atmosphereAt(Level level, BlockPos pos) {
        return AtmosphereReading.AMBIENT;
    }

    /** Anonymous, identity-free local reading. Scalars are neutral until the new environment facts replace callers. */
    public record AtmosphereReading(float heat, float humidity, float particulate) {
        public static final AtmosphereReading AMBIENT = new AtmosphereReading(0f, 0f, 0f);
    }
}
```

- [ ] **Step 5: Run compile and focused tests**

Run: `./gradlew test --tests EnvironmentSnapshotTest --tests EnvironmentEffectRegistryTest --tests EnvironmentSnapshotCacheTest --tests EnvironmentSignalRegistryTest`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/environment/EnvironmentScanner.java src/main/java/com/sanhiruzu/atelier/api/ZoneAPI.java src/test/java/com/sanhiruzu/atelier/api/EnvironmentSnapshotTest.java
git commit -m "feat: expose environment snapshots through zone api"
```

## Task 7: Dirty Invalidation Event Wiring

**Files:**
- Modify: `src/main/java/com/sanhiruzu/atelier/event/AtelierEvents.java`

- [ ] **Step 1: Add event handlers**

Modify `src/main/java/com/sanhiruzu/atelier/event/AtelierEvents.java`:

```java
import com.sanhiruzu.atelier.api.ZoneAPI;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
```

Add these methods before `onServerTick`:

```java
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ZoneAPI.markEnvironmentDirty(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ZoneAPI.markEnvironmentDirty(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ZoneAPI.markEnvironmentDirty(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ZoneAPI.clearEnvironmentCache(level);
        }
    }
```

- [ ] **Step 2: Run compile**

Run: `./gradlew compileJava`

Expected: PASS.

- [ ] **Step 3: Run focused tests**

Run: `./gradlew test --tests EnvironmentSnapshotCacheTest --tests EnvironmentSnapshotTest`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/sanhiruzu/atelier/event/AtelierEvents.java
git commit -m "feat: invalidate environment cache on world changes"
```

## Task 8: Full Verification

**Files:**
- No planned source edits.

- [ ] **Step 1: Run unit tests**

Run: `./gradlew test`

Expected: PASS.

- [ ] **Step 2: Inspect outgoing commit messages**

Run: `git log --format=%B origin/main..HEAD`

Expected: no `Co-Authored-By`, `generated by`, `Codex`, `Claude`, `OpenAI`, `ChatGPT`, `Anthropic`, or other AI-assistant attribution lines.

- [ ] **Step 3: Inspect final working tree**

Run: `git status --short`

Expected: only pre-existing unrelated dirty files remain, or no output if those were handled separately. The environment API files should be committed.

- [ ] **Step 4: Optional broader test**

Run: `./gradlew testQuick`

Expected: PASS. If this fails outside the environment API unit tests, inspect whether it is caused by pre-existing unrelated worktree changes before changing environment API code.
