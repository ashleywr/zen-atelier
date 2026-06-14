package com.sanhiruzu.atelier.space.analyze;

import java.util.List;

public record AnalysisResult(int chunkX, int chunkZ, long snapshotVersion, List<MicroRegion> microRegions) {}
