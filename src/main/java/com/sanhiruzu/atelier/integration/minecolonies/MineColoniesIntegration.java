package com.sanhiruzu.atelier.integration.minecolonies;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MineColoniesIntegration {
    private static final String MOD_ID = "minecolonies";
    private static final String BUILDING_CONSTRUCTION_EVENT = "com.minecolonies.api.eventbus.events.colony.buildings.BuildingConstructionModEvent";
    private static final String EVENT_HANDLER = "com.minecolonies.api.eventbus.EventBus$EventHandler";
    private static boolean initialized;

    private MineColoniesIntegration() {
    }

    public static void initialize() {
        if (initialized || !ModList.get().isLoaded(MOD_ID)) {
            return;
        }

        try {
            Class<?> apiClass = Class.forName("com.minecolonies.api.IMinecoloniesAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object eventBus = apiClass.getMethod("getEventBus").invoke(api);
            Class<?> eventClass = Class.forName(BUILDING_CONSTRUCTION_EVENT);
            Class<?> handlerClass = Class.forName(EVENT_HANDLER);

            Object handler = Proxy.newProxyInstance(
                    handlerClass.getClassLoader(),
                    new Class<?>[]{handlerClass},
                    new MineColoniesEventHandler()
            );

            Method subscribe = eventBus.getClass().getMethod("subscribe", Class.class, handlerClass);
            subscribe.setAccessible(true);
            subscribe.invoke(eventBus, eventClass, handler);
            initialized = true;
            if (Config.ENABLE_MINECOLONIES_COLONIST_EFFECTS.getAsBoolean()) {
                ColonistRoomBonuses.initialize();
                NeoForge.EVENT_BUS.addListener(MineColoniesIntegration::onServerTick);
                ZenAtelier.LOGGER.info("MineColonies integration enabled: residence comfort can recover builder materials, colonist room bonuses active");
            } else {
                ZenAtelier.LOGGER.info("MineColonies integration enabled: residence comfort can recover builder materials; colonist room bonuses disabled by config");
            }
        } catch (ReflectiveOperationException | LinkageError ex) {
            ZenAtelier.LOGGER.warn("MineColonies integration could not be initialized", ex);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        int tick = event.getServer().getTickCount();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            ColonistRoomBonuses.tick(level, tick);
        }
    }

    private static void onBuildingConstruction(Object event) {
        try {
            Object targetBuilding = call(event, "getBuilding");
            Object workOrder = call(event, "getWorkOrder");
            if (targetBuilding == null || workOrder == null || !isConstructionWork(workOrder)) {
                return;
            }

            Object colony = call(targetBuilding, "getColony");
            Object world = call(colony, "getWorld");
            if (!(world instanceof ServerLevel level)) {
                return;
            }

            RecoveryTier tier = RecoveryTier.forComfort(colonyResidenceComfort(level, colony));
            if (tier == RecoveryTier.NONE) {
                return;
            }

            Object destination = builderBuilding(colony, workOrder);
            if (destination == null) {
                destination = targetBuilding;
            }

            List<ItemStack> recovered = representativeMaterials(level, targetBuilding, tier);
            if (recovered.isEmpty()) {
                return;
            }

            BlockPos destinationPos = buildingPosition(destination);
            int returned = 0;
            for (ItemStack stack : recovered) {
                returned += stack.getCount();
                ItemStack remainder = forceTransfer(destination, stack, level);
                if (!remainder.isEmpty()) {
                    dropRemainder(level, destinationPos, remainder);
                }
            }

            ZenAtelier.LOGGER.debug("MineColonies builder recovered {} item(s) from Atelier residence comfort tier {}", returned, tier.name());
        } catch (ReflectiveOperationException | LinkageError ex) {
            ZenAtelier.LOGGER.warn("MineColonies material recovery failed", ex);
        }
    }

    private static boolean isConstructionWork(Object workOrder) throws ReflectiveOperationException {
        Object type = call(workOrder, "getWorkOrderType");
        if (type == null) {
            return false;
        }
        String name = type.toString();
        return "BUILD".equals(name) || "UPGRADE".equals(name) || "REPAIR".equals(name);
    }

    private static float colonyResidenceComfort(ServerLevel level, Object colony) {
        // Room bonuses disabled pending the atmosphere substrate.
        return 0f;
    }

    private static Object builderBuilding(Object colony, Object workOrder) throws ReflectiveOperationException {
        Object builderPos = call(workOrder, "getClaimedBy");
        if (!(builderPos instanceof BlockPos pos) || pos.equals(BlockPos.ZERO)) {
            return null;
        }

        Object manager = call(colony, "getServerBuildingManager");
        if (manager == null) {
            return null;
        }
        Method method = manager.getClass().getMethod("getBuilding", BlockPos.class);
        method.setAccessible(true);
        return method.invoke(manager, pos);
    }

    private static List<ItemStack> representativeMaterials(ServerLevel level, Object targetBuilding, RecoveryTier tier) throws ReflectiveOperationException {
        Corners corners = buildingCorners(targetBuilding);
        if (corners == null || corners.volume() > 32_768) {
            return List.of();
        }

        Map<Item, Integer> counts = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(corners.min, corners.max)) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || level.getBlockEntity(pos) != null) {
                continue;
            }

            Block block = state.getBlock();
            Item item = block.asItem();
            if (item == Items.AIR || isIntegrationInfrastructure(item)) {
                continue;
            }
            counts.merge(item, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 5)
                .sorted(Map.Entry.<Item, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(tier.maxStacks)
                .map(entry -> {
                    int amount = Math.min(tier.maxPerStack, Math.max(1, (int) Math.floor(entry.getValue() * tier.rate)));
                    return new ItemStack(entry.getKey(), amount);
                })
                .toList();
    }

    private static boolean isIntegrationInfrastructure(Item item) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (key == null) {
            return false;
        }

        String namespace = key.getNamespace();
        String path = key.getPath();
        return (MOD_ID.equals(namespace) && (path.startsWith("blockhut") || path.contains("construction_tape")))
                || ("structurize".equals(namespace) && path.contains("construction_tape"));
    }

    private static Corners buildingCorners(Object building) throws ReflectiveOperationException {
        Object tuple = call(building, "getCorners");
        if (tuple == null) {
            return null;
        }

        Object a = call(tuple, "getA");
        Object b = call(tuple, "getB");
        if (!(a instanceof BlockPos first) || !(b instanceof BlockPos second)) {
            return null;
        }

        BlockPos min = new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
        return new Corners(min, max);
    }

    private static ItemStack forceTransfer(Object building, ItemStack stack, Level level) throws ReflectiveOperationException {
        Method method = building.getClass().getMethod("forceTransferStack", ItemStack.class, Level.class);
        method.setAccessible(true);
        Object result = method.invoke(building, stack.copy(), level);
        return result instanceof ItemStack remainder ? remainder : stack;
    }

    private static BlockPos buildingPosition(Object building) throws ReflectiveOperationException {
        Object result = call(building, "getPosition");
        return result instanceof BlockPos pos ? pos : BlockPos.ZERO;
    }

    private static void dropRemainder(ServerLevel level, BlockPos pos, ItemStack remainder) {
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }

        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5d, pos.getY() + 1.0d, pos.getZ() + 0.5d, remainder);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private static Object call(Object target, String methodName) throws ReflectiveOperationException {
        if (target == null) {
            return null;
        }
        Method method = target.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private enum RecoveryTier {
        NONE(0f, 0, 0, 0f),
        MODEST(0.60f, 1, 1, 0.06f),
        STRONG(0.75f, 2, 2, 0.12f),
        EXCELLENT(0.90f, 3, 4, 0.20f);

        private final float minimumComfort;
        private final int maxStacks;
        private final int maxPerStack;
        private final float rate;

        RecoveryTier(float minimumComfort, int maxStacks, int maxPerStack, float rate) {
            this.minimumComfort = minimumComfort;
            this.maxStacks = maxStacks;
            this.maxPerStack = maxPerStack;
            this.rate = rate;
        }

        private static RecoveryTier forComfort(float comfort) {
            if (comfort >= EXCELLENT.minimumComfort) {
                return EXCELLENT;
            }
            if (comfort >= STRONG.minimumComfort) {
                return STRONG;
            }
            if (comfort >= MODEST.minimumComfort) {
                return MODEST;
            }
            return NONE;
        }
    }

    private record Corners(BlockPos min, BlockPos max) {
        private long volume() {
            return (long) (max.getX() - min.getX() + 1)
                    * (max.getY() - min.getY() + 1)
                    * (max.getZ() - min.getZ() + 1);
        }
    }

    private static final class MineColoniesEventHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("apply".equals(method.getName()) && args != null && args.length == 1) {
                onBuildingConstruction(args[0]);
                return null;
            }
            if ("toString".equals(method.getName())) {
                return "ZenAtelierMineColoniesEventHandler";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            return null;
        }
    }
}
