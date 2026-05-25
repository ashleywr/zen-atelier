# Mod Integration Guide

Zen Atelier is designed to let other mods and data packs teach it about new room
types without adding a hard dependency on Atelier. The main integration surface is
data driven: add room profile JSON, zone definition JSON, and optional Patchouli
book entries in your own namespace.

This guide targets Minecraft 1.21.1 and NeoForge.

## Add A Room Profile

Room profiles live under:

```text
data/<your_modid>/room_profiles/<profile_name>.json
```

Example:

```json
{
  "id": "example_mod:frog_habitat",
  "display_name": "room_type.example_mod.frog_habitat",
  "zone_definition": "example_mod:damp_habitat",
  "anchors": [
    "#c:frog_plants",
    "#c:flowers"
  ],
  "required_features": [
    {
      "signal": "frog_plant",
      "minimum": 2
    },
    {
      "signal": "water_coverage",
      "minimum": 3
    }
  ],
  "quality_signals": [
    "frog_plant",
    "water_coverage",
    "plant_diversity",
    "lighting"
  ],
  "penalty_signals": [
    "industrial_blocks"
  ]
}
```

Fields:

| Field | Required | Meaning |
|---|---:|---|
| `id` | yes | Stable room profile id. Use your own namespace. |
| `display_name` | yes | Translation key shown to players. |
| `zone_definition` | yes | Zone definition id used for room atmosphere metadata. |
| `anchors` | no | Block ids or block tags that suggest this room type. Tags use `#namespace:path`. |
| `required_features` | yes | Signal counts required for this room type to match. |
| `quality_signals` | no | Player-facing/design metadata for quality concepts. |
| `penalty_signals` | no | Player-facing/design metadata for things that hurt the room identity. |
| `required_surfaces` | no | Floor/surface blocks or tags required near the room. |

## Add A Zone Definition

Zone definitions live under:

```text
data/<your_modid>/zones/<zone_name>.json
```

Example:

```json
{
  "id": "example_mod:damp_habitat",
  "display_name": "zone.example_mod.damp_habitat",
  "sealed": false,
  "base_temperature": 22.0,
  "base_humidity": 85.0,
  "requires_ceiling": false
}
```

Fields:

| Field | Required | Meaning |
|---|---:|---|
| `id` | yes | Stable zone definition id. |
| `display_name` | yes | Translation key shown to players. |
| `sealed` | no | Whether the zone is expected to be sealed. Defaults to `false`. |
| `base_temperature` | no | Atmosphere metadata. Defaults to `20.0`. |
| `base_humidity` | no | Atmosphere metadata. Defaults to `50.0`. |
| `requires_ceiling` | no | Whether this zone expects a ceiling. Defaults to `false`. |

## Supported Signals

Signals are semantic names used in `required_features`. They are counted from
blocks adjacent to the room interior. Prefer common `#c:` block tags in your mod
so Atelier and other mods can agree on what your blocks mean.

Currently recognized signals:

| Signal | Good block/tag examples |
|---|---|
| `bed` | `#c:beds`, `#minecraft:beds` |
| `bookshelf` | `#c:bookshelves`, `#c:chiseled_bookshelves` |
| `brewing_stand` | `#c:brewing_stands`, `minecraft:brewing_stand` |
| `cartography_table` | `#c:cartography_tables`, `minecraft:cartography_table` |
| `cauldron` | `#c:cauldrons`, vanilla cauldrons |
| `composter` | `#c:composters`, `minecraft:composter` |
| `cooking_block` | `#c:player_workstations/furnaces`, `#c:furnaces`, `#c:smokers`, `#c:campfires` |
| `crafting_table` | `#c:player_workstations/crafting_tables`, `#c:crafting_tables`, `#c:workbench` |
| `enchanting_table` | `#c:enchanting_tables`, `minecraft:enchanting_table` |
| `fletching_table` | `#c:fletching_tables`, `minecraft:fletching_table` |
| `frog_plant` | `#c:frog_plants`, dripleaves, lily pads |
| `glass` | `#c:glass_blocks`, `#c:glass_panes` |
| `loom` | `#c:looms`, `minecraft:loom` |
| `plant` | `#c:crops`, `#c:flowers`, `#c:saplings`, `#c:leaves`, `#c:grass_like`, `#c:mushrooms` |
| `smithing_or_repair_block` | `#c:anvils`, `#c:smithing_tables`, `#c:grindstones`, `#c:blast_furnaces` |
| `stonecutter` | `#c:stonecutters`, `minecraft:stonecutter` |
| `storage` | `#c:chests`, `#c:barrels` |
| `villager_workstation` | `#c:villager_job_sites` |
| `tool_blocks` | Crafting, furnace, village job-site, grindstone, and stonecutter style blocks |
| `stone_or_metal_materials` | Common stone, cobblestone, obsidian, chains, iron/copper/gold storage blocks |
| `urban_block` | Concrete, glazed terracotta, glasswork, chains, iron bars, lanterns |
| `factory_block` | Machines, pipes, furnaces, hoppers, pistons, observers, metal/redstone blocks |
| `industrial_blocks` | Any `urban_block` or `factory_block` |
| `water_coverage` | Source water touching the room interior |

## Discovery And Patchouli Flags

When a player discovers a room, Atelier syncs discovery state to the client and
sets Patchouli config flags. You can use these flags to gate optional book
entries.

For a discovered profile id like `example_mod:frog_habitat`, Atelier sets:

```text
zen_atelier.discovered.frog_habitat
zen_atelier.discovered.namespace.example_mod
```

For amphibian/habitat-like ids, Atelier also sets:

```text
zen_atelier.discovered.amphibian_habitat
```

That broad habitat flag is intended for integrations such as Amphibia, where
different mods may name the room `frog_habitat`, `toad_terrarium`, `vivarium`,
or similar.

Patchouli supports mod-loaded flags with `mod:<modid>`. To add optional pages to
Atelier's book from your own mod, ship Patchouli assets under Atelier's book
asset path and gate them:

```json
{
  "name": "patchouli.example_mod.frog_habitat.name",
  "icon": "minecraft:lily_pad",
  "category": "zen_atelier:rooms",
  "flag": "&mod:amphibia,zen_atelier.discovered.amphibian_habitat",
  "pages": [
    {
      "type": "patchouli:text",
      "text": "patchouli.example_mod.frog_habitat.page.1"
    }
  ]
}
```

Book entries go under:

```text
assets/zen_atelier/patchouli_books/room_journal/en_us/entries/<category>/<entry>.json
```

Language keys can stay in your own namespace:

```text
assets/<your_modid>/lang/en_us.json
```

If Patchouli is absent, Atelier still works. If your mod is absent, entries gated
with `mod:<your_modid>` do not appear.

## Compatibility Tips

- Use common `#c:` tags whenever possible. This lets Atelier recognize modded
  variants without exact block ids.
- Keep room profile ids descriptive. Discovery integration can infer broad flags
  from names like `frog_habitat`, `toad_terrarium`, `vivarium`, and `habitat`.
- Use required features for hard identity checks. Use quality and penalty
  signals to communicate what makes the room feel better or worse.
- Avoid making broad materials hard requirements unless the room really needs
  them. For example, `industrial_blocks` is useful as a penalty for gardens and
  libraries, but usually too broad as a required feature.
- Test in a real world or GameTest when adding profiles that depend on water,
  block tags, room discovery, or Patchouli unlocks.
