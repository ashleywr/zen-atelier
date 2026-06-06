# Spatial Synthesis Redirection Spec

This spec captures the new synthesis direction: reagents are rich Atelier-style
materials, extraction condenses bulk resources into fewer better reagents, and
synthesis is a spatial board puzzle with risk, waste, and material sinks built
directly into the grid.

This supersedes the current half-implemented synthesis GUI direction. Existing
backend pieces can be reused where they fit, but the final UI and reagent model
should be redesigned around this spec.

## Goals

- Make reagents feel like alchemical objects, not generic dust counters.
- Start the mod with an alchemist's tool or text, not a flower-like trigger.
- Seed the Overworld with strange early ingredients so players discover the
  synthesis fantasy before they build their first station.
- Prevent bulk automation from flooding the player with thousands of reagent
  item stacks.
- Let tech-pack players dump mass resources into Atelier without letting them
  bypass the puzzle.
- Make manual play the best path for rare traits and perfect synthesis.
- Use the synthesis grid itself as the primary resource sink.
- Avoid passive room upkeep or arbitrary taxes that sit outside the core loop.

## Ignition: Starting The Mod

The first interaction should feel tactile and alchemical. Avoid making the entry
trigger a petal or flower, because that drifts too close to Botania's opening
language and makes the mod feel plant-centered when synthesis is the fantasy.

Preferred starter paths:

```text
Alchemist's Primer
Crucible Spoon
```

### Alchemist's Primer

The Primer is a custom book item that teaches the station blueprint.

Acquisition:

```text
village library loot
ruined portal loot
starter guide/test kit in dev contexts
rare wandering-trader offer, optional
```

Interaction:

```text
right-click vanilla cauldron with Alchemist's Primer
vanilla cauldron transforms into Extraction Cauldron
the book remains, gains a discovery note, or is consumed depending on balance
```

Fantasy:

```text
The player is not feeding a flower into a pot.
They are learning how to recognize and assemble an alchemical station.
```

### Crucible Spoon

The Spoon is the low-cost tactile alternative.

Suggested recipe:

```text
stick + copper ingot -> Crucible Spoon
```

Interaction:

```text
place water cauldron over a valid heat source
right-click/stir with Crucible Spoon
heated water cauldron becomes an Extraction Cauldron
```

Fantasy:

```text
heat + water + copper tool + deliberate stirring = first solvent awakening
```

Implementation direction:

```text
both Primer and Spoon can create the same first Extraction Cauldron state
Primer is discovery/loot-led
Spoon is craft-led
packs can enable one or both
```

## Overworld Scavenger Hunt

Players should encounter strange alchemical materials before they understand the
full system. These ingredients should be weak or odd as normal items, but
valuable once extracted and synthesized.

Design rule:

```text
raw ingredient: cute, weak, weird, situational
extracted reagent: clearly useful category/element/shape identity
synthesized output: reveals why the player should hoard the ingredient
```

Starter ingredient families:

### Uni / Spiky Burr

Atelier trope:

```text
small spiky throwable hazard
```

World source:

```text
occasional oak or birch leaf drop, similar to apples but rarer
```

Raw item behavior:

```text
throwable like a snowball
deals about 1 heart of damage
low durability/stack value as a weapon
```

Extraction identity:

```text
categories: abrasive, piercing, organic
elements: earth primary, wind or fire secondary
likely shapes: single, line_2, elbow
traits: spiky, piercing, brittle
```

Synthesis hook:

```text
starter abrasive for crude mining coatings
starter piercing reagent for spiky bombs or weapon oils
teaches that odd world drops can become serious synthesis ingredients
```

### Taun Herbs / Starter Weed

Atelier trope:

```text
humble medicinal plant whose true value is in synthesis
```

World source:

```text
subtle glowing plant in dark forests, cave mouths, and damp shaded patches
```

Raw item behavior:

```text
edible
brief almost-useless regeneration pulse, around 1 second
```

Extraction identity:

