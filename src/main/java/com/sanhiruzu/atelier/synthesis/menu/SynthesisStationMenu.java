package com.sanhiruzu.atelier.synthesis.menu;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.SpaceQuery;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import com.sanhiruzu.atelier.synthesis.core.ApparatusState;
import com.sanhiruzu.atelier.synthesis.core.AttemptContext;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.SynthesisRecipeCategory;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileRegistry;
import com.sanhiruzu.atelier.synthesis.engine.ResolvedFusionData;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisAttemptInput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisExecutionResult;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisExecutor;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutcome;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlan;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisPlanner;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile;
import com.sanhiruzu.atelier.synthesis.item.CarriedReagentInventory;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.item.SynthesisItemEvents;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputData;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputItemFactory;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentQuery;
import com.sanhiruzu.atelier.synthesis.vfx.AlchemyVfx;
import com.sanhiruzu.atelier.synthesis.world.PlayerSynthesisKnowledge;
import com.sanhiruzu.atelier.synthesis.world.RoomReagentStorage;
import com.sanhiruzu.atelier.synthesis.world.RoomAlchemyContextFactory;
import com.sanhiruzu.atelier.ui.network.ReagentVaultSyncPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisResultPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SynthesisStationMenu extends AbstractContainerMenu {
    public static final int BUTTON_PREVIOUS = 0;
    public static final int BUTTON_NEXT = 1;
    public static final int BUTTON_SYNTHESIZE = 2;
    public static final int BUTTON_CATEGORY_BASE = 20;
    public static final int BUTTON_PROFILE_BASE = 100;
    private static final int CRAFTED_MASK_SLOTS = 16;
    static final int ROOM_CONTEXT_OUTSIDE = 0;
    static final int ROOM_CONTEXT_INDOOR = 1;
    static final int ROOM_CONTEXT_ATELIER = 2;
    static final int ROOM_CONTEXT_FINE_ATELIER = 3;
    private static final int OUTSIDE_FAILURE_WEIGHT = 1200;
    private static final ResourceLocation ATELIER_ROOM_ID = ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "atelier");
    private static final int VAULT_X = 16;
    private static final int VAULT_Y = 220;
    private static final int VAULT_COLUMNS = 18;
    private static final int HOTBAR_Y = 256;
    private static final int SLOT_SIZE = 18;
    private final Inventory playerInventory;
    private final ContainerLevelAccess access;
    private int selectedProfileIndex;
    private int contextRisk;
    private int roomStorageStacks;
    private int roomStorageUnits;
    private int roomContext = ROOM_CONTEXT_OUTSIDE;
    private final int[] craftedRecipeMasks = new int[CRAFTED_MASK_SLOTS];
    private List<ReagentStack> clientRoomVaultReagents = List.of();
    private List<ReagentStack> lastServerRoomVaultReagents = List.of();
    private String lastSyncedRoomVaultSignature = "";
    private SynthesisBoardFusionPayload pendingFusionData;
    public static final int CATALYST_SLOT_INDEX = 36;
    private static final int CATALYST_X = 372;
    private static final int CATALYST_Y = 190;
    private final SimpleContainer catalystContainer = new SimpleContainer(1);

    public SynthesisStationMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public SynthesisStationMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ZenAtelier.SYNTHESIS_STATION_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.access = access;
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return selectedProfileIndex;
            }

            @Override
            public void set(int value) {
                selectedProfileIndex = Math.max(0, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return contextRisk;
            }

            @Override
            public void set(int value) {
                contextRisk = Math.max(0, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return roomStorageStacks;
            }

            @Override
            public void set(int value) {
                roomStorageStacks = Math.max(0, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return roomStorageUnits;
            }

            @Override
            public void set(int value) {
                roomStorageUnits = Math.max(0, value);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return roomContext;
            }

            @Override
            public void set(int value) {
                roomContext = Math.clamp(value, ROOM_CONTEXT_OUTSIDE, ROOM_CONTEXT_FINE_ATELIER);
            }
        });
        for (int i = 0; i < craftedRecipeMasks.length; i++) {
            int maskIndex = i;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return craftedRecipeMasks[maskIndex];
                }

                @Override
                public void set(int value) {
                    craftedRecipeMasks[maskIndex] = value;
                }
            });
        }
        forceStationRoomRefresh();
        refreshStationState();
        addPlayerInventorySlots(playerInventory);
        addSlot(new Slot(catalystContainer, 0, CATALYST_X, CATALYST_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.has(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get())
                       && SynthesisItemEvents.isFresh(stack);
            }
        });
    }

    public List<SynthesisProfile> profiles() {
        return SynthesisProfileRegistry.all().stream()
                .map(definition -> definition.toCore())
                .filter(profile -> recipeUnlocked(profile, roomContext))
                .sorted(Comparator.comparing(SynthesisProfile::category, SynthesisRecipeCategory.comparator())
                        .thenComparing(SynthesisProfile::id))
                .toList();
    }

    public Optional<SynthesisProfile> selectedProfile() {
        List<SynthesisProfile> profiles = profiles();
        if (profiles.isEmpty()) {
            return Optional.empty();
        }
        selectedProfileIndex = Math.clamp(selectedProfileIndex, 0, profiles.size() - 1);
        return Optional.of(profiles.get(selectedProfileIndex));
    }

    public Optional<SynthesisPlan> currentPlan() {
        return currentPlan(ResolvedFusionData.EMPTY);
    }

    public ResolvedFusionData catalystFusion() {
        ItemStack catalyst = catalystContainer.getItem(0);
        if (catalyst.isEmpty()) return ResolvedFusionData.EMPTY;
        SynthesisOutputData data = catalyst.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
        if (data == null || !SynthesisItemEvents.isFresh(catalyst)) return ResolvedFusionData.EMPTY;
        return ResolvedFusionData.EMPTY.withCatalyst(data.affixes(), data.qualityTier() * 5);
    }

    public Optional<SynthesisPlan> currentPlan(ResolvedFusionData fusion) {
        return selectedProfile().map(profile -> {
            SynthesisProfile effective = effectiveProfile(profile);
            if (fusion.successWeightBonus() > 0) {
                effective = applyFusionSuccessBonus(effective, fusion.successWeightBonus());
            }
            int risk = Math.clamp(contextRisk + fusion.resonanceCount() * 15, 0, 100);
            return new SynthesisPlanner().plan(effective, availableReagents(), risk);
        });
    }

    public int selectedProfileIndex() {
        return selectedProfileIndex;
    }

    public boolean isProfileCrafted(SynthesisProfile profile) {
        List<SynthesisProfile> profiles = profiles();
        for (int i = 0; i < profiles.size() && i < craftedRecipeMasks.length * Integer.SIZE; i++) {
            if (profiles.get(i).id().equals(profile.id())) {
                return (craftedRecipeMasks[i / Integer.SIZE] & (1 << (i % Integer.SIZE))) != 0;
            }
        }
        return false;
    }

    public int profileCountInCategory(String category) {
        String normalized = SynthesisRecipeCategory.normalize(category);
        return (int) profiles().stream()
                .filter(profile -> profile.category().equals(normalized))
                .count();
    }

    public boolean canSynthesize() {
        return currentPlan().map(SynthesisPlan::canSynthesize).orElse(false);
    }

    public int roomStorageStacks() {
        return roomStorageStacks;
    }

    public int roomStorageUnits() {
        return roomStorageUnits;
    }

    public List<ReagentStack> roomVaultReagents() {
        return clientSide() ? clientRoomVaultReagents : roomStorageAtStation().entries();
    }

    public void setClientRoomVaultReagents(List<ReagentStack> reagents) {
        clientRoomVaultReagents = List.copyOf(reagents);
        roomStorageStacks = clientRoomVaultReagents.size();
        roomStorageUnits = clientRoomVaultReagents.stream().mapToInt(ReagentStack::amount).sum();
    }

    public void setPendingFusionData(SynthesisBoardFusionPayload payload) {
        this.pendingFusionData = payload;
    }

    public boolean hasValidSynthesisRoom() {
        return roomContext >= ROOM_CONTEXT_INDOOR;
    }

    public boolean hasAtelierRoom() {
        return roomContext >= ROOM_CONTEXT_ATELIER;
    }

    public boolean hasFineAtelierRoom() {
        return roomContext >= ROOM_CONTEXT_FINE_ATELIER;
    }

    public int synthesisRoomContext() {
        return roomContext;
    }

    public int unlockedRecipeTier() {
        return maxRecipeTier(roomContext);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_PREVIOUS) {
            moveSelection(-1);
            return true;
        }
        if (buttonId == BUTTON_NEXT) {
            moveSelection(1);
            return true;
        }
        if (buttonId == BUTTON_SYNTHESIZE) {
            execute(player);
            return true;
        }
        if (buttonId >= BUTTON_CATEGORY_BASE) {
            if (buttonId >= BUTTON_PROFILE_BASE) {
                selectProfile(buttonId - BUTTON_PROFILE_BASE);
                return true;
            }
            selectCategory(buttonId - BUTTON_CATEGORY_BASE);
            return true;
        }
        return false;
    }

    public void moveSelection(int delta) {
        List<SynthesisProfile> profiles = profiles();
        int size = profiles.size();
        if (size == 0) {
            selectedProfileIndex = 0;
            return;
        }
        selectedProfileIndex = Math.clamp(selectedProfileIndex, 0, size - 1);
        String category = profiles.get(selectedProfileIndex).category();
        List<Integer> categoryIndexes = indexesInCategory(category, profiles);
        int current = categoryIndexes.indexOf(selectedProfileIndex);
        selectedProfileIndex = categoryIndexes.get(Math.floorMod(current + delta, categoryIndexes.size()));
    }

    public void selectCategory(int categoryIndex) {
        List<String> categories = SynthesisRecipeCategory.orderedIds();
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            return;
        }
        List<SynthesisProfile> profiles = profiles();
        List<Integer> indexes = indexesInCategory(categories.get(categoryIndex), profiles);
        if (!indexes.isEmpty()) {
            selectedProfileIndex = indexes.getFirst();
        }
    }

    public void selectProfile(int profileIndex) {
        int size = profiles().size();
        if (size > 0) {
            selectedProfileIndex = Math.clamp(profileIndex, 0, size - 1);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ZenAtelier.SYNTHESIS_STATION.get());
    }

    @Override
    public void broadcastChanges() {
        if (!playerInventory.player.level().isClientSide) {
            refreshStationState();
            syncRoomVaultSnapshot();
        }
        super.broadcastChanges();
    }

    private void refreshStationState() {
        if (!playerInventory.player.level().isClientSide) {
            roomContext = stationRoomContext();
            contextRisk = currentAttemptContext().risk();
            refreshRoomStorageStats();
        }
        refreshCraftedRecipeMask();
    }

    private void forceStationRoomRefresh() {
        if (playerInventory.player.level().isClientSide) {
            return;
        }
        access.evaluate((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                ZoneRegistry.get(serverLevel).bootstrapChunk(serverLevel.getChunk(pos), serverLevel);
            }
            return true;
        }, false);
    }

    private SynthesisAttemptInput buildAttemptInput(Player player, SynthesisBoardFusionPayload payload) {
        int executionContext = stationRoomContext();
        roomContext = executionContext;
        Optional<SynthesisProfile> profile = selectedProfile();
        if (profile.isEmpty()) {
            return null;
        }
        ReagentContainer carried = CarriedReagentInventory.snapshot(player.getInventory());
        ReagentContainer roomStorage = roomStorageAtStation();
        ReagentContainer combined = RoomReagentStorage.combine(carried, roomStorage);
        AttemptContext context = currentAttemptContext();
        ResolvedFusionData fusion = payload != null ? payload.resolve() : ResolvedFusionData.EMPTY;
        SynthesisProfile effective = effectiveProfile(profile.get(), executionContext);
        if (fusion.successWeightBonus() > 0) {
            effective = applyFusionSuccessBonus(effective, fusion.successWeightBonus());
        }
        return new SynthesisAttemptInput(effective, combined, context, fusion);
    }

    private void execute(Player player) {
        if (player.level().isClientSide) {
            return;
        }

        SynthesisAttemptInput input = buildAttemptInput(player, pendingFusionData);
        pendingFusionData = null;
        if (input == null) {
            return;
        }

        // Enrich fusion with catalyst affixes and quality bonus
        ItemStack catalystStack = catalystContainer.getItem(0);
        SynthesisOutputData catalystData = catalystStack.isEmpty() ? null
                : catalystStack.get(ZenAtelier.SYNTHESIS_OUTPUT_DATA.get());
        boolean usingCatalyst = catalystData != null && SynthesisItemEvents.isFresh(catalystStack);
        if (usingCatalyst) {
            ResolvedFusionData enriched = input.fusion().withCatalyst(
                    catalystData.affixes(), catalystData.qualityTier() * 5);
            input = new SynthesisAttemptInput(
                    input.effectiveProfile(), input.reagents(), input.context(), enriched);
        }

        boolean creative = player.getAbilities().instabuild;
        SynthesisPlan plan = new SynthesisPlanner().plan(input);
        if (!plan.canSynthesize()) {
            if (!creative) {
                return;
            }
            for (var status : plan.requirements()) {
                if (!status.satisfied()) {
                    input.reagents().insert(ReagentStack.simple(
                            ReagentQuery.DEBUG_UNIVERSAL_REAGENT_ID, status.missingAmount(), 1));
                }
            }
            plan = new SynthesisPlanner().plan(input);
            if (!plan.canSynthesize()) {
                return;
            }
        }

        long seed = player.level().getGameTime() ^ player.getUUID().getLeastSignificantBits();
        SynthesisExecutionResult result = new SynthesisExecutor().execute(input, seed);

        if (!creative) {
            ReagentContainer carried = CarriedReagentInventory.snapshot(player.getInventory());
            java.util.Map<net.minecraft.core.BlockPos, ReagentContainer> storageContainers = roomStorageContainersAtStation();
            java.util.Optional<RoomReagentStorage.ConsumptionPlan> consumption = RoomReagentStorage.planConsumption(
                    carried, storageContainers, result.consumedReagents());
            if (consumption.isEmpty()) {
                return;
            }
            if (!consumption.get().carriedConsumed().isEmpty()
                    && !CarriedReagentInventory.consume(player.getInventory(), consumption.get().carriedConsumed())) {
                return;
            }
            if (!RoomReagentStorage.consumeStorage((ServerLevel) player.level(), consumption.get())) {
                return;
            }
        }

        ResolvedFusionData fusion = input.fusion();
        java.util.List<SynthesisOutput> outputs = result.result().outputs();
        if (!fusion.fusedAffixes().isEmpty() || fusion.qualityBonus() > 0) {
            java.util.List<SynthesisOutput> enhanced = new java.util.ArrayList<>(outputs.size());
            for (SynthesisOutput output : outputs) {
                SynthesisOutput o = output.withAddedAffixes(fusion.fusedAffixes());
                o = o.withBoostedQuality(fusion.qualityBonus());
                enhanced.add(o);
            }
            outputs = enhanced;
        }
        for (SynthesisOutput output : outputs) {
            giveOrDrop(player, SynthesisOutputItemFactory.createStack(output));
        }
        for (ReagentStack byproduct : result.result().byproducts()) {
            giveOrDrop(player, ReagentItem.createStack(byproduct));
        }
        if (usingCatalyst && !creative) {
            catalystContainer.setItem(0, ItemStack.EMPTY);
        }
        player.getInventory().setChanged();
        PlayerSynthesisKnowledge.markCrafted(player, input.effectiveProfile().id());
        refreshRoomStorageStats();
        syncRoomVaultSnapshot();
        refreshCraftedRecipeMask(player);
        emitSynthesisVfx(player, input.effectiveProfile(), result);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SynthesisResultPayload(containerId, result.result().outcomeClass(), outputs, result.result().byproducts()));
        }
    }

    private AttemptContext currentAttemptContext() {
        return access.evaluate((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                ZoneData zone = SpaceQuery.getRoomContaining(serverLevel, pos);
                int context = roomContextFor(zone);
                return new AttemptContext(
                        apparatusForContext(context),
                        RoomAlchemyContextFactory.fromZoneData(zone),
                        6,
                        riskForContext(context)
                );
            }
            return new AttemptContext(
                    ApparatusState.crude("zen_atelier:synthesis_station"),
                    com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext.none(),
                    6,
                    0
            );
        }, new AttemptContext(
                ApparatusState.crude("zen_atelier:synthesis_station"),
                com.sanhiruzu.atelier.synthesis.core.RoomAlchemyContext.none(),
                6,
                0
        ));
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private void emitSynthesisVfx(Player player, SynthesisProfile profile, SynthesisExecutionResult result) {
        if (!(player.level() instanceof ServerLevel)) {
            return;
        }
        access.evaluate((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                AlchemyVfx.synthesisCompleted(serverLevel, pos, profile, result);
                SoundEvent primary = synthesisSound(profile, result.result().successful());
                serverLevel.playSound(null, pos, primary, SoundSource.BLOCKS, 0.75F, result.result().successful() ? 1.2F : 0.72F);
                serverLevel.playSound(null, pos,
                        result.result().successful() ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        result.result().successful() ? 0.45F : 0.55F,
                        result.result().successful() ? 1.55F : 0.9F);
            }
            return true;
        }, false);
    }

    private static SoundEvent synthesisSound(SynthesisProfile profile, boolean successful) {
        if (!successful) {
            return SoundEvents.DISPENSER_FAIL;
        }
        return switch (SynthesisRecipeCategory.normalize(profile.category())) {
            case "bombs" -> SoundEvents.FIRECHARGE_USE;
            case "healing", "food" -> SoundEvents.BREWING_STAND_BREW;
            case "tools" -> SoundEvents.ENCHANTMENT_TABLE_USE;
            default -> SoundEvents.BOTTLE_FILL;
        };
    }

    private void addPlayerInventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                int slot = column + row * 9;
                addSlot(new Slot(
                        inventory,
                        slot + 9,
                        VAULT_X + (slot % VAULT_COLUMNS) * SLOT_SIZE,
                        VAULT_Y + (slot / VAULT_COLUMNS) * SLOT_SIZE
                ));
            }
        }

        for (int column = 0; column < 9; ++column) {
            addSlot(new Slot(inventory, column, VAULT_X + column * SLOT_SIZE, HOTBAR_Y));
        }
    }

    private void refreshCraftedRecipeMask() {
        refreshCraftedRecipeMask(playerInventory.player);
    }

    private void refreshCraftedRecipeMask(Player player) {
        List<String> profileIds = profiles().stream().map(SynthesisProfile::id).toList();
        for (int i = 0; i < craftedRecipeMasks.length; i++) {
            craftedRecipeMasks[i] = PlayerSynthesisKnowledge.maskFor(player, profileIds, i);
        }
    }

    private static List<Integer> indexesInCategory(String category, List<SynthesisProfile> profiles) {
        String normalized = SynthesisRecipeCategory.normalize(category);
        java.util.ArrayList<Integer> indexes = new java.util.ArrayList<>();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).category().equals(normalized)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private ReagentContainer availableReagents() {
        return RoomReagentStorage.combine(
                CarriedReagentInventory.snapshot(playerInventory),
                clientSide() ? clientRoomVaultContainer() : roomStorageAtStation()
        );
    }

    private ReagentContainer clientRoomVaultContainer() {
        ReagentContainer container = new ReagentContainer();
        for (ReagentStack stack : clientRoomVaultReagents) {
            container.insert(stack);
        }
        return container;
    }

    private ReagentContainer roomStorageAtStation() {
        return access.evaluate((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                return RoomReagentStorage.aggregateInRoom(serverLevel, pos);
            }
            return new ReagentContainer();
        }, new ReagentContainer());
    }

    private java.util.Map<net.minecraft.core.BlockPos, ReagentContainer> roomStorageContainersAtStation() {
        return access.evaluate((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                return RoomReagentStorage.containersInRoom(serverLevel, pos);
            }
            return java.util.Map.<net.minecraft.core.BlockPos, ReagentContainer>of();
        }, java.util.Map.of());
    }

    private void refreshRoomStorageStats() {
        ReagentContainer storage = roomStorageAtStation();
        lastServerRoomVaultReagents = storage.entries();
        roomStorageStacks = lastServerRoomVaultReagents.size();
        roomStorageUnits = lastServerRoomVaultReagents.stream().mapToInt(ReagentStack::amount).sum();
    }

    private void syncRoomVaultSnapshot() {
        if (!(playerInventory.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        String signature = vaultSignature(lastServerRoomVaultReagents);
        if (signature.equals(lastSyncedRoomVaultSignature)) {
            return;
        }
        lastSyncedRoomVaultSignature = signature;
        PacketDistributor.sendToPlayer(serverPlayer, ReagentVaultSyncPayload.create(containerId, lastServerRoomVaultReagents));
    }

    private static String vaultSignature(List<ReagentStack> entries) {
        StringBuilder builder = new StringBuilder();
        for (ReagentStack stack : entries) {
            builder.append(stack.reagentId()).append('|')
                    .append(String.join(",", stack.categories().stream().sorted().toList())).append('|')
                    .append(stack.amount()).append('|')
                    .append(stack.tier()).append('|')
                    .append(stack.quality()).append('|')
                    .append(stack.purity()).append('|')
                    .append(stack.instability()).append('|')
                    .append(stack.elements().entrySet().stream()
                            .sorted(java.util.Map.Entry.comparingByKey())
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .collect(java.util.stream.Collectors.joining(","))).append('|')
                    .append(String.join(",", stack.traits())).append('|')
                    .append(stack.shape().id()).append(':')
                    .append(stack.shape().cells().stream()
                            .map(cell -> cell.x() + "," + cell.y())
                            .collect(java.util.stream.Collectors.joining("/"))).append('|')
                    .append(String.join(",", stack.sourceHints().stream().sorted().toList())).append(';');
        }
        return builder.toString();
    }

    private boolean clientSide() {
        return playerInventory.player.level().isClientSide;
    }

    private int stationRoomContext() {
        return access.evaluate((level, pos) -> {
            if (level instanceof ServerLevel serverLevel) {
                return roomContextFor(SpaceQuery.getRoomContaining(serverLevel, pos));
            }
            return ROOM_CONTEXT_OUTSIDE;
        }, ROOM_CONTEXT_OUTSIDE);
    }

    private static boolean validSynthesisRoom(ZoneData zone) {
        return zone != null && !zone.isOutdoor() && zone.hasSpatialExtent();
    }

    private SynthesisProfile effectiveProfile(SynthesisProfile profile) {
        return effectiveProfile(profile, roomContext);
    }

    static SynthesisProfile effectiveProfile(SynthesisProfile profile, int context) {
        if (context == ROOM_CONTEXT_INDOOR) {
            return profile;
        }

        java.util.List<SynthesisOutcome> outcomes = new java.util.ArrayList<>();
        boolean hasFailure = false;
        for (SynthesisOutcome outcome : profile.outcomes()) {
            if (context == ROOM_CONTEXT_OUTSIDE && outcome.outcomeClass().successful()) {
                outcomes.add(new SynthesisOutcome(outcome.outcomeClass(), 1, outcome.outputs(), outcome.byproducts()));
            } else if (context == ROOM_CONTEXT_OUTSIDE) {
                hasFailure = true;
                outcomes.add(new SynthesisOutcome(
                        outcome.outcomeClass(),
                        Math.max(OUTSIDE_FAILURE_WEIGHT, outcome.weight() * 20),
                        outcome.outputs(),
                        outcome.byproducts()
                ));
            } else if (outcome.outcomeClass().successful()) {
                outcomes.add(new SynthesisOutcome(
                        outcome.outcomeClass(),
                        outcome.weight() * successMultiplier(context),
                        outcome.outputs(),
                        outcome.byproducts()
                ));
            } else {
                hasFailure = true;
                outcomes.add(new SynthesisOutcome(
                        outcome.outcomeClass(),
                        Math.max(1, outcome.weight() / failureDivisor(context)),
                        outcome.outputs(),
                        outcome.byproducts()
                ));
            }
        }
        if (context == ROOM_CONTEXT_OUTSIDE && !hasFailure) {
            outcomes.add(new SynthesisOutcome(OutcomeClass.DUD, OUTSIDE_FAILURE_WEIGHT, java.util.List.of(), java.util.List.of()));
        }
        return new SynthesisProfile(
                profile.id(),
                profile.category(),
                profile.requirements(),
                profile.recipeTierCap(),
                outcomes
        );
    }

    static boolean recipeUnlocked(SynthesisProfile profile, int context) {
        return profile.recipeTierCap() <= maxRecipeTier(context);
    }

    static int maxRecipeTier(int context) {
        return switch (context) {
            case ROOM_CONTEXT_ATELIER -> 3;
            case ROOM_CONTEXT_FINE_ATELIER -> 4;
            default -> 2;
        };
    }

    private static int roomContextFor(ZoneData zone) {
        if (!validSynthesisRoom(zone)) {
            return ROOM_CONTEXT_OUTSIDE;
        }
        if (zone instanceof RoomData room && ATELIER_ROOM_ID.equals(room.getZoneTypeId())) {
            return room.getQuality() >= 0.75F ? ROOM_CONTEXT_FINE_ATELIER : ROOM_CONTEXT_ATELIER;
        }
        return ROOM_CONTEXT_INDOOR;
    }

    private static ApparatusState apparatusForContext(int context) {
        return switch (context) {
            case ROOM_CONTEXT_ATELIER -> new ApparatusState("zen_atelier:synthesis_station", 3, 10);
            case ROOM_CONTEXT_FINE_ATELIER -> new ApparatusState("zen_atelier:synthesis_station", 4, 20);
            case ROOM_CONTEXT_INDOOR -> new ApparatusState("zen_atelier:synthesis_station", 2, 0);
            default -> ApparatusState.crude("zen_atelier:synthesis_station");
        };
    }

    private static int riskForContext(int context) {
        return context == ROOM_CONTEXT_OUTSIDE ? 90 : 0;
    }

    private static int successMultiplier(int context) {
        return context >= ROOM_CONTEXT_FINE_ATELIER ? 3 : 2;
    }

    private static int failureDivisor(int context) {
        return context >= ROOM_CONTEXT_FINE_ATELIER ? 3 : 2;
    }

    private static SynthesisProfile applyFusionSuccessBonus(SynthesisProfile profile, int bonus) {
        java.util.List<SynthesisOutcome> boosted = new java.util.ArrayList<>();
        for (SynthesisOutcome outcome : profile.outcomes()) {
            if (outcome.outcomeClass().successful()) {
                boosted.add(new SynthesisOutcome(
                        outcome.outcomeClass(),
                        outcome.weight() + bonus,
                        outcome.outputs(),
                        outcome.byproducts()));
            } else {
                boosted.add(outcome);
            }
        }
        return new SynthesisProfile(
                profile.id(),
                profile.category(),
                profile.requirements(),
                profile.recipeTierCap(),
                boosted);
    }

}
