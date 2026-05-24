package com.sanhiruzu.atelier.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanhiruzu.atelier.space.SpaceRegionRegistry;
import com.sanhiruzu.atelier.space.zone.Zone;
import com.sanhiruzu.atelier.space.zone.ZoneRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class DebugRenderer {
    private static final double RENDER_DISTANCE = 128.0;

    public static void renderDebug(PoseStack stack, MultiBufferSource buffers, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        // In single-player the render thread shares the JVM with the integrated server,
        // so server-side registries are directly readable here.
        SpaceRegionRegistry registry = SpaceRegionRegistry.get(mc.level);
        ZoneRegistry zoneRegistry = ZoneRegistry.get(mc.level);
        Set<UUID> renderedZones = new HashSet<>();

        // Render one stable-color wireframe per live zone. We intentionally render
        // bounds instead of every air block so zone edges and overlaps are readable.
        try {
            for (Zone zone : zoneRegistry.getAllZones()) {
                if (zone.isDissolved()) continue;
                Set<BlockPos> blocks = registry.getBlocksInRegion(zone.getId());
                if (blocks.isEmpty()) {
                    blocks = zone.getInteriorBlocks();
                }
                if (blocks.isEmpty()) continue;

                renderBlockBoundaryWireframe(stack, buffers, blocks, cameraPos, colorForZone(zone.getId()));
                renderedZones.add(zone.getId());
            }
        } catch (ConcurrentModificationException e) {
            // Registry being modified on server thread — skip this frame
        }

        // Client cache fallback for multiplayer/lag frames where server block data
        // is unavailable on the render thread.
        try {
            for (com.sanhiruzu.atelier.space.zone.ZoneData zone : ClientZoneCache.getAllZones()) {
                UUID zoneId = zone.getRegionId();
                if (renderedZones.contains(zoneId)) continue;

                Set<BlockPos> blocks = registry.getBlocksInRegion(zoneId);
                if (!blocks.isEmpty()) {
                    renderBlockBoundaryWireframe(stack, buffers, blocks, cameraPos, colorForZone(zoneId));
                } else if (zone.hasSpatialExtent()) {
                    renderBox(stack, buffers,
                            zone.getMinX(), zone.getMinY(), zone.getMinZ(),
                            zone.getMaxX() + 1, zone.getMaxY() + 1, zone.getMaxZ() + 1,
                            cameraPos, colorForZone(zoneId));
                }
            }
        } catch (ConcurrentModificationException e) {
            // Skip frame
        }
    }

    private static void renderBlockBoundaryWireframe(PoseStack stack, MultiBufferSource buffers, Set<BlockPos> blocks, Vec3 cameraPos, int color) {
        if (blocks.isEmpty()) return;
        if (isOutOfRange(blocks, cameraPos)) return;

        Set<BlockPos> blockLookup = blocks instanceof HashSet<BlockPos> ? blocks : new HashSet<>(blocks);
        Map<FacePlaneKey, Set<EdgeKey>> planeEdges = new HashMap<>();

        for (BlockPos pos : blocks) {
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                if (!blockLookup.contains(pos.relative(dir))) {
                    addFaceEdges(planeEdges, pos, dir);
                }
            }
        }

        Set<EdgeKey> edges = new HashSet<>();
        for (Set<EdgeKey> plane : planeEdges.values()) {
            edges.addAll(plane);
        }
        renderEdges(stack, buffers, edges, cameraPos, color);
    }

    private static boolean isOutOfRange(Set<BlockPos> blocks, Vec3 cameraPos) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (BlockPos pos : blocks) {
            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getY() < minY) minY = pos.getY();
            if (pos.getY() > maxY) maxY = pos.getY();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();
        }
        double cx = (minX + maxX + 1) / 2;
        double cy = (minY + maxY + 1) / 2;
        double cz2 = (minZ + maxZ + 1) / 2;
        double dx = Math.abs(cx - cameraPos.x) - (maxX - minX + 1) / 2.0;
        double dy = Math.abs(cy - cameraPos.y) - (maxY - minY + 1) / 2.0;
        double dz = Math.abs(cz2 - cameraPos.z) - (maxZ - minZ + 1) / 2.0;
        double distSq = Math.max(0, dx) * Math.max(0, dx) + Math.max(0, dy) * Math.max(0, dy) + Math.max(0, dz) * Math.max(0, dz);
        return distSq > RENDER_DISTANCE * RENDER_DISTANCE;
    }

    private static void addFaceEdges(Map<FacePlaneKey, Set<EdgeKey>> planeEdges,
                                     BlockPos pos,
                                     net.minecraft.core.Direction dir) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        FacePlaneKey plane;
        EdgeKey e1;
        EdgeKey e2;
        EdgeKey e3;
        EdgeKey e4;

        switch (dir) {
            case DOWN -> {
                plane = new FacePlaneKey('Y', y);
                e1 = new EdgeKey(x, y, z, x + 1, y, z);
                e2 = new EdgeKey(x + 1, y, z, x + 1, y, z + 1);
                e3 = new EdgeKey(x + 1, y, z + 1, x, y, z + 1);
                e4 = new EdgeKey(x, y, z + 1, x, y, z);
            }
            case UP -> {
                plane = new FacePlaneKey('Y', y + 1);
                e1 = new EdgeKey(x, y + 1, z, x + 1, y + 1, z);
                e2 = new EdgeKey(x + 1, y + 1, z, x + 1, y + 1, z + 1);
                e3 = new EdgeKey(x + 1, y + 1, z + 1, x, y + 1, z + 1);
                e4 = new EdgeKey(x, y + 1, z + 1, x, y + 1, z);
            }
            case NORTH -> {
                plane = new FacePlaneKey('Z', z);
                e1 = new EdgeKey(x, y, z, x + 1, y, z);
                e2 = new EdgeKey(x + 1, y, z, x + 1, y + 1, z);
                e3 = new EdgeKey(x + 1, y + 1, z, x, y + 1, z);
                e4 = new EdgeKey(x, y + 1, z, x, y, z);
            }
            case SOUTH -> {
                plane = new FacePlaneKey('Z', z + 1);
                e1 = new EdgeKey(x, y, z + 1, x + 1, y, z + 1);
                e2 = new EdgeKey(x + 1, y, z + 1, x + 1, y + 1, z + 1);
                e3 = new EdgeKey(x + 1, y + 1, z + 1, x, y + 1, z + 1);
                e4 = new EdgeKey(x, y + 1, z + 1, x, y, z + 1);
            }
            case WEST -> {
                plane = new FacePlaneKey('X', x);
                e1 = new EdgeKey(x, y, z, x, y + 1, z);
                e2 = new EdgeKey(x, y + 1, z, x, y + 1, z + 1);
                e3 = new EdgeKey(x, y + 1, z + 1, x, y, z + 1);
                e4 = new EdgeKey(x, y, z + 1, x, y, z);
            }
            case EAST -> {
                plane = new FacePlaneKey('X', x + 1);
                e1 = new EdgeKey(x + 1, y, z, x + 1, y + 1, z);
                e2 = new EdgeKey(x + 1, y + 1, z, x + 1, y + 1, z + 1);
                e3 = new EdgeKey(x + 1, y + 1, z + 1, x + 1, y, z + 1);
                e4 = new EdgeKey(x + 1, y, z + 1, x + 1, y, z);
            }
            default -> throw new IllegalStateException("Unhandled direction: " + dir);
        }

        Set<EdgeKey> edges = planeEdges.computeIfAbsent(plane, ignored -> new HashSet<>());
        toggle(edges, e1);
        toggle(edges, e2);
        toggle(edges, e3);
        toggle(edges, e4);
    }

    private static void toggle(Set<EdgeKey> edges, EdgeKey edge) {
        if (!edges.add(edge)) {
            edges.remove(edge);
        }
    }

    private static void renderEdges(PoseStack stack, MultiBufferSource buffers, Set<EdgeKey> edges, Vec3 cameraPos, int color) {
        if (edges.isEmpty()) return;

        stack.pushPose();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        float r = ((color >> 16) & 255) / 255.0f;
        float g = ((color >> 8) & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        for (EdgeKey edge : edges) {
            renderEdge(stack, consumer, edge, r, g, b);
        }

        stack.popPose();
    }

    private static void renderEdge(PoseStack stack, VertexConsumer consumer, EdgeKey edge, float r, float g, float b) {
        float nx = Integer.compare(edge.x2, edge.x1);
        float ny = Integer.compare(edge.y2, edge.y1);
        float nz = Integer.compare(edge.z2, edge.z1);
        consumer.addVertex(stack.last().pose(), edge.x1, edge.y1, edge.z1).setColor(r, g, b, 1.0f).setNormal(stack.last(), nx, ny, nz);
        consumer.addVertex(stack.last().pose(), edge.x2, edge.y2, edge.z2).setColor(r, g, b, 1.0f).setNormal(stack.last(), nx, ny, nz);
    }

    public static int colorForZone(UUID zoneId) {
        long hash = zoneId.getMostSignificantBits() ^ Long.rotateLeft(zoneId.getLeastSignificantBits(), 21);
        float hue = ((hash & 0xFFFF) / 65535.0f);
        float saturation = 0.70f + (((hash >>> 16) & 0xFF) / 255.0f) * 0.25f;
        float value = 0.85f + (((hash >>> 24) & 0xFF) / 255.0f) * 0.15f;
        return hsvToRgb(hue, saturation, value);
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) Math.floor(h);
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * f);
        float t = value * (1.0f - saturation * (1.0f - f));

        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = value;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = value;
                b = p;
            }
            case 2 -> {
                r = p;
                g = value;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = value;
            }
            case 4 -> {
                r = t;
                g = p;
                b = value;
            }
            default -> {
                r = value;
                g = p;
                b = q;
            }
        }

        return ((int) (r * 255.0f) << 16)
                | ((int) (g * 255.0f) << 8)
                | (int) (b * 255.0f);
    }

    private static void renderBox(PoseStack stack, MultiBufferSource buffers,
                                  double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ,
                                  Vec3 cameraPos, int color) {
        stack.pushPose();
        stack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        var consumer = buffers.getBuffer(RenderType.lines());
        float r = ((color >> 16) & 255) / 255.0f;
        float g = ((color >> 8) & 255) / 255.0f;
        float b = (color & 255) / 255.0f;

        LevelRenderer.renderLineBox(stack, consumer, minX, minY, minZ, maxX, maxY, maxZ, r, g, b, 1.0f);

        stack.popPose();
    }

    private record FacePlaneKey(char axis, int coordinate) {
    }

    private record EdgeKey(int x1, int y1, int z1, int x2, int y2, int z2) {
        EdgeKey {
            boolean swap = x1 > x2
                    || (x1 == x2 && y1 > y2)
                    || (x1 == x2 && y1 == y2 && z1 > z2);
            if (swap) {
                int tx = x1;
                int ty = y1;
                int tz = z1;
                x1 = x2;
                y1 = y2;
                z1 = z2;
                x2 = tx;
                y2 = ty;
                z2 = tz;
            }
        }
    }
}