```text
categories: medicinal, organic
elements: water primary, life optional
likely shapes: single, line_2
traits: soothing, fragile, fresh
```

Synthesis hook:

```text
first natural path into instant salves
teaches that raw materials are weak but extracted reagents are meaningful
```

### Phlogiston Pebbles

Atelier trope:

```text
little heat stones that are better as alchemy fuel than normal fuel
```

World source:

```text
drops from netherrack
small ground deposits near Overworld lava pools
rare ruined portal loot
```

Raw item behavior:

```text
weak furnace fuel, slightly better than a stick
```

Extraction identity:

```text
categories: combustible, volatile, mineral
elements: fire high
likely shapes: single, line_2, tee for richer rolls
traits: hot, volatile, smoldering
```

Synthesis hook:

```text
early explosive recipes
fire nodes on the board
smelting or sparking equipment coatings
```

### Slime Mutants / Puniballs

Atelier trope:

```text
slime-derived colored gels with biome-tinted identities
```

World source:

```text
occasional extra drop from vanilla slimes
variant depends on biome, local block context, or dimension
```

Examples:

```text
Aqua Gel: damp, swamp, river, lush cave, ocean-adjacent slimes
Ember Gel: hot, desert, basalt, lava-adjacent, Nether-adjacent slimes
```

Raw item behavior:

```text
bouncy novelty item
possibly creates a tiny temporary jump-pad effect when placed or thrown
```

Extraction identity:

```text
categories: binding, elastic, organic
Aqua Gel elements: water primary
Ember Gel elements: fire primary
likely shapes: square_2, line_2, elbow
traits: bouncy, sticky, springy
```

Synthesis hook:

```text
first strong binding reagent
teaches that the same creature source can produce different elemental colors
based on where it came from
supports server trading because source context changes reagent desirability
```

## Progression Philosophy

Teach one layer at a time.

The system should escalate the way Atelier games do: the first craft should feel
obvious and satisfying, then later apparatus tiers should reveal that the same
materials have deeper implications.

### Tier 1: Crude Station

The opening synthesis experience should be readable immediately.

Rules:

```text
board: 3x3
shapes: mostly single and line_2
recipe requirements: simple categories
elements: small visible bonuses
traits: shown, but not heavily optimized
nodes: none or one simple element node
automation penalties: not relevant yet
```

Player takeaway:

```text
I extracted redstone into a conductive reagent.
I placed it on a small board.
I made a useful thing.
```

Era 1 should focus on quality-of-life, not invincibility. Pre-Nether outputs
should make Overworld travel, caving, mining, and Nether preparation less
tedious while staying below permanent endgame power.

Good Era 1 output families:

```text
wanderer's robes: low armor, step-assist or fall-damage reduction
lodestone ring: small item magnet range
lantern charm: tiny safety bubble or dynamic-lighting-style utility where
  supported
instant salves: no-drink-animation emergency healing
frost globe: low damage, freezing/slowing control
spark core: flashbomb escape tool, weakness and short aggro break
temporary coatings: strong tool/weapon effects with limited durability uses
```

Era 1 should introduce every major mode in a cheap, readable form:

```text
one wearable utility item
one instant consumable
one non-griefing tactical bomb
one temporary equipment coating
one catalyst or stabilizer
```

This gives players a taste of the full mod without exposing the later 5x5
optimization burden.

### Tier 2: Proper Cauldron

The spatial puzzle becomes real.

Rules:

```text
board: 5x5
shapes: line_3, square_2, elbow, tee
empty-space penalty: introduced
element nodes: introduced
filler reagents: valuable
```

Player takeaway:

```text
The same reagent can be good or bad depending on shape and board position.
```

### Tier 3: Atelier Apparatus

Trait play becomes important.

Rules:

```text
trait adjacency: introduced
purity: affects inheritance
instability: affects mutation and failure severity
codex: records useful trait and source discoveries
```

Player takeaway:

```text
I need this trait to touch that trait, and I need a clean reagent to preserve it.
```

### Tier 4: Advanced Apparatus

