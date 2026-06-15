package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.core.ReagentShape;
import com.sanhiruzu.atelier.synthesis.item.ActiveToolCoating;
import com.sanhiruzu.atelier.synthesis.item.ReagentItem;
import com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu;
import com.sanhiruzu.atelier.synthesis.gathering.GatheringBasketItem;
import com.sanhiruzu.atelier.synthesis.world.CauldronExtractionService;
import com.sanhiruzu.atelier.synthesis.world.ReagentCabinetSavedData;
import com.sanhiruzu.atelier.synthesis.world.ExtractionCauldronBlock;
import com.sanhiruzu.atelier.synthesis.world.ExtractionCauldronPhase;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainer;
import com.sanhiruzu.atelier.synthesis.storage.ReagentContainerSnapshot;
import com.sanhiruzu.atelier.synthesis.world.StarterIngredientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Set;

// Per NeoForge docs: @GameTestHolder(MODID) registers all @GameTest methods in this class.
// Template location rule: modid:classnamelower.methodnamelower
// So testModLoads -> zen_atelier:ateliergametests.testmodloads
// File: data/zen_atelier/structure/ateliergametests.testmodloads.nbt
@GameTestHolder(ZenAtelier.MODID)
public class AtelierGameTests {

    @GameTest
    public static void testModLoads(GameTestHelper helper) {
        // Verifies the mod loads without crashing in a live game instance.
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testSynthesisStationConsumesCarriedReagents(GameTestHelper helper) {
        BlockPos stationPos = new BlockPos(1, 1, 1);
        helper.setBlock(stationPos, ZenAtelier.SYNTHESIS_STATION.get());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, ReagentItem.createStack(new ReagentStack(
                "zen_atelier:abrasive_reagent",
                Set.of("zen_atelier:abrasive"),
                30,
                1,
                40,
                55,
                5,
                Map.of("earth", 1),
                java.util.List.of("zen_atelier:sharp"),
                ReagentShape.LINE_TWO,
                Set.of("minecraft:flint")
        )));
        player.getInventory().setItem(1, ReagentItem.createStack(new ReagentStack(
                "zen_atelier:binding_reagent",
                Set.of("zen_atelier:binding"),
                20,
                1,
                45,
                60,
                5,
                Map.of("water", 1),
                java.util.List.of("zen_atelier:binding"),
                ReagentShape.LINE_TWO,
                Set.of("minecraft:honey_bottle")
        )));

        SynthesisStationMenu menu = new SynthesisStationMenu(
                0,
                player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(stationPos))
        );

