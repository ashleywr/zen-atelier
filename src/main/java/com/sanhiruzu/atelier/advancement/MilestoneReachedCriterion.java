package com.sanhiruzu.atelier.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class MilestoneReachedCriterion extends SimpleCriterionTrigger<MilestoneReachedCriterion.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int count) {
        this.trigger(player, instance -> instance.matches(count));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<Integer> count) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.INT.optionalFieldOf("count").forGetter(TriggerInstance::count)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(int reachedCount) {
            return count.isEmpty() || count.get().equals(reachedCount);
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return player;
        }
    }

    public Criterion<TriggerInstance> createCriterion(Optional<Integer> count) {
        return new Criterion<>(this, new TriggerInstance(Optional.empty(), count));
    }
}
