# Synthesis System Design

This document defines the long-term synthesis direction for Zen Atelier. The current
prototype synthesis code is disposable; new implementation should be built around
the architecture and constraints here.

For the concrete first playable slice, implementation sequence, and acceptance
tests, see `docs/SYNTHESIS_IMPLEMENTATION_PLAN.md`.

## Design Goals

- Make alchemy feel like risky, rewarding gambling rather than deterministic
  conversion.
- Preserve normal Minecraft item stacking by keeping deep synthesis data out of
  ordinary ingredients.
- Make the room and apparatus part of the crafting system.
- Support tech-player workflows: search, batching, logistics, and future AE2-style
  automation.
- Avoid ProjectE-style universal transmutation and Apotheosis-style deterministic
  rarity dust upgrades.
- Keep platform-specific code isolated so a future Fabric or Forge port is feasible.
- Prefer diegetic, in-world crafting for execution, with UI used for search,
  planning, filtering, and storage.

## Core Fantasy

Atelier turns surplus resources, exploration finds, mob drops, and modded materials
into typed reagents. Those reagents are spent in risky synthesis attempts influenced
by the atelier room, apparatus, catalysts, and player-chosen risk.

The player loop is:

1. Explore, farm, mine, or automate normal resources.
2. Extract resources into typed reagents.
3. Store and search reagents in Atelier-specific storage.
4. Build better rooms and apparatus to raise caps and change odds.
5. Synthesize catalysts, coatings, accessories, consumables, and intermediates.
6. Use those results to improve exploration, automation, combat, and later synthesis.

## First Session Experience

The system must produce an obvious early win before it asks the player to learn
deep reagent storage, automation, or room optimization.

Target first loop:

1. The player extracts a familiar item, such as blaze powder, glowstone, honey,
   copper, redstone, slime, or a flower.
2. The player tosses the item into a heated cauldron of alchemical liquid and
   waits while it simmers.
3. The cauldron pops out a visible typed reagent plus a small chance at a better
   variant or residue.
4. The player uses a simple formula to make one useful item: a crude tool coating,
   stabilizing catalyst, small bomb, or salve.
5. The current room changes the displayed odds in an understandable way.
6. A failed or messy attempt creates a byproduct that is useful later.
7. The player sees why a better room, apparatus, or catalyst would improve the
   next attempt.

This first loop should work without external automation and without requiring the
player to understand every data field.

## Progression Timing

Atelier should become useful before the Nether, become meaningfully risky around
the Nether, and become scalable after the player has stable infrastructure.

### Pre-Nether: Practical Utility

The player should be able to build crude apparatus, extract common materials, use
a basic reagent cabinet, and craft practical low-tier outputs before entering the
Nether.

Pre-Nether alchemy should solve small problems that vanilla and many tech mods do
not make especially interesting:

- surplus copper with few compelling sinks
- surplus flint from gravel processing or Create-style crushing
- awkward early mining through deepslate
- weak emergency escape tools
- small healing or recovery options without committing to potion brewing
- common mob drops, flowers, honey, slime, and redstone piling up
- preparation for the first Nether trip

Good pre-Nether outputs:

- crude tool coatings for mining speed, durability smoothing, or deepslate work
- flint abrasives for sharpening, cutting, extraction, and fuses
- copper conductive reagents for low-tier apparatus and automation parts
- smoke or flash bombs for escape and crowd control
- weak salves or patch tonics that are useful but do not obsolete stronger healing
- glow dust coatings or pulses for cave utility
- heat-resistant coatings or charms for early lava and Nether preparation
- stabilizing catalysts that improve later rolls without raising hard caps

### Nether: Volatility And Higher Caps

Nether materials should unlock stronger reagents, higher caps, and more dangerous
rolls.

Examples:

- blaze powder: fire and volatile reagents
- quartz: crystalline and stabilizing reagents
- magma cream: heatproof, binding, and molten reagents
- glowstone: light and amplifying reagents
- nether wart: transformative and medicinal reagents
- gold: resonant, luck, preservation, and formula-registration reagents

