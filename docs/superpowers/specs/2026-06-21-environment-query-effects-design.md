# Environment Query and Effect Rules Design

## Context

Atelier's old room and zone system tried to identify room types and score room quality. That model is too rigid for the next direction. Blocks, items, recipes, and integrations need to ask focused questions about their surroundings and apply their own bonuses, debuffs, or other effects.

The old `room_profiles`, `zones`, journal content, and disabled integration stubs will remain in place for now. This design adds a separate API beside that dormant system. Removing or migrating the old room model is explicitly out of scope for this pass.

## Goals

- Let internal code and other mods query local environment state without knowing how to scan the world.
- Support declarative block/action effect registrations so mod authors do not maintain their own good/bad block classifications or enclosure scans.
- Keep advanced Java callbacks available for cases that cannot be expressed declaratively.
- Make repeated queries performant through caching and invalidation.
- Provide reusable facts such as covered, indoors-like, enclosed, temperature band, air hazards, named block signal counts, and nearby entity presence.

## Non-Goals

- Do not score room quality.
- Do not classify a location as a bedroom, kitchen, smithy, or other room type.
- Do not remove or rewrite the old room journal, room profile data, or MineColonies room bonus stubs in this pass.
- Do not expose raw BFS or scan implementation details to addon authors.

## Public API Shape

`ZoneAPI` remains the public facade and gains a snapshot API:

```java
EnvironmentSnapshot snapshot = ZoneAPI.environmentAt(level, pos, radius);
```

`EnvironmentSnapshot` is immutable from the caller's perspective and answers direct questions:

- `countSignal(String signal)`
- `hasSignal(String signal)`
- `hasSignalAtLeast(String signal, int count)`
- `isCovered()`
- `isIndoorsLike()`
- `isEnclosed()`
- `temperatureBand()`
- `hasAirHazard(EnvironmentAirHazard hazard)`
- `nearEntity(EntityType<?> type)`
- `nearEntityAtLeast(EntityType<?> type, int count)`

`ZoneAPI.hasNearby(...)` stays available for compatibility, but new code should prefer `environmentAt(...)`.

## Effect Registration

Atelier will provide an `EnvironmentEffectRegistry` for Java registration:

```java
ZoneAPI.registerEnvironmentEffects(registrar -> registrar
        .forBlockTag(METALLURGY_BLOCKS)
        .when(EnvironmentConditions.signalAtLeast("smithing_or_repair_block", 2))
        .then(EnvironmentEffects.modifySpeed("metallurgy", 0.15f)));
```

A rule has four parts:

- **Target**: block, block tag, action id, recipe category, or custom predicate.
- **Context**: where and when the rule should run, such as block tick, block use, sleep check, recipe craft, smelt, or synthesis action.
- **Conditions**: declarative checks against `EnvironmentSnapshot`.
- **Effects**: typed outputs consumed by the caller or a callback for custom logic.

Declarative rules are the default path. Callback rules receive the same snapshot and a contextual object, but they still rely on Atelier's cached scan results instead of running their own world search.

## Built-In Conditions

The first pass will include conditions for:

- Required or forbidden named block signals.
- Minimum and maximum signal counts.
- Covered, indoors-like, and enclosed checks.
- Temperature bands: cold, cool, pleasant, warm, hot.
- Air hazards: smoky, lava heat, fire particles, damp, dusty.
- Nearby entity type counts, including cats and frogs.
- Absence of negative signals such as dirt-like, mossy, rough stone, or industrial metal around contexts where those matter.

Existing `Signals` predicates remain the source of named block classifications. New environment-specific signals can be added there or in a small adjacent registry if `Signals` becomes too room-profile-oriented.

## Built-In Effect Types

The first pass will define typed effect outputs rather than hardcoding room quality:

- Speed modifier.
- Yield modifier.
- Risk modifier.
- Stability modifier.
- Comfort modifier.
- Mob effect application request.
- Veto or requirement failure.
- Informational labels for UI/debug output.
- Custom callback result.

Callers choose which effect types they honor. For example, a furnace recipe can consume speed and yield modifiers, a bed can consume comfort and veto results, and the synthesis cauldron can consume stability and risk modifiers.

## Environment Facts

The scanning layer owns reusable local facts:

- **Covered**: there is meaningful overhead cover above the queried position.
- **Indoors-like**: the position is covered and has enough nearby boundary mass to behave like an interior without requiring perfect sealing.
- **Enclosed**: a bounded flood fill or equivalent constrained search suggests the local air volume is closed enough for atmosphere-sensitive effects.
- **Temperature band**: derived from nearby heat and cold signals, with neutral default.
- **Air hazards**: derived from lava, fire, campfire, furnace, smoke-like, damp, and dusty signals.
- **Entity presence**: nearby counts for requested entity types.

These facts are intentionally local and action-oriented. They do not produce a named room or global room object.

## Caching and Invalidation

Environment snapshots are cached per level, center block position, radius, and scan profile. A scan profile distinguishes block-only queries from entity-sensitive queries so static facts can stay cached longer.

Block updates mark nearby cache entries dirty. The invalidation radius must cover the largest supported query radius. Entity-sensitive facts either use a short TTL or explicit dirty marks from spawn, despawn, and movement hooks. This keeps common block-only calls cheap while still supporting familiars and other nearby entities.

The cache must fail safe. If an entry is missing, dirty, expired, or from a different level lifecycle, the next query recomputes it.

## Data Flow

1. A block, action, recipe, or integration requests `ZoneAPI.environmentAt(...)` or asks the effect registry to evaluate rules for a context.
2. The cache returns a valid snapshot or performs a bounded scan.
3. The snapshot exposes named counts and derived facts.
4. Declarative rules evaluate against the snapshot.
5. Matching rules return typed effects.
6. The caller applies only the effect types relevant to that action.

## Example Uses

- A bed checks for cover, indoors-like enclosure, carpet, warmth, and absence of damp or rough negative signals.
- A furnace producing ingots checks for metallurgy signals such as anvils, smithing tables, blast furnaces, and metal material blocks.
- A cooking action distinguishes kitchen-like signals from heavy metal or industrial surroundings.
- The synthesis cauldron checks for bookshelves, candles, cats, frogs, and air hazards before adjusting stability or risk.

## Testing

Unit tests should cover:

- Signal counts from a synthetic local block volume.
- Covered, indoors-like, and enclosed facts.
- Temperature and air hazard derivation from heat/cold/hazard signals.
- Declarative condition matching and forbidden-signal behavior.
- Effect aggregation for a target context.
- Cache reuse before invalidation.
- Cache recomputation after a dirty mark.
- Entity-sensitive query expiration or dirty behavior.

## Implementation Notes

The initial implementation should keep public names stable but narrow:

- Add new API classes in `com.sanhiruzu.atelier.api`.
- Put cache and scan internals in an internal package.
- Reuse `Signals` before creating a second classifier registry.
- Avoid connecting this to old room discovery, journal, or MineColonies bonuses until the new API has settled.