The board becomes a discovery and optimization engine.

Rules:

```text
morph nodes
catalysts
over-tuning
recoverable failure slag
condensed elite reagents
```

Player takeaway:

```text
I can bend a recipe into a better one if I solve the board and pay the risk.
```

### Mastery: Workshop Automation

Automation arrives after the manual loop is fun.

Rules:

```text
condensation
formula registration
assistant/workshop repeat crafting
AE2-style reagent predicates
mechanical interference mitigation
```

Player takeaway:

```text
Automation helps me feed the atelier, but mastery still comes from identity,
planning, rooms, apparatus, and board constraints.
```

## Reagent Anatomy

Reagents carry multiple independent layers of data:

```text
reagent id
categories
elements
shape
quality
purity
instability
traits
source hints
amount
```

### Categories

Categories are functional recipe keys.

Examples:

```text
conductive
binding
abrasive
caustic
organic
crystalline
metallic
preserving
volatile
medicinal
```

Recipes should usually ask for categories rather than exact reagent ids. A
recipe can require `conductive`, and the player can satisfy it with redstone
dust, conductive slime, treated copper, or a rare condensed crystal if those
reagents carry the category.

### Elements

Elements are board energy, not recipe eligibility.

Suggested core elements:

```text
fire
water
earth
wind
```

Optional later elements:

```text
light
dark
lightning
life
decay
arcane
```

Elements have integer values. They activate board nodes, fill gauges, enable
recipe morphs, and boost output quality. They should not replace categories.

### Quality

Quality is the output multiplier. Synthesis should average or otherwise combine
input quality, then apply board bonuses, node bonuses, catalyst bonuses, and
failure penalties.

High-quality reagents should matter even when their elements and traits are
ordinary.

### Purity

Purity controls trait preservation and clean inheritance. Low-purity reagents
can still provide categories, elements, and filler coverage, but should be worse
at passing rare traits cleanly.

### Instability

Instability is a risk lever. It can increase rare/mutated outcomes while also
increasing failure severity, negative trait inheritance, and board volatility.

### Traits

Traits are inheritable modifiers and the long-term optimization hunt.

Examples:

```text
destruction
redstone_resonance
lightweight
critical_rate
fragile
volatile
sticky
preserving
```

Trait fusion should be strongly tied to spatial placement. Reagents with matching
or compatible traits that touch by edge can fuse, level up, mutate, or preserve
their traits more reliably.

### Shape

Shape is the grid footprint. It is the spatial cost of using a reagent.

Low-tier reagents should mostly be easy:

```text
single
line_2
```

Higher-value reagents can become awkward:

```text
line_3
square_2
elbow
tee
large_l
cross
chunky_3x3
```

A reagent with excellent quality, strong elements, and rare traits may still be
hard to use because it consumes awkward board space.

## Example Reagent

Redstone block extraction might produce:

```text
Name: Crystalline Dust
Categories: conductive, abrasive
Elements: fire 2, wind 1
Shape: line_2
Quality: 45
Purity: 60
Instability: 15
Traits: redstone_resonance 1
Source hints: minecraft:redstone_block
```

A recipe may ask for `conductive`, while a recipe morph node may require a
`fire` reagent to cover a specific board tile. This means `conductive + fire`
is a meaningful search target.

## Reagent Specificity And Player Trade

The system should create value through specificity, not only rarity.

On a server, players should eventually care about trading exact reagent profiles:

```text
high-purity binding + water, single shape, no bad traits
conductive + fire, line_2 shape, redstone_resonance
cheap earth filler, chunky shape, low trade value but useful coverage
awkward tee reagent with destruction 2
mechanical-interference-free catalyst, single shape
slag with huge fire value and manageable instability
```

This creates a player economy because the best reagent depends on:

```text
selected recipe
board size
open board spaces
node positions
morph targets
trait chain
quality target
purity needs
acceptable instability
```

The UI and cabinet search should support this economy. Players need to be able
to search, compare, and advertise reagents by category, element value, shape,
purity, trait, source, and interference state.

