package com.sanhiruzu.atelier.advancement;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class AtelierCriteriaTriggers {
    public static final RoomDiscoveryCriterion ROOM_DISCOVERY = new RoomDiscoveryCriterion();
    public static final MilestoneReachedCriterion MILESTONE_REACHED = new MilestoneReachedCriterion();

    public static void submitTriggerRegistrations(BiConsumer<ResourceLocation, CriterionTrigger<?>> consumer) {
        consumer.accept(
                ResourceLocation.fromNamespaceAndPath("zen_atelier", "room_discovery"),
                ROOM_DISCOVERY
        );
        consumer.accept(
                ResourceLocation.fromNamespaceAndPath("zen_atelier", "milestone_reached"),
                MILESTONE_REACHED
        );
    }
}
