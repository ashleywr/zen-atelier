package com.sanhiruzu.atelier.gametest;

import com.sanhiruzu.atelier.space.zone.OutdoorZoneData;
import com.sanhiruzu.atelier.space.zone.RoomData;
import com.sanhiruzu.atelier.space.zone.ZoneData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent assertion builder for ZoneData in GameTests.
 *
 * <p>Only checks fields you explicitly set. UUID, generated names, epithets, and
 * disabled/initialized state are always ignored — these either change every run or
 * are irrelevant to zone correctness.</p>
 *
 * <pre>{@code
 * // Assert the zone has a bed and decent quality:
 * ZoneAssertion.expect()
 *     .indoor()
 *     .volume(16)
 *     .enclosureAbove(0.75f)
 *     .furniture("minecraft:oak_bed", 1)
 *     .qualityAbove(0.3f)
 *     .check(helper, zone, "after placing bed");
 *
 * // Assert nothing changed structurally after picking the bed back up:
 * helper.assertTrue(ZoneAssertion.equivalent(before, after),
 *     "zone must be identical after bed removal");
 * }</pre>
 */
public final class ZoneAssertion {

    private static final int UNSET_INT = Integer.MIN_VALUE;
    private static final float UNSET_FLOAT = Float.NaN;

    // --- fields that may be set ---
    @Nullable
    private Boolean indoor;
    private int volume = UNSET_INT;
    private float enclosureMin = UNSET_FLOAT;
    private float qualityMin = UNSET_FLOAT;
    private float qualityMax = UNSET_FLOAT;
    @Nullable
    private Boolean degraded;
    private boolean zoneTypeSet;
    @Nullable
    private ResourceLocation zoneType;
    private final Map<String, Integer> requiredFurniture = new LinkedHashMap<>();
    private final Map<String, Integer> forbiddenFurniture = new LinkedHashMap<>();

    private ZoneAssertion() {
    }

    public static ZoneAssertion expect() {
        return new ZoneAssertion();
    }

    // ---- builder methods ----

    public ZoneAssertion indoor() {
        indoor = true;
        return this;
    }

    public ZoneAssertion outdoor() {
        indoor = false;
        return this;
    }

    public ZoneAssertion volume(int expected) {
        volume = expected;
        return this;
    }

    public ZoneAssertion enclosureAbove(float min) {
        enclosureMin = min;
        return this;
    }

    public ZoneAssertion qualityAbove(float min) {
        qualityMin = min;
        return this;
    }

    public ZoneAssertion qualityBelow(float max) {
        qualityMax = max;
        return this;
    }

    public ZoneAssertion qualityBetween(float min, float max) {
        qualityMin = min;
        qualityMax = max;
        return this;
    }

    public ZoneAssertion degraded() {
        degraded = true;
        return this;
    }

    public ZoneAssertion notDegraded() {
        degraded = false;
        return this;
    }

    /**
     * Assert the zone matched this zone type id, e.g. {@code "zen_atelier:bedroom"}.
     */
    public ZoneAssertion type(String id) {
        zoneType = ResourceLocation.parse(id);
        zoneTypeSet = true;
        return this;
    }

    public ZoneAssertion type(ResourceLocation id) {
        zoneType = id;
        zoneTypeSet = true;
        return this;
    }

    /**
     * Assert no zone type was matched (generic room).
     */
    public ZoneAssertion noType() {
        zoneType = null;
        zoneTypeSet = true;
        return this;
    }

    /**
     * Assert exactly {@code count} instances of {@code blockId} in the zone's furniture map.
     */
    public ZoneAssertion furniture(String blockId, int count) {
        requiredFurniture.put(blockId, count);
        return this;
    }

    /**
     * Assert {@code blockId} is absent from the zone's furniture map.
     */
    public ZoneAssertion noFurniture(String blockId) {
        forbiddenFurniture.put(blockId, 0);
        return this;
    }

    // ---- assertion ----

