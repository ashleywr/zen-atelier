package com.sanhiruzu.atelier.synthesis.gathering;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GatheringPointSpawner {
    private static final int CHECK_INTERVAL_TICKS = 60;
    private static final int MAX_NEARBY_POINTS = 4;
    private static final double NEARBY_RADIUS = 18.0;
    private static final long COOLDOWN_RESET_TICKS = 48_000L;
    private static final Map<UUID, PlayerChunkCooldowns> PLAYER_CHUNK_COOLDOWNS = new HashMap<>();

    private GatheringPointSpawner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (heldBasket(player).isEmpty()) {
            return;
        }
        Level level = player.level();
        List<GatheringPoint> existing = level.getEntitiesOfClass(
                GatheringPoint.class,
                player.getBoundingBox().inflate(NEARBY_RADIUS)
        );
        if (existing.size() >= MAX_NEARBY_POINTS || player.getRandom().nextFloat() > 0.45F) {
            return;
        }
        PlayerChunkCooldowns cooldowns = cooldownsFor(player);
        findSpawnPos(player, cooldowns).ifPresent(pos -> {
            cooldowns.markSpawned(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            level.addFreshEntity(new GatheringPoint(
                    level,
                    pos.getX() + 0.5,
                    pos.getY(),
                    pos.getZ() + 0.5,
                    markerTypeFor(level.getBlockState(pos.below()))
            ));
        });
    }

    public static ItemStack heldBasket(Player player) {
        ItemStack main = player.getMainHandItem();
        if (GatheringBasketItem.isBasket(main)) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        return GatheringBasketItem.isBasket(offhand) ? offhand : ItemStack.EMPTY;
    }

    private static java.util.Optional<BlockPos> findSpawnPos(ServerPlayer player, PlayerChunkCooldowns cooldowns) {
        Level level = player.level();
        BlockPos origin = player.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            int dx = player.getRandom().nextIntBetweenInclusive(-10, 10);
            int dz = player.getRandom().nextIntBetweenInclusive(-10, 10);
            if (Math.abs(dx) < 3 && Math.abs(dz) < 3) {
                continue;
            }
            BlockPos sample = origin.offset(dx, player.getRandom().nextIntBetweenInclusive(-3, 3), dz);
            BlockPos ground = findGround(level, sample);
            if (ground != null && canSpawnAt(player, ground, cooldowns)) {
                return java.util.Optional.of(ground);
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean canSpawnAt(ServerPlayer player, BlockPos pos, PlayerChunkCooldowns cooldowns) {
        if (cooldowns.isCoolingDown(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4))) {
            return false;
        }
        return isOpenSky(player.level(), pos);
    }

    /** Non-semantic "is this outdoors": open to sky, or no nearby solid ceiling. Avoids spawning inside builds. */
    static boolean isOpenSky(Level level, BlockPos pos) {
        if (level.canSeeSky(pos)) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int dy = 1; dy <= 6; dy++) {
            cursor.setY(pos.getY() + dy);
            if (level.getBlockState(cursor).isFaceSturdy(level, cursor, net.minecraft.core.Direction.DOWN)) {
                return false; // solid ceiling within 6 blocks → treat as indoors
            }
        }
        return true;
    }

    private static BlockPos findGround(Level level, BlockPos sample) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - 2;
        BlockPos.MutableBlockPos cursor = sample.mutable();
        for (int y = Math.min(maxY, sample.getY() + 6); y >= Math.max(minY, sample.getY() - 8); y--) {
            cursor.setY(y);
            BlockState below = level.getBlockState(cursor.below());
            BlockState here = level.getBlockState(cursor);
            BlockState above = level.getBlockState(cursor.above());
            if (below.isFaceSturdy(level, cursor.below(), net.minecraft.core.Direction.UP)
                    && here.isAir()
                    && above.isAir()) {
                return cursor.immutable();
            }
        }
        return null;
    }

    private static GatheringMarkerType markerTypeFor(BlockState source) {
        if (source.is(BlockTags.COAL_ORES)
                || source.is(BlockTags.IRON_ORES)
                || source.is(BlockTags.GOLD_ORES)
                || source.is(BlockTags.DIAMOND_ORES)
                || source.is(BlockTags.EMERALD_ORES)
                || source.is(BlockTags.COPPER_ORES)
                || source.is(BlockTags.REDSTONE_ORES)
                || source.is(BlockTags.LAPIS_ORES)) {
            return GatheringMarkerType.ORE;
        }
        if (source.is(BlockTags.LOGS)) {
            return GatheringMarkerType.STRIKE;
        }
        return GatheringMarkerType.FORAGE;
    }

    private static PlayerChunkCooldowns cooldownsFor(ServerPlayer player) {
        long resetEpoch = player.level().getGameTime() / COOLDOWN_RESET_TICKS;
        PlayerChunkCooldowns cooldowns = PLAYER_CHUNK_COOLDOWNS.computeIfAbsent(
                player.getUUID(),
                ignored -> new PlayerChunkCooldowns(resetEpoch)
        );
        cooldowns.resetIfNeeded(resetEpoch);
        return cooldowns;
    }

    private static final class PlayerChunkCooldowns {
        private final Map<Long, Long> chunkEpochs = new HashMap<>();
        private long resetEpoch;

        private PlayerChunkCooldowns(long resetEpoch) {
            this.resetEpoch = resetEpoch;
        }

        private boolean isCoolingDown(long chunkKey) {
            return chunkEpochs.getOrDefault(chunkKey, -1L) == resetEpoch;
        }

        private void markSpawned(long chunkKey) {
            chunkEpochs.put(chunkKey, resetEpoch);
        }

        private void resetIfNeeded(long nextResetEpoch) {
            if (nextResetEpoch == resetEpoch) {
                return;
            }
            resetEpoch = nextResetEpoch;
            chunkEpochs.clear();
        }
    }
}
