# Pokédex-Style Room Discovery UI Design

## Issues to Fix

### 1. Score Format Error
**Issue**: Score display may have formatting issues  
**Solution**: Format as clean "XX%" with proper tier labels

### 2. Color 1-Bit Icons to Match Theme
**Issue**: Icons are monochrome, don't fit mod aesthetic  
**Solution**: 
- Extract icons and recolor using mod's color scheme:
  - Primary: #365b3d (green)
  - Secondary: #2f261c (brown)
  - Accent: #d8b46a (gold)
  - Warning: #9f3d35 (red)
- Create colored versions of key anchor/signal icons

### 3. Pokédex-Style UI Implementation

#### Design Concept
Similar to Pokédex from Pokémon:
- **Undiscovered Rooms**: Silhouette/shadow with "?" and hints
- **Discovered Rooms**: Full card with details and stats

#### Card Layout (Discovered)
```
┌─────────────────────┐
│  [Room Icon]        │
│  Atelier            │ (Room Name)
├─────────────────────┤
│ ANCHOR:             │
│  🧪 Brewing Stand   │
│  🍲 Cauldron        │
├─────────────────────┤
│ AFFINITY:           │
│  ✓ Plants           │
│  ✓ Potions          │
│  ✗ Machines         │
├─────────────────────┤
│ BEST SCORE: 92%     │ ⭐⭐⭐⭐
│ Excellent           │
├─────────────────────┤
│ BONUSES:            │
│  🧪 Brewing Speed   │
│  💊 Potion Quality  │
└─────────────────────┘
```

#### Card Layout (Undiscovered)
```
┌─────────────────────┐
│  [? Silhouette]     │
│  ? ? ? ? ?          │
├─────────────────────┤
│ HINTS:              │
│  • Heat & flame     │
│  • Metal & stone    │
│  • Loud work        │
│                     │
│ [CLICK FOR MORE]    │
└─────────────────────┘
```

## Implementation Plan

### Phase 1: Fix Score Display
- Clean up tooltip formatting
- Add star rating display (⭐ scale)
- Improve tier labels

### Phase 2: Color Icons
Use Python/ImageMagick to create themed versions:
- Green (#365b3d) - Active/positive
- Brown (#2f261c) - Neutral/material
- Gold (#d8b46a) - Important
- Red (#9f3d35) - Warning/avoid

### Phase 3: Create Stat Card Component
New Patchouli custom component or expand existing:
- Render full room details
- Show anchors with icons
- Show affinity signals
- Display bonuses
- Score bar with stars

### Phase 4: Pokédex Grid Page
Main discovery page showing:
- Grid of all room types
- Compact cards for undiscovered (silhouettes + hints)
- Compact cards for discovered (icon + score)
- Click-to-expand for full details

### Phase 5: Detailed Page (Optional)
Individual pages for each room with:
- Large icon/art
- Complete stats
- Bonus descriptions
- Building tips

## Data Needed from Server

Update discovery sync to include:
```java
{
  profileId: "zen_atelier:atelier",
  discovered: true,
  bestScore: 92,
  anchors: ["brewing_stand", "cauldron"],
  signals: ["plants", "potions", "pots"],
  warnings: ["machines", "smoke"],
  bonuses: ["brewing_speed", "potion_quality"],
  hints: ["heat", "flame", "work"] // for undiscovered
}
```

## Files to Create/Modify

### New Components
- `RoomStatCardComponent.java` - Detailed stat card display
- `RoomPokedexGridComponent.java` - Grid of room cards

### Modified Files
- `RoomDiscoveryPageComponent.java` - Improve layout
- `DiscoveryDataSyncPayload.java` - Add bonus/hint data
- Language file - Add bonus descriptions

### Assets
- `room_icons_colored/` - Recolored icon versions
- `room_bonuses.json` - Define bonuses per room

## Priority Order
1. **HIGH**: Fix score format (quick win)
2. **HIGH**: Color icons (visual improvement)
3. **MEDIUM**: Stat card component (main feature)
4. **MEDIUM**: Pokédex grid (better organization)
5. **LOW**: Detailed pages (polish)

## Success Criteria
✓ Score displays cleanly with tier and stars  
✓ Icons match mod's color theme  
✓ Undiscovered rooms show hints, not full data  
✓ Discovered rooms show as stat cards  
✓ Card layout is readable and attractive  
✓ Pokédex feeling: player wants to "catch 'em all"