## Extraction

Extraction should be a manual magical crucible first, with automation arriving
later as progression.

The first version should feel like:

1. Prime an extraction cauldron or crucible.
2. Add one item stack or a controlled batch.
3. Wait through a visible simmer.
4. Manually collect the produced reagent.
5. The codex records source knowledge.

Automation should be supported later, but the first experience should teach
players that extraction is interpretation, not item crushing.

## Condensation

Condensation solves bulk item dumps.

When a player extracts many copies of the same input, the system should produce
fewer, denser reagents instead of one reagent per source item.

### Rule

Bulk input increases reagent value, not reagent count.

Example:

```text
1 redstone dust -> 1 weak conductive reagent
64 redstone dust -> 1 stronger conductive reagent
1000 redstone dust -> 1 exceptional condensed conductive reagent
```

Condensation can improve:

```text
quality
purity
element values
trait roll chance
amount
shape size
instability
```

The important balance rule is that condensation should not be pure upside.

Large dumps can create powerful reagents, but those reagents may be spatially
awkward, unstable, or expensive to place cleanly.

### Suggested Scaling

Condensation should use diminishing returns with milestone jumps:

```text
1 item: baseline roll
8 items: noticeable quality/purity bump
64 items: strong condensed roll
512 items: rare trait odds improve
4096+ items: elite condensed reagent candidate
```

Use logarithmic or tiered scaling rather than linear scaling.

### Ultimate Sink

Very large dumps can produce special named condensed reagents.

Examples:

```text
Crystallized Avarice
Redstone Heart
Molten Verdigris Core
Honeyed Amber Mass
Compressed Slag Bloom
```

These should be exciting, but not universal. They must still respect source
identity: diamond-derived reagents should not become the best organic, binding,
or medicinal reagent unless a specific profile says so.

## Mechanical Interference

Automation should be useful for bulk baseline reagents, but manual extraction
should be best for rare traits and perfect synthesis.

### Rule

If a reagent is removed from an extraction apparatus by non-player logistics,
apply Mechanical Interference.

Examples of non-player movement:

```text
hopper
AE2 import bus
Create chute
pipe or item transport
generic automation adapter
```

### Penalty

Mechanical Interference should attack the data players care about most:

```text
purity drops sharply, possibly to 0
rare traits are removed, scrambled, or downgraded
trait inheritance reliability drops
instability may increase
quality may be capped
```

The reagent should remain usable for categories, elements, and filler. This
keeps automation valuable without letting it dominate high-end crafting.

### Manual Extraction Bonus

Manual collection can preserve or improve:

```text
purity
rare trait retention
codex discovery credit
perfect synthesis eligibility
special first-discovery rolls
```

This gives players a reason to personally handle important extractions without
making basic automation pointless.

## Synthesis Board

Tier 1 can use a familiar 3x3 board. Tier 2+ should move to a 5x5 board, with
possible later 7x7 or apparatus-specific boards.

The board has:

```text
placed reagent shapes
empty cells
element/catalyst/morph nodes
trait adjacency links
previewed success odds
previewed perfect odds
predicted output
failure/byproduct preview
```

## Empty Space Tax

Empty board space is an active penalty.

Each empty cell lowers:

```text
success chance
perfect chance
trait preservation
quality bonus
preview confidence, optionally
```

This creates an organic material sink. Players can use low-tier automated
reagents as filler mortar around premium pieces.

### Filler Reagents

Filler should be a real strategy:

```text
low-tier single cells
line_2 mortar
slag chunks
cheap organic paste
conductive dust traces
binding resin
```

Filler should help coverage and element gauges, but it can also pollute traits,
lower average quality, or add instability if used carelessly.

## Trait Adjacency

Traits interact when occupied cells touch by edge.

Possible interactions:

```text
same trait + same trait -> trait level up
compatible traits -> fused trait
conflicting traits -> instability or negative trait
pure reagent adjacency -> preservation bonus
low-purity adjacency -> mutation chance
```