This is where risky synthesis should become noticeably more exciting: better
output ceilings, higher failure stakes, rare affix pools, and stronger catalysts.

### Post-Nether: Mastery And Scale

After the player has brewing stands, blaze rods, quartz, and likely some tech
infrastructure, Atelier can introduce serious scaling:

- batch extraction
- improved cabinets
- formula registration
- assistant or homunculus-like workstations
- stabilizer fixtures
- advanced tool coatings
- registered catalyst duplication or refill
- AE2/automation bridges

The pacing target is: manual discovery first, risky optimization second, mastery
automation third.

## Early Resource Sinks

Common surplus resources should have clear alchemical identities without becoming
universal currency.

Copper should represent conductivity, apparatus, low-tier automation, and
verdigris-style transformation:

- cauldron and extractor upgrades
- alembic coils
- heat exchangers
- reagent interfaces
- conductive coatings
- verdigris reagents
- low-mid tier catalysts
- stability circuits for automation

Gold should represent resonance, luck, preservation, registration, and Nether
utility:

- formula registration costs
- catalyst binding
- rare outcome bias
- charm and accessory crafting
- reagent duplication or refill costs
- trait preservation modifiers
- piglin or Nether preparation utility

Flint should represent abrasion, ignition, cutting, and risky cheap power:

- sharpening oils
- extraction grinding
- volatile spark powder
- bomb fuses
- coating preparation
- cheap failure-prone catalysts

Renewable organic inputs should be valuable in bulk but capped:

- flowers: color, scent, life, and light reagent families
- honey: binding, soothing, preservation, and medicine
- slime: elasticity, adhesion, insulation, and failure byproducts
- crops and kelp: low-tier life, bulk filler, and fermentation
- rotten flesh and bones: vital, calcified, decay, and fertilizer reagents

These sinks should make excess resources feel useful while respecting tier caps.

## Non-Goals

- No universal item value currency.
- No "any item into any item" conversion.
- No mandatory replacement for vanilla crafting, brewing, or enchanting.
- No random data components on every ordinary ingredient stack.
- No loot-table pollution where every mob drops Atelier junk by default.
- No screen-only crafting as the main fantasy.

## Layering

Implementation should be shaped as if it can become multi-loader later.

```text
atelier-core
  Pure Java synthesis, extraction, caps, storage math, and roll logic.
  No Level, ItemStack, BlockEntity, DeferredRegister, events, packets, or loader APIs.

atelier-minecraft-common
  Minecraft-facing ids, tags, codecs, data definitions, serialization adapters.
  Keep this as loader-neutral as practical.

atelier-neoforge
  NeoForge registrations, events, attachments, reload listeners, menus, packets,
  block entities, and world effect execution.

future atelier-fabric / atelier-forge
  Loader-specific adapters around the same core.
```

This does not require Gradle subprojects on day one, but packages should respect
the boundary.

Recommended packages:

```text
com.sanhiruzu.atelier.api.synthesis
com.sanhiruzu.atelier.synthesis.core
com.sanhiruzu.atelier.synthesis.data
com.sanhiruzu.atelier.synthesis.engine
com.sanhiruzu.atelier.synthesis.storage
com.sanhiruzu.atelier.synthesis.world
com.sanhiruzu.atelier.synthesis.block
com.sanhiruzu.atelier.synthesis.item
com.sanhiruzu.atelier.synthesis.network
com.sanhiruzu.atelier.synthesis.compat
```

## Core Model

Normal Minecraft items are mostly stateless. They feed extraction, recipes, and
catalyst slots through item ids, tags, and data profiles.

Reagents carry the deeper Atelier data:

```text
reagent id
amount
tier
quality
purity
instability
elements
traits
source hints
```

Finished high-value items may carry data components. Common materials should use
finite item variants instead of arbitrary per-stack affixes.

Examples:

