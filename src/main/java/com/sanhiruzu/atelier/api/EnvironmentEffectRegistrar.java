package com.sanhiruzu.atelier.api;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
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
        if (block == null) {
            throw new IllegalArgumentException("block must not be null");
        }
        return new RuleBuilder(context -> context.matchesBlock(block));
    }

    public RuleBuilder forBlockTag(TagKey<Block> tag) {
        if (tag == null) {
            throw new IllegalArgumentException("tag must not be null");
        }
        return new RuleBuilder(context -> "block".equals(context.kind())
                && context.blockState() != null
                && context.blockState().is(tag));
    }

    public RuleBuilder forRecipeCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new IllegalArgumentException("categoryId must not be blank");
        }
        return new RuleBuilder(context -> context.matchesRecipeCategory(categoryId));
    }

    public RuleBuilder forTarget(Predicate<EnvironmentEffectContext> target) {
        if (target == null) {
            throw new IllegalArgumentException("target must not be null");
        }
        return new RuleBuilder(target);
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
            if (condition == null) {
                throw new IllegalArgumentException("condition must not be null");
            }
            this.condition = condition;
            return this;
        }

        public EnvironmentEffectRegistrar then(EnvironmentEffect effect) {
            if (effect == null) {
                throw new IllegalArgumentException("effect must not be null");
            }
            rules.add(new EnvironmentEffectRule(target, condition, (context, snapshot) -> List.of(effect)));
            return EnvironmentEffectRegistrar.this;
        }

        public EnvironmentEffectRegistrar then(
                BiFunction<EnvironmentEffectContext, EnvironmentSnapshot, List<EnvironmentEffect>> factory
        ) {
            if (factory == null) {
                throw new IllegalArgumentException("factory must not be null");
            }
            rules.add(new EnvironmentEffectRule(target, condition, factory));
            return EnvironmentEffectRegistrar.this;
        }
    }
}
