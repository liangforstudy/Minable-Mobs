# How it works under the hood

Minable Mobs makes entities behave like blocks you can break: hold attack with a pickaxe, cracks spread across the mob, and when it's "broken" it's gone. This page explains how that's put together.

All paths below are relative to `src/main/java/io/github/liangforstudy/minemobs/`.

## The big picture

The mod copies how vanilla Minecraft breaks blocks: **the server owns the mining**, and the client just reports what the player is doing.

```
 Client                                    Server
 ──────                                    ──────
 MinecraftMixin (attack button)
   └─ ClientMiningController
        tap?  → normal vanilla hit
        hold? → MineEntityPayload ───────▶ MiningManager
                (START / CONTINUE / ABORT)   ├─ freezes the target
                                             ├─ adds progress every tick
                                             └─ breaks it at 100%
 ClientMiningState  ◀─────────────────────── MiningProgressPayload (0-99%, or -1)
   ├─ crack overlay on the model
   └─ freezes animations
```

The two halves share the rules for what can be mined and how fast in `mining/Mining.java`.

## 1. The rules (`mining/Mining.java`)

**What counts as a mining tool:** anything that can harvest iron ore (stone pickaxe or better), plus whatever is in the `#minemobs:extra_mining_tools` item tag (by default the golden pickaxe, which can't harvest iron ore in vanilla). Asking "can it harvest iron ore?" instead of checking a list means modded pickaxes just work.

**What can be mined:** any living entity that is alive, isn't you, and isn't in the `#minemobs:unminable` entity tag. Players follow the same rules as hitting them: the server's PvP setting and team friendly-fire. Spectators can't be mined.

**The Ender Dragon is made of several hitboxes.** Hitting any of them (`EnderDragonPart`) resolves to the dragon itself.

**How fast:** each mob has a break time in ticks (20 ticks = 1 second), set in the server config. There's a separate, shorter time for the `#minemobs:fast_mining_tools` tag (the netherite pickaxe). The time is then scaled by the same modifiers as block breaking, copied from `Player#getDestroySpeed`:

| Modifier | Effect |
| --- | --- |
| Haste | +20% per level |
| Mining Fatigue | ×0.3, ×0.09, ×0.0027, ×0.00081 |
| `block_break_speed` attribute | multiplied in |
| Head underwater | × `submerged_mining_speed` (0.2 without Aqua Affinity) |
| Not on the ground | ÷5 |

Progress per tick = `modifiers / ticks`. Creative mode breaks instantly.

## 2. The client: tap or hold? (`client/ClientMiningController.java`)

Left click has to do two jobs: **tap** to attack the mob normally, **hold** to mine it. Two mixins in `mixin/client/MinecraftMixin.java` hook vanilla's attack handling:

- `startAttack` (the button was just pressed): if you're aiming at a minable mob with a mining tool, the mod takes over the press, and vanilla doesn't attack yet.
- `continueAttack` (every tick while the button is held): this is vanilla's hook for "keep breaking the block". The mod uses it to keep mining the entity.

After the press, the client waits for the **hold delay** (4 ticks by default, set per player in `config/minemobs-client.json` or with `/minemobs holddelay`):

- **Released before the delay ends:** it's a tap. The client does a normal vanilla attack on whatever is under the crosshair.
- **Still held when it ends:** it's mining. The client sends `START`, then `CONTINUE` every tick, and `ABORT` when you let go, look away, switch items or open a menu.
- **Delay set to 0:** it mines right away and never taps.

The time spent waiting isn't wasted. `START` carries `heldTicks`, and the server counts it toward progress, up to its own cap (`maxHoldCreditTicks`, so a modified client can't claim it held for an hour).

If you're already holding attack and sweep the crosshair onto a mob, mining starts on that mob without waiting again.

## 3. The server: mining sessions (`mining/MiningManager.java`)

Each player has at most one **session**: a target, a progress value from 0 to 1, and the tick it last heard from the client.

Every server tick (`END_SERVER_TICK`), each session:

1. **Ends** if the client hasn't sent `CONTINUE` for 5 ticks, the mod was switched off, or the player can no longer mine the target (out of reach, changed tool, target died or changed dimension, and so on). Reach uses the same allowance as vanilla's attack check.
2. **Adds** `Mining.progressPerTick(...)`.
3. **Breaks** the target once progress reaches 1.0.
4. **Tells nearby clients** the new percentage, but only when something visible changes: a new crack stage (every 10%) or a new iron golem crack level (every 25%). This keeps network traffic low.

