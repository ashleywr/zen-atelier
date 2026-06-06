package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.ZenAtelier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ExtractionKnowledgeSyncPayload(
        Map<String, List<String>> knownSourceReagents,
        Set<String> testedEmptySources,
        Map<String, SourceKnowledge> knownSourceDetails
) implements CustomPacketPayload {
    public static final Type<ExtractionKnowledgeSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "extraction_knowledge_sync"));

    public static final StreamCodec<FriendlyByteBuf, ExtractionKnowledgeSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.knownSourceReagents.size());
                        for (Map.Entry<String, List<String>> entry : packet.knownSourceReagents.entrySet()) {
                            buf.writeUtf(entry.getKey());
                            buf.writeVarInt(entry.getValue().size());
                            for (String reagentId : entry.getValue()) {
                                buf.writeUtf(reagentId);
                            }
                        }

                        buf.writeVarInt(packet.testedEmptySources.size());
                        for (String sourceId : packet.testedEmptySources) {
                            buf.writeUtf(sourceId);
                        }

                        buf.writeVarInt(packet.knownSourceDetails.size());
                        for (Map.Entry<String, SourceKnowledge> entry : packet.knownSourceDetails.entrySet()) {
                            buf.writeUtf(entry.getKey());
                            SourceKnowledge details = entry.getValue();
                            buf.writeVarInt(details.attempts());
                            writeStringList(buf, details.reagents());
                            writeStringList(buf, details.traits());
                            buf.writeVarInt(details.elements().size());
                            for (Map.Entry<String, Integer> element : details.elements().entrySet()) {
                                buf.writeUtf(element.getKey());
                                buf.writeVarInt(element.getValue());
                            }
                        }
                    },
                    buf -> {
                        Map<String, List<String>> known = new HashMap<>();
                        int knownSize = buf.readVarInt();
                        for (int i = 0; i < knownSize; i++) {
                            String sourceId = buf.readUtf();
                            int reagentCount = buf.readVarInt();
                            List<String> reagents = new ArrayList<>();
                            for (int j = 0; j < reagentCount; j++) {
                                reagents.add(buf.readUtf());
                            }
                            known.put(sourceId, List.copyOf(reagents));
                        }

                        Set<String> empty = new HashSet<>();
                        int emptySize = buf.readVarInt();
                        for (int i = 0; i < emptySize; i++) {
                            empty.add(buf.readUtf());
                        }

                        Map<String, SourceKnowledge> details = new HashMap<>();
                        int detailSize = buf.readVarInt();
                        for (int i = 0; i < detailSize; i++) {
                            String sourceId = buf.readUtf();
                            int attempts = buf.readVarInt();
                            List<String> reagents = readStringList(buf);
                            List<String> traits = readStringList(buf);
                            Map<String, Integer> elements = new HashMap<>();
                            int elementCount = buf.readVarInt();
                            for (int j = 0; j < elementCount; j++) {
                                elements.put(buf.readUtf(), buf.readVarInt());
                            }
                            details.put(sourceId, new SourceKnowledge(attempts, reagents, traits, elements));
                        }
                        return new ExtractionKnowledgeSyncPayload(known, empty, details);
                    }
            );

    public ExtractionKnowledgeSyncPayload(Map<String, List<String>> knownSourceReagents, Set<String> testedEmptySources) {
        this(knownSourceReagents, testedEmptySources, defaultDetails(knownSourceReagents));
    }

    public ExtractionKnowledgeSyncPayload {
        knownSourceReagents = knownSourceReagents.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                ));
        testedEmptySources = Set.copyOf(testedEmptySources);
        knownSourceDetails = knownSourceDetails.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().normalized()
                ));
    }

    @Override
    public Type<ExtractionKnowledgeSyncPayload> type() {
        return TYPE;
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(buf.readUtf());
        }
        return List.copyOf(values);
    }

    private static Map<String, SourceKnowledge> defaultDetails(Map<String, List<String>> knownSourceReagents) {
        Map<String, SourceKnowledge> details = new HashMap<>();
        knownSourceReagents.forEach((sourceId, reagents) ->
                details.put(sourceId, new SourceKnowledge(0, reagents, List.of(), Map.of())));
        return details;
    }

    public record SourceKnowledge(
            int attempts,
            List<String> reagents,
            List<String> traits,
            Map<String, Integer> elements
    ) {
        public SourceKnowledge {
            attempts = Math.max(0, attempts);
            reagents = List.copyOf(reagents);
            traits = List.copyOf(traits);
            elements = Map.copyOf(elements);
        }

        private SourceKnowledge normalized() {
            return new SourceKnowledge(
                    attempts,
                    reagents.stream().sorted().toList(),
                    traits.stream().sorted().toList(),
                    elements
            );
        }
    }
}
