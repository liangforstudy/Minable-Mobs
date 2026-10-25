# Minable Mobs roadmap

## Done
- Mine any living entity like a block: hold attack with a stone+ pickaxe (or `#minemobs:extra_mining_tools`, default golden pickaxe)
- Mining time comes from per-mob ticks (see below); Haste, Mining Fatigue, underwater and mid-air always apply; creative is instant
- Server-owned progress (start / continue / abort packets, 5-tick timeout)
- Mobs freeze while mined (AI, movement, gravity; legs, wings and idle animations hold their pose, incl. dragon wings); bosses included, dragon parts resolve to the dragon
- Players can be mined (respects PvP and teams) and die with "X was mined by Y"
- `/minemobs` toggles, `/minemobs on|off` sets it (ops only)
- Crack overlay (destroy_stage_0–9) on the body model, incl. the Ender Dragon; iron golems show their own cracks (worse of health vs mining)
- Break overlay mapping: `fit` (per face, follows the skin) and `projection` (1 texture per block) modes, per-face adjustments (rotation, flip, offset, scale, stretch) from `break_overlay` JSON; reloads with F3+T from `assets/*/break_overlay/` and `config/minemobs/break_overlay/`
- `/minemobs exportmodel [mob]` writes a mob's faces to `minemobs-export/` for the tuner app
- `/minemobs overlay fit|projection|files`: preview plain modes ignoring the JSON files (client-only, not saved)
- Break overlay tuner (`tools/break-overlay-tuner.html`, published as an artifact): load an export + client.jar, tune faces, save/copy the break_overlay JSON
- Tap to hit, hold to mine: hold delay (default 4 ticks, 0 = mine right away); the wait counts toward mining (server caps the credit at `maxHoldCreditTicks`); `/minemobs holddelay 4|4t|0.2s`
- Per-mob break time and netherite time in ticks (server `config/minemobs.json`, defaults 60 / 5), with "as hard as" profiles computed from vanilla: wood/fist, stone/stone pickaxe, stone/diamond pickaxe, stone/netherite pickaxe
- Settings screen (own, vanilla widgets): Mod Menu + `/minemobs settings [mob]` (opens straight to that mob); General (mining on/off, tap or hold delay) and Mobs (search, per-mob times, profiles, reset, break overlay mode); ‹ ticks | sec › unit switcher (click or scroll), scroll on a time box steps one tick; server settings sent on close, ops/singleplayer owner only; profile presets are block + tool icon buttons
- Dev-only `/minemobs debug attack <ticks>`: simulated press-and-hold for automated tests through MCP Fabric (tap hits, hold credit verified 2026-10-26)

## Next
- Mining feedback: hit sound + particles every 4 ticks, break sound + particle burst (entity-texture particles, sound data file)
- `mob_figure` drop holding the entity's NBT: scaled-down model render, place to respawn, eat it

## Later
- Tuner: buttons to copy one mob's face settings onto other mobs (match by part names)
- Mined players: a proper death animation (currently just the vanilla death)
- Mined players can't be held still (they move on their own client)
- Creeper that's already swelling keeps swelling while frozen
- Mod icon
- Cracks on layers too (armor, saddles, sheep wool, held items); right now only the body model cracks
- Port: 1.21.2+ (render states), 1.21.9+ (submit queue), NeoForge