The UI must clearly show active adjacency links so players understand why the
preview changed.

## Catalyst And Morph Nodes

Board nodes are fixed tiles or recipe-defined tiles.

### Element Nodes

Element nodes require a matching element on the reagent covering that tile.

Example:

```text
fire node covered by fire 2 reagent -> quality bonus
water node covered by water 3 reagent -> stability bonus
earth node covered by earth reagent -> durability bias
```

### Morph Nodes

Morph nodes can transform the selected recipe preview.

Example:

```text
iron_sword recipe + fire morph node -> flamberge recipe
healing_salve recipe + life morph node -> regeneration_salve recipe
```

Morphs should be discoverable and recorded in the codex.

### Catalyst Reagents

Catalyst reagents are special pieces whose main purpose is synthesis tuning.

Typical catalyst profile:

```text
shape: single
elements: none or very low
traits: non-inheritable
categories: catalyst
effect: odds, cap, morph, stability, preview, trait preservation
```

Catalysts can:

```text
raise perfect odds
reduce empty-space penalty
improve trait preservation
force a node activation
temporarily relax a soft cap
alter failure byproducts
improve preview accuracy
```

Catalysts should not casually bypass hard caps. If a catalyst can bypass a cap,
the spec should mark that as a rare explicit exception with high cost and clear
trace output.

Catalyst families:

```text
stabilizing catalyst: lowers instability and failure severity
amplifying catalyst: raises element node output and quality bonus
preserving catalyst: improves trait inheritance and trait lock behavior
morph catalyst: reveals or strengthens recipe morph paths
precision catalyst: improves preview accuracy and placement forgiveness
cap catalyst: relaxes a specific soft cap under explicit recipe rules
interference ward: protects automation-collected reagents from part of the
  mechanical interference penalty
```

Catalysts are strong chase outputs because they feed back into the synthesis
loop. They should be valuable even when they do not create a flashy item by
themselves.

## Over-Tuning

Over-tuning is the act of spending special catalysts or condensed reagents to
push a synthesis beyond its comfortable bounds.

Over-tuning can improve:

```text
perfect chance
quality ceiling
trait fusion chance
recipe morph chance
soft tier cap
```

But it should also increase:

```text
instability
failure severity
negative byproduct chance
board volatility
mechanical interference sensitivity
```

Over-tuning should feel tempting, not mandatory.

## Recoverable Failure And Alchemical Slag

Recoverable failures should produce useful waste.

### Slag

Alchemical Slag is a reagent byproduct from failed or messy synthesis.

Typical profile:

```text
categories: filler, waste, volatile
elements: high values, source-biased
shape: large or awkward
quality: low to medium
purity: low
instability: high
traits: fragile, volatile, contaminated, inert
```

Slag is useful because it fills space and provides element value. It is risky
because it can poison traits, reduce purity, and increase failure chance.

This makes failure hurt without being pure deletion.

## Overlap Policy

Default placement should block overlaps.

Accidental overlap should never silently destroy placed reagents. The player is
often handling rare, high-value, high-NBT materials, and accidental loss would
feel hostile.

### Deliberate Overwrite

The system can support deliberate overwrite later as an advanced mechanic:

```text
hold modifier key
switch to scrape/overwrite tool mode
confirm if rare traits would be destroyed
destroyed cells become slag, residue, or instability
```

This should be a feature, not the default. It can create interesting Sophie-like
placement tension, but only when the UI makes the cost obvious.

Recommended first implementation:

```text
left-click empty board cell: place carried reagent if it fits
left-click placed reagent: pick it up
right-click carried reagent: rotate
right-click placed reagent: rotate in place if it fits
overlap: placement preview turns red and placement is blocked
advanced overwrite: not implemented yet
```

## UI Requirements

The final synthesis UI needs a fresh design.

It should prioritize:

