package com.sanhiruzu.atelier.synthesis.core;

import com.sanhiruzu.atelier.api.EnvironmentAirHazard;
import com.sanhiruzu.atelier.api.EnvironmentEffect;
import com.sanhiruzu.atelier.api.EnvironmentEffectContext;
import com.sanhiruzu.atelier.api.EnvironmentEffectType;
import com.sanhiruzu.atelier.api.EnvironmentSnapshot;
import com.sanhiruzu.atelier.api.EnvironmentTemperatureBand;
import com.sanhiruzu.atelier.api.ZoneAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EnvironmentAlchemyContext {
    public static final int DEFAULT_RADIUS = 6;
    public static final String EXTRACTION_ACTION = "zen_atelier:extraction";
    public static final String SYNTHESIS_ACTION = "zen_atelier:synthesis";

    private EnvironmentAlchemyContext() {
    }

    public static RoomAlchemyContext forAction(Level level, BlockPos pos, String actionId) {
        return forAction(level, pos, DEFAULT_RADIUS, actionId);
    }

    public static RoomAlchemyContext forAction(Level level, BlockPos pos, int radius, String actionId) {
        EnvironmentSnapshot snapshot = ZoneAPI.environmentAt(level, pos, radius);
        List<EnvironmentEffect> effects = ZoneAPI.evaluateEnvironmentEffects(
                EnvironmentEffectContext.action(actionId),
                level,
                pos,
                radius
        );
        return from(snapshot, effects);
    }

    public static RoomAlchemyContext from(EnvironmentSnapshot snapshot, List<EnvironmentEffect> effects) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(effects, "effects must not be null");

        Set<String> signals = new LinkedHashSet<>();
        snapshot.signalCounts().forEach((signal, count) -> {
            if (count != null && count > 0) {
                signals.add(signal);
            }
        });
        addSnapshotSignals(snapshot, signals);

        int riskBias = 0;
        int stability = 0;
        for (EnvironmentEffect effect : effects) {
            if (effect == null) {
                continue;
            }
            if (effect.type() == EnvironmentEffectType.RISK_MODIFIER) {
                riskBias += effectPoints(effect);
            } else if (effect.type() == EnvironmentEffectType.STABILITY_MODIFIER) {
                stability += effectPoints(effect);
            } else if (effect.type() == EnvironmentEffectType.LABEL) {
                signals.add("effect:" + effect.id());
            } else if (effect.type() == EnvironmentEffectType.VETO) {
                signals.add("effect:veto/" + effect.id());
            }
        }

        return new RoomAlchemyContext("", 6, 0, stability, riskBias, Map.of(), signals);
    }

    private static void addSnapshotSignals(EnvironmentSnapshot snapshot, Set<String> signals) {
        if (snapshot.isCovered()) {
            signals.add("environment:covered");
        }
        if (snapshot.isIndoorsLike()) {
            signals.add("environment:indoors_like");
        }
        if (snapshot.isEnclosed()) {
            signals.add("environment:enclosed");
        }
        if (snapshot.temperatureBand() != EnvironmentTemperatureBand.PLEASANT) {
            signals.add("environment:temperature/" + snapshot.temperatureBand().name().toLowerCase(java.util.Locale.ROOT));
        }
        for (EnvironmentAirHazard hazard : snapshot.airHazards()) {
            signals.add("environment:air/" + hazard.name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static int effectPoints(EnvironmentEffect effect) {
        return Math.round(effect.amount() * 100.0F);
    }
}
