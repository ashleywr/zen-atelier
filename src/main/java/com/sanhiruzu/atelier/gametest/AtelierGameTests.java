package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
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