    /**
     * Runs all configured checks against {@code zone}.
     * Fails the GameTest with a descriptive message on the first mismatch.
     *
     * @param helper the GameTestHelper (provides fail/assertTrue)
     * @param zone   the zone to check — fails immediately if null
     * @param label  a short description included in every failure message for context
     */
    public void check(GameTestHelper helper, @Nullable ZoneData zone, String label) {
        if (zone == null) {
            helper.fail("[" + label + "] zone is null");
            return;
        }

        // ---- base ZoneData fields ----
        if (indoor != null) {
            helper.assertTrue(zone.isOutdoor() == !indoor,
                    "[" + label + "] expected " + (indoor ? "indoor" : "outdoor")
                            + " but zone isOutdoor=" + zone.isOutdoor());
        }
        if (volume != UNSET_INT) {
            helper.assertTrue(zone.getVolume() == volume,
                    "[" + label + "] volume: expected " + volume + " but got " + zone.getVolume());
        }
        if (!Float.isNaN(enclosureMin)) {
            helper.assertTrue(zone.getEnclosureScore() >= enclosureMin,
                    "[" + label + "] enclosureScore: expected >= " + enclosureMin
                            + " but got " + zone.getEnclosureScore());
        }

        // ---- RoomData fields ----
        if (zone instanceof RoomData room) {
            if (!Float.isNaN(qualityMin)) {
                helper.assertTrue(room.getQuality() >= qualityMin,
                        "[" + label + "] quality: expected >= " + qualityMin
                                + " but got " + room.getQuality());
            }
            if (!Float.isNaN(qualityMax)) {
                helper.assertTrue(room.getQuality() <= qualityMax,
                        "[" + label + "] quality: expected <= " + qualityMax
                                + " but got " + room.getQuality());
            }
            if (degraded != null) {
                helper.assertTrue(room.isDegraded() == degraded,
                        "[" + label + "] degraded: expected " + degraded
                                + " but got " + room.isDegraded());
            }
            if (zoneTypeSet) {
                ResourceLocation actual = room.getZoneTypeId();
                if (zoneType == null) {
                    helper.assertTrue(actual == null,
                            "[" + label + "] zoneType: expected null but got " + actual);
                } else {
                    helper.assertTrue(zoneType.equals(actual),
                            "[" + label + "] zoneType: expected " + zoneType + " but got " + actual);
                }
            }
            Map<String, Integer> counts = room.getFurnitureCounts();
            for (Map.Entry<String, Integer> entry : requiredFurniture.entrySet()) {
                int actual = counts.getOrDefault(entry.getKey(), 0);
                helper.assertTrue(actual == entry.getValue(),
                        "[" + label + "] furniture[" + entry.getKey() + "]: expected "
                                + entry.getValue() + " but got " + actual);
            }
            for (String key : forbiddenFurniture.keySet()) {
                int actual = counts.getOrDefault(key, 0);
                helper.assertTrue(actual == 0,
                        "[" + label + "] furniture[" + key + "]: expected 0 but got " + actual);
            }
        } else if (zone instanceof OutdoorZoneData outdoor) {
            // Quality and furniture assertions on an OutdoorZoneData are always treated as pass
            // unless the caller also set indoor() which would have already failed above.
            Map<String, Integer> counts = outdoor.getFurnitureCounts();
            for (Map.Entry<String, Integer> entry : requiredFurniture.entrySet()) {
                int actual = counts.getOrDefault(entry.getKey(), 0);
                helper.assertTrue(actual == entry.getValue(),
                        "[" + label + "] furniture[" + entry.getKey() + "]: expected "
                                + entry.getValue() + " but got " + actual);
            }
            for (String key : forbiddenFurniture.keySet()) {
                int actual = counts.getOrDefault(key, 0);
                helper.assertTrue(actual == 0,
                        "[" + label + "] furniture[" + key + "]: expected 0 but got " + actual);
            }
        }
    }

    // ---- equivalence ----

