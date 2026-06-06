package com.sanhiruzu.atelier.synthesis.world;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext;
import com.sanhiruzu.atelier.synthesis.data.SynthesisCatalog;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionExecutionResult;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionExecutor;
import com.sanhiruzu.atelier.synthesis.engine.ExtractionProfile;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.vfx.AlchemyVfx;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CauldronExtractionService {
    private static final int SIMMER_TICKS = 100;
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int EXTRACTING_FEEDBACK_INTERVAL_TICKS = 10;
    private static final int DISSOLVE_TICKS = 18;
    private static final int VISUAL_ENTITY_LIFESPAN_TICKS = SIMMER_TICKS + 40;
    private static final String VISUAL_INGREDIENT_KEY = "zen_atelier.extraction_visual";
    private static final Map<LevelPos, SimmerJob> JOBS = new HashMap<>();

    private CauldronExtractionService() {
    }

    public static void tick(ServerLevel level) {
        updateVisualIngredients(level);
        completeJobs(level);
        emitStateFeedback(level);
        if (level.getGameTime() % SCAN_INTERVAL_TICKS != 0) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            AABB nearby = player.getBoundingBox().inflate(24.0D);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, nearby, ItemEntity::isAlive)) {
                if (isVisualIngredient(item)) {
                    discardOrphanedVisualIngredient(item);
                    continue;
                }
                tryProcessItem(level, item);
            }
        }
    }

    public static boolean tryProcessItem(ServerLevel level, ItemEntity item) {
        BlockPos cauldronPos = BlockPos.containing(item.getX(), item.getY() - 0.45D, item.getZ());
        BlockState cauldronState = level.getBlockState(cauldronPos);
        if (!isCandidateCauldron(cauldronState) || !hasHeatSource(level, cauldronPos)) {
            return false;
        }

        LevelPos key = LevelPos.of(level, cauldronPos);
        if (!JOBS.containsKey(key) && isExtracting(cauldronState)) {
            setExtractionPhase(level, cauldronPos, ExtractionCauldronPhase.READY);
            cauldronState = level.getBlockState(cauldronPos);
        }

        ItemStack stack = item.getItem();
        if (JOBS.containsKey(key) || isExtracting(cauldronState)) {
            rejectItem(level, cauldronPos, item, RejectionReason.BUSY);
            return true;
        }

        if (isVanillaWaterCauldron(cauldronState)) {
            if (stack.is(ZenAtelier.DEWPETAL.get())) {
                primeExtractionCauldron(level, cauldronPos, cauldronState, stack, item);
            } else {
                rejectItem(level, cauldronPos, item, RejectionReason.MISSING_SOLVENT);
            }
            return true;
        }

        startExtraction(level, cauldronPos, stack, item, key);
        return true;
    }

    static boolean isWaterCauldron(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.WATER_CAULDRON);
    }

    static boolean isExtractionCauldron(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ZenAtelier.EXTRACTION_CAULDRON.get());
    }

    public static boolean tryPrimeFromTool(ServerLevel level, BlockPos pos, boolean requireHeat) {
        BlockState state = level.getBlockState(pos);
        if (!isVanillaCauldron(state) && !isVanillaWaterCauldron(state)) {
            return false;
        }
        if (requireHeat && (!isVanillaWaterCauldron(state) || !hasHeatSource(level, pos))) {
            return false;
        }
        primeExtractionCauldron(level, pos, state);
        return true;
    }

    private static boolean isCandidateCauldron(BlockState state) {
        return isVanillaWaterCauldron(state) || state.is(ZenAtelier.EXTRACTION_CAULDRON.get());
    }

    private static boolean isVanillaCauldron(BlockState state) {
        return state.is(Blocks.CAULDRON);
    }

    private static boolean isVanillaWaterCauldron(BlockState state) {
        return state.is(Blocks.WATER_CAULDRON);
    }

    private static boolean isExtracting(BlockState state) {
        return state.is(ZenAtelier.EXTRACTION_CAULDRON.get())
                && state.getValue(ExtractionCauldronBlock.PHASE) == ExtractionCauldronPhase.EXTRACTING;
    }

    static boolean hasHeatSource(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(Blocks.CAMPFIRE)
                || below.is(Blocks.SOUL_CAMPFIRE)
                || below.is(Blocks.FIRE)
                || below.is(Blocks.SOUL_FIRE)
                || below.is(Blocks.MAGMA_BLOCK)
                || below.is(Blocks.LAVA);
    }

    private static void primeExtractionCauldron(
            ServerLevel level,
            BlockPos cauldronPos,
            BlockState currentState,
            ItemStack stack,
            ItemEntity item
    ) {
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            ejectRemainder(level, cauldronPos, item);
        }
        primeExtractionCauldron(level, cauldronPos, currentState);
    }

    private static void primeExtractionCauldron(
            ServerLevel level,
            BlockPos cauldronPos,
            BlockState currentState
    ) {
        int levelValue = currentState.hasProperty(LayeredCauldronBlock.LEVEL)
                ? currentState.getValue(LayeredCauldronBlock.LEVEL)
                : 3;
        BlockState extractionState = ZenAtelier.EXTRACTION_CAULDRON.get().defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, levelValue)
                .setValue(ExtractionCauldronBlock.PHASE, ExtractionCauldronPhase.READY);
        level.setBlockAndUpdate(cauldronPos, extractionState);
        level.playSound(null, cauldronPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.8F, 1.35F);
        AlchemyVfx.extractionPrimed(level, cauldronPos);
    }

    private static void startExtraction(
            ServerLevel level,
            BlockPos cauldronPos,
            ItemStack stack,
            ItemEntity item,
            LevelPos key
    ) {
        if (stack.isEmpty() || stack.is(ZenAtelier.DEWPETAL.get())) {
            rejectItem(level, cauldronPos, item, RejectionReason.INVALID);
            return;
        }

        ItemSourceSnapshot source = ItemSourceSnapshot.fromStack(stack);
        List<ExtractionProfile> profiles = SynthesisCatalog.findExtractionProfiles(source.itemId(), source.tags());
        if (profiles.isEmpty()) {
            if (item.getOwner() instanceof ServerPlayer owner) {
                PlayerExtractionKnowledge.recordTestedEmpty(owner, source);
            }
            rejectItem(level, cauldronPos, item, RejectionReason.INVALID);
            return;
        }

        UUID ownerId = item.getOwner() instanceof ServerPlayer owner ? owner.getUUID() : null;

        ItemStack visualStack = stack.copy();
        visualStack.setCount(1);

        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        } else {
            ejectRemainder(level, cauldronPos, item);
        }

        long seed = level.getGameTime() ^ cauldronPos.asLong() ^ source.itemId().hashCode();
        ExtractionProfile profile = profiles.getFirst();
        ItemEntity visualItem = spawnVisualIngredient(level, cauldronPos, visualStack);
        long startAt = level.getGameTime();
        JOBS.put(key, new SimmerJob(
                cauldronPos.immutable(),
                profile,
                source,
                ownerId,
                startAt,
                startAt + SIMMER_TICKS,
                seed,
                visualItem,
                volatility(profile)
        ));
        setExtractionPhase(level, cauldronPos, ExtractionCauldronPhase.EXTRACTING);
        level.playSound(null, cauldronPos, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.BLOCKS, 0.75F, 1.0F);
        AlchemyVfx.extractionStarted(level, cauldronPos, source);
    }

    private static ItemEntity spawnVisualIngredient(ServerLevel level, BlockPos pos, ItemStack visualStack) {
        ItemEntity visualItem = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 1.06D,
                pos.getZ() + 0.5D,
                visualStack,
                0.0D,
                0.0D,
                0.0D
        );
        visualItem.setNoGravity(true);
        visualItem.setNeverPickUp();
        visualItem.lifespan = VISUAL_ENTITY_LIFESPAN_TICKS;
        visualItem.getPersistentData().putBoolean(VISUAL_INGREDIENT_KEY, true);
        visualItem.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(visualItem);
        return visualItem;
    }

    private static void emitStateFeedback(ServerLevel level) {
        long now = level.getGameTime();
        if (now % EXTRACTING_FEEDBACK_INTERVAL_TICKS == 0) {
            for (Map.Entry<LevelPos, SimmerJob> entry : JOBS.entrySet()) {
                if (entry.getKey().is(level)) {
                    emitExtractingFeedback(level, entry.getValue());
                }
            }
        }
    }

    private static void emitExtractingFeedback(ServerLevel level, SimmerJob job) {
        if (!isExtractionCauldron(level, job.cauldronPos()) || !hasHeatSource(level, job.cauldronPos())) {
            return;
        }

        AlchemyVfx.extractionTick(level, job.cauldronPos(), job.source(), progress(level, job), job.volatility());
        emitInstabilitySound(level, job);
    }

    private static void updateVisualIngredients(ServerLevel level) {
        Iterator<Map.Entry<LevelPos, SimmerJob>> iterator = JOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<LevelPos, SimmerJob> entry = iterator.next();
            if (!entry.getKey().is(level)) {
                continue;
            }
            SimmerJob job = entry.getValue();
            if (!isExtractionCauldron(level, job.cauldronPos()) || !hasHeatSource(level, job.cauldronPos())) {
                cancelJob(level, job);
                iterator.remove();
                continue;
            }
            updateVisualIngredient(level, job);
        }
    }

    private static void updateVisualIngredient(ServerLevel level, SimmerJob job) {
        ItemEntity visualItem = job.visualItem();
        if (visualItem == null || visualItem.isRemoved()) {
            return;
        }

        double progress = progress(level, job);
        if (progress >= dissolveProgress()) {
            AlchemyVfx.ingredientDissolved(level, job.cauldronPos(), job.source());
            discardVisualIngredient(job);
            return;
        }

        double spin = progress * Math.PI * 6.0D;
        double radius = 0.18D * (1.0D - progress);
        double bob = Math.sin(spin * 1.6D) * 0.025D;
        double x = job.cauldronPos().getX() + 0.5D + Math.cos(spin) * radius;
        double y = job.cauldronPos().getY() + 1.05D - progress * 0.34D + bob;
        double z = job.cauldronPos().getZ() + 0.5D + Math.sin(spin) * radius;

        visualItem.setPos(x, y, z);
        visualItem.setDeltaMovement(Vec3.ZERO);
        visualItem.setNoGravity(true);
        visualItem.setNeverPickUp();
        visualItem.setYRot((float) ((progress * 720.0D) % 360.0D));
        visualItem.setXRot((float) (Math.sin(spin) * 22.0D));
        visualItem.hasImpulse = true;

        AlchemyVfx.ingredientDissolving(level, job.cauldronPos(), job.source(), progress);
    }

    private static void emitInstabilitySound(ServerLevel level, SimmerJob job) {
        double progress = progress(level, job);
        if (job.volatility() <= 0.34D && progress < 0.86D) {
            return;
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        SoundEvent sound = job.volatility() > 0.42D ? SoundEvents.LAVA_POP : SoundEvents.AMETHYST_BLOCK_CHIME;
        float volume = job.volatility() > 0.42D ? 0.35F : 0.22F;
        float pitch = (float) (0.75D + progress * 0.7D + level.random.nextDouble() * 0.12D);
        level.playSound(null, job.cauldronPos(), sound, SoundSource.BLOCKS, volume, pitch);
    }

    private static void completeJobs(ServerLevel level) {
        long now = level.getGameTime();
        JOBS.entrySet().removeIf(entry -> {
            if (!entry.getKey().is(level) || entry.getValue().completeAt() > now) {
                return false;
            }
            completeJob(level, entry.getValue());
            return true;
        });
    }

    private static void completeJob(ServerLevel level, SimmerJob job) {
        if (!isExtractionCauldron(level, job.cauldronPos())) {
            discardVisualIngredient(job);
            return;
        }
        if (!hasHeatSource(level, job.cauldronPos())) {
            discardVisualIngredient(job);
            setExtractionPhase(level, job.cauldronPos(), ExtractionCauldronPhase.READY);
            return;
        }
        discardVisualIngredient(job);

        ReagentContainer transientTarget = new ReagentContainer();
        ExtractionExecutionResult execution = new ExtractionExecutor().execute(
                job.profile(),
                1,
                transientTarget,
                attemptContext(level, job.cauldronPos()),
                job.seed()
        );

        for (ReagentStack reagent : execution.depositedReagents()) {
            popReagentPlaceholder(level, job.cauldronPos(), reagent);
            AlchemyVfx.reagentPopped(level, job.cauldronPos(), reagent);
        }
        if (job.ownerId() != null && level.getPlayerByUUID(job.ownerId()) instanceof ServerPlayer player) {
            PlayerExtractionKnowledge.recordSuccess(player, job.source(), execution.depositedReagents());
        }
        level.playSound(null, job.cauldronPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.9F, 1.2F);
        level.playSound(null, job.cauldronPos(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.45F, 1.6F);
        AlchemyVfx.extractionCompleted(level, job.cauldronPos(), execution.depositedReagents());
        setExtractionPhase(level, job.cauldronPos(), ExtractionCauldronPhase.READY);
    }

    private static AttemptContext attemptContext(ServerLevel level, BlockPos pos) {
        ZoneData zone = SpaceQuery.getRoomAt(level, pos);
        RoomAlchemyContext room = RoomAlchemyContextFactory.fromZoneData(zone);
        return new AttemptContext(
                ApparatusState.crude("zen_atelier:heated_cauldron"),
                room,
                6,
                0
        );
    }

    private static void popReagentPlaceholder(ServerLevel level, BlockPos pos, ReagentStack reagent) {
        ItemStack stack = ReagentItem.createStack(reagent);
        Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, stack);
    }

    private static void setExtractionPhase(ServerLevel level, BlockPos pos, ExtractionCauldronPhase phase) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ZenAtelier.EXTRACTION_CAULDRON.get()) && state.getValue(ExtractionCauldronBlock.PHASE) != phase) {
            level.setBlockAndUpdate(pos, state.setValue(ExtractionCauldronBlock.PHASE, phase));
        }
    }

    private static void ejectRemainder(ServerLevel level, BlockPos cauldronPos, ItemEntity item) {
        dropAndDiscard(level, cauldronPos, item);
        notifyThrower(item, "message.zen_atelier.extraction.single_item");
        level.playSound(null, cauldronPos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.45F, 0.7F);
    }

    private static void rejectItem(ServerLevel level, BlockPos cauldronPos, ItemEntity item, RejectionReason reason) {
        dropAndDiscard(level, cauldronPos, item);
        notifyThrower(item, reason.messageKey());
        level.playSound(null, cauldronPos, reason.sound(), SoundSource.BLOCKS, reason.volume(), reason.pitch());
        AlchemyVfx.extractionRejected(level, cauldronPos, reason.particleCount());
    }

    private static void dropAndDiscard(ServerLevel level, BlockPos cauldronPos, ItemEntity item) {
        ItemStack stack = item.getItem().copy();
        item.discard();
        Containers.dropItemStack(level, cauldronPos.getX() + 0.5D, cauldronPos.getY() + 1.0D, cauldronPos.getZ() + 0.5D, stack);
    }

    private static void notifyThrower(ItemEntity item, String translationKey) {
        if (item.getOwner() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(translationKey), true);
        }
    }

    private static boolean isVisualIngredient(ItemEntity item) {
        for (SimmerJob job : JOBS.values()) {
            if (job.visualItem() != null && job.visualItem().getUUID().equals(item.getUUID())) {
                return true;
            }
        }
        return item.getPersistentData().getBoolean(VISUAL_INGREDIENT_KEY);
    }

    private static void discardOrphanedVisualIngredient(ItemEntity item) {
        if (isActiveVisualIngredient(item)) {
            return;
        }
        item.discard();
    }

    private static boolean isActiveVisualIngredient(ItemEntity item) {
        for (SimmerJob job : JOBS.values()) {
            if (job.visualItem() != null && job.visualItem().getUUID().equals(item.getUUID())) {
                return true;
            }
        }
        return false;
    }

    private static void cancelJob(ServerLevel level, SimmerJob job) {
        discardVisualIngredient(job);
        if (isExtractionCauldron(level, job.cauldronPos())) {
            setExtractionPhase(level, job.cauldronPos(), ExtractionCauldronPhase.READY);
        }
    }

    private static void discardVisualIngredient(SimmerJob job) {
        ItemEntity visualItem = job.visualItem();
        if (visualItem != null && !visualItem.isRemoved()) {
            visualItem.discard();
        }
    }

    private static double progress(ServerLevel level, SimmerJob job) {
        long duration = Math.max(1L, job.completeAt() - job.startAt());
        return Math.clamp((level.getGameTime() - job.startAt()) / (double) duration, 0.0D, 1.0D);
    }

    private static double dissolveProgress() {
        return Math.clamp((SIMMER_TICKS - DISSOLVE_TICKS) / (double) SIMMER_TICKS, 0.0D, 1.0D);
    }

    private static double volatility(ExtractionProfile profile) {
        int total = 0;
        int volatileWeight = 0;
        for (var outcome : profile.outcomes()) {
            total += outcome.weight();
            volatileWeight += outcome.weight() * volatilityWeight(outcome.outcomeClass());
        }
        return total <= 0 ? 0.0D : Math.clamp(volatileWeight / (double) (total * 4), 0.0D, 1.0D);
    }

    private static int volatilityWeight(OutcomeClass outcomeClass) {
        return switch (outcomeClass) {
            case PERFECT_SUCCESS, SUCCESS -> 0;
            case PARTIAL_SUCCESS, MUTATED_SUCCESS -> 1;
            case UNSTABLE_SUCCESS, DUD, RECOVERABLE_FAILURE -> 2;
            case MESSY_FAILURE -> 3;
            case CATASTROPHIC_FAILURE -> 4;
        };
    }

    private record SimmerJob(
            BlockPos cauldronPos,
            ExtractionProfile profile,
            ItemSourceSnapshot source,
            UUID ownerId,
            long startAt,
            long completeAt,
            long seed,
            ItemEntity visualItem,
            double volatility
    ) {
    }

    private enum RejectionReason {
        INVALID(SoundEvents.DISPENSER_FAIL, 0.7F, 0.7F, 10, "message.zen_atelier.extraction.invalid_item"),
        MISSING_SOLVENT(SoundEvents.DISPENSER_FAIL, 0.7F, 0.85F, 8, "message.zen_atelier.extraction.missing_solvent"),
        BUSY(SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 0.6F, 1.35F, 6, "message.zen_atelier.extraction.busy");

        private final SoundEvent sound;
        private final float volume;
        private final float pitch;
        private final int particleCount;
        private final String messageKey;

        RejectionReason(SoundEvent sound, float volume, float pitch, int particleCount, String messageKey) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
            this.particleCount = particleCount;
            this.messageKey = messageKey;
        }

        private SoundEvent sound() {
            return sound;
        }

        private float volume() {
            return volume;
        }

        private float pitch() {
            return pitch;
        }

        private int particleCount() {
            return particleCount;
        }

        private String messageKey() {
            return messageKey;
        }
    }

    private record LevelPos(String dimension, BlockPos pos) {
        private static LevelPos of(ServerLevel level, BlockPos pos) {
            return new LevelPos(level.dimension().location().toString(), pos.immutable());
        }

        private boolean is(ServerLevel level) {
            return dimension.equals(level.dimension().location().toString());
        }
    }
}
