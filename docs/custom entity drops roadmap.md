# Custom entity drops roadmap

What a mob leaves behind when it's mined (the `mob_figure`) or blown up (issue #2), and how the code for it changes between Minecraft versions.

Versions covered:

| Version | Why it matters |
| --- | --- |
| **1.21.1** | Today's build target (`stonecutter.properties.toml`) |
| **26.2** (Chaos Cubed, June 2026) | Next target on the main roadmap |
| **26.3** (Wilderness Bound, 15 Sep 2026) | Current Java release |
| 26.4 | In snapshots now; only noted where it already changes something |

## 1. The `mob_figure` item

### Decision: no GeckoLib

GeckoLib draws models made by hand in Blockbench. The mod mines *any* living entity, including mobs from other mods, so GeckoLib would mean drawing a figure for every mob and still missing modded ones. It would also be an extra dependency on every loader and version.

Instead, **the item draws the mob itself, using the mob's own renderer.** Vanilla does something like this already: the copper golem statue and player head items have their own special renderers.

### How it works

1. **Mining drops the figure.** In `MiningManager` (the `TODO` before `discard()`), save the mob's entity type and NBT on a new `minemobs:mob_figure` item stack and drop it where the mob was.
2. **The item renderer draws the mob:**
   - Build a client-only copy of the entity from the saved data. **Cache it per stack**, never rebuild it every frame.
   - Freeze its pose like the mining freeze: fixed limb swing, head rotation and bob (age in ticks).
   - Scale it so its bounding box fits inside one block. Big mobs such as the Ender Dragon, ghast and warden shrink down; small ones stay figure-sized.
   - Draw it with the normal entity render dispatcher.
3. **It shows everywhere for free.** The inventory, the hand, item frames and dropped items on the ground all use the item renderer, so the 3D figure shows up in all of them.
4. **Eat it.** Add a food component, "edible like a porkchop".
5. **Right-click places the mob back.** Spawn the entity from the saved NBT at the clicked block face (same name, variant, armour, saddle and so on). Then take the item out of the inventory, even in creative, as the design says.
6. **Tooltip and name.** The tooltip says "Figure of Pig", or the mob's custom name.

### Edge cases

- **Players:** a mined player is not turned into a figure that could respawn a player entity. Make it a figure that's only for show (skin from the game profile), or drop nothing. Decide this before the drop goes in.
- **Mobs that don't render or are huge:** fit them to the bounding box, but cap how much they can shrink, so a figure never ends up an invisible speck.
- **Unknown or removed mod mobs:** if the entity type is missing (mod uninstalled), show a placeholder model and keep the NBT, so the mob comes back if the mod returns.
- **Bosses:** a Wither or dragon figure could spawn a boss in a base. Use a server config (or the `#minemobs:unminable` tag) to block placing bosses back, or to turn their figures into display-only items.
- **Riders:** already ejected before `discard()`, so each figure holds one mob only.

### What changes between versions