        List<com.sanhiruzu.atelier.synthesis.engine.SynthesisProfile> profiles = menu.profiles();
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id().equals("zen_atelier:crude_mining_coating")) {
                menu.selectProfile(i);
                break;
            }
        }

        helper.assertTrue(
                menu.selectedProfile().map(profile -> profile.id().equals("zen_atelier:crude_mining_coating")).orElse(false),
                "Expected bundled crude_mining_coating synthesis profile to be loaded"
        );
        helper.assertTrue(menu.canSynthesize(), "Expected carried reagents to satisfy crude_mining_coating");

        menu.clickMenuButton(player, SynthesisStationMenu.BUTTON_SYNTHESIZE);

        helper.assertFalse(hasReagent(player, "zen_atelier:abrasive_reagent"), "Abrasive reagent should be consumed");
        helper.assertFalse(hasReagent(player, "zen_atelier:binding_reagent"), "Binding reagent should be consumed");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testRightClickReagentStorageWithGatheringBasketDumpsAll(GameTestHelper helper) {
        BlockPos storagePos = new BlockPos(1, 1, 1);
        helper.setBlock(storagePos, ZenAtelier.REAGENT_STORAGE.get());

        ItemStack basket = new ItemStack(ZenAtelier.GATHERING_BASKET.get());
        ReagentContainer basketContents = new ReagentContainer();
        basketContents.insert(new ReagentStack(
                "zen_atelier:abrasive_reagent",
                12,
                1,
                30,
                40,
                5,
                Map.of("binding", 1),
                List.of("zen_atelier:binding"),
                Set.of()
        ));
        basketContents.insert(new ReagentStack(
                "zen_atelier:binding_reagent",
                8,
                1,
                35,
                30,
                3,
                Map.of("water", 1),
                List.of(),
                Set.of()
        ));
        GatheringBasketItem.setContents(basket, basketContents);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, basket);
        helper.useBlock(storagePos, player);

        ReagentContainerSnapshot snapshot = ReagentCabinetSavedData.get(helper.getLevel())
                .getSnapshot(helper.absolutePos(storagePos));
        helper.assertTrue(snapshot.entries().size() == 2, "Expected both basket reagent stacks to be inserted");
        helper.assertTrue(
                snapshot.entries().stream().mapToInt(ReagentStack::amount).sum() == 20,
                "Expected total transferred reagent units to match basket contents"
        );
        helper.assertTrue(GatheringBasketItem.entries(player.getMainHandItem()).isEmpty(), "Expected basket to be emptied after transfer");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testSynthesisStationConsumesGatheringBasketReagents(GameTestHelper helper) {
        BlockPos stationPos = new BlockPos(1, 1, 1);
        helper.setBlock(stationPos, ZenAtelier.SYNTHESIS_STATION.get());

        ItemStack basket = new ItemStack(ZenAtelier.GATHERING_BASKET.get());
        ReagentContainer basketContents = new ReagentContainer();
        basketContents.insert(new ReagentStack(
                "zen_atelier:abrasive_reagent",
                Set.of("zen_atelier:abrasive"),
                30,
                1,
                30,
                40,
                5,
                Map.of("earth", 1),
                List.of(),
                ReagentShape.SINGLE,
                Set.of()
        ));
        basketContents.insert(new ReagentStack(
                "zen_atelier:binding_reagent",
                Set.of("zen_atelier:binding"),
                20,
                1,
                35,
                30,
                3,
                Map.of("water", 1),
                List.of(),
                ReagentShape.SINGLE,
                Set.of()
        ));
        GatheringBasketItem.setContents(basket, basketContents);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, basket);

        SynthesisStationMenu menu = new SynthesisStationMenu(
                0,
                player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), helper.absolutePos(stationPos))
        );

        for (int i = 0; i < menu.profiles().size(); i++) {
            if (menu.profiles().get(i).id().equals("zen_atelier:crude_mining_coating")) {
                menu.selectProfile(i);
                break;
            }
        }

        helper.assertTrue(menu.canSynthesize(), "Expected basket-backed reagents to satisfy crude_mining_coating");
        menu.clickMenuButton(player, SynthesisStationMenu.BUTTON_SYNTHESIZE);
        helper.assertTrue(GatheringBasketItem.entries(player.getInventory().getItem(0)).isEmpty(), "Expected basket to be emptied after synthesis consumes reagents");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testSmeltingMiningCoatingSmeltsBlockDrops(GameTestHelper helper) {
        BlockPos orePos = new BlockPos(1, 1, 1);
        BlockPos absoluteOrePos = helper.absolutePos(orePos);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.set(ZenAtelier.ACTIVE_TOOL_COATING.get(), new ActiveToolCoating(
                ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "smelting_mining_coating"),
                1,
                1.5F
        ));
        player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);

        ArrayList<ItemEntity> drops = new ArrayList<>();
        drops.add(new ItemEntity(helper.getLevel(), absoluteOrePos.getX() + 0.5D, absoluteOrePos.getY() + 0.5D, absoluteOrePos.getZ() + 0.5D, new ItemStack(Items.RAW_IRON)));

        NeoForge.EVENT_BUS.post(new BlockDropsEvent(
                helper.getLevel(),
                absoluteOrePos,
                Blocks.IRON_ORE.defaultBlockState(),
                null,
                drops,
                player,
                player.getMainHandItem()
        ));

        helper.assertTrue(drops.getFirst().getItem().is(Items.IRON_INGOT), "Expected smelting coating to convert raw iron drops into iron ingots");
        helper.assertFalse(
                player.getMainHandItem().has(ZenAtelier.ACTIVE_TOOL_COATING.get()),
                "Expected the last smelting coating charge to be consumed after affecting the mined block"
        );
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testUniProjectileDealsMinorDamage(GameTestHelper helper) {
        Cow target = helper.spawn(EntityType.COW, new BlockPos(1, 1, 1));
        target.setHealth(20.0F);
        target.invulnerableTime = 0;
        target.hurtTime = 0;
        target.setInvulnerable(false);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Snowball snowball = new Snowball(helper.getLevel(), target.getX(), target.getEyeY(), target.getZ());
        snowball.setOwner(player);
        snowball.setItem(new ItemStack(ZenAtelier.UNI.get()));
        helper.getLevel().addFreshEntity(snowball);

        helper.assertTrue(StarterIngredientEvents.applyUniImpact(snowball, target), "Expected Uni impact damage to be accepted");
        helper.assertTrue(target.getHealth() <= 18.0F, "Expected Uni impact to deal one heart of damage");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testDroppedGelBouncesPlayerAndConsumesOneGel(GameTestHelper helper) {
        ItemEntity gel = new ItemEntity(helper.getLevel(), 1.5D, 1.0D, 1.5D, new ItemStack(ZenAtelier.AQUA_GEL.get()));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        helper.assertTrue(StarterIngredientEvents.bouncePlayerFromGel(gel, player), "Expected Aqua Gel to act as a jump pad");
        helper.assertTrue(player.getDeltaMovement().y >= 0.85D, "Expected gel jump pad to launch the player upward");
        helper.assertTrue(gel.isRemoved(), "Expected one-count gel pad to be consumed");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testStarterIngredientForageDropsCanAppear(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        ArrayList<ItemEntity> uniDrops = new ArrayList<>();
        rollBlockDrop(helper, pos, Blocks.OAK_LEAVES.defaultBlockState(), uniDrops, 400, 11L);
        helper.assertTrue(containsDrop(uniDrops, ZenAtelier.UNI.get()), "Expected oak leaves to be able to drop Uni");

        ArrayList<ItemEntity> taunDrops = new ArrayList<>();
        rollBlockDrop(helper, pos, Blocks.SHORT_GRASS.defaultBlockState(), taunDrops, 120, 12L);
        helper.assertTrue(containsDrop(taunDrops, ZenAtelier.TAUN_HERB.get()), "Expected grass forage to be able to drop Taun Herb");

        ArrayList<ItemEntity> phlogistonDrops = new ArrayList<>();
        rollBlockDrop(helper, pos, Blocks.NETHERRACK.defaultBlockState(), phlogistonDrops, 80, 13L);
        helper.assertTrue(containsDrop(phlogistonDrops, ZenAtelier.PHLOGISTON_PEBBLE.get()), "Expected Netherrack to be able to drop Phlogiston Pebbles");

        Slime slime = new Slime(EntityType.SLIME, helper.getLevel());
        slime.setSize(3, false);
        slime.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        ArrayList<ItemEntity> gelDrops = new ArrayList<>();
        RandomSource random = RandomSource.create(14L);
        for (int i = 0; i < 40; i++) {
            StarterIngredientEvents.maybeAddSlimeDrop(slime, gelDrops, random);
        }
        helper.assertTrue(
                containsDrop(gelDrops, ZenAtelier.AQUA_GEL.get()) || containsDrop(gelDrops, ZenAtelier.EMBER_GEL.get()),
                "Expected slimes to be able to drop elemental gels"
        );
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testPrimerPromotesCauldron(GameTestHelper helper) {
        BlockPos cauldronPos = new BlockPos(1, 1, 1);
        helper.setBlock(cauldronPos, Blocks.CAULDRON);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ZenAtelier.ALCHEMIST_PRIMER.get()));

        ZenAtelier.ALCHEMIST_PRIMER.get().useOn(useOnContext(helper, player, cauldronPos));

        var state = helper.getBlockState(cauldronPos);
        helper.assertTrue(state.is(ZenAtelier.EXTRACTION_CAULDRON.get()), "Expected primer to create an extraction cauldron");
        helper.assertTrue(
                state.getValue(ExtractionCauldronBlock.PHASE) == ExtractionCauldronPhase.READY,
                "Expected extraction cauldron to be ready after primer use"
        );
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "Primer should be reusable");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testCrucibleSpoonPromotesHeatedWaterCauldron(GameTestHelper helper) {
        BlockPos heatPos = new BlockPos(1, 1, 1);
        BlockPos cauldronPos = heatPos.above();
        helper.setBlock(heatPos, Blocks.CAMPFIRE);
        helper.setBlock(cauldronPos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ZenAtelier.CRUCIBLE_SPOON.get()));

        ZenAtelier.CRUCIBLE_SPOON.get().useOn(useOnContext(helper, player, cauldronPos));

        var state = helper.getBlockState(cauldronPos);
        helper.assertTrue(state.is(ZenAtelier.EXTRACTION_CAULDRON.get()), "Expected spoon to create an extraction cauldron");
        helper.assertTrue(
                state.getValue(ExtractionCauldronBlock.PHASE) == ExtractionCauldronPhase.READY,
                "Expected extraction cauldron to be ready after spoon use"
        );
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "Crucible Spoon should be reusable");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "ateliergametests.testmodloads")
    public static void testDewpetalPromotesHeatedWaterCauldron(GameTestHelper helper) {
        BlockPos heatPos = new BlockPos(1, 1, 1);
        BlockPos cauldronPos = heatPos.above();
        helper.setBlock(heatPos, Blocks.CAMPFIRE);
        helper.setBlock(cauldronPos, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));

        ItemEntity dewpetal = helper.spawnItem(ZenAtelier.DEWPETAL.get(), new Vec3(1.5D, 2.6D, 1.5D));
        CauldronExtractionService.tryProcessItem(helper.getLevel(), dewpetal);

        var state = helper.getBlockState(cauldronPos);
        helper.assertTrue(state.is(ZenAtelier.EXTRACTION_CAULDRON.get()), "Expected dewpetal to create an Atelier-owned extraction cauldron");
        helper.assertTrue(
                state.getValue(ExtractionCauldronBlock.PHASE) == ExtractionCauldronPhase.READY,
                "Expected extraction cauldron to be ready after dewpetal priming"
        );
        helper.succeed();
    }

    private static boolean hasReagent(Player player, String reagentId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ReagentStack reagent = ReagentItem.getReagent(player.getInventory().getItem(slot));
            if (reagent != null && reagent.reagentId().equals(reagentId)) {
                return true;
            }
        }
        return false;
    }

    private static void rollBlockDrop(GameTestHelper helper, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, ArrayList<ItemEntity> drops, int attempts, long seed) {
        RandomSource random = RandomSource.create(seed);
        for (int i = 0; i < attempts; i++) {
            StarterIngredientEvents.maybeAddBlockDrop(helper.getLevel(), pos, state, drops, random);
        }
    }

    private static boolean containsDrop(List<ItemEntity> drops, net.minecraft.world.item.Item item) {
        for (ItemEntity drop : drops) {
            if (drop.getItem().is(item)) {
                return true;
            }
        }
        return false;
    }

    private static UseOnContext useOnContext(GameTestHelper helper, Player player, BlockPos relativePos) {
        BlockPos absolutePos = helper.absolutePos(relativePos);
        return new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolutePos), Direction.UP, absolutePos, false)
        );
    }
}
