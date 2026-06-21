package com.sanhiruzu.atelier.api;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.loading.LoadingModList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvironmentEffectRegistryTest {
    private static EnvironmentSnapshot SNAPSHOT;

    @BeforeAll
    static void prepareMinecraftEntityTypes() {
        if (LoadingModList.get() == null) {
            LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        SNAPSHOT = new EnvironmentSnapshot(
                Map.of("bookshelf", 3, "carpet", 1, "industrial_blocks", 0),
                true,
                true,
                true,
                EnvironmentTemperatureBand.PLEASANT,
                Set.of(),
                Map.of(EntityType.CAT, 1)
        );
    }

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

    @Test
    void rejectsNullConditionArrays() {
        assertThatThrownBy(() -> EnvironmentConditions.allOf((EnvironmentCondition[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.anyOf((EnvironmentCondition[]) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidConditionArgumentsAtFactoryTime() {
        assertThatThrownBy(() -> EnvironmentConditions.signalAtLeast(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAtLeast("", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAbsent(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.signalAbsent(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.nearEntityAtLeast(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.airHazard(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.allOf((EnvironmentCondition) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentConditions.anyOf((EnvironmentCondition) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

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

    @Test
    void registryEvaluatesMatchingBlockRules() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(registrar -> registrar
                .forBlock(Blocks.STONE)
                .then(EnvironmentEffects.label("stone_workspace")));

        List<EnvironmentEffect> stoneEffects = registry.evaluate(
                EnvironmentEffectContext.block(Blocks.STONE.defaultBlockState()),
                SNAPSHOT
        );
        List<EnvironmentEffect> dirtEffects = registry.evaluate(
                EnvironmentEffectContext.block(Blocks.DIRT.defaultBlockState()),
                SNAPSHOT
        );

        assertThat(stoneEffects).containsExactly(EnvironmentEffects.label("stone_workspace"));
        assertThat(dirtEffects).isEmpty();
    }

    @Test
    void registryEvaluatesMatchingRecipeCategoryRules() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(registrar -> registrar
                .forRecipeCategory("zen_atelier:food")
                .then(EnvironmentEffects.label("kitchen")));

        List<EnvironmentEffect> foodEffects = registry.evaluate(
                EnvironmentEffectContext.recipeCategory("zen_atelier:food"),
                SNAPSHOT
        );
        List<EnvironmentEffect> metalEffects = registry.evaluate(
                EnvironmentEffectContext.recipeCategory("zen_atelier:metal"),
                SNAPSHOT
        );

        assertThat(foodEffects).containsExactly(EnvironmentEffects.label("kitchen"));
        assertThat(metalEffects).isEmpty();
    }

    @Test
    void blockTagRulesAcceptVanillaTags() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(registrar -> registrar
                .forBlockTag(BlockTags.MINEABLE_WITH_PICKAXE)
                .then(EnvironmentEffects.label("pickaxe_work")));

        assertThat(registry.evaluate(EnvironmentEffectContext.action("zen_atelier:mine"), SNAPSHOT)).isEmpty();
    }

    @Test
    void registryEvaluatesCustomTargetRules() {
        EnvironmentEffectRegistry registry = new EnvironmentEffectRegistry();
        registry.register(registrar -> registrar
                .forTarget(context -> context.kind().equals("recipe_category")
                        && context.id().startsWith("zen_atelier:"))
                .then(EnvironmentEffects.label("atelier_recipe")));

        List<EnvironmentEffect> atelierEffects = registry.evaluate(
                EnvironmentEffectContext.recipeCategory("zen_atelier:food"),
                SNAPSHOT
        );
        List<EnvironmentEffect> vanillaEffects = registry.evaluate(
                EnvironmentEffectContext.recipeCategory("minecraft:crafting"),
                SNAPSHOT
        );

        assertThat(atelierEffects).containsExactly(EnvironmentEffects.label("atelier_recipe"));
        assertThat(vanillaEffects).isEmpty();
    }

    @Test
    void blockContextsRequireBlockState() {
        assertThatThrownBy(() -> new EnvironmentEffectContext("block", "minecraft:stone", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actionContextMatchingRejectsInvalidActionIds() {
        EnvironmentEffectContext context = EnvironmentEffectContext.action("zen_atelier:test");

        assertThatThrownBy(() -> context.matchesAction(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> context.matchesAction(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockContextMatchingRejectsNullBlock() {
        EnvironmentEffectContext context = EnvironmentEffectContext.block(Blocks.STONE.defaultBlockState());

        assertThatThrownBy(() -> context.matchesBlock(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recipeCategoryContextMatchingRejectsInvalidCategoryIds() {
        EnvironmentEffectContext context = EnvironmentEffectContext.recipeCategory("zen_atelier:test");

        assertThatThrownBy(() -> EnvironmentEffectContext.recipeCategory(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnvironmentEffectContext.recipeCategory(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> context.matchesRecipeCategory(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> context.matchesRecipeCategory(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarRejectsInvalidTargetArguments() {
        EnvironmentEffectRegistrar registrar = new EnvironmentEffectRegistrar();

        assertThatThrownBy(() -> registrar.forRecipeCategory(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar.forRecipeCategory(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar.forBlockTag(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registrar.forTarget(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void effectRulesRejectNullContextAtEvaluateTime() {
        EnvironmentEffectRule rule = new EnvironmentEffectRule(
                context -> true,
                EnvironmentConditions.always(),
                (context, snapshot) -> List.of(EnvironmentEffects.label("test"))
        );

        assertThatThrownBy(() -> rule.evaluate(null, SNAPSHOT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void effectRulesRejectNullSnapshotAtEvaluateTime() {
        EnvironmentEffectRule rule = new EnvironmentEffectRule(
                context -> true,
                EnvironmentConditions.always(),
                (context, snapshot) -> List.of(EnvironmentEffects.label("test"))
        );

        assertThatThrownBy(() -> rule.evaluate(EnvironmentEffectContext.action("zen_atelier:test"), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
