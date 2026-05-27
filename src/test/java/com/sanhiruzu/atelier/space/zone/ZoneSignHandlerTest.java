package com.sanhiruzu.atelier.space.zone;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneSignHandlerTest {

    @Test
    void readSignTextJoinsNonBlankTrimmedLines() {
        SignText text = new SignText()
                .setMessage(0, Component.literal("  Lavender  "))
                .setMessage(1, Component.literal(""))
                .setMessage(2, Component.literal(" Loft "))
                .setMessage(3, Component.literal("  "));

        assertThat(ZoneSignHandler.readSignText(text, false)).isEqualTo("Lavender Loft");
    }
}
