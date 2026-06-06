# Atelier Art Commission Brief

This document is a working brief for commissioning high-impact art for the Atelier mod. It is not final art direction; it is meant to help explain what assets matter most, why they matter, and what technical shape would be useful for implementation.

## Project Feel

The mod should feel whimsical, tactile, and craft-focused, closer to Atelier and Ars Nouveau than dark occult alchemy. The core fantasy is experimentation: toss materials into equipment, watch them react, learn what they become, and use those discoveries to synthesize better items.

The art should support a playful workshop mood:

- Warm, hand-crafted, readable at Minecraft scale.
- Magical but not grim or horror-coded.
- Clear visual feedback for states like ready, active, unstable, complete, and failed.
- Icon-first where possible, because the design avoids text-heavy guidebook reading.

## Highest Priority Assets

### 1. Extraction Cauldron

This is one of the most important commissions because players interact with it constantly during discovery.

Needed assets:

- Custom block model and texture for the extraction cauldron.
- Visual states:
  - Empty or normal.
  - Ready/primed with weak solvent awakened by Alchemist's Primer or Crucible
    Spoon.
  - Actively extracting.
  - Unstable/rejecting/failing.
- Liquid surface treatment:
  - Greenish ready solvent.
  - Orange/yellow active extraction.
  - Red unstable/rejection state.
  - Optional animated ripple/bubble surface.

Implementation notes:

- A block model plus texture atlas is ideal.
- If liquid is separate from the cauldron body, it can be tinted or swapped by state.
- Keep silhouettes readable from normal Minecraft camera distance.

### 1A. Ignition Items And Starter Ingredients

These assets define the first emotional read of the mod and should feel like
alchemist tools and field discoveries rather than generic magic flowers.

Needed assets:

- Alchemist's Primer item icon: a practical annotated booklet, not an ornate
  spell tome.
- Crucible Spoon item icon: copper-tipped stirring tool, readable at 16x16.
- Uni / Spiky Burr item icon: small chestnut-like spiked throwable.
- Taun Herb item icon and plant block concept: humble but faintly luminous.
- Phlogiston Pebble item icon: small ember-stone, warm and mineral.
- Aqua Gel and Ember Gel item icons: slime-derived gels with clear elemental
  color differences.

Implementation notes:

- Primer and Spoon should communicate "start the workstation".
- Starter ingredients should look collectible and odd even before the player
  knows their reagent use.
- Keep shapes distinct in inventory so players naturally hoard and compare them.

### 2. Synthesis Station

This is the other major workstation and should visually anchor the synthesis loop.

Needed assets:

- Custom block model and texture for the synthesis station.
- It can look like an atelier workbench, mixing station, or compact alchemy apparatus.
- Optional details:
  - Small flask rack.
  - Burner or candle.
  - Notebook/codex surface.
  - Mortar, tools, or reagent tray.

Useful visual states:

- Idle.
- Ready to synthesize.
- Active synthesis.
- Successful completion.
- Failed or unstable synthesis.

Implementation notes:

- Static model first is enough.
- Separate small moving/rendered parts would be useful later, but not required for the first pass.

### 3. Reagent Vials

Reagents are core rewards and inventory items, so their icons should feel valuable and collectible.

Recommended asset structure:

- Base glass vial sprite.
- Tintable liquid layer.
- Rarity/quality border overlays.
- Optional small symbol overlays for reagent families.

Useful reagent family symbols:

- Binding/preserving: honey, seal, wax, knot.
- Organic/life: leaf, sprout, droplet.
- Abrasive/stone: shard, grit, angular chip.
- Spark/conductive: spark, redstone pulse, lightning mark.
- Elastic/sticky: slime/goop mark.
- Fibrous: thread, strand, weave.
- Luminous: star, glow, sun fleck.
- Harmonic/crystal: crystal shard, ring wave.

Implementation notes:

- Layered sprites are better than making every possible vial manually.
- Transparent PNG layers are preferred.
- If possible, liquid should be designed to tint cleanly.

### 4. Codex and UI Kit

The codex is intended to be a custom interactive UI, not a Patchouli-style reading book. It should support discovery through icons and experimentation.

Needed assets:

- Alchemist codex item icon.
- Codex screen frame/background.
- 9-slice panel textures for scalable windows and panels.
- Button/slot frames.
- Selected/hover states.
- Unknown recipe/source outline treatment.
- Category icons:
  - Bombs.
  - Healing.
  - Food.
  - Tools.
  - Materials.
  - Misc.