Several players can mine the same mob at once. Clients see whoever is furthest along. A player who walks into range of a mob that's already cracked gets its progress right away (`EntityTrackingEvents.START_TRACKING`).

### Breaking

- Riders and mounts are ejected first, so only the mined entity disappears.
- A stone break sound plays, and the pickaxe loses 1 durability.
- **Mobs** are removed with `discard()`. Nothing drops yet; a `mob_figure` item is planned (see `ROADMAP.md`).
- **Players** can't be removed from the world, so they're killed instead, with the custom damage type `minemobs:mined` ("X was mined by Y"). The mod adds that damage type to the vanilla `bypasses_armor`, `bypasses_invulnerability`, `bypasses_resistance` and `no_knockback` tags, so it works like `/kill` with a nicer death message. Totems don't save you.

## 4. Freezing mobs in place

A mob being mined shouldn't walk away. Freezing happens on both sides.

**Server** (`mixin/LivingEntityMixin.java`): while an entity is in `MiningManager`'s frozen set, `LivingEntity#aiStep` is cancelled. That one method covers AI, pathfinding, jumping and `travel` (which is also where gravity is applied), so a mob that was mid-jump just hangs in the air. Its velocity is zeroed and pushed to clients.

The frozen set is a `WeakHashMap` that lives only in memory. It's never saved, so a world can't be saved with a mob stuck frozen.

The Ender Dragon overrides `aiStep` without calling `super`, so it needs its own mixin (`mixin/EnderDragonMixin.java`).

Players aren't frozen, because their movement comes from their own client.

**Client:** stopping movement isn't enough, because models keep animating. Three hooks hold the pose:

- `LivingEntityClientMixin` cancels `updateWalkAnimation`, so legs stay mid-stride.
- `LivingEntityRendererMixin` wraps `getBob`. "Bob" is the age-in-ticks value that every model animates from: bee and bat wings, blaze rods, chicken flapping, idle keyframe animations. The mod records it on the first frozen frame and returns that same value afterwards, which stops all of those animations at once.
- `EnderDragonClientMixin` saves and restores the dragon's `flapTime` around each tick, because the dragon's wings (and flap sound) run on their own timer.

## 5. Cracks on the mob (`client/overlay/`)

This is the trickiest part.

### The basic trick

Vanilla already draws cracks on non-block things: when you mine a chest, it draws the chest model a second time using the `destroy_stage_0` to `destroy_stage_9` render types, which blend the crack texture over whatever is underneath.

The mod does the same for mobs. `LivingEntityRendererMixin` injects right after the body model is drawn, and `BreakOverlayRenderer` draws the model again into `ModelBakery.DESTROY_TYPES[stage]`, where stage = percent ÷ 10.

### The problem: UVs

Draw the model again with its own UVs and the crack texture gets mapped like the mob's skin: squashed, stretched and scrambled across the texture atlas layout. It looks wrong.

So the second draw goes through `BreakOverlayConsumer`, a custom `VertexConsumer` that sits between the model and the crack buffer. It:

1. Collects each face's 4 vertices.
2. Undoes the render pose (an inverted copy of the pose matrix), giving positions relative to the mob, in blocks.
3. Computes fresh crack UVs for that face (`FaceUv`).
4. Applies any per-face adjustment (`FaceAdjust`).
5. Passes the face on to the real buffer with the new UVs.

### Two mapping modes (`FaceUv.java`)

- **`fit`** (the default): each face gets its own copy of the crack texture, lined up with the face's corners and oriented the same way as the skin. It uses one texture pixel per model pixel, so a 16-pixel-wide face shows the texture exactly once. `stretch` fits one whole texture over the face instead. Zero-width faces (the edges of flat planes) fall back to projection.
- **`projection`**: one texture per block, projected across the whole mob along the axis each face points toward, like a block. Neighbouring faces line up, but the texture ignores face edges.

### Naming every face (`ModelFaces.java`)

