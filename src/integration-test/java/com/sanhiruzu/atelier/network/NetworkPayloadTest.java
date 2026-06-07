package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;
import com.sanhiruzu.atelier.ui.network.RoomCatalogSyncPayload;
import com.sanhiruzu.atelier.ui.network.RoomInspectPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisCatalogSyncPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    void testSyncChunkClassificationPayloadSerializeDeserialize() {
        // Create test data with some classified blocks
        ChunkClassificationData testData = new ChunkClassificationData();
        testData.setBlockState(5, 10, 5, ClassificationState.INSIDE);
        testData.setBlockState(6, 10, 5, ClassificationState.PARTIAL);
        testData.setBlockState(7, 10, 5, ClassificationState.OUTSIDE);

        int chunkX = 42;
        int chunkZ = 99;

        SyncChunkClassificationPayload original = new SyncChunkClassificationPayload(chunkX, chunkZ, testData);
        SyncChunkClassificationPayload.CODEC.encode(testBuffer, original);

        testBuffer.readerIndex(0);
        SyncChunkClassificationPayload decoded = SyncChunkClassificationPayload.CODEC.decode(testBuffer);

        assertEquals(chunkX, decoded.chunkX(), "Chunk X coordinate should be preserved");
        assertEquals(chunkZ, decoded.chunkZ(), "Chunk Z coordinate should be preserved");

        assertNotNull(decoded.data(), "Decoded data should not be null");
        assertEquals(ClassificationState.INSIDE, decoded.data().getBlockState(5, 10, 5), "INSIDE state should be preserved");
        assertEquals(ClassificationState.PARTIAL, decoded.data().getBlockState(6, 10, 5), "PARTIAL state should be preserved");
        assertEquals(ClassificationState.OUTSIDE, decoded.data().getBlockState(7, 10, 5), "OUTSIDE state should be preserved");
    }

    @Test
    void testSyncChunkClassificationPayloadWithEmptyData() {
        ChunkClassificationData testData = new ChunkClassificationData();
        int chunkX = 10;
        int chunkZ = 20;

        SyncChunkClassificationPayload original = new SyncChunkClassificationPayload(chunkX, chunkZ, testData);
        SyncChunkClassificationPayload.CODEC.encode(testBuffer, original);

        testBuffer.readerIndex(0);
        SyncChunkClassificationPayload decoded = SyncChunkClassificationPayload.CODEC.decode(testBuffer);

        assertEquals(chunkX, decoded.chunkX());
        assertEquals(chunkZ, decoded.chunkZ());
        assertNotNull(decoded.data(), "Empty data should still be preserved");
    }

    @Test
    void testSyncChunkClassificationPayloadType() {
        ChunkClassificationData testData = new ChunkClassificationData();
        SyncChunkClassificationPayload payload = new SyncChunkClassificationPayload(0, 0, testData);

        CustomPacketPayload.Type<SyncChunkClassificationPayload> type = payload.type();

        assertNotNull(type, "Payload type should not be null");
        assertEquals(SyncChunkClassificationPayload.TYPE, type, "Payload should return correct type");
    }

    @Test
    void testSyncChunkClassificationPayloadNegativeCoordinates() {
        ChunkClassificationData testData = new ChunkClassificationData();
        int chunkX = -42;
        int chunkZ = -99;

        SyncChunkClassificationPayload original = new SyncChunkClassificationPayload(chunkX, chunkZ, testData);
        SyncChunkClassificationPayload.CODEC.encode(testBuffer, original);

        testBuffer.readerIndex(0);
        SyncChunkClassificationPayload decoded = SyncChunkClassificationPayload.CODEC.decode(testBuffer);

        assertEquals(chunkX, decoded.chunkX(), "Negative chunk X should be preserved");
        assertEquals(chunkZ, decoded.chunkZ(), "Negative chunk Z should be preserved");
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
    void testRoomCatalogSyncPayloadSerializeDeserialize() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        RoomCatalogSyncPayload original = new RoomCatalogSyncPayload(List.of(
                new RoomCatalogSyncPayload.Entry(
                        "zen_atelier:bedroom",
                        "room_type.zen_atelier.bedroom",
                        "minecraft:red_bed",
                        List.of("Look for a bed.")
                ),
                new RoomCatalogSyncPayload.Entry(
                        "example_mod:frog_habitat",
                        "room_type.example_mod.frog_habitat",
                        "minecraft:lily_pad",
                        List.of("Look for frog-friendly plants.", "Look for nearby source water.")
                )
        ));

        RoomCatalogSyncPayload.CODEC.encode(buffer, original);

        buffer.readerIndex(0);
        RoomCatalogSyncPayload decoded = RoomCatalogSyncPayload.CODEC.decode(buffer);

        assertEquals(original.entries(), decoded.entries(), "Room catalog entries should round-trip exactly");
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
    void testRoomInspectPayloadSerializeDeserialize() {
        BlockPos pos = new BlockPos(-12, 73, 2048);

        RoomInspectPayload original = new RoomInspectPayload(pos);
        RoomInspectPayload.CODEC.encode(testBuffer, original);

        testBuffer.readerIndex(0);
        RoomInspectPayload decoded = RoomInspectPayload.CODEC.decode(testBuffer);

        assertEquals(pos, decoded.pos(), "Inspect block position should be preserved");
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
        ResourceLocation roomCatalogId = RoomCatalogSyncPayload.TYPE.id();
        ResourceLocation synthesisCatalogId = SynthesisCatalogSyncPayload.TYPE.id();
        ResourceLocation roomInspectId = RoomInspectPayload.TYPE.id();

        assertEquals("zen_atelier", discoveryId.getNamespace());
        assertEquals("discovery_sync", discoveryId.getPath());
        assertEquals("zen_atelier", extractionKnowledgeId.getNamespace());
        assertEquals("extraction_knowledge_sync", extractionKnowledgeId.getPath());
        assertEquals("zen_atelier", roomCatalogId.getNamespace());
        assertEquals("room_catalog_sync", roomCatalogId.getPath());
        assertEquals("zen_atelier", synthesisCatalogId.getNamespace());
        assertEquals("synthesis_catalog_sync", synthesisCatalogId.getPath());
        assertEquals("zen_atelier", roomInspectId.getNamespace());
        assertEquals("room_inspect", roomInspectId.getPath());
    }

    @Test
    void testPayloadTypeResourceLocations() {
        ResourceLocation toggleDebugId = ToggleDebugPayload.TYPE.id();
        ResourceLocation syncChunkId = SyncChunkClassificationPayload.TYPE.id();

        assertNotNull(toggleDebugId, "ToggleDebugPayload should have an ID");
        assertNotNull(syncChunkId, "SyncChunkClassificationPayload should have an ID");

        assertEquals("zen_atelier", toggleDebugId.getNamespace(), "Toggle debug payload should use zen_atelier namespace");
        assertEquals("zen_atelier", syncChunkId.getNamespace(), "Sync chunk payload should use zen_atelier namespace");

        assertEquals("toggle_debug", toggleDebugId.getPath(), "Toggle debug payload should have correct path");
        assertEquals("sync_chunk_classification", syncChunkId.getPath(), "Sync chunk payload should have correct path");
    }

    // --- SyncZoneGridPayload ---

    @Test
    void testSyncZoneGridPayload_indoorNoType() {
        UUID regionId = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        SyncZoneGridPayload original = new SyncZoneGridPayload(regionId, false, 85, 0.92f, 0.78f, 7, null, false, null, null, -50, 0, -50, 50, 100, 50, 1.0f, null);

        SyncZoneGridPayload.CODEC.encode(testBuffer, original);
        testBuffer.readerIndex(0);
        SyncZoneGridPayload decoded = SyncZoneGridPayload.CODEC.decode(testBuffer);

        assertEquals(regionId, decoded.zoneId(), "zoneId should round-trip");
        assertFalse(decoded.isOutdoor(), "isOutdoor should be false");
        assertEquals(85, decoded.volume(), "volume should round-trip");
        assertEquals(0.92f, decoded.enclosureScore(), 0.001f, "enclosureScore should round-trip");
        assertEquals(0.78f, decoded.quality(), 0.001f, "quality should round-trip");
        assertEquals(7, decoded.lightLevel(), "lightLevel should round-trip");
        assertNull(decoded.zoneTypeId(), "null zoneTypeId should round-trip as null");
        assertFalse(decoded.degraded(), "degraded should round-trip as false");
    }

    @Test
    void testSyncZoneGridPayload_indoorWithZoneType() {
        UUID regionId = UUID.randomUUID();
        ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath("zen_atelier", "atelier");
        SyncZoneGridPayload original = new SyncZoneGridPayload(regionId, false, 120, 0.95f, 1.0f, 15, typeId, true, null, null, -100, 0, -100, 100, 150, 100, 0.85f, "MineColonies");

        SyncZoneGridPayload.CODEC.encode(testBuffer, original);
        testBuffer.readerIndex(0);
        SyncZoneGridPayload decoded = SyncZoneGridPayload.CODEC.decode(testBuffer);

        assertEquals(regionId, decoded.zoneId(), "zoneId should round-trip");
        assertEquals(typeId, decoded.zoneTypeId(), "zoneTypeId should round-trip");
        assertEquals(1.0f, decoded.quality(), 0.001f, "quality=1.0 should round-trip");
        assertEquals(15, decoded.lightLevel(), "lightLevel should round-trip");
        assertTrue(decoded.degraded(), "degraded=true should round-trip");
    }

    @Test
    void testSyncZoneGridPayload_outdoor() {
        UUID regionId = UUID.randomUUID();
        SyncZoneGridPayload original = new SyncZoneGridPayload(regionId, true, 0, 0.0f, 0.0f, -1, null, false, null, null, -50, 0, -50, 50, 100, 50, 1.0f, null);

        SyncZoneGridPayload.CODEC.encode(testBuffer, original);
        testBuffer.readerIndex(0);
        SyncZoneGridPayload decoded = SyncZoneGridPayload.CODEC.decode(testBuffer);

        assertEquals(regionId, decoded.zoneId(), "zoneId should round-trip");
        assertTrue(decoded.isOutdoor(), "isOutdoor=true should round-trip");
        assertEquals(-1, decoded.lightLevel(), "outdoor lightLevel should round-trip");
        assertNull(decoded.zoneTypeId(), "outdoor zone has no zoneTypeId");
    }

    @Test
    void testSyncZoneGridPayload_typeResourceLocation() {
        ResourceLocation id = SyncZoneGridPayload.TYPE.id();
        assertEquals("zen_atelier", id.getNamespace());
        assertEquals("sync_zone_grid", id.getPath());
    }
}
