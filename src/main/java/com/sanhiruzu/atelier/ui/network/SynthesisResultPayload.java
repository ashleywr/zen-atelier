package com.sanhiruzu.atelier.ui.network;

import com.sanhiruzu.atelier.ZenAtelier;
import com.sanhiruzu.atelier.synthesis.core.OutcomeClass;
import com.sanhiruzu.atelier.synthesis.core.ReagentStack;
import com.sanhiruzu.atelier.synthesis.engine.SynthesisOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SynthesisResultPayload(
        int containerId,
        OutcomeClass outcomeClass,
        List<SynthesisOutput> outputs,
        List<ReagentStack> byproducts
) implements CustomPacketPayload {
    public static final Type<SynthesisResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ZenAtelier.MODID, "synthesis_result"));

    public static final StreamCodec<FriendlyByteBuf, SynthesisResultPayload> CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeVarInt(packet.containerId);
                        buf.writeEnum(packet.outcomeClass);
                        buf.writeVarInt(packet.outputs.size());
                        for (SynthesisOutput out : packet.outputs) {
                            buf.writeUtf(out.outputId());
                            buf.writeVarInt(out.count());
                            buf.writeVarInt(out.tier());
                            buf.writeVarInt(out.quality());
                            buf.writeVarInt(out.affixes().size());
                            for (String affix : out.affixes()) {
                                buf.writeUtf(affix);
                            }
                        }
                        buf.writeVarInt(packet.byproducts.size());
                        for (ReagentStack bp : packet.byproducts) {
                            buf.writeUtf(bp.reagentId());
                            buf.writeVarInt(bp.amount());
                            buf.writeVarInt(bp.tier());
                            buf.writeVarInt(bp.quality());
                        }
                    },
                    buf -> {
                        int containerId = buf.readVarInt();
                        OutcomeClass outcomeClass = buf.readEnum(OutcomeClass.class);
                        int outputCount = buf.readVarInt();
                        List<SynthesisOutput> outputs = new ArrayList<>(outputCount);
                        for (int i = 0; i < outputCount; i++) {
                            String outputId = buf.readUtf();
                            int count = buf.readVarInt();
                            int tier = buf.readVarInt();
                            int quality = buf.readVarInt();
                            int affixCount = buf.readVarInt();
                            List<String> affixes = new ArrayList<>(affixCount);
                            for (int j = 0; j < affixCount; j++) {
                                affixes.add(buf.readUtf());
                            }
                            outputs.add(new SynthesisOutput(outputId, count, tier, quality, affixes));
                        }
                        int bpCount = buf.readVarInt();
                        List<ReagentStack> byproducts = new ArrayList<>(bpCount);
                        for (int i = 0; i < bpCount; i++) {
                            String bpId = buf.readUtf();
                            int bpAmount = buf.readVarInt();
                            int bpTier = buf.readVarInt();
                            int bpQuality = buf.readVarInt();
                            byproducts.add(new ReagentStack(bpId, bpAmount, bpTier, bpQuality, 0, 0, Map.of(), List.of(), Set.of()));
                        }
                        return new SynthesisResultPayload(containerId, outcomeClass, outputs, byproducts);
                    }
            );

    public SynthesisResultPayload {
        outputs = List.copyOf(outputs);
        byproducts = List.copyOf(byproducts);
    }

    @Override
    public Type<SynthesisResultPayload> type() {
        return TYPE;
    }
}
