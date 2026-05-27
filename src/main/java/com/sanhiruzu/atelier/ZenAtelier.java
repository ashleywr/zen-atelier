package com.sanhiruzu.atelier;

import com.mojang.logging.LogUtils;
import com.sanhiruzu.atelier.command.CommandEventHandler;
import com.sanhiruzu.atelier.data.DataReloadEventHandler;
import com.sanhiruzu.atelier.event.AtelierEvents;
import com.sanhiruzu.atelier.integration.minecolonies.MineColoniesIntegration;
import com.sanhiruzu.atelier.network.NetworkHandler;
import com.sanhiruzu.atelier.space.ChunkClassificationAttachment;
import com.sanhiruzu.atelier.space.ClassificationEventHandler;
import com.sanhiruzu.atelier.space.ClassificationTickHandler;
import com.sanhiruzu.atelier.space.zone.BlockRarityCache;
import com.sanhiruzu.atelier.space.zone.ZoneAttachment;
import com.sanhiruzu.atelier.space.zone.ZoneSignHandler;
import com.sanhiruzu.atelier.synthesis.AlchemyWandItem;
import com.sanhiruzu.atelier.synthesis.AlchemyWandTier;
import com.sanhiruzu.atelier.synthesis.FlashBombItem;
import com.sanhiruzu.atelier.synthesis.HealingSalveItem;
import com.sanhiruzu.atelier.synthesis.SynthesisCauldronBlock;
import com.sanhiruzu.atelier.synthesis.SynthesisCauldronBlockEntity;
import com.sanhiruzu.atelier.synthesis.SynthesizedItem;
import com.sanhiruzu.atelier.ui.UiBootstrap;
import com.sanhiruzu.atelier.ui.journal.RoomJournalItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ZenAtelier.MODID)
public class ZenAtelier {
    public static final String MODID = "zen_atelier";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredItem<RoomJournalItem> ROOM_JOURNAL = ITEMS.register("room_journal", () -> new RoomJournalItem(new Item.Properties().stacksTo(1)));
    public static final DeferredBlock<SynthesisCauldronBlock> SYNTHESIS_CAULDRON = BLOCKS.registerBlock(
            "synthesis_cauldron",
            SynthesisCauldronBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).lightLevel(state -> 5)
    );
    public static final DeferredItem<BlockItem> SYNTHESIS_CAULDRON_ITEM = ITEMS.registerSimpleBlockItem(SYNTHESIS_CAULDRON);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SynthesisCauldronBlockEntity>> SYNTHESIS_CAULDRON_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("synthesis_cauldron", () -> BlockEntityType.Builder.of(SynthesisCauldronBlockEntity::new, SYNTHESIS_CAULDRON.get()).build(null));

    public static final DeferredItem<AlchemyWandItem> COPPER_ALCHEMY_WAND = ITEMS.register(
            "copper_alchemy_wand",
            () -> new AlchemyWandItem(AlchemyWandTier.COPPER, new Item.Properties())
    );
    public static final DeferredItem<AlchemyWandItem> SILVER_ALCHEMY_WAND = ITEMS.register(
            "silver_alchemy_wand",
            () -> new AlchemyWandItem(AlchemyWandTier.SILVER, new Item.Properties())
    );
    public static final DeferredItem<AlchemyWandItem> GOLD_ALCHEMY_WAND = ITEMS.register(
            "gold_alchemy_wand",
            () -> new AlchemyWandItem(AlchemyWandTier.GOLD, new Item.Properties())
    );
    public static final DeferredItem<HealingSalveItem> HEALING_SALVE = ITEMS.register(
            "healing_salve",
            () -> new HealingSalveItem(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<FlashBombItem> FLASH_BOMB = ITEMS.register(
            "flash_bomb",
            () -> new FlashBombItem(new Item.Properties().stacksTo(16))
    );
    public static final DeferredItem<SynthesizedItem> REFINED_COPPER_INGOT = ITEMS.register(
            "refined_copper_ingot",
            () -> new SynthesizedItem(new Item.Properties())
    );
    public static final DeferredItem<SynthesizedItem> REFINED_IRON_INGOT = ITEMS.register(
            "refined_iron_ingot",
            () -> new SynthesizedItem(new Item.Properties())
    );
    public static final DeferredItem<SynthesizedItem> REFINED_GOLD_INGOT = ITEMS.register(
            "refined_gold_ingot",
            () -> new SynthesizedItem(new Item.Properties())
    );
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SYNTHESIS_MODIFIER =
            DATA_COMPONENTS.registerComponentType("synthesis_modifier", builder -> builder
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SYNTHESIS_QUALITY =
            DATA_COMPONENTS.registerComponentType("synthesis_quality", builder -> builder
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public ZenAtelier(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::registerPayloadHandlers);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        ChunkClassificationAttachment.ATTACHMENT_TYPES.register(modEventBus);
        ZoneAttachment.ATTACHMENT_TYPES.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(ClassificationEventHandler.class);
        NeoForge.EVENT_BUS.register(ClassificationTickHandler.class);
        NeoForge.EVENT_BUS.register(CommandEventHandler.class);
        NeoForge.EVENT_BUS.register(DataReloadEventHandler.class);
        NeoForge.EVENT_BUS.register(AtelierEvents.class);
        NeoForge.EVENT_BUS.register(ZoneSignHandler.class);

        UiBootstrap.registerClientIfPresent(modEventBus);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        MineColoniesIntegration.initialize();

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ROOM_JOURNAL);
            event.accept(COPPER_ALCHEMY_WAND);
            event.accept(SILVER_ALCHEMY_WAND);
            event.accept(GOLD_ALCHEMY_WAND);
            event.accept(SYNTHESIS_CAULDRON_ITEM);
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(REFINED_COPPER_INGOT);
            event.accept(REFINED_IRON_INGOT);
            event.accept(REFINED_GOLD_INGOT);
        }

        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(HEALING_SALVE);
        }

        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FLASH_BOMB);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
        BlockRarityCache.initialize(event.getServer().registryAccess(), event.getServer().getRecipeManager());
        LOGGER.info("Block rarity cache initialized");
    }
}
