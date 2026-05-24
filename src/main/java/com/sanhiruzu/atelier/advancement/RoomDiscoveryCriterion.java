package com.sanhiruzu.atelier.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class RoomDiscoveryCriterion extends SimpleCriterionTrigger<RoomDiscoveryCriterion.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, String profileId) {
        this.trigger(player, instance -> instance.matches(profileId));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  Optional<String> profileId) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.optionalFieldOf("profile_id").forGetter(TriggerInstance::profileId)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(String discoveredProfileId) {
            return profileId.isEmpty() || profileId.get().equals(discoveredProfileId);
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return player;
        }
    }

    public Criterion<TriggerInstance> createCriterion(Optional<String> profileId) {
        return new Criterion<>(this, new TriggerInstance(Optional.empty(), profileId));
    }
}