```text
tempered_iron_ingot
volatile_iron_ingot
resonant_gold_ingot
reinforced_cloth
binding_resin
```

## Reagent Storage

The main reagent inventory should be Atelier-specific storage, not normal chests.
Early extraction should not send reagents directly into storage by default. The
first experience is physical: reagents pop out of the cauldron, the player picks
them up, and storage becomes valuable once the player has enough reagent variety
to need search and organization.

The reagent cabinet should store entries as structured reagent records and expose
search/query operations:

```text
family
element
trait
tier
quality range
purity range
instability range
source item
recipe compatibility
affix pool
```

Physical reagent drops or vials should exist for transfer, display, early manual
crafting, and extraction feedback. They should carry enough reagent metadata to
be spendable in early synthesis before cabinets are unlocked. They should not be
the primary bulk storage format once cabinets and automation are unlocked.
At minimum, reagent item tooltips should expose amount, tier, quality, purity,
instability, elements, traits, and source hints. The player needs to understand
why two reagents from the same input are similar but not identical.

Future AE2 integration should expose reagent predicates and quantities, not only
exact item stacks.

The cabinet should feel closer to a searchable alchemist terminal than a fancy
chest. Search and recipe discovery are core gameplay, not polish.

Required search workflows:

- What can I make with these reagents?
- What can use this reagent or trait?
- Find reagents by predicate, such as `fire tier>=3 purity>=60`.
- Filter by recipe compatibility.
- Save searches or formula presets.
- Sort by expected outcome, quality, purity, instability, or amount.
- Surface JEI/EMI-style recipe visibility when those integrations exist.

## Extraction

Extraction converts normal items into reagents through chance tables.
The default world interaction is a heated cauldron containing alchemical liquid:
the player tosses in an item, the cauldron simmers for a short duration, and the
rolled reagents or byproducts pop out as visible drops. This makes extraction
feel like a ritual rather than an instant conversion or a storage pipe.
The starter liquid is weak alchemical solvent. The old Dewpetal prototype should
be replaced by a more alchemist-coded ignition path:

```text
Alchemist's Primer: found in village libraries or ruined portals; right-click a
  vanilla cauldron to teach/convert it into an Extraction Cauldron.
Crucible Spoon: cheap stick + copper tool; stir a heated water cauldron to
  awaken the first solvent.
```

The entry signal should be a tool, text, or deliberate stirring action, not a
flower-like ingredient. This keeps the mod's opening language centered on
alchemy and experimentation rather than plant magic.

Early Overworld scavenging should also seed the fantasy before the first station:

```text
Uni / Spiky Burr: leaf drop, weak throwable, abrasive/piercing reagent
Taun Herbs: dark-forest or cave-mouth plant, weak raw regen, medicinal/water
Phlogiston Pebbles: lava-pool or netherrack-adjacent heat stones, combustible/fire
Slime mutant gels: biome-tinted slime drops, binding plus water/fire variants
```

Inputs may be exact items or tags. Unknown modded items can match generic tag-based
profiles with conservative caps.

Unknown generic tag matches must never produce rare named reagents unless a data
pack or compat profile explicitly allows it. This prevents kitchen-sink packs from
turning obscure farmable items, compressed resources, or accidental tags into
premium reagent sources.

Extraction profiles define:

```text
input item/tag
source category
base tier cap
element bias
trait pool
result pools
failure byproducts
required apparatus tier
optional room modifiers
allow/deny constraints
generic fallback policy
```

Extraction is not deterministic rarity dust. It produces typed reagents with
profiles, caps, and noisy outcomes.
Direct extraction into a reagent cabinet is a later progression feature for
upgraded apparatus, assistants, or automation. It should not be the first
interaction players see.

Input identity constrains output identity. Organic inputs should roll organic,
life, decay, binding, or preserving reagents; metals should roll metallic,
conductive, resonant, or oxidized reagents; minerals should roll earthy,
abrasive, crystalline, or spark-adjacent reagents. Randomness changes the exact
amount, quality, purity, instability, and rare variant inside that pool. It
should not turn a common organic input into an unrelated metal or magic reagent
unless a specific profile says so.

