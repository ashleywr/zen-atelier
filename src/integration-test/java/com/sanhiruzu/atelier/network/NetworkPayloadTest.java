package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.space.ChunkClassificationData;
import com.sanhiruzu.atelier.space.ClassificationState;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.RoomInspectPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkPayloadTest {
    private RegistryFriendlyByteBuf testBuffer;

    @BeforeEach
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
    void testRoomInspectPayloadSerializeDeserialize() {
        BlockPos pos = new BlockPos(-12, 73, 2048);

        RoomInspectPayload original = new RoomInspectPayload(pos);
        RoomInspectPayload.CODEC.encode(testBuffer, original);

        testBuffer.readerIndex(0);
        RoomInspectPayload decoded = RoomInspectPayload.CODEC.decode(testBuffer);

        assertEquals(pos, decoded.pos(), "Inspect block position should be preserved");
    }

    @Test
    void testUiPayloadTypeResourceLocations() {
        ResourceLocation discoveryId = DiscoveryDataSyncPayload.TYPE.id();
        ResourceLocation roomInspectId = RoomInspectPayload.TYPE.id();

        assertEquals("zen_atelier", discoveryId.getNamespace());
        assertEquals("discovery_sync", discoveryId.getPath());
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
        SyncZoneGridPayload original = new SyncZoneGridPayload(regionId, false, 85, 0.92f, 0.78f, null, false, null, null, -50, 0, -50, 50, 100, 50);

        SyncZoneGridPayload.CODEC.encode(testBuffer, original);
        testBuffer.readerIndex(0);
        SyncZoneGridPayload decoded = SyncZoneGridPayload.CODEC.decode(testBuffer);

        assertEquals(regionId, decoded.zoneId(), "zoneId should round-trip");
        assertFalse(decoded.isOutdoor(), "isOutdoor should be false");
        assertEquals(85, decoded.volume(), "volume should round-trip");
        assertEquals(0.92f, decoded.enclosureScore(), 0.001f, "enclosureScore should round-trip");
        assertEquals(0.78f, decoded.quality(), 0.001f, "quality should round-trip");
        assertNull(decoded.zoneTypeId(), "null zoneTypeId should round-trip as null");
        assertFalse(decoded.degraded(), "degraded should round-trip as false");
    }

    @Test
    void testSyncZoneGridPayload_indoorWithZoneType() {
        UUID regionId = UUID.randomUUID();
        ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath("zen_atelier", "atelier");
        SyncZoneGridPayload original = new SyncZoneGridPayload(regionId, false, 120, 0.95f, 1.0f, typeId, true, null, null, -100, 0, -100, 100, 150, 100);

        SyncZoneGridPayload.CODEC.encode(testBuffer, original);
        testBuffer.readerIndex(0);
        SyncZoneGridPayload decoded = SyncZoneGridPayload.CODEC.decode(testBuffer);

        assertEquals(regionId, decoded.zoneId(), "zoneId should round-trip");
        assertEquals(typeId, decoded.zoneTypeId(), "zoneTypeId should round-trip");
        assertEquals(1.0f, decoded.quality(), 0.001f, "quality=1.0 should round-trip");
        assertTrue(decoded.degraded(), "degraded=true should round-trip");
    }

    @Test
    void testSyncZoneGridPayload_outdoor() {
        UUID regionId = UUID.randomUUID();
        SyncZoneGridPayload original = new SyncZoneGridPayload(regionId, true, 0, 0.0f, 0.0f, null, false, null, null, -50, 0, -50, 50, 100, 50);

        SyncZoneGridPayload.CODEC.encode(testBuffer, original);
        testBuffer.readerIndex(0);
        SyncZoneGridPayload decoded = SyncZoneGridPayload.CODEC.decode(testBuffer);

        assertEquals(regionId, decoded.zoneId(), "zoneId should round-trip");
        assertTrue(decoded.isOutdoor(), "isOutdoor=true should round-trip");
        assertNull(decoded.zoneTypeId(), "outdoor zone has no zoneTypeId");
    }

    @Test
    void testSyncZoneGridPayload_typeResourceLocation() {
        ResourceLocation id = SyncZoneGridPayload.TYPE.id();
        assertEquals("zen_atelier", id.getNamespace());
        assertEquals("sync_zone_grid", id.getPath());
    }
}
