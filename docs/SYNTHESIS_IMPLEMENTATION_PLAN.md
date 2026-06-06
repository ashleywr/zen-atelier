# Synthesis Implementation Plan

This plan turns `SYNTHESIS_DESIGN.md` into the first playable data-driven slice.
The broad design doc remains the source of product and balance direction; this
file is the build order and acceptance contract.

## Current State

Implemented:

- Pure core value objects for reagents, room context, apparatus context, roll
  traces, extraction attempts, and synthesis attempts.
- Deterministic extraction and synthesis engines.
- Reagent container, query, snapshot, and Codec-backed snapshot persistence.
- Codec-backed extraction and synthesis profile definitions.
- Reload registries for extraction and synthesis profiles.
- Resource-id versus embedded-id validation during reload.
- Parse-time validation for profile weights, caps, requirements, queries, and
  stored reagent values.
- Debug commands:
  - `/atelier synthesis preview <profile> [risk]`
  - `/atelier extraction inspect_item`
  - `/atelier extraction extract_held <x> <y> <z> [risk] [seed]`
  - `/atelier reagent dump_storage <x> <y> <z>`
  - `/atelier reagent give <x> <y> <z> <reagent> <amount> [tier]`
  - `/atelier reagent clear <x> <y> <z>`
  - `/atelier synthesis execute <x> <y> <z> <profile> [risk] [seed]`
  - `/atelier synthesis execute_carried <profile> [risk] [seed]`
- Position-keyed placeholder reagent cabinet storage using vanilla barrels,
  chests, and trapped chests as temporary cabinet blocks.
- Current extraction-to-cabinet command behavior is a backend/debug harness, not
  the intended early-game world interaction.
- Dewpetal placeholder item for priming weak solvent. This is now legacy slice
  scaffolding; the intended starter interaction should move to Alchemist's
  Primer and/or Crucible Spoon.
- Heated cauldron extraction service:
  - water cauldron over campfire, soul campfire, fire, soul fire, magma, or lava
  - legacy tossed Dewpetal primes weak solvent
  - target design: right-click/stir heated water cauldron with Alchemist's Primer
    or Crucible Spoon to create the Extraction Cauldron
  - tossed profiled items start a delayed simmer job
  - completed jobs pop visible vanilla placeholder drops with reagent names
- Extraction output templates support bounded rolls for `amount`, `quality`,
  `purity`, and `instability` while keeping reagent family, elements, traits,
  and source hints constrained by the input profile.
- Starter extraction profiles for flint, copper ingot, honey bottle, and rotten
  flesh use bounded attribute ranges.
- Real reagent item path:
  - `zen_atelier:reagent` carries a full `ReagentStack` data component
  - cauldron extraction now pops reagent items instead of renamed vanilla
    placeholders
  - reagent tooltips show amount, tier, quality, purity, instability, elements,
    traits, and source hints
- Carried reagent synthesis path:
  - `/atelier synthesis execute_carried <profile> [risk] [seed]` plans against
    reagent items in the player's inventory
  - successful execution consumes the selected reagent item amounts
  - synthesis byproducts are returned as reagent items in inventory, or dropped
    if the inventory is full
- Starter JSON profiles under `data/zen_atelier/atelier/`.
- Unit tests for core rolls, data parsing, resource files, storage, and room
  context snapshots, placeholder cabinet saved-data round trips, and weak-solvent
  cauldron saved-data round trips.

Removed:

- Hardcoded prototype cauldron synthesis.
- Prototype wands, result items, cauldron block/entity, overlay, recipes, models,
  loot table, data components, and GameTest.

## First Playable Slice

Goal: a player can toss normal items into a heated alchemical cauldron, wait for
the extraction to finish, collect visible reagent drops, and spend those reagents
on one data-driven synthesis attempt.

This slice should be intentionally plain. It proves persistence, reload data,
room context, and deterministic execution before adding richer UI.

### Blocks And Items

Use vanilla blocks as placeholders until the rules feel right. For now, barrels,
chests, and trapped chests can act as reagent cabinets through commands; they do
not expose vanilla item inventory as reagent storage.

Add three world-facing objects in this order:

1. Heated Extraction Cauldron
   - Placeholder version uses an existing cauldron over an existing heat source.
   - A valid liquid is required. Water can stand in for alchemical solvent until
     custom liquid behavior is worth adding.
   - Tossed `ItemEntity` inputs are accepted if an extraction profile matches.
   - Accepted inputs are not processed instantly. They simmer for a short fixed
     duration, then the rolled reagents/byproducts pop out as visible drops.
   - Failed or messy rolls can still pop useful residue.
   - Direct deposit into reagent storage is not the default early-game behavior.