Extraction JSON may use bounded ranges for rolled attributes:

```json
{
  "reagent": "zen_atelier:organic_reagent",
  "amount": 28,
  "amount_range": { "min": 20, "max": 36 },
  "tier": 1,
  "quality_range": { "min": 5, "max": 35 },
  "purity_range": { "min": 15, "max": 50 },
  "instability_range": { "min": 20, "max": 60 },
  "elements": { "life": 1, "decay": 1 },
  "traits": [ "zen_atelier:organic", "zen_atelier:fermenting" ]
}
```

Example:

```text
minecraft:blaze_powder
  source: volatile_fire_powder
  cap: 3
  common: fire reagent
  uncommon: volatile fire reagent
  rare: scorching catalyst seed
  failure: ash residue
```

Pack-facing controls should include:

- exact item overrides
- per-tag caps
- allow lists and deny lists
- renewable-source caps
- generic fallback profiles
- compressed-resource handling
- rare-output opt-in for unknown modded items

## Synthesis

Synthesis consumes reagents, catalysts, costs, and context to roll an outcome.

Recipes should be data-driven and category-based where possible:

```text
recipe id
required reagent families
required amounts
minimum apparatus
base success
base instability
hard caps
allowed output families
affix pool
outcome table
room hooks
automation policy
```

Outcome classes should be richer than pass/fail:

```text
perfect_success
success
unstable_success
partial_success
mutated_success
dud
recoverable_failure
messy_failure
catastrophic_failure
```

Failures should often produce byproducts, residue, damaged apparatus, smoke, item
pops, clouds, fire, or other memorable effects.

## Bad Luck Protection

The gambling should be exciting, not hostile. Repeated failures should drain
resources, but they should not feel like pure deletion.

Failed risky attempts may produce:

- typed residue
- cracked catalyst seeds
- unstable sludge
- partial-progress intermediates
- failed-output variants
- instability buildup that changes later rolls
- recoverable waste for stabilizers or low-tier recipes

These materials can improve future odds, reduce failure severity, or feed side
recipes. They must not become deterministic rarity dust that directly buys the
rare result.

Useful rule of thumb: one failed attempt can hurt, but one hundred failed attempts
should leave the player with strategically useful leftovers.

## Caps

Caps are first-class system rules, not scattered recipe checks.

The cap resolver should consider:

```text
source cap
reagent cap
apparatus cap
room cap
recipe cap
config/datapack cap
```

Hard caps must always win. Room bonuses, catalysts, XP, and instability can improve
odds inside the allowed range but cannot bypass caps.

Cheap/common sources should remain useful as bulk material while never producing
top-tier results.

## Balance Budgets

Each output family needs a power budget so the system stays useful without
overriding every other mod in a pack.

Suggested constraints:

- Bombs: strong, consumable, and configurable; should respect common block
  protection and griefing expectations.
- Tool coatings: temporary, charge-limited, durability-limited, or time-limited.
- Weapon oils: situational bonuses rather than permanent weapon upgrades.
- Armor treatments: bounded resistance or utility effects with clear duration or
  charge limits.
- Catalysts: affect synthesis only; they should not become universal power items.
- Accessories: low permanent stat ceilings, stronger conditional or reactive
  effects.
- Treated materials: finite variants, opt-in recipe compatibility, and no
  automatic buff inheritance through every crafting grid recipe.
- Room fixtures: improve odds or caps inside the atelier, not global player power.

Pack controls should allow global tier caps, per-output-family multipliers,
automation multipliers, failure severity tuning, and extraction source
blacklists/whitelists.

## Room And Apparatus Context

The current room system is a major differentiator. Synthesis should consume a
snapshot of room context, not query the world directly from the roll engine.

Room context may include:

