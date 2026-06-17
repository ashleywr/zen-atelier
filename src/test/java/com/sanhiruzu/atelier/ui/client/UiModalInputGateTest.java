package com.sanhiruzu.atelier.ui.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UiModalInputGateTest {
    private static final ScreenRect CONFIRM = new ScreenRect(100, 80, 48, 18);

    @Test
    void inactiveGateDoesNotBlockInput() {
        assertThat(UiModalInputGate.blocks(false)).isFalse();
        assertThat(UiModalInputGate.consumeClickUnlessAllowed(false, CONFIRM, 4, 4)).isFalse();
        assertThat(UiModalInputGate.allowsClick(false, CONFIRM, 4, 4)).isTrue();
    }

    @Test
    void activeGateAllowsOnlyClicksInsideAllowedRect() {
        assertThat(UiModalInputGate.blocks(true)).isTrue();
        assertThat(UiModalInputGate.allowsClick(true, CONFIRM, 110, 90)).isTrue();
        assertThat(UiModalInputGate.consumeClickUnlessAllowed(true, CONFIRM, 110, 90)).isFalse();

        assertThat(UiModalInputGate.allowsClick(true, CONFIRM, 99, 90)).isFalse();
        assertThat(UiModalInputGate.consumeClickUnlessAllowed(true, CONFIRM, 99, 90)).isTrue();
    }

    @Test
    void activeGateConsumesScrollAndTooltips() {
        assertThat(UiModalInputGate.consumeScroll(true)).isTrue();
        assertThat(UiModalInputGate.blockUnderlyingTooltips(true)).isTrue();
        assertThat(UiModalInputGate.consumeScroll(false)).isFalse();
        assertThat(UiModalInputGate.blockUnderlyingTooltips(false)).isFalse();
    }
}
