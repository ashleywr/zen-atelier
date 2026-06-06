package com.sanhiruzu.atelier.ui.network;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ReagentVaultSyncPayload(
        int containerId,
        List<Entry> entries
) implements CustomPacketPayload {
    private static final Gson GSON = new Gson();

    public static final Type<ReagentVaultSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "reagent_vault_sync"));

    public static final StreamCodec<FriendlyByteBuf, ReagentVaultSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.containerId);
                        writeEntries(buf, packet.entries);
                    },
                    buf -> new ReagentVaultSyncPayload(buf.readVarInt(), readEntries(buf))
            );

    public ReagentVaultSyncPayload {
        entries = List.copyOf(entries);
    }

    public static ReagentVaultSyncPayload create(int containerId, List<ReagentStack> reagents) {
        return new ReagentVaultSyncPayload(
                containerId,
                reagents.stream()
                        .map(reagent -> new Entry(encode(reagent)))
                        .toList()
        );
    }

    public List<ReagentStack> decodeEntries() {
        List<ReagentStack> decoded = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            JsonElement json = JsonParser.parseString(entry.json());
            ReagentStack.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> ZenAtelier.LOGGER.error("Failed to sync reagent vault entry: {}", error))
                    .ifPresent(decoded::add);
        }
        return List.copyOf(decoded);
    }

    @Override
    public Type<ReagentVaultSyncPayload> type() {
        return TYPE;
    }

    private static String encode(ReagentStack reagent) {
        return GSON.toJson(ReagentStack.CODEC.encodeStart(JsonOps.INSTANCE, reagent).getOrThrow());
    }

    private static void writeEntries(FriendlyByteBuf buf, List<Entry> entries) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUtf(entry.json());
        }
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf.readUtf()));
        }
        return entries;
    }

    public record Entry(String json) {
    }
}
