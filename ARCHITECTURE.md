# Atelier — Codebase Guide

MC 1.21.1 · NeoForge 21.1.220 · Java 21

---

## Zone Classification System

This is the most complex subsystem. Read this carefully before touching any file under `space/` or `space/zone/`.

### What it does

The mod classifies every loaded chunk's air blocks into OUTSIDE, INSIDE, SOLID, or PARTIAL states. Connected INSIDE regions across chunk boundaries are stitched into a single logical "zone" (e.g. a room in a house). Zone metadata (volume, enclosure score, furniture counts, quality, type) is evaluated and synced to clients so the HUD can display the current zone.

### Pipeline — server side

```
Block change / chunk load
        │
        ▼
ClassificationEventHandler          marks chunk dirty, schedules it
        │
        ▼
ClassificationScheduler.tick()      processes up to CHUNKS_PER_TICK per tick
        │
        ▼
classifyChunk(chunk)
  ├─ 1. Cascade pre-snapshot               snapshot boundary faces before mutation
  ├─ 2. Snapshot oldOwnedIds + blocks       for cleanup + metadata transfer
  ├─ 3. Unmap old in-chunk blocks           cross-chunk blocks left intact
  ├─ 4. Sweep stale solid entries           clean blockToRegion for now-solid blocks
  ├─ 5. ChunkClassifier.classify()          full flood-fill reclassification
  ├─ 6. Restore validated zone interiors    re-stamp INSIDE + BFS-expand into new air
  ├─ 7. Remap UUIDs (stable identity)       match old zones to restored regions
  ├─ 8. Register new regions                populate blockToRegion, set ownedRegionIds
  ├─ 9. Transfer metadata + clean old       preserve custom names, disable empty old regions
  ├─ 10. Stitch with loaded neighbors       merge touching regions across chunk seams
  ├─ 11. Evaluate + sync                    evaluateAndSyncZones + syncToPlayers
  └─ 12. Cascade to neighbors               reclassify adjacent chunks whose boundary faces changed
```

### Pipeline — client side

```
SyncChunkClassificationPayload → ClientChunkClassificationData (bitfield + region list)
SyncZoneDataPayload            → ClientZoneCache (UUID → zone display data)
        │
        ▼
ZoneHudAdapter   looks up the player's block pos in ClientChunkClassificationData,
                 finds the region UUID, then looks it up in ClientZoneCache
        │
        ▼
ZenMeterOverlay  renders the HUD
```

---

### Key classes

| Class | Role |
|---|---|
| `ChunkClassifier` | Full flood-fill per chunk. Marks OUTSIDE from y=319 down, then flood-fills remaining air as INSIDE regions. Captures `lastRegionBlocks` (UUID → Set\<BlockPos\>) for the caller. |
| `ChunkClassificationData` | Per-chunk attachment. Bitfield (2 bits/block) for states + list of `ClassifiedRegion`. Also holds `ownedRegionIds` — the UUIDs born from this chunk's own flood fill (not stitched-in IDs). |
| `ClassifiedRegion` | Lightweight record: UUID, volume, openingArea. Lives in `ChunkClassificationData.regions`. |
| `SpaceRegion` | Server-side region with mutable volume/openingArea. Computes `enclosureScore = 1 - openingArea/(volume*6)`. Lives in `SpaceRegionRegistry`. |
| `SpaceRegionRegistry` | Per-dimension singleton. Three maps: `regions` (UUID→SpaceRegion), `blockToRegion` (BlockPos→UUID), `regionToBlocks` (UUID→Set\<BlockPos\>). All three must stay consistent. |
| `ClassificationScheduler` | Owns the priority work queue (closer chunks first). Drives the full classify→stitch→sync pipeline. |
| `ClassificationEventHandler` | NeoForge event hooks: chunk load (schedule), block place/break/explosion (mark dirty + schedule), level unload (invalidate zone cache). |
| `ZoneRegistry` | Per-dimension singleton. Computed zone cache (UUID→ZoneData) + user metadata map (UUID→customName). Invalidated after merges; re-evaluated lazily. |
| `ZoneEvaluator` | Computes `RoomData` or `OutdoorZoneData` from a `SpaceRegion`. Furniture counts use `regionToBlocks` blocks. |

---

### Cross-chunk stitching — the hardest part

A single room that spans multiple chunks is initially classified as separate regions (one UUID per chunk). `stitchLoadedNeighbors` unifies them.

**How stitch works:**

1. Walk every block on the shared boundary (all Y, all 16 positions along the seam).
2. If both sides are INSIDE or PARTIAL, look up each side's UUID via `blockToRegion`.
3. If they differ and the right-side UUID still exists in `regions`, call `mergeRegions(left, right)`.
   - `mergeRegions` absorbs right into left: adds volume/openingArea, moves all of right's blocks to left in both maps, removes right from `regions`.
   - The `regions.containsKey(right)` guard prevents duplicate events when multiple boundary blocks belong to the same pair.
