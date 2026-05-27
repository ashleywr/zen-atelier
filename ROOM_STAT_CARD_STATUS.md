# Pokédex-Style UI Implementation Status

## ✅ Completed

### 1. Score Display Improvements
**File**: `RoomDiscoveryPageComponent.java`
- ✅ Added `formatScore()` method for clean "XXX%" display
- ✅ Added `getStarRating()` method showing ⭐ ratings based on score
- ✅ Improved tooltip formatting with hints for undiscovered rooms
- ✅ Better tier labels (Fair, Good, Excellent)

**Changes**:
```java
// Old: "Best: 92% Excellent"
// New: "Score: 092% Excellent ⭐⭐⭐⭐"
```

### 2. Icon Coloring System
**Files Created**:
- `ICON_COLORING_GUIDE.md` — Step-by-step Aseprite instructions

**Color Palette**:
- Primary Green (#365b3d) — Anchors, positive
- Bright Green (#70B55B) — Discovered, active
- Brown (#2f261c) — Materials, neutral
- Gold (#d8b46a) — Important, special
- Blue (#416f8f) — Signals, information
- Red (#9f3d35) — Warnings, avoid

**Instructions Provided**: Color 10 key icons in Aseprite, save to colored/ folder

### 3. Pokédex Stat Card Component
**File Created**: `RoomStatCardComponent.java` (291 lines)

**Features**:
- 120×160px card layout (Pokédex-style)
- **Discovered rooms show**:
  - Room icon (32×32)
  - Name in green
  - Score percentage
  - Tier label (Fair/Good/Excellent/etc.)
  - Star rating (☆ to ⭐⭐⭐⭐⭐)
- **Undiscovered rooms show**:
  - Gray silhouette icon with "?"
  - "Undiscovered" label
  - First 2 hints to guide discovery
  - Darker coloring to indicate locked

**Colors Used**:
- Background: #0F1A10 (very dark green)
- Border: #182A18 (dark green)
- Discovered: #70B55B (bright green)
- Labels: #D8B46A (gold), #416F8F (blue)
- Undiscovered: #888888 (gray)

## 🔄 In Progress / Ready to Integrate

### Component Registration
**Next Step**: Register `RoomStatCardComponent` with Patchouli in `NetworkHandler.java`:
```java
ComponentRegistry.register(
    "zen_atelier:room_stat_card",
    RoomStatCardComponent.class
);
```

### Usage in Patchouli Pages
Once registered, use in JSON entries like:
```json
{
  "type": "zen_atelier:room_stat_card",
  "room_id": "zen_atelier:atelier"
}
```

## 📋 To-Do

### HIGH Priority
- [ ] Register stat card component with Patchouli
- [ ] Create colored versions of 10 key icons in Aseprite
- [ ] Test stat cards render correctly in book
- [ ] Verify score display works (test with different scores)

### MEDIUM Priority
- [ ] Create Pokédex grid component to show all rooms
- [ ] Add bonus descriptions to room profiles
- [ ] Create detailed stat pages per room type
- [ ] Add click-to-expand functionality (if possible with Patchouli)

### LOW Priority
- [ ] Custom animations for card flip (discovered/undiscovered)
- [ ] Sound effects on discovery
- [ ] Special visual effects for rare discoveries
- [ ] Achievement badges

## 📊 Design Reference

### Stat Card Layout (Discovered)
```
┌──────────────────┐
│   [32x32 Icon]   │  (Room icon or item)
│   Room Name      │  (Green colored text)
├──────────────────┤
│ Score:        92%│  (Gold label, green value)
│ Excellent ⭐⭐⭐⭐│  (Blue tier, gold stars)
└──────────────────┘
```

### Stat Card Layout (Undiscovered)
```
┌──────────────────┐
│   [???  ?]       │  (Gray silhouette + ?)
│ Undiscovered     │  (Gray text)
├──────────────────┤
│ Hints:           │  (Gray label)
│ • Heat & flame   │  (Dark gray hint)
│ • Metal & stone  │
└──────────────────┘
```

## Implementation Notes

### Color Integration
- All colors match book.json theme (green #365b3d, etc.)
- Cards use darker variant (#0F1A10) for better contrast
- Text colors follow Patchouli macro system ($() syntax)

### Score System
- Score range: 0-100%
- ☆ = 0-29%
- ⭐ = 30-49%
- ⭐⭐ = 50-69%
- ⭐⭐⭐ = 70-79%
- ⭐⭐⭐⭐ = 80-89%
- ⭐⭐⭐⭐⭐ = 90-100%

### Discovery Logic
- Undiscovered rooms pulled from `ClientDiscoveryData.isDiscovered()`
- Shows first 2 hints from `RoomCatalogSyncPayload.Entry.hints()`
- Discovered rooms show actual best score from player data
- Proper fallback if data not synced yet

## Files Ready for Testing

1. ✅ `RoomDiscoveryPageComponent.java` — Updated with star ratings
2. ✅ `RoomStatCardComponent.java` — Complete stat card implementation
3. ✅ `ICON_COLORING_GUIDE.md` — Instructions for icon coloring
4. ✅ `POKEDEX_UI_DESIGN.md` — Full design documentation

## Next Session Tasks

1. Register component in NetworkHandler
2. Color the 10 key icons (takes ~5 min per icon in Aseprite)
3. Test in-game with stat card pages
4. Iterate on layout if needed
5. Create grid component for full Pokédex view

---

**Estimated Time to MVP**: 30-45 minutes
- Component registration: 5 min
- Icon coloring: 5-10 min per icon (50 min total, can skip some)
- Testing & fixes: 15-20 min