To tune single faces, each face needs a stable name. `EntityModelSetMixin` hooks `bakeLayer`, and every model layer the game bakes is walked with `ModelPart#visit`. Each box is registered as `partPath/cubeN`, for example `head/cube0`.

Faces are named from the box's own normal in model space: `top`, `bottom`, `front`, `back`, `left`, `right`. A full name looks like `head/cube0/left`. Because names come from the model's part names, they stay the same across restarts and work for modded mobs too.

While the overlay draws, `ModelPartCubeMixin` tells the consumer which box is about to emit its faces.

### Per-mob tuning files (`BreakOverlayMappings.java`)

Each mob can have a `break_overlay` JSON file. For example, `assets/minemobs/break_overlay/minecraft/pig.json`:

```json
{
  "mode": "projection",
  "default": { "offset_u": 8, "offset_v": 8 },
  "faces": {
    "head/cube0/left": { "offset_u": 0, "offset_v": 0 }
  }
}
```

Per-face fields: `rotation` (0/90/180/270), `flip_u`, `flip_v`, `offset_u`, `offset_v` (in texture pixels), `scale`, and `stretch`. They're applied around the face's center in this order: scale, flip, rotate, offset.

Files are loaded on every resource reload (F3+T) from two places:

1. `assets/<any namespace>/break_overlay/<mob namespace>/<mob>.json`, from the mod and resource packs.
2. `config/minemobs/break_overlay/<mob namespace>/<mob>.json`, which wins. This lets you try files without rebuilding the mod.

`/minemobs overlay fit|projection` temporarily ignores all files to preview a plain mode. `/minemobs overlay files` switches back to the files.

### The web app (`tools/break-overlay-web-app.html`)

Tuning faces by hand-editing JSON is slow, so there's a browser tool:

1. In game, `/minemobs exportmodel [mob]` (`ModelExporter.java`) draws the mob once through the overlay, into a buffer that throws everything away. It records every face (name, corners, skin UVs, and the base crack UVs for each mode) to `minemobs-export/<mob>.json`.
2. Load that export and your `client.jar` (for the textures) into the web app. It shows the mob in 3D and lets you adjust faces.
3. Save the JSON into `config/minemobs/break_overlay/` and press F3+T.

The web app uses the same math as `FaceAdjust.apply`, so what you see in the web app is what you get in game.

### Special cases

- **Iron golems** already have crack textures that depend on their health. `IronGolemMixin` makes `getCrackiness` return whichever is worse: health cracks or mining cracks (25/50/75%). The block-crack overlay is skipped for golems.
- **The Ender Dragon** has its own renderer rather than a `LivingEntityRenderer`, so `EnderDragonRendererMixin` adds the cracks after its third model pass (the eyes, the last pass that uses the body pose).

## 6. Settings and syncing (`config/`)

There are two config files:

| File | Side | Contents |
| --- | --- | --- |
| `config/minemobs.json` | server | on/off switch, default break / netherite ticks (60 / 5), `maxHoldCreditTicks` (20), per-mob times |
| `config/minemobs-client.json` | client | hold delay (default 4, max 20 ticks) |

The default times aren't arbitrary: 60 ticks is how long punching wood takes, and 5 ticks is stone with a netherite pickaxe. The "as hard as …" buttons in the settings screen (`client/gui/BreakProfile.java`) work out those values with vanilla's own block-breaking formula (`speed / hardness / 30`, or `/ 100` without the right tool), so they follow any mod that changes block hardness or tool speeds.

**Syncing** (`SettingsSync.java`):

- On join, the server sends the whole config as JSON (`ServerSettingsPayload`) along with whether this player may edit it (ops, or the owner of a singleplayer world, even with cheats off).
- The settings screen (`client/gui/MineMobsSettingsScreen.java`, opened from Mod Menu or `/minemobs settings [mob]`) edits a copy and sends it back on close (`UpdateSettingsPayload`).
- The server checks permission, parses and clamps every value (0 to 72000 ticks), saves the file, and broadcasts the result to everyone. Anyone who isn't allowed to edit just gets the real values sent back.

If the server doesn't have the mod, the client never receives settings, so mining stays off and attacks behave normally.

## 7. Networking summary (`network/`)

