package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import com.sanhiruzu.atelier.synthesis.AlchemyWandTier;
import com.sanhiruzu.atelier.synthesis.SynthesisCauldronBlockEntity;
import com.sanhiruzu.atelier.synthesis.SynthesisRecipe;
import com.sanhiruzu.atelier.synthesis.SynthesisResultComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

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
    public static void testSynthesisCauldronStoresIngredientsAndRollsOutput(GameTestHelper helper) {
        BlockPos cauldronPos = new BlockPos(1, 1, 1);
        helper.setBlock(cauldronPos, ZenAtelier.SYNTHESIS_CAULDRON.get());
        helper.succeedIf(() -> {
            var blockEntity = helper.getBlockEntity(cauldronPos);
            helper.assertTrue(blockEntity instanceof SynthesisCauldronBlockEntity,
                    "Expected synthesis cauldron block entity");
            SynthesisCauldronBlockEntity cauldron = (SynthesisCauldronBlockEntity) blockEntity;
            cauldron.addIngredient(new ItemStack(Items.HONEY_BOTTLE));
            cauldron.addIngredient(new ItemStack(Items.GLOW_BERRIES));
            cauldron.addIngredient(new ItemStack(Items.AMETHYST_SHARD));

            var recipe = SynthesisRecipe.find(cauldron.ingredients(), AlchemyWandTier.COPPER, 100);
            helper.assertTrue(recipe.isPresent(), "Expected healing salve synthesis recipe");
            ItemStack output = recipe.get().assemble(AlchemyWandTier.COPPER, 100);
            helper.assertTrue(output.is(ZenAtelier.HEALING_SALVE.get()), "Expected healing salve output");
            helper.assertTrue(!SynthesisResultComponents.modifier(output).isBlank(), "Expected synthesized modifier");
            helper.assertTrue(SynthesisResultComponents.quality(output) > 0, "Expected synthesized quality");
        });
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "zoneresiliencegametests.testzonesurvivestcascade")
    public static void testWaxedSignAppliesZoneName(GameTestHelper helper) {
        BlockPos signPos = new BlockPos(1, 1, 1);
        BlockPos absoluteSignPos = helper.absolutePos(signPos);
        UUID zoneId = UUID.randomUUID();

        helper.getLevel().setBlock(absoluteSignPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
        SignBlockEntity sign = (SignBlockEntity) helper.getLevel().getBlockEntity(absoluteSignPos);
        helper.assertTrue(sign != null, "Expected placed oak sign to create a sign block entity");

        sign.getPersistentData().putString("atelier_zone_id", zoneId.toString());
        sign.updateText(text -> text.setMessage(0, Component.literal("Lavender Loft")), true);
        sign.setWaxed(true);

        helper.useBlock(signPos, helper.makeMockPlayer(GameType.CREATIVE));

        helper.succeedIf(() -> {
            String customName = ZoneRegistry.get(helper.getLevel()).getCustomName(zoneId);
            helper.assertTrue("Lavender Loft".equals(customName), "Expected sign interaction to apply zone custom name");
            ZoneRegistry.get(helper.getLevel()).remove(zoneId);
        });
    }
}