2. Reagent Drops
   - The player should pick up the output before storage automation exists.
   - Reagent drops use a dedicated reagent item carrying one `ReagentStack`.
   - Tooltip inspection must show the rolled attributes clearly enough to compare
     multiple outputs from the same input.
   - Reagent item form is for transport and immediate use, not bulk long-term
     storage.

3. Reagent Cabinet
   - Placeholder version stores `ReagentContainerSnapshot` values in world
     `SavedData` keyed by block position.
   - Later block-entity version can move the same snapshot contract onto a custom
     block when UI and visuals are ready.
   - No item-stack inventory; it stores reagent records only.
   - Right-click with empty hand shows a compact text summary for now.
   - Debug command can dump exact contents.
   - Manual deposit/retrieval can come after reagent drops exist; direct
     extraction-to-cabinet is a later player upgrade.

4. Crude Extractor Upgrade
   - Later block or interaction that accepts one normal item stack.
   - Uses `ItemSourceSnapshot` plus `SynthesisCatalog.findExtractionProfiles`.
   - Rolls `ExtractionExecutor` with room/apparatus/config context.
   - May deposit output reagents into an adjacent or linked cabinet only after
     the player has built the relevant upgrade.
   - Consumes input only after a profile is found and execution succeeds.

5. Crude Apparatus
   - Uses a selected synthesis profile id.
   - Plans against a linked cabinet before consuming anything.
   - Executes through `SynthesisExecutor`.
   - Deposits byproducts back into the cabinet.
   - Converts `SynthesisOutput` to a temporary visible result path. The first
     version may report the result via chat/debug output instead of creating a
     final item system.

Do not add a custom screen in this slice unless the text/debug flow blocks
testing. A menu UI is a follow-up after persistence and world rules are stable.

## Storage Contract

Cabinet storage must use these rules:

- Stored records are `ReagentStack` values, not ItemStacks.
- Save data persists `ReagentContainerSnapshot` records. Unit-level snapshots use
  `ReagentContainerSnapshot.CODEC`; world placeholder cabinet data currently uses
  explicit NBT fields so malformed entries can be skipped without crashing the
  world.
- Invalid saved data should fail closed: log, clear only the invalid cabinet data,
  and avoid crashing the world.
- Insert merges only identical reagent profiles.
- Search and extraction spend the same best-match order:
  tier descending, purity descending, quality descending, reagent id ascending.
- Cabinet capacity should start as a simple record-count limit, not fluid/item
  slot semantics.

Initial cabinet limits:

```text
max records: 64
max amount per merged record: no explicit cap yet
side access: none
network sync: not required for first GameTest
```

## Interaction Rules

Extractor:

- Tossed item entities are the input for the first world-facing version.
- A valid cauldron liquid and heat source are required.
- The accepted item simmers for a fixed delay before outputs spawn.
- Survival should consume exactly one source item when the cauldron accepts the
  job. Creative/debug commands can bypass consumption.
- If the profile is missing, the item should remain in-world and no job should
  start.
- If storage is missing, extraction can still complete by popping reagent drops.
- Direct extraction into linked storage is a later upgrade, not the default
  early-game flow.

Apparatus:

- Profile selection can be command-driven in the first slice.
- Preview must call `SynthesisPlanner`.
- Execute must call `SynthesisExecutor`.
- Missing reagents must not mutate cabinet contents.
- Result handling can be text-only until output item semantics are designed.

Room context:

- Use `RoomAlchemyContextFactory.fromZoneData` when the block is inside a known
  room.
- Fall back to `RoomAlchemyContext.none()` outside rooms.
- Room quality may improve caps but cannot bypass profile, apparatus, or config
  caps.

## Commands

Keep and extend debug commands before UI:

```text
/atelier extraction inspect_item
/atelier extraction extract_held <x> <y> <z> [risk] [seed]
/atelier reagent dump_storage <x> <y> <z>
/atelier reagent give <x> <y> <z> <reagent> <amount> [tier]
/atelier reagent clear <x> <y> <z>
/atelier synthesis preview <profile> [risk]
/atelier synthesis execute <x> <y> <z> <profile> [risk] [seed]
/atelier synthesis execute_carried <profile> [risk] [seed]
/atelier synthesis inspect_room
```

Command rules:

