package com.sanhiruzu.atelier.synthesis.vfx.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

/** A value that is either a single number or a 4-entry per-quality-tier array. */
public final class TieredValue {
    private final float[] values; // length 1 (scalar) or 4 (per tier)

    private TieredValue(float[] values) {
        this.values = values;
    }

    public static TieredValue scalar(float v) {
        return new TieredValue(new float[]{v});
    }

    public float floatAt(int qt) {
        if (values.length == 1) {
            return values[0];
        }
        return values[Math.max(0, Math.min(qt, 3))];
    }

    public int intAt(int qt) {
        return Math.round(floatAt(qt));
    }

    public static final Codec<TieredValue> CODEC = Codec.either(Codec.FLOAT, Codec.FLOAT.listOf())
            .comapFlatMap(
                    either -> either.map(
                            scalar -> DataResult.success(new TieredValue(new float[]{scalar})),
                            list -> list.size() == 4
                                    ? DataResult.success(new TieredValue(new float[]{list.get(0), list.get(1), list.get(2), list.get(3)}))
                                    : DataResult.error(() -> "tier array must have exactly 4 entries, got " + list.size())
                    ),
                    tv -> tv.values.length == 1
                            ? Either.left(tv.values[0])
                            : Either.right(List.of(tv.values[0], tv.values[1], tv.values[2], tv.values[3]))
            );
}