4. Return `List<MergeEvent>` so the scheduler can update `ChunkClassificationData` on both sides and re-sync.

**After a merge:**

- The *eliminated* UUID is gone from `regions` and `blockToRegion`.
- The neighbor chunk whose data held the eliminated UUID gets `replaceRegionId(eliminated, surviving, ...)` so its region list uses the surviving UUID.
- `ZoneRegistry.invalidate(surviving)` forces re-evaluation with the combined volume.
- `transferMetadata(eliminated, surviving)` preserves any custom name.

**Why `ownedRegionIds` is distinct from `data.regions`:**

After stitching, a chunk's `data.regions` may contain the *neighbor's* surviving UUID (if this chunk's UUID was eliminated). `ownedRegionIds` is set once in `registerClassifiedRegions` and never modified by `replaceRegionId`. This lets cleanup distinguish "UUIDs I created" from "UUIDs I inherited from a neighbor's merge." Only owned UUIDs are candidates for removal on reclassification.

---

### Reclassification lifecycle (block change)

When a block changes in chunk A:

1. `ClassificationEventHandler` marks A dirty and calls `scheduler.scheduleChunk(A.x, A.z)`.
2. `classifyChunk(A)`:
   - Snapshot `oldOwnedIds` and their block sets.
   - **Unmap only A's in-chunk blocks** from old owned regions. Cross-chunk blocks that merged in from neighbors stay — if the old region had B's blocks merged in, those remain so the upcoming stitch can re-merge them into the new region under a new UUID.
   - Full reclassify → register new regions → set new `ownedRegionIds`.
   - **Remove old owned regions only if `regionToBlocks` is now empty.** Non-empty means they still hold a neighbor's blocks from a previous merge; the stitch will merge them into the new region and naturally remove them then.
   - Stitch with loaded neighbors → may update neighbor's `data.regions` and re-sync neighbor.
   - Sync A + evaluate zones.

**Why not remove all old regions eagerly:** If old region A1 had B's blocks merged in, removing A1 also removes B's `blockToRegion` entries. Then when stitching the new A2 with B, `blockToRegion.get(B boundary)` returns null → no merge → two separate zones for the same room.

---

### Invariants — do not break these

1. **`blockToRegion`, `regionToBlocks`, and `regions` must stay in sync.** Always use `mapBlockToRegion` (not direct map puts) to move a block between regions. `mergeRegions` and `unmapBlock` handle the three-way consistency.

2. **`ownedRegionIds` is write-once per classification cycle.** It is set by `registerClassifiedRegions` and must not be updated by `replaceRegionId`. It must only contain UUIDs produced by this chunk's own flood fill.

3. **Never remove a region that still has blocks in `regionToBlocks`.** Those blocks' `blockToRegion` entries would dangle. Check `getBlocksInRegion(id).isEmpty()` before calling `removeRegion`.

4. **`ZoneRegistry.remove` clears all three stores** (cache, dirty, customNames). Do not use `invalidate` when retiring a region — use `remove` so custom names don't leak.

5. **Block change events must call `scheduleChunk`.** Marking dirty alone is not enough — the scheduler only processes chunks in `workQueue`.

6. **Stitch fires from the newly-classified chunk toward all loaded neighbors.** It does not fire symmetrically (the neighbor does not independently re-stitch unless it also reclassifies). This is intentional: the mergeEvent handler updates the neighbor's `data.regions` and re-syncs it without requiring the neighbor to reclassify.

---

### Known technical debt

**Stale `blockToRegion` entries for now-solid blocks.** When a room partially closes (some INSIDE blocks become solid), those positions are not re-mapped by the new classification (only new INSIDE blocks get `mapBlockToRegion` called). The old entries linger in `blockToRegion`. They are harmless — stitch and `getRegionAt` both gate on state checks (`shouldMergeRegions`, `switch(state)`) so SOLID blocks are never looked up — but they waste memory. A full sweep of the chunk's `blockToRegion` entries after reclassification would fix this.

**`ZoneEvaluator` TODO comment.** The stale "regionToBlocks is not yet populated" comment was accurate before the stitch fix but is now out of date — `blockToRegion` and `regionToBlocks` are fully populated. The TODO can be removed; furniture counting works.

---

### Adding new zone metadata

User-settable metadata (custom names, etc.) lives in `ZoneRegistry.customNames`, separate from the computed `cache`. Use `setCustomName` / `getCustomName`. The `transferMetadata(from, to)` method must be called anywhere a UUID changes (stitch merge, reclassification match). See `ClassificationScheduler.transferZoneMetadata` for the spatial-overlap matching used during reclassification.