- Commands should report enough roll trace detail to reproduce a result.
- Commands that mutate storage require permission level 2.
- Preview commands should not mutate world state.
- Execute commands should exercise shared engine/service code where possible, but
  current direct-to-cabinet extraction commands are allowed to remain debug-only
  until heated cauldron extraction exists.

## Data Set For Slice One

Minimum bundled extraction profiles:

- `minecraft:flint`
- `minecraft:copper_ingot`
- `minecraft:honey_bottle`
- `minecraft:rotten_flesh`
- `minecraft:redstone`
- `minecraft:glowstone_dust`
- `minecraft:slime_ball`
- `minecraft:bone`
- one common flower tag
- one common crop or kelp profile

Minimum synthesis profiles:

- crude mining coating
- conductive stabilizer
- weak salve or patch tonic
- flash/smoke escape item placeholder
- treated copper intermediate

The output ids may remain abstract until item semantics are implemented, but the
profiles must execute and produce traceable `SynthesisOutput` records.

## Tests Required

Unit tests:

- cabinet insert, merge, capacity, snapshot save/load
- extraction block/service does not consume input when no profile exists
- extraction block/service starts a delayed cauldron job for a valid input
- extraction block/service pops reagent/byproduct drops after the delay
- extraction does not require a cabinet for early-game use
- upgraded extraction deposits reagents and byproducts into cabinet
- synthesis preview and execute spend the same reagent candidates
- synthesis missing-reagent path leaves cabinet unchanged
- saved cabinet data rejects invalid reagent records

GameTests:

- cabinet persists reagents through block entity save/load
- heated cauldron accepts a tossed profiled item and starts a delayed job
- heated cauldron pops reagent/byproduct drops when the job finishes
- extractor upgrade consumes one input and writes reagents to a cabinet
- apparatus consumes reagents from a cabinet and returns byproducts/results
- room context changes the trace or effective cap in-world

Runtime smoke:

- load a single-player dev world
- `/reload` reloads extraction and synthesis profiles without stale registry data
- `/atelier extraction inspect_item` works on a bundled input
- Tossing a bundled input into a heated liquid cauldron starts a delayed
  extraction and pops visible output.
- Temporary in-game smoke:
  1. Place a water cauldron over a campfire or magma block.
  2. Current slice: toss `Dewpetal` into it to prime weak solvent.
     Target slice: stir it with `Crucible Spoon` or use `Alchemist's Primer`.
  3. Toss `flint`, `copper_ingot`, or `honey_bottle` into the cauldron.
  4. Wait about five seconds for named placeholder reagent drops.
- `/atelier synthesis preview zen_atelier:crude_mining_coating 25` reports odds

## Implementation Order

1. Add a small `synthesis.block` or `synthesis.world` service layer that adapts
   block/entity state to `ExtractionExecutor` and `SynthesisExecutor`.
2. Add reagent item/drop representation, using existing items as placeholders if
   necessary for the first pass.
3. Add heated cauldron extraction job state and delayed completion.
4. Add heated cauldron item-entity acceptance.
5. Add cauldron extraction unit tests and GameTest.
6. Keep placeholder cabinet commands as a backend/debug harness.
7. Add Reagent Cabinet block/entity with save/load only.
8. Add manual reagent deposit/retrieval between drops and cabinet.
9. Add Crude Apparatus service and command-driven synthesis against carried or
   stored reagents.
10. Add apparatus block interaction.
11. Add apparatus GameTest.
12. Add upgraded extraction-to-cabinet routing.
13. Expand bundled data to the minimum slice-one set.
14. Only then consider a menu/screen for browsing cabinet contents and selecting
    synthesis profiles.

## Non-Goals For This Slice

- No AE2/automation integration.
- No JEI/EMI integration.
- No custom synthesis screen.
- No default direct extraction-to-storage flow.
- No final affix inheritance system.
- No universal item conversion.
- No random data on normal input ItemStacks.
- No resurrection of the old cauldron/wand prototype.

## Definition Of Done

The slice is playable when:

- A clean dev world can extract at least three vanilla items into cabinet
- A clean dev world can extract at least three vanilla items through a heated
  cauldron into visible reagent drops.
- A data-driven synthesis profile can spend those reagents.
- Reloaded JSON controls the available profiles.
- Cabinet contents survive save/load once manual storage is introduced.
- Debug commands can inspect item extraction, cabinet storage, room context,
  synthesis preview, and a deterministic execution trace.
- Unit tests and the required GameTests pass.
