#!/usr/bin/env python3
"""
Generate a Minecraft structure NBT file suitable for use as a GameTest template.

Usage:
    pip install nbtlib
    python scripts/make_nbt_template.py

Output: src/main/resources/data/zen_atelier/structure/<name>.nbt

The generated structure is a flat stone platform of configurable dimensions
with air above it — a clean blank canvas for programmatic GameTests.
"""

import os
import nbtlib
from nbtlib import tag

# ── configuration ─────────────────────────────────────────────────────────────
OUTPUT_DIR = os.path.join(
    os.path.dirname(__file__), "..",
    "src", "main", "resources", "data", "zen_atelier", "structure"
)

TEMPLATES = [
    # (filename_without_extension, width, height, depth, floor_thickness)
    # width/depth include the floor; height is total including floor
    ("zonecreationgametests.testbuildroom", 12, 8, 12, 1),   # 12×12 pad, 7 air above
]
# ──────────────────────────────────────────────────────────────────────────────


def make_flat_platform(width: int, height: int, depth: int, floor_thickness: int) -> nbtlib.File:
    """
    Returns an nbtlib File representing a structure with:
      - stone floor blocks at y=0..floor_thickness-1
      - air above
    Dimensions: width × height × depth  (X × Y × Z)
    """
    # Palette: index 0 = stone, index 1 = air
    palette = tag.List[tag.Compound]([
        tag.Compound({"Name": tag.String("minecraft:stone")}),
        tag.Compound({"Name": tag.String("minecraft:air")}),
    ])

    blocks = tag.List[tag.Compound]()
    for y in range(height):
        state = 0 if y < floor_thickness else 1   # stone floor, air above
        for x in range(width):
            for z in range(depth):
                if state == 1:
                    continue  # air blocks don't need to be listed explicitly
                blocks.append(tag.Compound({
                    "pos": tag.List[tag.Int]([tag.Int(x), tag.Int(y), tag.Int(z)]),
                    "state": tag.Int(state),
                }))

    root = tag.Compound({
        "size": tag.List[tag.Int]([tag.Int(width), tag.Int(height), tag.Int(depth)]),
        "palette": palette,
        "blocks": blocks,
        "entities": tag.List[tag.Compound](),
        "DataVersion": tag.Int(3953),   # MC 1.21.1
    })
    return nbtlib.File(root)


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    for (name, w, h, d, floor) in TEMPLATES:
        nbt = make_flat_platform(w, h, d, floor)
        path = os.path.join(OUTPUT_DIR, f"{name}.nbt")
        nbt.save(path)
        print(f"Written: {path}  ({w}×{h}×{d}, floor_thickness={floor})")


if __name__ == "__main__":
    main()