```text
profile id
quality
enclosure
temperature
humidity
detected signals
penalty signals
nearby apparatus
stability
element biases
failure containment
```

Examples:

```text
library/bookshelves -> discovery, preview, controlled traits
greenhouse/plants -> life, growth, organic reagents
smithy/heat/metal -> metal, fire, tool coatings
enchanting room -> arcane traits, higher instability
storage room -> batch efficiency, retrieval, loss reduction
```

Apparatus should be separate from room identity:

```text
cauldron: max tier, batch size, failure containment
stirrer: trait inheritance, mutation chance
extractor/alembic: extraction quality, purity
heater: speed, element bias, volatility
stabilizer: failure severity reduction
formula desk: planning, preview accuracy
reagent cabinet: storage and search
```

## Diegetic Execution

Crafting should primarily happen through world interaction:

- insert or throw ingredients into apparatus
- heat with blocks or fuels
- stir with tools
- stabilize with nearby fixtures
- consume catalysts
- emit particles, sounds, block states, and byproducts

Manual world interaction is for setup, rare attempts, and spectacle. Routine
bulk workflows must not require hundreds of repeated item throws or stir clicks.

Screens are for planning and logistics:

- reagent search
- formula book
- recipe preview
- probability explanation
- cabinet filtering
- saved synthesis presets
- automation pattern configuration

## Automation

Automation should be supported, especially for tech-pack players, but it should
not erase the atelier. It is a progression goal: players should first learn
manual heated-cauldron extraction, reagent drops, and basic synthesis before
building direct storage routing or batch extraction.

For Stoneblock, SkyFactory, AllTheMods, and AE2-heavy players, a minimal automation
contract is an adoption requirement for the complete system, but not for the
first playable loop.

Suggested split:

- Bulk extraction: automatable.
- Conservative synthesis: automatable.
- Risky synthesis: automatable with explicit risk settings and waste handling.
- Best odds/perfect outcomes: benefit from high-quality rooms, apparatus, and
  player-built infrastructure.

Possible future integration blocks:

```text
reagent interface
formula pattern encoder
batch extractor
risk controller
reagent import/export adapter
formula registry / assistant workstation
```

Minimum automation requirements:

- batch extraction from item stacks
- sided item I/O
- redstone pause/enable behavior
- cabinet auto-pull for synthesis
- saved formula presets
- machine-readable reagent predicates
- explicit risk settings
- waste/byproduct handling
- odds or expected-value preview for configured attempts

AE2-style integration should model virtual reagent storage first. Exporting exact
physical reagent vials is secondary to queries like:

```text
fire reagent, tier >= 3, purity >= 60, amount >= 100
```

## Mastery Automation

Atelier-inspired automation should arrive after the player has learned and proven
the system. This is distinct from generic item logistics.

Useful progression patterns:

- Recipe mastery: repeated successful crafts improve preview accuracy, reduce
  routine crafting time, or unlock batch execution.
- Formula registration: the player can register a good intermediate, catalyst,
  coating, or consumable as a reproducible target.
- Assistant work: a helper block, spirit, homunculus-like assistant, or workshop
  fixture can repeat registered formulas over time.
- Duplication/refill: selected registered items can be copied or refilled by
  spending reagents, residue, time, fuel, XP, or room capacity.
- Auto-resolve: low-risk mastered recipes can skip manual stirring and resolve
  directly from cabinet resources.

Registration should not be free power. It should preserve the value of making a
great item once while still consuming meaningful resources to reproduce it.

Suggested registration rules:

- Only synthesized outputs, catalysts, treated intermediates, coatings, and
  selected consumables are registerable.
- Registered entries store the target profile and required reagent predicates,
  not a universal item value.
- Better registered targets cost more to reproduce.
- Registered rare affixes may require matching rare residues or catalyst seeds.
- Reproduction may be slower or more expensive than a fresh risky craft, but it is
  predictable.
- Pack configs can disable registration for specific output families.

