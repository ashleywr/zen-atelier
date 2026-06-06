# Alchemy Discovery Slice Spec

## Goal

Make extraction and synthesis feel like a learnable alchemy practice instead of a hidden recipe table. The player should be able to choose a reagent goal, inspect possible sources, test materials, and have the game remember what they learned.

This slice borrows the strongest pattern from Ars Nouveau: the book is part of the gameplay loop, not just external documentation. More importantly, it should be usable without reading long pages. The best version is icon-first, button-driven, and testable by doing. Text exists as labels, tooltips, and fallback explanation, not as the primary way to understand the system.

It also borrows from Atelier and crafting JRPGs by making materials carry categories, traits, and quality, while keeping early information readable enough that players do not need a spreadsheet.

## Player Loop

1. The synthesis station shows a missing reagent need, such as Binding, Heat, Organic, Conductive, or a required trait.
2. The player uses an Alchemist's Lens or Alchemist's Codex while browsing inventory and chests.
3. Tooltips and item overlays reveal whether an item is known, suspected, or known-empty for extraction.
4. The player throws one valid item into a primed extraction cauldron.
5. The cauldron produces reagents and records the source in the player's codex.
6. The synthesis UI and item tooltips can now point back to known sources for that reagent family.

Failed tests are still progress. If an item cannot extract, the codex can remember it as a known-empty source so the player does not repeat the same dead end.

## First Playable Slice

- Add an Alchemist's Codex item.
- Add an Alchemist's Lens item.
- Store per-player extraction knowledge:
  - source item id
  - number of attempts
  - discovered reagent ids
  - discovered traits
  - discovered elements
  - known-empty tested sources
- Update extraction cauldron completion to record the source and produced reagents.
- Update invalid extraction handling to record known-empty sources when a primed extraction cauldron rejects an item with no profile.
- Sync discovered extraction knowledge to the client.
- Add tooltip hints as a temporary surface:
  - known source: exact known reagent families
  - known-empty source: no cauldron extraction found
  - with lens/codex and a matching profile: suspected extraction potential
  - with lens/codex and no matching profile: no response
- Replace the text-heavy tooltip surface with icon rows as soon as the custom codex/synthesis UI exists.
- Add basic codex use behavior:
  - opens Patchouli/custom UI later
  - for now prints a concise field index of known sources

## Best-Version Codex UI

The best version should be a custom screen with gameplay-backed pages. Patchouli can remain useful for optional reference pages, but the discovery loop should not require reading a guidebook.

- Source Index:
  - searchable item grid
  - known/suspected/failed filter tabs
  - item icon, source name, discovered reagents, traits, attempts
- Reagent Index:
  - reagent families grouped by role: binding, heat, organic, sharp, conductive, preserving, etc.
  - "known sources" and "suspected sources" lists
  - quality/tier ranges after repeated extraction
- Research Goals:
  - pin a missing synthesis requirement
  - show matching known sources first
  - show icon clusters when no known source exists
- Extraction Notes:
  - timeline of recent tests using source icons and result icons
  - failed tests as useful negative knowledge
  - "try similar materials" suggestions shown as tag/category icons, not sentences
- Recipe Support:
  - synthesis recipe pages pull directly from known reagent sources
  - unknown recipes show silhouettes and category hints

The UI should be dense and readable, closer to an interactive alchemy index than a prose journal. Use the nine-slice window assets already introduced for scalable panels.

## Icon-First UX Rules

- Prefer clickable icons, item grids, progress pips, and reagent swatches over paragraphs.
- Let players test directly from the UI wherever possible: pin, compare, search, filter, and queue experiments.
- Use text as secondary support: short labels, hover tooltips, and optional details.
- Replace ecological prose hints with visual categories:
  - sticky: honey/slime/resin-like icon
  - fibrous: string/vine/plant-fiber icon
  - mineral: stone/crystal icon
  - heat: flame/ember icon
  - organic: leaf/bone/meat icon
  - conductive: copper/spark icon
