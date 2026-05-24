package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;

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
}
