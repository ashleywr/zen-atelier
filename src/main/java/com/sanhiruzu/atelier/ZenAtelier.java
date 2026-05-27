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
import com.sanhiruzu.atelier.ui.UiBootstrap;
import com.sanhiruzu.atelier.ui.journal.RoomJournalItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(ZenAtelier.MODID)
public class ZenAtelier {
    public static final String MODID = "zen_atelier";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<RoomJournalItem> ROOM_JOURNAL = ITEMS.register("room_journal", () -> new RoomJournalItem(new Item.Properties().stacksTo(1)));

    public ZenAtelier(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::registerPayloadHandlers);

        ITEMS.register(modEventBus);
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
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
        BlockRarityCache.initialize(event.getServer().registryAccess(), event.getServer().getRecipeManager());
        LOGGER.info("Block rarity cache initialized");
    }
}