    /**
     * Returns true when two zones are structurally identical, ignoring fields that
     * are non-deterministic or identity-only:
     * <ul>
     *   <li>{@code regionId} (UUID) — always different across bootstrap cycles</li>
     *   <li>{@code generatedName} — procedurally generated, may vary</li>
     *   <li>{@code epithetName} — awarded at evaluation time, may vary</li>
     *   <li>{@code disabled} / {@code initialized} — lifecycle state, not zone content</li>
     *   <li>spatial extent — position in world, not zone identity</li>
     * </ul>
     *
     * <p>Compared fields: {@code volume}, {@code enclosureScore} (within 0.001),
     * {@code isOutdoor}, {@code quality}, {@code degraded}, {@code zoneTypeId},
     * {@code furnitureCounts}, {@code signalCounts}.</p>
     */
    public static boolean equivalent(@Nullable ZoneData a, @Nullable ZoneData b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.getClass() != b.getClass()) return false;
        if (a.isOutdoor() != b.isOutdoor()) return false;
        if (a.getVolume() != b.getVolume()) return false;
        if (Math.abs(a.getEnclosureScore() - b.getEnclosureScore()) > 0.001f) return false;

        if (a instanceof RoomData ra && b instanceof RoomData rb) {
            if (Math.abs(ra.getQuality() - rb.getQuality()) > 0.001f) return false;
            if (ra.isDegraded() != rb.isDegraded()) return false;
            if (!Objects.equals(ra.getZoneTypeId(), rb.getZoneTypeId())) return false;
            if (!ra.getFurnitureCounts().equals(rb.getFurnitureCounts())) return false;
            if (!ra.getSignalCounts().equals(rb.getSignalCounts())) return false;
        }

        if (a instanceof OutdoorZoneData oa && b instanceof OutdoorZoneData ob) {
            if (!oa.getFurnitureCounts().equals(ob.getFurnitureCounts())) return false;
            if (!oa.getSignalCounts().equals(ob.getSignalCounts())) return false;
        }

        return true;
    }

    /**
     * Convenience overload: fails the GameTest if the two zones are not equivalent.
     */
    public static void assertEquivalent(GameTestHelper helper,
                                        @Nullable ZoneData expected,
                                        @Nullable ZoneData actual,
                                        String label) {
        if (!equivalent(expected, actual)) {
            String diff = describeDifference(expected, actual);
            helper.fail("[" + label + "] zones not equivalent: " + diff);
        }
    }

    // ---- internal diff helper ----

    private static String describeDifference(@Nullable ZoneData a, @Nullable ZoneData b) {
        if (a == null) return "expected is null";
        if (b == null) return "actual is null";
        if (a.getClass() != b.getClass())
            return "type mismatch: " + a.getClass().getSimpleName() + " vs " + b.getClass().getSimpleName();

        StringBuilder sb = new StringBuilder();
        if (a.isOutdoor() != b.isOutdoor())
            sb.append("isOutdoor(").append(a.isOutdoor()).append("!=").append(b.isOutdoor()).append(") ");
        if (a.getVolume() != b.getVolume())
            sb.append("volume(").append(a.getVolume()).append("!=").append(b.getVolume()).append(") ");
        if (Math.abs(a.getEnclosureScore() - b.getEnclosureScore()) > 0.001f)
            sb.append("enclosure(").append(String.format("%.3f", a.getEnclosureScore()))
                    .append("!=").append(String.format("%.3f", b.getEnclosureScore())).append(") ");

        if (a instanceof RoomData ra && b instanceof RoomData rb) {
            if (Math.abs(ra.getQuality() - rb.getQuality()) > 0.001f)
                sb.append("quality(").append(String.format("%.3f", ra.getQuality()))
                        .append("!=").append(String.format("%.3f", rb.getQuality())).append(") ");
            if (ra.isDegraded() != rb.isDegraded())
                sb.append("degraded(").append(ra.isDegraded()).append("!=").append(rb.isDegraded()).append(") ");
            if (!Objects.equals(ra.getZoneTypeId(), rb.getZoneTypeId()))
                sb.append("type(").append(ra.getZoneTypeId()).append("!=").append(rb.getZoneTypeId()).append(") ");
            if (!ra.getFurnitureCounts().equals(rb.getFurnitureCounts()))
                sb.append("furniture(").append(ra.getFurnitureCounts())
                        .append("!=").append(rb.getFurnitureCounts()).append(") ");
            if (!ra.getSignalCounts().equals(rb.getSignalCounts()))
                sb.append("signals(").append(ra.getSignalCounts())
                        .append("!=").append(rb.getSignalCounts()).append(") ");
        }

        return sb.isEmpty() ? "(no difference found — floating point tolerance may differ)" : sb.toString().trim();
    }
}
