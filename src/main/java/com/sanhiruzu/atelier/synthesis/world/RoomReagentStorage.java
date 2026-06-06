package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RoomReagentStorage {
    private RoomReagentStorage() {
    }

    public static Map<BlockPos, ReagentContainer> containersInRoom(ServerLevel level, BlockPos origin) {
        Map<BlockPos, ReagentContainer> containers = new LinkedHashMap<>();
        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        for (BlockPos pos : positionsInRoom(level, origin)) {
            containers.put(pos, data.getContainer(pos));
        }
        return containers;
    }

    public static ReagentContainer aggregateInRoom(ServerLevel level, BlockPos origin) {
        ReagentContainer aggregate = new ReagentContainer();
        for (ReagentContainer container : containersInRoom(level, origin).values()) {
            for (ReagentStack stack : container.entries()) {
                aggregate.insert(stack);
            }
        }
        return aggregate;
    }

    public static List<BlockPos> positionsInRoom(ServerLevel level, BlockPos origin) {
        ZoneData room = indoorRoomAt(level, origin);
        if (room == null) {
            return List.of();
        }

        List<BlockPos> positions = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                room.getMinX(), room.getMinY(), room.getMinZ(),
                room.getMaxX(), room.getMaxY(), room.getMaxZ()
        )) {
            if (room.contains(pos)
                    && level.getBlockState(pos).is(ZenAtelier.REAGENT_STORAGE.get())
                    && sharesIndoorRoom(level, room, pos)) {
                positions.add(pos.immutable());
            }
        }
        positions.sort(BlockPos::compareTo);
        return List.copyOf(positions);
    }

    public static Optional<ConsumptionPlan> planConsumption(
            ReagentContainer carried,
            Map<BlockPos, ReagentContainer> storageContainers,
            List<ReagentStack> consumed
    ) {
        List<ReagentStack> carriedAvailable = new ArrayList<>(carried.entries());
        Map<BlockPos, List<ReagentStack>> storageAvailable = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, ReagentContainer> entry : storageContainers.entrySet()) {
            storageAvailable.put(entry.getKey(), new ArrayList<>(entry.getValue().entries()));
        }

        List<ReagentStack> carriedConsumed = new ArrayList<>();
        Map<BlockPos, List<ReagentStack>> storageConsumed = new LinkedHashMap<>();
        for (ReagentStack stack : consumed) {
            int remaining = stack.amount();
            int fromCarried = takeFrom(carriedAvailable, stack, remaining);
            if (fromCarried > 0) {
                carriedConsumed.add(stack.withAmount(fromCarried));
                remaining -= fromCarried;
            }

            for (Map.Entry<BlockPos, List<ReagentStack>> entry : storageAvailable.entrySet()) {
                if (remaining <= 0) {
                    break;
                }
                int taken = takeFrom(entry.getValue(), stack, remaining);
                if (taken > 0) {
                    storageConsumed.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>())
                            .add(stack.withAmount(taken));
                    remaining -= taken;
                }
            }

            if (remaining > 0) {
                return Optional.empty();
            }
        }

        return Optional.of(new ConsumptionPlan(
                List.copyOf(carriedConsumed),
                copyConsumed(storageConsumed)
        ));
    }

    public static boolean consumeStorage(ServerLevel level, ConsumptionPlan plan) {
        ReagentCabinetSavedData data = ReagentCabinetSavedData.get(level);
        Map<BlockPos, ReagentContainer> updated = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, List<ReagentStack>> entry : plan.storageConsumed().entrySet()) {
            List<ReagentStack> entries = new ArrayList<>(data.getContainer(entry.getKey()).entries());
            for (ReagentStack stack : entry.getValue()) {
                if (!removeExact(entries, stack)) {
                    return false;
                }
            }
            ReagentContainer container = new ReagentContainer();
            for (ReagentStack remaining : entries) {
                if (remaining != null) {
                    container.insert(remaining);
                }
            }
            updated.put(entry.getKey(), container);
        }

        for (Map.Entry<BlockPos, ReagentContainer> entry : updated.entrySet()) {
            data.putContainer(entry.getKey(), entry.getValue());
        }
        return true;
    }

    public static ReagentContainer combine(ReagentContainer carried, ReagentContainer roomStorage) {
        ReagentContainer combined = new ReagentContainer();
        for (ReagentStack stack : carried.entries()) {
            combined.insert(stack);
        }
        for (ReagentStack stack : roomStorage.entries()) {
            combined.insert(stack);
        }
        return combined;
    }

    private static int takeFrom(List<ReagentStack> available, ReagentStack target, int amount) {
        int remaining = amount;
        for (int i = 0; i < available.size() && remaining > 0; i++) {
            ReagentStack stack = available.get(i);
            if (!sameProfile(stack, target)) {
                continue;
            }
            int taken = Math.min(stack.amount(), remaining);
            remaining -= taken;
            int left = stack.amount() - taken;
            available.set(i, left <= 0 ? null : stack.withAmount(left));
        }
        return amount - remaining;
    }

    private static boolean removeExact(List<ReagentStack> available, ReagentStack target) {
        return takeFrom(available, target, target.amount()) == target.amount();
    }

    private static Map<BlockPos, List<ReagentStack>> copyConsumed(Map<BlockPos, List<ReagentStack>> consumed) {
        Map<BlockPos, List<ReagentStack>> copy = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, List<ReagentStack>> entry : consumed.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    private static boolean sameProfile(ReagentStack left, ReagentStack right) {
        return left != null
                  && right != null
                  && left.reagentId().equals(right.reagentId())
                  && left.categories().equals(right.categories())
                  && left.tier() == right.tier()
                && left.quality() == right.quality()
                && left.purity() == right.purity()
                  && left.instability() == right.instability()
                  && left.elements().equals(right.elements())
                  && left.traits().equals(right.traits())
                  && left.shape().equals(right.shape())
                  && left.sourceHints().equals(right.sourceHints());
    }

    private static boolean sharesIndoorRoom(ServerLevel level, ZoneData room, BlockPos pos) {
        ZoneData storageRoom = indoorRoomAt(level, pos);
        return storageRoom != null && storageRoom.getRegionId().equals(room.getRegionId());
    }

    private static ZoneData indoorRoomAt(ServerLevel level, BlockPos pos) {
        return SpaceQuery.getIndoorRoomContaining(level, pos);
    }

    public record ConsumptionPlan(
            List<ReagentStack> carriedConsumed,
            Map<BlockPos, List<ReagentStack>> storageConsumed
    ) {
        public ConsumptionPlan {
            carriedConsumed = List.copyOf(carriedConsumed);
            storageConsumed = copyConsumed(storageConsumed);
        }
    }
}