| Part | 1.21.1 (target now) | 26.2 (main roadmap) | 26.3 (current release) |
| --- | --- | --- | --- |
| **Saving the mob on the stack** | `DataComponents.ENTITY_DATA` (custom data that has to include `id`, the same one spawn eggs use) | The component is still there, but its shape changed after 1.21.1 (typed entity data). Check the names in the unobfuscated jar | Same as 26.2 |
| **Food** | One `FOOD` component holds everything | `CONSUMABLE` (how it's eaten, sound, effects) is split from `FOOD` (hunger, saturation), since 1.21.2 | Same as 26.2 |
| **Hooking the item renderer** | Fabric API `BuiltinItemRendererRegistry`: one class that draws the item straight away | That registry is gone since 1.21.4. The item gets an item model file (`assets/minemobs/items/mob_figure.json`) with type `minecraft:special` and a custom **special model renderer** that we register | Same as 26.2 |
| **Drawing the mob** | Call the entity renderer straight away with a pose stack and buffer | Fill an **entity render state** (since 1.21.2) and **submit** it to the render queue (since 1.21.9). There's no drawing straight away | Same as 26.2 |
| **Figure placement in GUI / hand / frame** | The display transforms in the item model JSON | Same, plus since 26.1 the item model can transform each sub-model, so the per-context tweaks can all stay in the JSON | Same as 26.2 |
| **Graphics backend** | OpenGL | OpenGL by default, Vulkan experimental. Fine as long as we only go through vanilla's render pipeline and never make raw OpenGL calls | Same as 26.2 (26.4 snapshots make Vulkan the default, so "no raw OpenGL" becomes a must) |
| **Code names** | Mojang + Parchment mappings | The game ships unobfuscated, so there are no mappings to set up | Same as 26.2 |

**What this means for the code:** keep three parts apart so the Stonecutter port only changes the edges:

- `MobFigureData`: reads and writes the entity on the stack. It's the only class that touches the component.
- `MobFigureEntityCache`: builds the frozen client entity. It's shared by all versions.
- `MobFigureRenderer`: one file per render API: the 1.21.1 built-in renderer, then the special model renderer that uses render states and the submit queue (26.2 / 26.3).

## 2. Issue #2: flint and steel → primed mob

### The behaviour

- Using flint and steel on a mob (with the mod on) **freezes it the same way as mining**, and turns it into a primed TNT: fuse, white flashing and hiss, then an explosion the size of TNT's.
- **When it explodes**, one of two things happens, set by a toggle in the settings:
  - **No drop:** the mob drops nothing of itself (no figure, no loot).
  - **Ragdoll:** the mob's frozen model is thrown and tumbles.
- **Bones don't ragdoll**, for now. Only the idle models do (as written in the issue).

### Steps

1. **Ignite:** a `UseEntityCallback` (Fabric) for flint and steel (and fire charge?) on a minable mob. It uses up durability like vanilla and plays the ignite sound.
2. **Fuse:** a per-entity fuse in a small `PrimedMobManager` (like `MiningManager`). It reuses the mining freeze (AI, movement, gravity, animations) and syncs the fuse to clients for the flashing.
3. **Flashing:** the same white flash as TNT, as an overlay pass drawn the same way the crack overlay is.
4. **Explode:** a `level.explode(...)` that counts as TNT, then `discard()` with no drops (or the ragdoll).
5. **Ragdoll, easy version first:** a client-only visual that takes the frozen pose and throws the **whole model as one solid piece**, which tumbles, bounces and settles, then fades out. It uses no physics library.
6. **Real per-limb ragdoll:** a later stretch goal. It would turn each model part into its own physics body. Vanilla mobs only at first, because modded mobs build their models differently.

### What changes between versions

| Part | 1.21.1 | 26.2 | 26.3 |
| --- | --- | --- | --- |
| **Explosion call** | `level.explode(...)` with the old argument list | The signature changed (since 1.21.2), and explosions take the explosion particles and sound as arguments | Same as 26.2 |
| **Flashing overlay** | Extra render pass in `LivingEntityRendererMixin` (like the cracks) | Store the fuse in the render state, then submit an extra model pass | Same as 26.2 |
| **Ragdoll visual** | Draw the model with our own pose matrix | Build a render state with our pose, then submit it. The freeze has to fill the render state instead of wrapping `getBob` | Same as 26.2 |
| **Freezing animations (shared with mining)** | Wraps `getBob`, which works today | Animation values now come from the render state, so the freeze sets them while the state is filled | Same as 26.2 |

## 3. Order of work

1. `mob_figure` on 1.21.1: drop, render, eat, place back (Next on the main roadmap).
2. Issue #2 on 1.21.1: ignite, fuse, explode, the no-drop / easy-ragdoll toggle.
3. Port both to **26.2** together with the rest of the mod (render states + submit queue). The figure renderer and the ragdoll are the parts that change the most.
4. Check them on **26.3** (it's expected to need few or no changes beyond 26.2's), then keep an eye on 26.4 (Vulkan becomes the default).
5. Stretch: real per-limb ragdoll; figures for players that are only for show.

## Open questions

- Should figures of bosses be placeable?
- Mined players: show-only figure, or no drop?
- Fire charge too, or only flint and steel?
- Ragdolls: should other players see them too (synced), or only the player's own client?

---
*Drafted by Claude (Anthropic), Claude Opus 5.5.*
