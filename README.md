# Zen Atelier

A Minecraft mod (NeoForge 1.21.1) that automatically detects and analyzes rooms, zones, and enclosed spaces. Zen Atelier classifies interior air volumes, evaluates room quality, and syncs zone data to clients for HUD display and room-based mechanics.

## What It Does

Zen Atelier solves a fundamental problem: **Minecraft has no native concept of "rooms."** This mod automatically:

- **Detects zones**: Flood-fills connected air blocks across chunk boundaries to identify unique enclosed spaces
- **Evaluates quality**: Computes room metrics like volume, ceiling profile, enclosure score, and furniture density
- **Tracks ownership**: Maintains persistent zone identity and custom naming across world saves
- **Syncs to clients**: Displays zone information in a HUD overlay showing your current room's quality and type
- **Supports creativity**: Handles complex designs like multi-level rooms, basements, atriums, mid-room elevators, and Create mod integration

## Features

### Core Zone Detection
- **Cross-chunk stitching**: Single rooms spanning multiple chunks are unified into one logical zone
- **Intelligent classification**: Distinguishes between interior air, exterior, solid blocks, and boundary openings
- **Live expansion/shrinking**: Zones grow when walls are broken, shrink when blocks are placed, and split when separated
- **Entry point tracking**: Identifies doors, stairs, trapdoors, and slabs as zone access points

### Quality Evaluation
- **Volume calculation**: Total breathable air space (m³)
- **Spaciousness scoring**: Height, open area, and accessibility metrics
- **Furniture detection**: Block-based heuristics for beds, tables, bookshelves, and decorations
- **Enclosure scoring**: Ratio of openings to surface area (sealing = higher scores)
- **Room typing**: Automatic classification as bedroom, library, kitchen, storage, etc.

### Persistence & Metadata
- **Zone preservation**: Zones are saved and restored across world sessions
- **Custom naming**: Players can name zones, names persist through world saves
- **Grace periods**: Temporarily disabled zones are restored if issues are fixed within 60 seconds
- **Reliable identity**: UUIDs ensure zones maintain identity through merges and reclassification

### Client-Side Display
- **HUD overlay**: Shows current zone name, quality, type, and atmospheric rating
- **Real-time updates**: Responds instantly to room changes and player movement
- **Discovery system**: Tracks and displays discovered rooms in a journal

## Installation

### Requirements
- Minecraft 1.21.1
- NeoForge 21.1.220 or later
- Java 21+

### Setup

1. Download the latest release JAR
2. Place it in your `mods` folder
3. Launch Minecraft with NeoForge

## Configuration

See `config/zen_atelier-common.toml` for options:

- `CHUNKS_PER_TICK`: How many chunks to classify per tick (higher = faster, more server load)
- `MAX_ZONE_VOLUME`: Largest zone before triggering a warning
- `ENABLE_DEBUG_COMMANDS`: Toggle debug visualization commands

## Development

### Building from Source

```bash
./gradlew build
```

Output JAR: `build/libs/zen_atelier-*.jar`

### Project Structure

```
src/main/java/com/sanhiruzu/atelier/
├── space/                    # Zone classification pipeline
│   ├── zone/                 # Zone entities and evaluation
│   ├── ChunkClassifier.java  # Flood-fill algorithm
│   └── SpaceRegionRegistry.java  # Block-to-zone mapping
├── ui/                       # Client-side HUD and overlays
├── network/                  # Server-client synchronization
├── advancement/              # Criteria and triggers
└── command/                  # Debug commands
```

### Architecture Overview

#### Pipeline (Server-Side)

When a block changes or chunk loads:

1. **ClassificationEventHandler** marks chunk dirty
2. **ClassificationScheduler** processes chunks up to `CHUNKS_PER_TICK`
3. **ChunkClassifier** performs flood-fill to find air regions
4. **Zone** entity creates/expands/shrinks/merges regions
5. **ZoneEvaluator** computes quality metrics
6. **SyncZoneDataPayload** sends updates to connected clients
7. **ZoneRegistry** caches computed data for persistence

#### Zone Lifecycle

- **Bootstrap**: Classification detects an enclosed air region with at least one entry point
- **Expansion**: When adjacent walls are destroyed, the zone grows via BFS
- **Shrinking**: Blocks placed inside the zone trigger potential splits if components become disconnected
- **Merging**: Adjacent zones with touching boundaries unify into a single entity
- **Dissolution**: Zones with no entry points are disabled and removed

#### Cross-Chunk Stitching

Rooms spanning multiple chunks are initially classified as separate regions (one per chunk). The `stitchLoadedNeighbors` algorithm unifies them by:

1. Walking the boundary between adjacent chunks
2. Matching region UUIDs on both sides
3. Merging same-room regions into a single zone
4. Re-evaluating quality metrics with combined volume

### Key Classes

| Class | Role |
|-------|------|
| `Zone` | Runtime zone entity; handles expansion, shrinking, merging, and lifecycle |
| `ChunkClassifier` | Flood-fill algorithm producing initial air region classification |
| `SpaceRegionRegistry` | Maps block positions to zone UUIDs; maintains `blockToRegion`, `regionToBlocks`, `regions` |
| `ZoneRegistry` | Caches computed zone data; manages custom naming and metadata |
| `ZoneEvaluator` | Computes room quality, type, furniture counts from zone geometry |
| `ClassificationScheduler` | Drives the full classify → stitch → sync pipeline |
| `ZoneHudAdapter` | Client-side zone lookup and HUD rendering |

### Testing

Run the full test suite:

```bash
./gradlew test
```

Key test suites:
- `ZoneValidationTest`: Entry point and structural validity
- `ZoneLifecycleTest`: Expansion, shrinking, merging, dissolution
- `ZoneExpansionTest`: BFS correctness and cross-chunk boundary handling
- `ZoneGeometryProfileTest`: Ceiling and walkable profile computation
- `ZoneResilienceGameTests`: Multi-scenario integration tests

## Known Limitations

- **Stale blockToRegion entries**: Now-solid blocks retain `blockToRegion` entries (harmless; can be cleaned up with a full sweep)
- **Create mod elevators**: Multi-block elevators are detected as entry blocks but don't yet integrate with the full access-point system (planned)
- **Sloped roofs**: Angled ceilings may be underestimated in slope calculation (acceptable approximation)

## Contributing

Contributions are welcome! Areas of interest:

- **Ladder support**: Extend entry detection to support ladder blocks
- **Create integration**: Better handling for elevator and conveyor systems
- **Performance**: Optimize flood-fill for large open spaces
- **New metrics**: Room dampness, temperature, light level scoring
- **Visualization**: Better debug rendering tools

### Workflow

1. Create a new branch for your feature
2. Write tests for new functionality (see `src/test/java/` for examples)
3. Ensure all tests pass: `./gradlew test`
4. Verify compilation: `./gradlew compileJava`
5. Submit a pull request with a clear description

## License

MIT License — see [LICENSE](LICENSE) file for details.

## Support

- **Bug reports**: Open an issue on GitHub
- **Questions**: Check existing issues or start a discussion
- **Debug mode**: Use `/debug zone` command in-game to visualize zones and classification state

## Acknowledgments

Built with [NeoForge](https://neoforged.net/) and inspired by room-detection challenges in survival Minecraft.
