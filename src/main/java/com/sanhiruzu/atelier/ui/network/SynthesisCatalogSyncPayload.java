package com.sanhiruzu.atelier.ui.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.ExtractionProfileRegistry;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileDefinition;
import com.sanhiruzu.atelier.synthesis.data.SynthesisProfileRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record SynthesisCatalogSyncPayload(
        List<Entry> extractionProfiles,
        List<Entry> synthesisProfiles
) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final Type<SynthesisCatalogSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "synthesis_catalog_sync"));

    public static final StreamCodec<FriendlyByteBuf, SynthesisCatalogSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        writeEntries(buf, packet.extractionProfiles);
                        writeEntries(buf, packet.synthesisProfiles);
                    },
                    buf -> new SynthesisCatalogSyncPayload(readEntries(buf), readEntries(buf))
            );

    public SynthesisCatalogSyncPayload {
        extractionProfiles = List.copyOf(extractionProfiles);
        synthesisProfiles = List.copyOf(synthesisProfiles);
    }

    public static SynthesisCatalogSyncPayload current() {
        List<Entry> extractions = ExtractionProfileRegistry.all().stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(profile -> new Entry(profile.id().toString(), encodeExtraction(profile)))
                .toList();
        List<Entry> syntheses = SynthesisProfileRegistry.all().stream()
                .sorted(Comparator.comparing(profile -> profile.id().toString()))
                .map(profile -> new Entry(profile.id().toString(), encodeSynthesis(profile)))
                .toList();
        return new SynthesisCatalogSyncPayload(extractions, syntheses);
    }

    public Map<ResourceLocation, ExtractionProfileDefinition> decodeExtractionProfiles() {
        Map<ResourceLocation, ExtractionProfileDefinition> decoded = new LinkedHashMap<>();
        for (Entry entry : extractionProfiles) {
            JsonElement json = JsonParser.parseString(entry.json());
            ExtractionProfileDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ZenAtelier.LOGGER.error("Failed to sync extraction profile {}: {}", entry.id(), error))
                    .ifPresent(profile -> decoded.put(ResourceLocation.parse(entry.id()), profile));
        }
        return decoded;
    }

    public Map<ResourceLocation, SynthesisProfileDefinition> decodeSynthesisProfiles() {
        Map<ResourceLocation, SynthesisProfileDefinition> decoded = new LinkedHashMap<>();
        for (Entry entry : synthesisProfiles) {
            JsonElement json = JsonParser.parseString(entry.json());
            SynthesisProfileDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ZenAtelier.LOGGER.error("Failed to sync synthesis profile {}: {}", entry.id(), error))
                    .ifPresent(profile -> decoded.put(ResourceLocation.parse(entry.id()), profile));
        }
        return decoded;
    }

    @Override
    public Type<SynthesisCatalogSyncPayload> type() {
        return TYPE;
    }

    private static String encodeExtraction(ExtractionProfileDefinition profile) {
        return GSON.toJson(ExtractionProfileDefinition.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow());
    }

    private static String encodeSynthesis(SynthesisProfileDefinition profile) {
        return GSON.toJson(SynthesisProfileDefinition.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow());
    }

    private static void writeEntries(FriendlyByteBuf buf, List<Entry> entries) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUtf(entry.id());
            buf.writeUtf(entry.json());
        }
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf.readUtf(), buf.readUtf()));
        }
        return entries;
    }

    public record Entry(String id, String json) {
    }
}
