package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRegistry;
import com.sanhiruzu.atelier.synthesis.data.TraitFusionRule;
import com.sanhiruzu.atelier.synthesis.engine.ResolvedFusionData;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisBoardEvaluation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record SynthesisBoardFusionPayload(
        int containerId,
        List<String> activeRuleIds,
        int resonanceCount
) implements CustomPacketPayload {
    private static final int MAX_RULES = 64;
    private static final int MAX_RESONANCE = 49;

    public static final Type<SynthesisBoardFusionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "board_fusion"));

    public static final StreamCodec<FriendlyByteBuf, SynthesisBoardFusionPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.containerId);
                        buf.writeVarInt(packet.activeRuleIds.size());
                        for (String ruleId : packet.activeRuleIds) {
                            buf.writeUtf(ruleId);
                        }
                        buf.writeVarInt(packet.resonanceCount);
                    },
                    buf -> {
                        int containerId = buf.readVarInt();
                        int ruleCount = buf.readVarInt();
                        List<String> ruleIds = new ArrayList<>(ruleCount);
                        for (int i = 0; i < ruleCount; i++) {
                            ruleIds.add(buf.readUtf());
                        }
                        int resonanceCount = buf.readVarInt();
                        return new SynthesisBoardFusionPayload(containerId, ruleIds, resonanceCount);
                    }
            );

    public SynthesisBoardFusionPayload {
        LinkedHashSet<String> uniqueRuleIds = new LinkedHashSet<>();
        for (String ruleId : activeRuleIds) {
            if (ruleId != null && !ruleId.isBlank()) {
                uniqueRuleIds.add(ruleId);
            }
            if (uniqueRuleIds.size() >= MAX_RULES) {
                break;
            }
        }
        activeRuleIds = List.copyOf(uniqueRuleIds);
        resonanceCount = Math.clamp(resonanceCount, 0, MAX_RESONANCE);
    }

    public static SynthesisBoardFusionPayload fromEvaluation(SynthesisBoardEvaluation eval, int containerId) {
        LinkedHashSet<String> ruleIds = new LinkedHashSet<>();

        for (SynthesisBoardEvaluation.ActiveFusion fusion : eval.activeFusions()) {
            ruleIds.add(fusion.rule().id());
        }

        return new SynthesisBoardFusionPayload(
                containerId,
                new ArrayList<>(ruleIds),
                eval.resonantPlacementIds().size()
        );
    }

    public ResolvedFusionData resolve() {
        if (activeRuleIds.isEmpty()) {
            return ResolvedFusionData.EMPTY;
        }
        java.util.List<TraitFusionRule> rules = new java.util.ArrayList<>();
        for (String ruleId : activeRuleIds) {
            TraitFusionRegistry.findById(ruleId).ifPresent(rules::add);
        }
        return ResolvedFusionData.fromRules(rules, resonanceCount);
    }

    @Override
    public Type<SynthesisBoardFusionPayload> type() {
        return TYPE;
    }
}