Implementation notes:

- 9-slice textures are highly useful. A small frame texture can scale to many UI sizes while keeping pixel corners sharp.
- UI art should be readable and restrained. The codex can have charm, but the screen still needs to scan quickly.

### 5. Alchemy Particle Sprite Sheet

Particles are a high-impact way to make crafting feel rewarding. Even a small custom particle sheet would make the mod feel much more distinct than vanilla-only particles.

Useful particle sprites:

- Soft mist puff.
- Smoke curl.
- Bubble.
- Spark/star glint.
- Droplet.
- Flame wisp.
- Magic fleck or tiny glyph.
- Slime/goop fleck.
- Crystal shimmer.
- Dissolve ring/ripple.

Implementation notes:

- Small transparent PNG sprites or a particle atlas are useful.
- Particles should be readable on both light and dark backgrounds.
- Variants that can be tinted are more reusable than many fixed-color sprites.

## Secondary Assets

These are important, but should come after the workstations, vials, codex UI, and particle sheet.

### Key Result Items

Start with the items players will craft or use most:

- Bombs.
- Healing salves or potions.
- Mining/tool coatings.
- Stabilizers/catalysts.
- Failed byproducts/residue.

### Ingredient Dissolution Effects

These support the moment where an item enters a cauldron and reacts.

Useful assets:

- Surface ripple.
- Dissolve burst.
- Colored reaction ring.
- Small residue flecks.
- Steam/smoke puff.

### Future Apparatus / Multiblock Parts

If the mod expands into larger workshop structures:

- Alembic.
- Distillation tubes.
- Flask rack.
- Burner.
- Stirrer.
- Condenser.
- Pump or piston assembly.

These can support later moving parts, but do not need to be commissioned before the core loop is fun.

## Animation and VFX Direction

Useful effects to design toward:

- Ingredient bobs on the cauldron surface, spins, sinks, then dissolves.
- Ready liquid gently bubbles or glows.
- Active extraction swirls more strongly.
- Unstable reactions shake, smoke, flash, or sputter.
- Successful synthesis emits a category-colored burst.
- High-tier reactions can spill mist over the station edge.

Technical caution:

- True dynamic liquid shaders, volumetric fog, real colored lighting, and heat distortion are more complex than sprites/models.
- The first version should fake these with animated textures, particles, emissive-looking art, and block/render states.

## Technical Preferences

Preferred deliverables:

- PNG textures with transparency where appropriate.
- Pixel art compatible with Minecraft scale.
- Separate layers for tintable pieces when useful.
- Source files if available, such as Aseprite, PSD, or layered image files.
- Clear naming for variants and states.

Useful sizes:

- Item icons: 16x16 is standard, but 32x32 source files can be useful if they are downscaled cleanly.
- Block textures: usually 16x16 per face unless the model needs more detail.
- UI panels: small 9-slice textures, commonly 24x24 or 48x48 source pieces.
- Particle sprites: small transparent sprites, often 8x8, 16x16, or 32x32 depending on style.

Important constraints:

- Assets need to remain readable in Minecraft lighting.
- Avoid overly noisy details that disappear at normal camera distance.
- Prefer modular/reusable pieces over many one-off variants.
- Avoid relying only on text; icons and silhouettes should communicate function.

## Suggested First Commission Package

If commissioning a first batch, the strongest package would be:

1. Extraction cauldron model/textures with ready, active, and unstable visual states.
2. Synthesis station model/texture.
3. Layered reagent vial sprites with tintable liquid and rarity borders.
4. Codex/UI kit with 9-slice panels, buttons, slots, and category icons.
5. Small alchemy particle sprite sheet.

This package covers the assets players will see constantly and gives the implementation enough reusable art to make the core discovery/synthesis loop feel intentional.

## Questions To Ask Artists

- Are you comfortable making Minecraft-style block models and textures?
- Can you provide layered source files for tintable vials/UI parts?
- Can you design 9-slice UI textures?
- Can you make small particle sprites that read well at Minecraft scale?
- Do you prefer working from rough in-game screenshots, mood boards, or written briefs?
- Can you separate model, texture, and animation work into milestones?
- What deliverable formats do you prefer?

## First Internal Art Decisions To Finalize

- Exact workstation silhouettes.
- Whether reagent vials are bottles, ampoules, crystals, capsules, or another form.
- How whimsical vs. ornate the codex UI should be.
- Whether the main palette leans warm workshop, bright magical, or soft botanical.
- How many reagent families need unique symbols in the first slice.
