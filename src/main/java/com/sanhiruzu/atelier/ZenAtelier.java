package com.sanhiruzu.atelier;

import com.mojang.logging.LogUtils;
import com.sanhiruzu.atelier.command.CommandEventHandler;
import com.sanhiruzu.atelier.data.DataReloadEventHandler;
import com.sanhiruzu.atelier.event.AtelierEvents;
import com.sanhiruzu.atelier.integration.minecolonies.MineColoniesIntegration;
import com.sanhiruzu.atelier.integration.thermoo.ThermooIntegration;
import com.sanhiruzu.atelier.network.NetworkHandler;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringBasketItem;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringPoint;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringPointSpawner;
import com.sanhiruzu.atelier.synthesis.item.ActiveToolCoating;
import com.sanhiruzu.atelier.synthesis.item.AlchemicalIgnitionItem;
import com.sanhiruzu.atelier.synthesis.item.AlchemicalThrowable;
import com.sanhiruzu.atelier.synthesis.item.AlchemicalThrowableItem;
import com.sanhiruzu.atelier.synthesis.item.SynthesisItemEvents;
import com.sanhiruzu.atelier.synthesis.item.SynthesisOutputData;
import com.sanhiruzu.atelier.synthesis.item.AlchemistCodexItem;
import com.sanhiruzu.atelier.synthesis.item.AlchemistLensItem;
import com.sanhiruzu.atelier.synthesis.item.GelItem;
import com.sanhiruzu.atelier.synthesis.item.InstantSalveItem;
import com.sanhiruzu.atelier.synthesis.item.PhlogistonPebbleItem;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.item.StructureCharmItem;
import com.sanhiruzu.atelier.synthesis.item.ToolCoatingApplicationRecipe;
import com.sanhiruzu.atelier.synthesis.item.ToolCoatingEvents;
import com.sanhiruzu.atelier.synthesis.item.ToolCoatingItem;
import com.sanhiruzu.atelier.synthesis.item.UniItem;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import com.sanhiruzu.atelier.synthesis.world.ExtractionCauldronBlock;
import com.sanhiruzu.atelier.synthesis.world.ReagentStorageBlock;
import com.sanhiruzu.atelier.synthesis.world.StarterIngredientEvents;
import com.sanhiruzu.atelier.synthesis.world.SynthesisStationBlock;
import com.sanhiruzu.atelier.ui.UiBootstrap;
import com.sanhiruzu.atelier.ui.journal.RoomJournalItem;
import com.sanhiruzu.atelier.synthesis.vfx.ScaledParticleOptions;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosCapability;