```text
large readable board
piece palette / reagent source list
rotation controls
node highlights
adjacency link visualization
empty-space penalty preview
success/perfect/failure odds
output and morph preview
trait inheritance preview
quality/purity/instability summary
filter/search by category + element + shape + trait
```

The screen should feel like a workbench, not a decorative recipe browser.

Important search targets:

```text
category:conductive element:fire shape:line_2
category:binding purity>=60 shape:single
trait:redstone_resonance
element:water>=3
filler shape:single
mechanical_interference:false
```

## Automation Contract

Automation should have tiers:

### Basic Automation

Can produce and move basic reagents, but Mechanical Interference makes them poor
for rare traits and perfect synthesis.

Good for:

```text
filler
bulk elements
slag processing
low-tier routine crafts
condensed baseline reagents
```

### Workshop Automation

Unlocked later through Atelier apparatus or room fixtures. Can reduce or avoid
Mechanical Interference for specific flows.

Requirements might include:

```text
reagent interface
stabilized output tray
registered formula
assistant workstation
high-quality atelier room
manual first discovery
```

### AE2-Style Integration

The ideal AE2 integration should deal in reagent predicates, not exact physical
vial stacks.

Examples:

```text
conductive, fire >= 2, purity >= 60
binding, shape single, amount >= 8
filler, any element, mechanical_interference allowed
```

## Chase Output Philosophy

Chase outputs should be rule-breakers, not simple stat sticks.

The best high-end synthesis rewards should break existing Minecraft rules in
satisfying, bounded ways:

```text
revive without holding a totem
explode without griefing terrain
return home without building a rail line
carry beacon-like buffs without moving a pyramid
fly without constantly spamming fireworks
repair, cleanse, or awaken rare modded relics
```

These rewards justify the cost of spatial planning, rare reagent trading,
condensed material sinks, catalyst spending, and failure risk.

### Longevity Rule

Do not usually make the player synthesize the whole chase item again when it
runs out.

Recommended model:

```text
hard synthesis creates the vessel
routine use consumes fuel, charges, cooldown, repair resources, or catalysts
perfect synthesis reduces upkeep or adds safety
important personal progression items should not vanish forever by accident
```

This preserves the achievement of making the item while keeping a long-term
material sink.

Consumable bombs are the main exception. Using up a ridiculous bomb is part of
the fantasy.

### Auto-Elixir

Working names:

```text
Goddess Cup
Pepped-Up Elixir
Auto-Elixir
```

Role:

```text
inventory-usable totem upgrade
```

Mechanic:

```text
sits anywhere in inventory
triggers on fatal damage
consumes a charge
revives the player
grants regeneration
optionally clears debuffs
has a cooldown so stacked copies cannot trivialize death
```

Chase traits:

```text
extra charges
shorter cooldown
debuff cleanse
post-revive resistance
refill efficiency
replicating/endless-style behavior as a rare capped trait
```

Balance:

The vessel should be permanent or repairable. Charges should be refilled with
expensive elixir reagents or synthesized medicine.

### Smart Bomb

Working names:

```text
Omega Craft
N/A Core
World-Safe Cataclysm
```

Role:

```text
ultimate bomb line
```

Mechanic:

```text
huge radius explosive or field effect
NBT/data-driven target filters
terrain-safe hostile mob purge
mining variant that breaks stone while preserving ores and drops
configured griefing limits
```

Chase traits:

```text
hostile-only
terrain-safe
ore-preserving
larger radius
lower friendly-fire risk
drop magnetism
reduced instability
```

Balance:

Bombs can be consumable or use rechargeable cores. Terrain-safe and ore-safe
variants should be expensive and traceable because they are very strong in
Vanilla+ and modpacks.

### Return Tool

Working names:

```text
Warp Bell
Return Wing
Meteorite Compass
```

Role:

```text
portable return-to-base tool
```

Mechanic:

```text
binds to an atelier anchor block
channels briefly
returns the player to the anchor
later upgrades allow multiple anchors or cross-dimensional return
```

Chase traits:

```text
shorter channel
lower cooldown
cross-dimensional travel
multiple anchors
safe arrival bubble
emergency return on near-death, if heavily gated
```

