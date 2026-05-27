# Icon Coloring Guide - Using Aseprite

Your mod theme colors:
- **Primary Green**: #365b3d (anchors, positive elements)
- **Bright Green**: #70B55B (discovered, active)
- **Brown**: #2f261c (materials, neutral)
- **Gold**: #d8b46a (special, important)
- **Blue**: #416f8f (signals, information)
- **Red**: #9f3d35 (warnings, avoid)

## Quick Steps in Aseprite

1. Open an icon: `C:\Users\ashle\Downloads\1-bit_Pixel_Icons\Sprites\[icon_name].png`

2. **Color > Color Balance** or **Image > Mode > Indexed** to change colors

3. **Easier Method - Hue-Saturation**:
   - Select > By Color (select all white pixels)
   - Colors > Hue-Saturation
   - Adjust to desired color

4. **Best Method - Replace Color**:
   - Select > By Color (white pixels)
   - Edit > Fill > With Color
   - Choose your mod color from the palette

5. Save to: `C:\WorkDir\Minecraft Mods\Atelier\src\main\resources\assets\zen_atelier\textures\gui\book_visual_guide\colored\[icon_name]_[color].png`

## Icon Color Assignments

| Icon | Color | Purpose |
|------|-------|---------|
| brewing_anchor.png | Blue (#416f8f) | Atelier anchor |
| cauldron_signal.png | Red (#9f3d35) | Hot/fire signal |
| book_anchor.png | Blue (#416f8f) | Library anchor |
| lantern_signal.png | Gold (#d8b46a) | Light signal |
| hammer_anchor.png | Brown (#2f261c) | Smithy anchor |
| torch_signal.png | Red (#9f3d35) | Heat signal |
| plant_signal.png | Green (#365b3d) | Nature/growth |
| wrench_signal.png | Brown (#2f261c) | Tools/crafting |
| magic_signal.png | Blue (#416f8f) | Magic/enchanting |
| water_signal.png | Blue (#416f8f) | Resource signal |

## Batch Process

For all icons quickly:
1. Open each source PNG from Sprites folder
2. Select white pixels (Select > By Color)
3. Fill with color (Edit > Fill > With Foreground Color)
4. Save to colored/ folder with descriptive name
5. Done! One icon takes ~30 seconds

## After Coloring

The colored icons will be used in:
- Patchouli book pages (visual reference)
- Room discovery stat cards
- Pokédex-style grid layout

Example in book:
```
$(room)Anchors:$()
  🔵 Brewing Stand (blue colored icon)
  🔵 Cauldron (red colored icon)
```

No additional code needed - just reference the new colored icons in the texture paths!
