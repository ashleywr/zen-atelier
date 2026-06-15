package com.sanhiruzu.atelier.synthesis.vfx;

/**
 * Per-tier tuning for an eruption-style impact effect, indexed by quality tier 0..3.
 * One instance per effect (e.g. {@link #ICE}); this is the single place to tweak how
 * an effect scales: qt0 = small sparse, qt3 = huge dense.
 */
public final class EruptionTuning {
    private final int[] crystalCount;
    private final double[] ringRadius;
    private final float[] crystalPeakScale;
    private final int[] crystalLifetime;
    private final float[] burstPeakScale;
    private final int[] accentCount;

    public EruptionTuning(int[] crystalCount, double[] ringRadius, float[] crystalPeakScale,
                          int[] crystalLifetime, float[] burstPeakScale, int[] accentCount) {
        this.crystalCount = crystalCount;
        this.ringRadius = ringRadius;
        this.crystalPeakScale = crystalPeakScale;
        this.crystalLifetime = crystalLifetime;
        this.burstPeakScale = burstPeakScale;
        this.accentCount = accentCount;
    }

    private static int clamp(int qt) {
        return Math.max(0, Math.min(qt, 3));
    }

    public int    crystalCount(int qt)     { return crystalCount[clamp(qt)]; }
    public double ringRadius(int qt)       { return ringRadius[clamp(qt)]; }
    public float  crystalPeakScale(int qt) { return crystalPeakScale[clamp(qt)]; }
    public int    crystalLifetime(int qt)  { return crystalLifetime[clamp(qt)]; }
    public float  burstPeakScale(int qt)   { return burstPeakScale[clamp(qt)]; }
    public int    accentCount(int qt)      { return accentCount[clamp(qt)]; }

    // qt:                                       0     1     2      3
    public static final EruptionTuning ICE = new EruptionTuning(
            new int[]    {4,    7,    11,    16},
            new double[] {1.2,  1.8,  2.5,   3.2},
            new float[]  {1.0f, 1.4f, 1.9f,  2.6f},
            new int[]    {12,   14,   16,    20},
            new float[]  {1.5f, 2.2f, 3.0f,  4.0f},
            new int[]    {6,    10,   16,    26}
    );
}