This gives players the Atelier-like reward of "I finally made a great one; now I
can build around it" without becoming ProjectE-style free duplication.

## Outputs

Atelier should not center on better vanilla potions. Strong output categories:

- bombs and field consumables
- salves, tonics, and emergency items
- tool coatings
- weapon oils
- armor treatments
- catalysts for later synthesis
- intermediate materials
- room fixtures
- exploration aids
- farming and ecology reagents
- Curios/accessory items
- automation and search tools

Tool coatings are especially attractive because they are useful, bounded, and
compatible with many packs.

Affixed accessories are a good home for rare random modifiers without competing
directly with every modded armor system.

## Material Treatment

Universal affix inheritance through normal crafting should not be the first
implementation. It is powerful but difficult to balance and hard to apply safely
to every vanilla and modded recipe.

Prefer finite treated material variants first:

```text
tempered iron -> durability bias
volatile iron -> speed/risk bias
resonant gold -> enchantment/luck bias
conductive copper -> redstone/energy bias
reinforced cloth -> armor/coating bias
```

Later, recipe-level material inheritance can be opt-in and data-driven.

Treated materials should only be tag-compatible with vanilla or modded recipes
where that compatibility is explicitly intended. Do not accidentally make every
iron-ingot recipe inherit alchemical power in kitchen-sink packs.

## Data Definitions

Use Codec-backed datapack definitions with schema versions.

Suggested paths:

```text
data/<namespace>/atelier/reagents/*.json
data/<namespace>/atelier/traits/*.json
data/<namespace>/atelier/affixes/*.json
data/<namespace>/atelier/extraction_profiles/*.json
data/<namespace>/atelier/synthesis_recipes/*.json
data/<namespace>/atelier/apparatus/*.json
data/<namespace>/atelier/room_modifiers/*.json
```

Every major definition should include:

```json
{
  "schema": 1,
  "id": "namespace:path"
}
```

Definition ids are part of the authoring contract. A reload listener must reject
or clearly log a definition whose embedded `id` does not match the resource id
derived from its datapack path. The runtime catalog, debug output, and roll trace
must all refer to the same id.

Invalid definitions should fail closed for that definition only: skip the bad
entry, keep loading the rest of the pack, and include the resource id and parse
reason in the log. Do not silently repair negative weights, empty outcome tables,
blank ids, impossible caps, or malformed source keys.

## Prototype Migration Rules

The existing hardcoded cauldron prototype is not the target system and does not
need to be preserved. Remove prototype runtime code before building the first
data-driven path so new behavior cannot accidentally depend on old assumptions.

Migration rules:

- New synthesis gameplay must route through data-loaded profiles, the planner,
  and the deterministic executor.
- Hardcoded `SynthesisRecipe` tables should be deleted rather than migrated.
- Static random output assembly must not be used for new synthesis results.
- Cauldron/apparatus UI previews and execution must share the same planner logic.
- World adapters may translate ItemStacks, rooms, blocks, and player actions into
  core attempts, but roll decisions stay in the core engine.
- Prototype item ids and outputs do not need to survive unless a new profile
  deliberately reintroduces them.

## Public API Posture

Keep the public API small until the system proves itself.

Good early API views:

```text
ReagentView
ReagentStorageView
ExtractionContextView
ExtractionResultView
SynthesisContextView
SynthesisResultView
RoomAlchemyContextView
```

Good extension points:

```text
ExtractionProfileProvider
RoomAlchemyModifierProvider
SynthesisOutcomeModifier
ReagentPredicate
WorldEffectHandler
```

Avoid exposing mutable internal roll tables, storage maps, or engine classes too
early.

## Deterministic Rolls

Extraction and synthesis should be reproducible from attempt data plus seed.

No static randoms in core logic.

Roll results should include a trace for debugging:

```text
base success
apparatus modifiers
room modifiers
overcharge modifiers
cap decisions
rare outcome chance
failure severity chance
selected outcome
```