Balance:

The base vessel should be permanent. Normal same-dimension return can be
cooldown-based. Cross-dimensional or emergency use should consume fuel, charges,
or rare catalysts.

### Aura Charm

Working names:

```text
Aura Lantern
Incense Charm
Portable Beacon
```

Role:

```text
portable sustained buff item
```

Mechanic:

```text
kept in hotbar, charm slot, or inventory depending on compat
grants a focused persistent buff
drains synthesized fuel while active
```

Possible buffs:

```text
haste
night vision
slow falling
water breathing
fire resistance
step assist
mining stability
```

Chase traits:

```text
fuel efficiency
larger buff radius
stronger effect tier
multi-buff harmony
automatic pause when not needed
low-fuel safety behavior
```

Balance:

This is the best home for nearly infinite efficiency as a perfect synthesis
reward. Bad versions should drain fuel quickly; excellent versions should feel
like a hard-earned buff battery.

### Elytra Enhancer

Working names:

```text
Aero-Lace Coating
Sylphid Wings
Windwoven Membrane
```

Role:

```text
elytra upgrade or coating
```

Mechanic:

```text
combined with an existing elytra
provides self-propulsion
uses XP or synthesized mana/aero fuel instead of fireworks
does not replace normal elytra progression
```

Chase traits:

```text
fuel efficiency
better takeoff
safer landing
slow fall recovery
turn control
emergency stall prevention
reduced durability damage
```

Balance:

The upgrade should be permanent or repairable. Routine use should consume fuel,
XP, or charges. Perfect synthesis should make propulsion efficient, not free by
default.

### Other Strong Chase Families

Additional output families that fit the system:

```text
signature tool coatings
weapon oils
armor treatments
treated materials
room fixtures
formula registration tools
automation/workshop parts
rare catalysts
recipe morph discoveries
Curios/accessories
artifact surgery kits
gem catalysts
```

Early chase should lean toward tool coatings because they are useful and bounded.
Midgame chase should lean toward catalysts and room fixtures because they feed
back into the synthesis loop. Server economy chase should lean toward rare
trait-bearing reagents, catalysts, and artifact/gem services because those are
tradable without every player needing the same final item.

## Modpack Backbone And Compat

Zen Atelier should be able to become a modpack backbone, but not by becoming
ProjectE.

ProjectE-style transmutation says:

```text
everything has value
value can become anything
```

Zen Atelier should instead say:

```text
materials have identity
identity can be transformed through skill, risk, and puzzle constraints
```

Compat should focus on bounded item surgery, awakening, cleansing, repair,
rerolling, refueling, and trait work. It should not focus on freely duplicating
rare objects or converting generic farm output into arbitrary relics.

### Artifacts And Relics

Artifacts, relics, and Curios-like items can be synthesis targets.

Good transformations:

```text
repair a relic
cleanse a curse or bad modifier
reroll one modifier while locking another
add charges or improve recharge behavior
reduce cooldown
convert durability upkeep into fuel upkeep
stabilize a chaotic artifact
awaken a dormant relic after meeting room/catalyst requirements
```

Bad transformations:

```text
duplicate relics freely
turn any resource into any artifact
reroll forever with trivial inputs
ignore the source item's identity
```

Recommended model:

```text
input item + board reagents + catalyst -> modified same item
```

The input item remains the anchor. Synthesis changes or awakens it; it does not
erase identity and print a different treasure.

### Apotheosis Gems

Apotheosis-style gems are an especially strong fit because they already express
rarity, affixes, and build goals.

Possible compat:

```text
break gems into crystalline/resonant reagents
cleanse flawed gems with high-purity reagents
fuse low gems into one awkward high-value reagent
reroll one affix family through a spatial puzzle
create socket catalysts that bias future gem outcomes
stabilize a gem so it is safer to socket or unsocket
```

Guardrails:

