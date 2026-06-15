package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisCatalogSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkPayloadTest {
    private RegistryFriendlyByteBuf testBuffer;

    @BeforeEach
    @SuppressWarnings("deprecation")
    void setUp() {
        testBuffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), null);
    }

    @Test
    void testToggleDebugPayloadSerializeDeserialize() {
        // Test enabled state
        ToggleDebugPayload originalEnabled = new ToggleDebugPayload(true);
        ToggleDebugPayload.CODEC.encode(testBuffer, originalEnabled);

        testBuffer.readerIndex(0);
        ToggleDebugPayload decodedEnabled = ToggleDebugPayload.CODEC.decode(testBuffer);

        assertTrue(decodedEnabled.enabled(), "Enabled state should be preserved");

        // Test disabled state
        testBuffer.clear();
        ToggleDebugPayload originalDisabled = new ToggleDebugPayload(false);
        ToggleDebugPayload.CODEC.encode(testBuffer, originalDisabled);

        testBuffer.readerIndex(0);
        ToggleDebugPayload decodedDisabled = ToggleDebugPayload.CODEC.decode(testBuffer);

        assertFalse(decodedDisabled.enabled(), "Disabled state should be preserved");
    }

    @Test
    void testToggleDebugPayloadType() {
        ToggleDebugPayload payload = new ToggleDebugPayload(true);
        CustomPacketPayload.Type<ToggleDebugPayload> type = payload.type();

        assertNotNull(type, "Payload type should not be null");
        assertEquals(ToggleDebugPayload.TYPE, type, "Payload should return correct type");
    }

    @Test
    void testDiscoveryDataSyncPayloadSerializeDeserialize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        Map<String, Integer> discoveries = new LinkedHashMap<>();
        discoveries.put("zen_atelier:bedroom", 88);
        discoveries.put("zen_atelier:greenhouse", 71);
        discoveries.put("zen_atelier:atelier", 95);

        DiscoveryDataSyncPayload original = new DiscoveryDataSyncPayload(discoveries);
        DiscoveryDataSyncPayload.CODEC.encode(buffer, original);

        buffer.readerIndex(0);
        DiscoveryDataSyncPayload decoded = DiscoveryDataSyncPayload.CODEC.decode(buffer);

        assertEquals(discoveries, decoded.discoveredRooms(), "Discovery map should round-trip exactly");
    }

    @Test
    void testDiscoveryDataSyncPayloadWithEmptyData() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        DiscoveryDataSyncPayload original = new DiscoveryDataSyncPayload(Map.of());
        DiscoveryDataSyncPayload.CODEC.encode(buffer, original);

        buffer.readerIndex(0);
        DiscoveryDataSyncPayload decoded = DiscoveryDataSyncPayload.CODEC.decode(buffer);

        assertTrue(decoded.discoveredRooms().isEmpty(), "Empty discovery map should round-trip");
    }

    @Test
    void testExtractionKnowledgeSyncPayloadSerializeDeserialize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        Map<String, List<String>> known = new LinkedHashMap<>();
        known.put("minecraft:flint", List.of("zen_atelier:abrasive_reagent", "zen_atelier:spark_reagent"));
        known.put("minecraft:honey_bottle", List.of("zen_atelier:binding_reagent"));

        ExtractionKnowledgeSyncPayload original = new ExtractionKnowledgeSyncPayload(
                known,
                java.util.Set.of("minecraft:cobblestone", "minecraft:dirt"),
                Map.of(
                        "minecraft:flint", new ExtractionKnowledgeSyncPayload.SourceKnowledge(
                                2,
                                List.of("zen_atelier:abrasive_reagent", "zen_atelier:spark_reagent"),
                                List.of("zen_atelier:abrasive", "zen_atelier:sparking"),
                                Map.of("sharp", 2, "earth", 1)
                        ),
                        "minecraft:honey_bottle", new ExtractionKnowledgeSyncPayload.SourceKnowledge(
                                1,
                                List.of("zen_atelier:binding_reagent"),
                                List.of("zen_atelier:binding"),
                                Map.of("binding", 2)
                        )
                )
        );
        ExtractionKnowledgeSyncPayload.CODEC.encode(buffer, original);

        buffer.readerIndex(0);
        ExtractionKnowledgeSyncPayload decoded = ExtractionKnowledgeSyncPayload.CODEC.decode(buffer);

        assertEquals(original.knownSourceReagents(), decoded.knownSourceReagents(), "Known extraction sources should round-trip");
        assertEquals(original.testedEmptySources(), decoded.testedEmptySources(), "Tested-empty sources should round-trip");
        assertEquals(original.knownSourceDetails(), decoded.knownSourceDetails(), "Detailed extraction source knowledge should round-trip");
    }

    @Test
    void testSynthesisCatalogSyncPayloadSerializeDeserialize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SynthesisCatalogSyncPayload original = new SynthesisCatalogSyncPayload(
                List.of(new SynthesisCatalogSyncPayload.Entry("zen_atelier:flint", "{\"id\":\"zen_atelier:flint\"}")),
                List.of(new SynthesisCatalogSyncPayload.Entry("zen_atelier:coating", "{\"id\":\"zen_atelier:coating\"}"))
        );

        SynthesisCatalogSyncPayload.CODEC.encode(buffer, original);

        buffer.readerIndex(0);
        SynthesisCatalogSyncPayload decoded = SynthesisCatalogSyncPayload.CODEC.decode(buffer);

        assertEquals(original.extractionProfiles(), decoded.extractionProfiles(), "Extraction catalog entries should round-trip");
        assertEquals(original.synthesisProfiles(), decoded.synthesisProfiles(), "Synthesis catalog entries should round-trip");
    }

    @Test
    void testSynthesisBoardFusionPayloadSerializeDeserialize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SynthesisBoardFusionPayload original = new SynthesisBoardFusionPayload(
                17,
                List.of("zen_atelier:ab_rule", "zen_atelier:ab_rule", "zen_atelier:bc_rule"),
                99
        );

        SynthesisBoardFusionPayload.CODEC.encode(buffer, original);

        buffer.readerIndex(0);
        SynthesisBoardFusionPayload decoded = SynthesisBoardFusionPayload.CODEC.decode(buffer);

        assertEquals(17, decoded.containerId(), "Container id should round-trip");
        assertEquals(List.of("zen_atelier:ab_rule", "zen_atelier:bc_rule"), decoded.activeRuleIds(), "Rule ids should deduplicate and preserve order");
        assertEquals(49, decoded.resonanceCount(), "Resonance count should be clamped to board limits");
    }

    @Test
    void testUiPayloadTypeResourceLocations() {
        ResourceLocation discoveryId = DiscoveryDataSyncPayload.TYPE.id();
        ResourceLocation extractionKnowledgeId = ExtractionKnowledgeSyncPayload.TYPE.id();
        ResourceLocation synthesisCatalogId = SynthesisCatalogSyncPayload.TYPE.id();

        assertEquals("zen_atelier", discoveryId.getNamespace());
        assertEquals("discovery_sync", discoveryId.getPath());
        assertEquals("zen_atelier", extractionKnowledgeId.getNamespace());
        assertEquals("extraction_knowledge_sync", extractionKnowledgeId.getPath());
        assertEquals("zen_atelier", synthesisCatalogId.getNamespace());
        assertEquals("synthesis_catalog_sync", synthesisCatalogId.getPath());
    }

    @Test
    void testPayloadTypeResourceLocations() {
        ResourceLocation toggleDebugId = ToggleDebugPayload.TYPE.id();

        assertNotNull(toggleDebugId, "ToggleDebugPayload should have an ID");
        assertEquals("zen_atelier", toggleDebugId.getNamespace(), "Toggle debug payload should use zen_atelier namespace");
        assertEquals("toggle_debug", toggleDebugId.getPath(), "Toggle debug payload should have correct path");
    }
}