- Unknown discoveries should use silhouettes, question marks, dimmed reagent vials, and empty sockets.
- Failed tests should leave visible marks: a crossed-out source icon, muted row, or failed-test badge.
- Repeated tests should visibly fill progress pips or reveal more icon sockets instead of requiring the player to read a log.

## Art Needed

Temporary/free-asset approach:

- Alchemist's Codex:
  - reuse the existing room journal texture for now.
  - later replace with a darker notebook with green-gold alchemy clasp.
- Alchemist's Lens:
  - reuse an existing magic/discovery icon texture for now.
  - later replace with a handheld brass lens or monocle-like alchemy scope.
- Codex UI:
  - reuse synthesis nine-slice window/panel sprites.
  - use existing reagent vial icons and Minecraft item icons.
- Discovery states:
  - known: reagent vial icon + clear text.
  - suspected: faint outline or question-mark overlay.
  - failed: muted/gray source row.

Final art wishlist:

- 16x16 item icon for Alchemist's Codex.
- 16x16 item icon for Alchemist's Lens.
- 16x16 or 24x24 small category icons for reagent families.
- Small overlay glyphs for known, suspected, tracked, failed.
- Codex page tab sprites if we move beyond Patchouli.
- Visual hint icons for material categories: sticky, fibrous, mineral, heat, organic, conductive, preserving, sharp, wet, dry.
- UI badges for "known", "suspected", "tested empty", "tracked", and "new".

## Why This Slice Is Compelling

- It makes every extraction attempt produce knowledge, not just items.
- It turns existing Minecraft inventory browsing into alchemical investigation through icons and interaction, not reading.
- It supports the synthesis station directly: missing reagents become solvable goals.
- It gives the player a reason to carry a tool and revisit familiar materials.
- It can scale with new recipes because the source of truth stays data-driven.

## Implementation Notes

- Extraction profiles remain the authoritative data source for what can extract.
- Player knowledge should store discoveries, not duplicate all possible recipe data.
- Tooltips should avoid full spoilers unless the player has discovered the source or is using the lens.
- Current text tooltip hints are scaffolding. The desired final surface is icon rows and codex UI state.
- The codex should eventually be the arbiter of player-facing alchemy knowledge, while extraction profiles remain the arbiter of mechanics.

## Test Plan

Automated coverage:

- Unit test client discovery state replacement, clearing, and defensive snapshots.
- Integration test extraction knowledge payload serialization.
- Keep synthesis resource and codec tests covering extraction profile parsing.
- Use GameTests for server-side cauldron behavior once the room-zone fixture instability is isolated.

Manual slice smoke test:

1. Use AMI on the Alchemist's Codex or Lens and choose "Give Discovery Test Kit".
2. Current slice: prime a heated water cauldron with dewpetal. Target design:
   create the Extraction Cauldron with Alchemist's Primer or Crucible Spoon.
3. Hold the lens and hover kit materials such as flint, honey bottle, copper ingot, and rotten flesh.
4. Throw one source item into the extraction cauldron.
5. Confirm the reagent pops out and the codex icon grid changes that source from suspected to known.
6. Right-click the codex and verify source filters, source icons, and result icons work without reading page text.
7. Try a non-extractable item and verify it becomes a tested-empty visual state.

Useful tooling support:

- AMI should expose codex, lens, Alchemist's Primer, Crucible Spoon, extraction cauldron, synthesis station, and universal reagent as hero items. Dewpetal can remain exposed only while the legacy prototype path exists.
- AMI should provide a one-click discovery kit for the current playable slice.
- `/atelier extraction extract_held ...` should record codex knowledge so command-driven testing exercises the same discovery state as cauldron extraction.
- AutoMine is still useful for screenshot/smoke testing the custom codex screen once the local automation path is reliable again, but it should not block normal implementation.