```text
no generic farm-output-to-best-gem conversion
no free duplication
no bypassing the other mod's rarity structure without explicit pack config
mod compat definitions must be opt-in and data-driven
```

### Chase Item Longevity

Compat chase items should follow the same vessel/upkeep model as native Atelier
chase items. The source mod's item identity remains the vessel, and Atelier
synthesis modifies, awakens, repairs, refuels, or stabilizes it.

For example, a relic should not be consumed and replaced by an unrelated Atelier
item unless the compat definition explicitly treats that as an intended
transformation.

### Compat API Direction

Future compat should expose synthesis as constrained transformation hooks:

```text
Can this item be modified?
Which transformation families are allowed?
Which modifiers can be locked, cleansed, rerolled, or upgraded?
What reagent categories/elements/traits are meaningful for this item?
Which outcomes are forbidden by the source mod or pack config?
```

The pack author's job is to define what identities can transform into. Atelier's
job is to make that transformation expensive, readable, risky, and fun.

## Planner Requirements

The SynthesisPlanner should eventually evaluate:

```text
board bounds
shape occupancy
empty cell count
node coverage
element node activation
morph node activation
adjacency trait links
category requirements
element requirements
catalyst effects
mechanical interference penalties
quality/purity/instability aggregation
success/perfect/failure odds
recoverable failure byproducts
cap decisions
```

The preview and execution path must use the same board evaluation. The UI must
not have separate hidden rules.

## Roll Trace Requirements

Every synthesis result should be able to explain:

```text
base recipe odds
room/apparatus modifiers
empty-space penalty
coverage score
node bonuses
trait adjacency results
catalyst effects
quality aggregation
purity/instability effects
cap decisions
mechanical interference penalties
selected outcome
byproducts
```

This is required for debugging, tuning, and player-facing advanced tooltips.

## Data Shape

Extraction output definitions should support:

```json
{
  "reagent": "zen_atelier:crystalline_dust",
  "categories": [ "zen_atelier:conductive", "zen_atelier:abrasive" ],
  "elements": { "fire": 2, "wind": 1 },
  "shape": {
    "id": "line_2",
    "cells": [
      { "x": 0, "y": 0 },
      { "x": 1, "y": 0 }
    ]
  },
  "quality_range": { "min": 35, "max": 60 },
  "purity_range": { "min": 45, "max": 75 },
  "instability_range": { "min": 5, "max": 25 },
  "traits": [ "zen_atelier:redstone_resonance" ],
  "source_hints": [ "minecraft:redstone_block" ]
}
```

Recipe requirements should support:

```json
{
  "query": {
    "required_categories": [ "zen_atelier:conductive" ],
    "min_elements": { "fire": 2 },
    "min_purity": 60
  },
  "amount": 1
}
```

Board recipes should later add:

```text
board size
fixed nodes
node requirements
empty-space penalty curve
allowed catalyst slots
morph targets
failure slag profile
overwrite policy
```

## Open Questions

- Should Mechanical Interference be a trait, a boolean flag, or a penalty record?
- Should condensed reagents grow in amount, shape size, or both?
- Should slag be generated per failed reagent, per empty board cell, or per
  failure severity?
- Should catalysts occupy normal board cells, special catalyst slots, or both?
- Should perfect synthesis require full board coverage, or only sharply reward
  it?
- When advanced overwrite exists, should destroyed cells become slag, disappear,
  or merge into the new reagent as contamination?

## Near-Term Implementation Order

1. Finish persistent reagent data: categories, elements, shape, quality, purity,
   instability, traits, source hints.
2. Update extraction profiles to author categories and shapes.
3. Add condensation rules to extraction attempts.
4. Add a mechanical collection path distinction: player-collected versus
   automation-collected.
5. Replace the temporary synthesis screen with a fresh board-first screen.
6. Move board placement/evaluation into core planner classes.
7. Add empty-space penalty, adjacency links, and node activation.
8. Add recoverable failure slag.
9. Add catalysts and over-tuning.
10. Add automation interfaces after the manual and board-first loop is fun.