@Mod(ZenAtelier.MODID)
public class ZenAtelier {
    public static final String MODID = "zen_atelier";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);

    public static final DeferredHolder<ParticleType<?>, ScaledParticleOptions.Type> ICE_CRYSTAL =
            PARTICLE_TYPES.register("ice_crystal", ScaledParticleOptions.Type::new);
    public static final DeferredHolder<ParticleType<?>, ScaledParticleOptions.Type> ICE_BURST =
            PARTICLE_TYPES.register("ice_burst", ScaledParticleOptions.Type::new);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_SHATTER =
            PARTICLE_TYPES.register("ice_shatter", () -> new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ICE_SPARK =
            PARTICLE_TYPES.register("ice_spark", () -> new SimpleParticleType(false));

    public static final DeferredBlock<SynthesisStationBlock> SYNTHESIS_STATION = BLOCKS.registerBlock(
            "synthesis_station",
            SynthesisStationBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).noOcclusion()
    );
    public static final DeferredBlock<ExtractionCauldronBlock> EXTRACTION_CAULDRON = BLOCKS.registerBlock(
            "extraction_cauldron",
            ExtractionCauldronBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER_CAULDRON).noOcclusion()
    );
    public static final DeferredBlock<ReagentStorageBlock> REAGENT_STORAGE = BLOCKS.registerBlock(
            "reagent_storage",
            ReagentStorageBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.BARREL)
    );
    public static final DeferredItem<RoomJournalItem> ROOM_JOURNAL = ITEMS.register("room_journal", () -> new RoomJournalItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<AlchemistCodexItem> ALCHEMIST_CODEX = ITEMS.register("alchemist_codex", () -> new AlchemistCodexItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<AlchemistLensItem> ALCHEMIST_LENS = ITEMS.register("alchemist_lens", () -> new AlchemistLensItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<AlchemicalIgnitionItem> ALCHEMIST_PRIMER = ITEMS.register(
            "alchemist_primer",
            () -> new AlchemicalIgnitionItem(
                    new Item.Properties().stacksTo(1),
                    false,
                    false,
                    "tooltip.zen_atelier.alchemist_primer"
            )
    );
    public static final DeferredItem<AlchemicalIgnitionItem> CRUCIBLE_SPOON = ITEMS.register(
            "crucible_spoon",
            () -> new AlchemicalIgnitionItem(
                    new Item.Properties().stacksTo(1),
                    true,
                    false,
                    "tooltip.zen_atelier.crucible_spoon"
            )
    );
    public static final DeferredItem<Item> DEWPETAL = ITEMS.register("dewpetal", () -> new Item(new Item.Properties()));
    public static final DeferredItem<UniItem> UNI = ITEMS.register("uni", () -> new UniItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> TAUN_HERB = ITEMS.register("taun_herb", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(1)
            .saturationModifier(0.1F)
            .fast()
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 20, 0), 1.0F)
            .build())));
    public static final DeferredItem<PhlogistonPebbleItem> PHLOGISTON_PEBBLE = ITEMS.register("phlogiston_pebble", () -> new PhlogistonPebbleItem(new Item.Properties()));
    public static final DeferredItem<GelItem> AQUA_GEL = ITEMS.register("aqua_gel", () -> new GelItem(new Item.Properties(), false));
    public static final DeferredItem<GelItem> EMBER_GEL = ITEMS.register("ember_gel", () -> new GelItem(new Item.Properties(), true));
    public static final DeferredItem<ReagentItem> REAGENT = ITEMS.register("reagent", () -> new ReagentItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<InstantSalveItem> INSTANT_SALVE = ITEMS.register("instant_salve", () -> new InstantSalveItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<StructureCharmItem> STRUCTURE_CHARM = ITEMS.register("structure_charm", () -> new StructureCharmItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<EntityType<?>, EntityType<AlchemicalThrowable>> ALCHEMICAL_THROWABLE =
            ENTITY_TYPES.register("alchemical_throwable", () -> EntityType.Builder
                    .<AlchemicalThrowable>of(AlchemicalThrowable::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("zen_atelier:alchemical_throwable"));
    public static final DeferredHolder<EntityType<?>, EntityType<GatheringPoint>> GATHERING_POINT =
            ENTITY_TYPES.register("gathering_point", () -> EntityType.Builder
                    .<GatheringPoint>of(GatheringPoint::new, MobCategory.MISC)
                    .sized(0.7f, 1.6f)
                    .clientTrackingRange(32)
                    .updateInterval(20)
                    .build("zen_atelier:gathering_point"));
    public static final DeferredItem<AlchemicalThrowableItem> FROST_GLOBE = ITEMS.register("frost_globe",
            () -> new AlchemicalThrowableItem(new Item.Properties().stacksTo(16), "tooltip.zen_atelier.frost_globe"));
    public static final DeferredItem<AlchemicalThrowableItem> SPARK_CORE = ITEMS.register("spark_core",
            () -> new AlchemicalThrowableItem(new Item.Properties().stacksTo(16), "tooltip.zen_atelier.spark_core"));
    public static final DeferredItem<ToolCoatingItem> CRUDE_MINING_COATING = ITEMS.register(
            "crude_mining_coating",
            () -> new ToolCoatingItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.fromNamespaceAndPath(MODID, "crude_mining_coating"),
                    64,
                    1.15f
            )
    );
    public static final DeferredItem<ToolCoatingItem> SMELTING_MINING_COATING = ITEMS.register(
            "smelting_mining_coating",
            () -> new ToolCoatingItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.fromNamespaceAndPath(MODID, "smelting_mining_coating"),
                    80,
                    1.1f
            )
    );
    public static final DeferredItem<ToolCoatingItem> SPARKING_MINING_COATING = ITEMS.register(
            "sparking_mining_coating",
            () -> new ToolCoatingItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.fromNamespaceAndPath(MODID, "sparking_mining_coating"),
                    96,
                    1.25f
            )
    );
    public static final DeferredItem<ToolCoatingItem> KEEN_WEAPON_COATING = ITEMS.register(
            "keen_weapon_coating",
            () -> new ToolCoatingItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.fromNamespaceAndPath(MODID, "keen_weapon_coating"),
                    ToolCoatingItem.Target.WEAPON,
                    64,
                    1.15f
            )
    );
    public static final DeferredItem<ToolCoatingItem> SPARKING_WEAPON_COATING = ITEMS.register(
            "sparking_weapon_coating",
            () -> new ToolCoatingItem(
                    new Item.Properties().stacksTo(16),
                    ResourceLocation.fromNamespaceAndPath(MODID, "sparking_weapon_coating"),
                    ToolCoatingItem.Target.WEAPON,
                    96,
                    1.25f
            )
    );
    public static final DeferredItem<AlchemicalThrowableItem> VOLATILE_BOMB_CORE = ITEMS.register("volatile_bomb_core",
            () -> new AlchemicalThrowableItem(new Item.Properties().stacksTo(16), "tooltip.zen_atelier.volatile_bomb_core"));
    public static final DeferredItem<AlchemicalThrowableItem> RESONANT_BOMB_CORE = ITEMS.register("resonant_bomb_core",
            () -> new AlchemicalThrowableItem(new Item.Properties().stacksTo(16), "tooltip.zen_atelier.resonant_bomb_core"));
    public static final DeferredItem<GatheringBasketItem> GATHERING_BASKET = ITEMS.register("gathering_basket",
            () -> new GatheringBasketItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> SYNTHESIS_STATION_ITEM = ITEMS.registerSimpleBlockItem(SYNTHESIS_STATION, new Item.Properties());
    public static final DeferredItem<BlockItem> EXTRACTION_CAULDRON_ITEM = ITEMS.registerSimpleBlockItem(EXTRACTION_CAULDRON, new Item.Properties());
    public static final DeferredItem<BlockItem> REAGENT_STORAGE_ITEM = ITEMS.registerSimpleBlockItem(REAGENT_STORAGE, new Item.Properties());
    public static final DeferredHolder<MenuType<?>, MenuType<SynthesisStationMenu>> SYNTHESIS_STATION_MENU =
            MENUS.register("synthesis_station", () -> IMenuTypeExtension.create(SynthesisStationMenu::new));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReagentStack>> REAGENT_STACK =
            DATA_COMPONENTS.registerComponentType("reagent_stack", builder -> builder
                    .persistent(ReagentStack.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecTrusted(ReagentStack.CODEC)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ActiveToolCoating>> ACTIVE_TOOL_COATING =
            DATA_COMPONENTS.registerComponentType("active_tool_coating", builder -> builder
                    .persistent(ActiveToolCoating.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecTrusted(ActiveToolCoating.CODEC)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SynthesisOutputData>> SYNTHESIS_OUTPUT_DATA =
            DATA_COMPONENTS.registerComponentType("synthesis_output_data", builder -> builder
                    .persistent(SynthesisOutputData.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecTrusted(SynthesisOutputData.CODEC)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ReagentContainerSnapshot>> BASKET_REAGENTS =
            DATA_COMPONENTS.registerComponentType("basket_reagents", builder -> builder
                    .persistent(ReagentContainerSnapshot.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodecTrusted(ReagentContainerSnapshot.CODEC)));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolCoatingApplicationRecipe>> TOOL_COATING_APPLICATION_RECIPE =
            RECIPE_SERIALIZERS.register(
                    "crafting_special_tool_coating_application",
                    () -> new SimpleCraftingRecipeSerializer<>(ToolCoatingApplicationRecipe::new)
            );

    public ZenAtelier(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(NetworkHandler::registerPayloadHandlers);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        PARTICLE_TYPES.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(CommandEventHandler.class);
        NeoForge.EVENT_BUS.register(DataReloadEventHandler.class);
        NeoForge.EVENT_BUS.register(AtelierEvents.class);
        NeoForge.EVENT_BUS.register(ToolCoatingEvents.class);
        NeoForge.EVENT_BUS.register(StarterIngredientEvents.class);
        NeoForge.EVENT_BUS.register(SynthesisItemEvents.class);
        NeoForge.EVENT_BUS.register(GatheringPointSpawner.class);

        UiBootstrap.registerClientIfPresent(modEventBus);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (!ModList.get().isLoaded("curios")) {
            return;
        }

        event.registerItem(CuriosCapability.ITEM, (stack, context) -> () -> stack, STRUCTURE_CHARM.get());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        MineColoniesIntegration.initialize();
        ThermooIntegration.initialize();

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ROOM_JOURNAL);
            event.accept(ALCHEMIST_CODEX);
            event.accept(ALCHEMIST_LENS);
            event.accept(ALCHEMIST_PRIMER);
            event.accept(CRUCIBLE_SPOON);
            event.accept(DEWPETAL);
            event.accept(UNI);
            event.accept(TAUN_HERB);
            event.accept(PHLOGISTON_PEBBLE);
            event.accept(AQUA_GEL);
            event.accept(EMBER_GEL);
            event.accept(REAGENT);
            event.accept(INSTANT_SALVE);
            event.accept(STRUCTURE_CHARM);
            event.accept(FROST_GLOBE);
            event.accept(SPARK_CORE);
            event.accept(CRUDE_MINING_COATING);
            event.accept(SMELTING_MINING_COATING);
            event.accept(SPARKING_MINING_COATING);
            event.accept(KEEN_WEAPON_COATING);
            event.accept(SPARKING_WEAPON_COATING);
            event.accept(VOLATILE_BOMB_CORE);
            event.accept(RESONANT_BOMB_CORE);
            event.accept(GATHERING_BASKET);
            event.accept(SYNTHESIS_STATION_ITEM);
            event.accept(EXTRACTION_CAULDRON_ITEM);
            event.accept(REAGENT_STORAGE_ITEM);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
