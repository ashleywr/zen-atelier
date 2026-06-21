# Environment Influence Tiering Design

## Goal

Make tiered zone benefits harder to max out by accident while keeping the rules understandable for players and reusable for future environment effects. The first consumer is bedroom sleep comfort, but the same model should support synthesis, extraction, integration bonuses, and future action-based zone rewards.

## Core Model

Zone rules still emit plain `EnvironmentEffect` values so the public API stays small and easy for mods to use. Tiered consumers should resolve those effects through a shared influence resolver instead of summing raw amounts directly.

The resolver groups contributions into understandable categories, applies caps and diminishing returns, rewards variety, and then maps the normalized score to tiers. A room should usually need several kinds of support to reach high tiers. Many copies of the same contributor should help at first, then taper off.

## Influence Profile

Each tiered use case defines an influence profile:

- effect type to read, such as `COMFORT_MODIFIER`
- category mapping, usually from effect id or an explicit group
- per-category cap
- duplicate decay for repeated contributors
- optional variety bonus
- tier thresholds

For bedroom comfort, expected categories are shelter, bedding, decor, lighting, and nature. A room with shelter, soft bedding, decor, and a plant should outperform a room full of only carpets or candles.

## API Shape

Add a small API-side resolver, tentatively:

```java
EnvironmentInfluenceResolver.resolve(effects, profile)
```

The result should expose:

- normalized score
- resolved tier
- per-category contribution totals
- capped or diminished categories
- source effect ids that contributed

Consumers can use the tier directly, while debug UI and special inspection items can explain why the tier was reached.

## Player Feedback

Use an existing alchemist inspection item if one fits, likely the lens, codex, or room journal. While held or used, it should surface the relevant room contributors:

- strong contributors are highlighted clearly
- diminished duplicate contributors are highlighted more faintly
- capped categories are called out as capped
- a short summary names the active categories, such as `Bedroom comfort: Shelter + Bedding + Decor`

The feedback should teach the rule: variety matters, duplicates have diminishing value, and high tiers come from a coherent room rather than block spam.

## Data And Modpack Support

The resolver should work with existing environment effects and facet-derived signals. Modders can keep registering effects normally. Modpacks can tune mappings and thresholds through data later, but the first implementation can use code-defined profiles for Atelier-owned effects.

The design should not require every mod to learn a large formula system before participating.

## Testing

Add resolver tests for:

- duplicate contributors diminishing before max tier
- varied categories reaching higher tiers than repeated single-category contributors
- capped categories reporting capped state
- threshold boundaries
- sleep comfort using the shared resolver instead of local scoring

Keep existing sleep reward behavior covered by tests, including no reward, mild reward, higher tier reward, and explanation messaging.
