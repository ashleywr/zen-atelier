package com.sanhiruzu.atelier.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public final class ZoneVfxManager {
    private static final int BOUNDS_DISPLAY_MS = 10000;
    private static final int PARTICLE_RING_SPACING = 2;

    private static long showBoundsUntilMs = -1;
    private static final Queue<ParticleEmission> particleQueue = new LinkedList<>();

    public static void onZoneRegistered(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        try {
            queueParticleRing(minX, minY, minZ, maxX, maxY, maxZ, ParticleTypes.END_ROD, 0.0, 0.05, 0.0);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Particle types not initialized (unit test or headless environment)
        }
    }

    public static void onQualityChanged(float oldQuality, float newQuality, int triggerX, int triggerY, int triggerZ) {
        try {
            if (Minecraft.getInstance().level == null) return;
            boolean improved = newQuality > oldQuality;
            queueQualityBurst(triggerX + 0.5, triggerY + 0.5, triggerZ + 0.5, improved);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Particle types not initialized (unit test or headless environment)
        }
    }

    private static void queueQualityBurst(double cx, double cy, double cz, boolean improved) {
        if (Minecraft.getInstance().level == null) return;
        Random rng = new Random();
        var particleType = improved ? ParticleTypes.END_ROD : ParticleTypes.CAMPFIRE_COSY_SMOKE;
        double vy = improved ? 0.12 : -0.06;
        for (int i = 0; i < 24; i++) {
            double ox = (rng.nextDouble() - 0.5) * 0.8;
            double oy = (rng.nextDouble() - 0.5) * 0.4;
            double oz = (rng.nextDouble() - 0.5) * 0.8;
            double vx = (rng.nextDouble() - 0.5) * 0.05;
            double vz = (rng.nextDouble() - 0.5) * 0.05;
            particleQueue.offer(new ParticleEmission(cx + ox, cy + oy, cz + oz, vx, vy, vz, particleType));
        }
    }

    public static void onZoneExpired(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        try {
            queueParticleRing(minX, minY, minZ, maxX, maxY, maxZ, ParticleTypes.SMOKE, 0.0, -0.05, 0.0);
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Particle types not initialized (unit test or headless environment)
        }
    }

    private static void queueParticleRing(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                          net.minecraft.core.particles.ParticleOptions particleType,
                                          double vx, double vy, double vz) {
        // Only queue particles if a level is available (not in unit tests or headless environments)
        if (Minecraft.getInstance().level == null) return;

        int floorY = minY;
        for (int x = minX; x <= maxX; x += PARTICLE_RING_SPACING) {
            particleQueue.offer(new ParticleEmission(x + 0.5, floorY + 0.1, minZ + 0.5, vx, vy, vz, particleType));
            particleQueue.offer(new ParticleEmission(x + 0.5, floorY + 0.1, maxZ + 0.5, vx, vy, vz, particleType));
        }
        for (int z = minZ; z <= maxZ; z += PARTICLE_RING_SPACING) {
            particleQueue.offer(new ParticleEmission(minX + 0.5, floorY + 0.1, z + 0.5, vx, vy, vz, particleType));
            particleQueue.offer(new ParticleEmission(maxX + 0.5, floorY + 0.1, z + 0.5, vx, vy, vz, particleType));
        }
    }

    public static void showBoundsTemporarily() {
        showBoundsUntilMs = System.currentTimeMillis() + BOUNDS_DISPLAY_MS;
    }

    public static boolean isBoundsVisible() {
        return showBoundsUntilMs > System.currentTimeMillis();
    }

    public static float getBoundsAlpha() {
        if (!isBoundsVisible()) return 0.0f;
        long remainingMs = showBoundsUntilMs - System.currentTimeMillis();
        float alpha = Math.min(1.0f, remainingMs / (float) BOUNDS_DISPLAY_MS);
        return Math.max(0.0f, alpha);
    }

    public static void drainParticleQueue() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        while (!particleQueue.isEmpty()) {
            ParticleEmission emission = particleQueue.poll();
            level.addParticle(emission.particleType, emission.x, emission.y, emission.z, emission.vx, emission.vy, emission.vz);
        }
    }

    public static void clearBounds() {
        showBoundsUntilMs = -1;
    }

    private record ParticleEmission(double x, double y, double z, double vx, double vy, double vz,
                                    net.minecraft.core.particles.ParticleOptions particleType) {
    }

    private ZoneVfxManager() {
    }
}