| Payload | Direction | Contents |
| --- | --- | --- |
| `MineEntityPayload` | client → server | entity id, `START` / `CONTINUE` / `ABORT`, held ticks |
| `MiningProgressPayload` | server → clients | entity id, percent 0-99 or -1 (stopped) |
| `ServerSettingsPayload` | server → client | config JSON, can-edit flag |
| `UpdateSettingsPayload` | client → server | new config JSON |

The client never sends progress, only intent. The server decides everything, so a modified client can't instantly break mobs.

## 8. Commands

| Command | Side | What it does |
| --- | --- | --- |
| `/minemobs`, `/minemobs on\|off` | server, ops | toggle mining for the whole server |
| `/minemobs settings [mob]` | client | open the settings screen (optionally on that mob) |
| `/minemobs holddelay <4\|4t\|0.2s>` | client | set your tap-or-hold delay |
| `/minemobs overlay fit\|projection\|files` | client | preview overlay modes |
| `/minemobs exportmodel [mob]` | client | export a model for the web app |
| `/minemobs debug attack <ticks>` | client, dev only | simulated press-and-hold for automated tests |

The client-side commands are also registered on the server as stubs, so they tab-complete; if your client doesn't have the mod, they tell you it's needed.

## 9. Build setup

- **Fabric** mod, built with Loom and [Stonecutter](https://stonecutter.kikugie.dev/) for multi-version builds. The current target is Minecraft 1.21–1.21.1. Ports to 1.21.2+ (render states), 1.21.9+ (submit queue) and NeoForge are on the roadmap.
- Mod properties live in `stonecutter.properties.toml`. `gradle.properties` holds only Gradle options.
- `tools/mcprpc.py` is a small helper for driving a running dev client through the MCP Fabric bridge, used together with `/minemobs debug attack` for in-game testing.

## 10. Why this is hard (especially on older versions)

Mining mobs sounds like a small idea, but it fights the game at almost every step, and for a long time modders had very little help with that kind of work.

### The game wasn't built for it

- **Cracks only exist for blocks.** Vanilla draws break cracks on blocks and a few block entities like chests, never on mobs. The mod fakes it by drawing the whole model a second time and replacing every UV on the fly (section 5). Even the "where does the texture go on this face" question has no vanilla answer, which is why the fit/projection modes, per-face tuning files and the web app exist.
- **Older rendering had no clean place to hook in.** Since the 1.15 rendering rewrite, models send their vertices to render types through buffers (`MultiBufferSource`, `VertexConsumer`) that are drawn later in batches. That is what lets `BreakOverlayConsumer` sit between the model and the crack buffer. In older versions, models drew more directly through OpenGL-style calls, so there was nothing to slot in between; you'd have to patch the drawing code itself.
- **Model parts had no stable names.** Around 1.17, entity models started being built from named layer definitions and baked through `EntityModelSet`. That's the hook `ModelFaces` uses to name faces like `head/cube0/left`. Without it, per-face tuning and the web app couldn't address a face in a way that survives a restart.
- **Freezing touches many separate systems.** AI, movement and gravity, the walk cycle, the "bob" timer that drives most animations, and the dragon's own wing timer all run independently, and each needs its own hook (section 4). The dragon alone needs three mixins because it skips the code paths every other mob uses.
- **Newer versions move the target.** From 1.21.2, renderers work from a snapshot of the entity's state rather than the entity itself, and from 1.21.9 drawing is submitted to a queue and done later. Both break how the overlay hooks in today, which is why those ports are separate roadmap items.

### There was little help for modders

- **Tutorials stopped at the basics.** Most modding videos back in the day were "hello world" mods: add a block, add an item, add a sword. A guide like "how to add a custom sword in 1.12.2" is common; nothing walks you through hijacking entity rendering or freezing AI. This kind of mod is learned by reading Minecraft's own code and other mods.
- **That code used to be hard to read.** Minecraft ships with scrambled names. Mojang only started publishing its official name mappings in 2019, around 1.14.4. Before that, modders relied on community-made names, and plenty of methods were still numbered placeholders like `func_78785_a`, so finding the method that draws a model box took real digging. This project uses Mojang's names plus [Parchment](https://parchmentmc.org/), which adds parameter names and comments that Mojang's mappings leave out.
- **Tooling was fragile.** Setting up a workspace for old versions often broke. Fabric's tooling and newer tools like Stonecutter (which this repo uses for multi-version builds) make it much smoother.
