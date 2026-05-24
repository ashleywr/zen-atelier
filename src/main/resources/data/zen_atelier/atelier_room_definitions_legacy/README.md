Legacy Room Definition Data
===========================

These files were copied from the old `zen-atelier` project as reference data for
the new Atelier zone implementation.

They are intentionally stored under `atelier_room_definitions_legacy` instead of
their original active datapack paths (`room_types`, `room_profiles`, `zones`,
`advancements`, etc.). This keeps the old `zen_zones`-era definitions packaged
for review and migration without allowing current loaders to consume them
accidentally.

When the new zone backend is ready, migrate these files through an explicit
adapter or loader rather than moving them back into active paths wholesale.