The trace can power debug commands, tests, and optional advanced UI.

## Testing

Start with unit tests before world integration.

Core tests:

- extraction roll determinism
- synthesis roll determinism
- cap invariants
- instability increases both rare chance and failure risk
- room bonuses affect odds but cannot bypass hard caps
- trait incompatibilities
- affix pool filtering
- byproduct generation
- no normal input stack requires random data

Data tests:

- Codec round-trip tests
- invalid JSON failure tests
- default value tests
- schema version tests

Storage tests:

- reagent merge/split rules
- serialization round trips
- query/filter correctness
- stable sorting for UI/search
- extraction consumes the same ranked reagent candidates shown by planning/search

GameTests:

- extractor consumes inputs and writes reagents
- cabinet persists and syncs
- cauldron/apparatus consumes reagents correctly
- room bonuses apply at runtime
- failure world effects are bounded
- automation interfaces respect caps and contents
- a datapack recipe can be loaded, previewed, executed, and produce the traced
  outcome in-world

## Debug Tools

Add commands early:

```text
/atelier synthesis inspect_room
/atelier synthesis preview <recipe>
/atelier synthesis roll <recipe> <seed>
/atelier extraction inspect_item
/atelier reagent give ...
/atelier reagent dump_storage
```

Tuning a gambling system without roll traces and commands will be slow and brittle.

## First Implementation Milestone

The first playable slice should be small and prove the loop:

- data-loaded reagent definitions
- data-loaded extraction profiles
- pure extraction engine
- pure synthesis engine
- reagent storage model
- simple reagent cabinet block entity
- simple extractor or cauldron extraction interaction
- one synthesis apparatus path
- room context snapshot from existing room system
- pre-Nether progression viability
- 10-20 vanilla extraction profiles
- 5-8 synthesis recipes
- 8-12 traits/affixes
- searchable reagent cabinet basics
- batch extraction from item stacks
- sided I/O or an explicit automation adapter seam
- unit tests for core rules
- one or two GameTests for world integration

Suggested first output families:

- tool coating line
- stabilizing/amplifying catalysts
- treated metal or cloth intermediate
- one room fixture or automation/search tool
- optional basic bomb or salve line

The second milestone should consider one mastery automation feature, such as
registering a treated intermediate or catalyst and reproducing it through a
resource-costed assistant/workstation.

## Implementation Order

1. Delete or quarantine prototype synthesis assumptions.
2. Build core value objects and deterministic roll engine.
3. Add Codec-backed data definitions and reload registries.
4. Enforce resource-id and embedded-id consistency during reload.
5. Add reagent storage model and tests.
6. Make reagent planning and reagent spending use the same candidate ordering.
7. Add extraction profiles and extraction engine.
8. Add synthesis recipe definitions and synthesis engine.
9. Build minimal world adapters: room snapshot, extractor, cauldron/apparatus.
10. Replace the cauldron prototype execution path with the data-driven executor.
11. Add debug commands and roll traces.
12. Add cabinet UI/search.
13. Add batch extraction and basic automation seams.
14. Add broader AE2/JEI/EMI integrations after storage and recipes are stable.

Near-term code audit:

- Keep hardcoded `SynthesisRecipe` behavior deleted; reintroduce gameplay only
  through data-driven profiles and world adapters.
- Make reload tests cover mismatched resource ids and embedded ids.
- Decide whether reagent spending should prefer highest-tier candidates, oldest
  matching candidates, or explicit player-selected candidates; preview and
  execution must agree.
- Add one runtime GameTest after the first datapack-backed cauldron path lands.

## Design Test

When adding a feature, ask:

- Does this create an interesting decision, or only extra steps?
- Does it preserve normal item stacking?
- Does it respect hard caps?
- Does it make the room or apparatus matter?
- Can it be tested without launching Minecraft?
- Can a datapack or modpack author extend it?
- Does it avoid universal transmutation?

If the answer is no, the feature probably needs a narrower shape.
