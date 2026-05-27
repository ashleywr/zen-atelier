# Patchouli Book Visual Overhaul - Design Plan

## Overview
Transform the Room Journal from text-heavy to visually-rich using 1-bit pixel art icons from the extracted sprite collection.

## Design Principles
- **Consistent 1-bit aesthetic**: All icons follow the same monochrome pixel art style
- **Minecraft compatible**: Icons scale well at 16x16 and maintain readability
- **Color scheme**: Use existing book colors (#365b3d green, #2f261c brown, #416f8f blue)
- **Progressive disclosure**: Simple to complex, visual then textual
- **Thematic consistency**: Each room type has a visual "signature"

## Implementation Strategy

### Phase 1: Create Icon Sheets (HIGH PRIORITY)
Extract curated sprites for each room type and arrange into Minecraft-friendly 256x256 sheets:

**Sheet 1: Anchors (Key Blocks)**
- Brewing Stand (Atelier)
- Bed (Bedroom)
- Furnace/Cauldron (Kitchen)
- Bookshelf/Lectern (Library)
- Chest/Storage (Storage)
- Anvil (Smithy)
- Enchanting Table (Enchanting)
- Loom (Loom Room)
- Composter (Gardener's Shed)
- Cartography Table (Map Room)
- And more...

**Sheet 2: Positive Signals (What Helps)**
- Light sources (torches, lanterns, candles)
- Decorative blocks (carpet, colored blocks)
- Organized storage
- Tools and equipment
- Plants and flowers
- Books and shelves

**Sheet 3: Negative Signals (What Hurts)**
- Industrial machinery
- Redstone clutter
- Mismatched materials
- Poor lighting
- Disorganization
- Industrial vs. peaceful contrast icons

**Sheet 4: Room Type Identifiers**
- Small visual "badges" for each room type showing anchor + key signal combo

### Phase 2: Update Patchouli Pages (MEDIUM PRIORITY)
Enhance existing pages with visual elements:

**Page Layout Improvements:**
1. **Page 1 (Overview)**: Add anchor icon at top, description
2. **Page 2 (Details)**: Add 3-4 signal icons with short labels
3. **Page 3 (Blueprint Reference)**: NEW visual guide showing:
   ```
   ANCHOR(S): [icon] [icon name]
   HELPS: [icon] [icon] [icon] [name] [name] [name]
   AVOID: [icon] [icon] [icon] [name] [name] [name]
   ```

**Category Pages:**
- Add visual anchor badges to "How to Use" and "Room Hints" pages
- Create visual comparison pages

### Phase 3: Create Reference Guides (MEDIUM PRIORITY)
New Patchouli entries using visual layouts:

**Entry: "Room Type Identification"**
- Grid showing all room types with their anchor icons and signature signals
- Quick visual reference guide for players

**Entry: "Visual Quick Start"**
- Show room type "blueprints" using icon combinations
- Example layouts for successful rooms

**Entry: "Building Your First Room"**
- Visual step-by-step guide using icons

### Phase 4: Design Visual Page Type (OPTIONAL)
Custom Patchouli component or text styling to display:
- Icon + label combinations inline
- Icon grids with descriptions
- Visual comparisons side-by-side

## File Structure

```
assets/zen_atelier/textures/gui/
├── book_visual_guide/
│   ├── anchors.png                    (256x256 icon sheet)
│   ├── positive_signals.png           (256x256 icon sheet)
│   ├── negative_signals.png           (256x256 icon sheet)
│   ├── room_type_badges.png           (256x256 icon sheet)
│   └── reference_grid.png             (256x512+ reference chart)
└── ...existing files...
```

## Language Strings to Add

For each room type, we'll add icon references:
```json
"patchouli.zen_atelier.room_journal.entry.bedrooms.anchor": "Bed",
"patchouli.zen_atelier.room_journal.entry.bedrooms.signals": "Light • Calm • Storage",
"patchouli.zen_atelier.room_journal.entry.bedrooms.warns": "Heat • Work • Clutter"
```

## Visual Examples

### Bedroom Blueprint Reference
```
┌─────────────────┐
│ 🛏️ BED          │ ANCHOR
├─────────────────┤
│ 🕯️ 💡 🏺        │ HELPS (Calm light, Storage)
├─────────────────┤
│ ❌ 🔥 ⚙️        │ AVOID (Heat, Machines)
└─────────────────┘
```

### Atelier Blueprint Reference
```
┌─────────────────────────┐
│ 🧪 BREWING + 🍲 CAULDRON│ ANCHORS
├─────────────────────────┤
│ 🌱 💧 🏺 🕯️            │ HELPS (Plants, Water, Calm)
├─────────────────────────┤
│ ⚙️ 🔥 (alone) 🏗️       │ AVOID (Machines, just fire)
└─────────────────────────┘
```

## Phases Summary

| Phase | Focus | Priority | Effort | Impact |
|-------|-------|----------|--------|--------|
| 1 | Create icon sheets | HIGH | 2-3 hrs | Major visual upgrade |
| 2 | Update page layouts | HIGH | 2-3 hrs | Better usability |
| 3 | Reference guides | MEDIUM | 2 hrs | Educational boost |
| 4 | Custom components | LOW | 3-4 hrs | Polish & uniqueness |

## Success Criteria
✓ Icons are consistent with 1-bit aesthetic  
✓ Players can visually scan room types quickly  
✓ Anchors, signals, and warnings are immediately identifiable  
✓ Pages load without errors  
✓ Icons remain readable at all scales  
✓ Performance unaffected  

## Notes
- Start with Phase 1 (icon sheets) — this unblocks Phases 2-4
- Can be done incrementally (do one room type at a time)
- Icons can be reused across multiple room types where appropriate
- Consider adding custom mod-specific icons later (e.g., room type badges)
