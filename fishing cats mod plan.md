# Fishing Cats — mod plan

Nov 22, 2026 · liangforstudy

Working name. A Minecraft Java mod idea: give a cat a fishing rod and it goes fishing.

## The idea in one line

Right-click a cat with a fishing rod: it runs to the nearest good fishing spot, sits on the edge of the shore like a person (keeping its normal cat body), and casts the line.

## Shared behaviour (all cats)

- **Giving the rod:** right-click a cat with a fishing rod. The cat takes the rod (it leaves your hand) and holds it.
- **Finding a spot:** the cat runs to the nearest valid fishing spot within its range (about 80–160 blocks for strays, 26 for tamed cats). A valid spot is a shore block next to open water that isn't already taken by another fishing cat. No spot in range: the cat drops the rod and goes back to its usual AI.
- **Sitting pose:** the cat sits on the edge of the shore, upright with its back legs hanging over the water, holding the rod in its front paws. It uses the normal cat model, posed differently, with no new model.
- **Casting:** it throws a real fishing bobber, so fish bites work like they do for a player.
- **Enchantments count:** each pull is a normal rod pull, so the rod's enchantments apply:
  - Lure: fish bite sooner
  - Luck of the Sea: better catch odds
  - Unbreaking: less durability lost per pull
  - Mending: no effect, since cats don't collect XP
- **Rod at zero durability:** the rod breaks and the cat goes back to its usual AI.

## Stray cats

Stray means untamed. **Ocelots count as stray cats** and always follow these rules, since they can't be tamed.

- **Range: the same as other mobs, about 80–160 blocks** to find a fishing spot. It follows the server's simulation distance, since mobs only run near players. The cat walks there in stages, rather than in one long path.

- **Only fish count:** raw cod, raw salmon and tropical fish. **Junk, treasure and pufferfish are ignored.** The cat reels them in and leaves them on the ground, so **junk piles up around the cat** while it fishes.
- **Once it catches a fish, it drops the rod** where it was sitting. If you're close by, it **runs away from you** first, then **eats the fish**.
- **The rod can break** if it hits zero durability before a fish bites. Then the cat goes back to its usual AI.

## Tamed cats (more helpful)

- **Range: 26 blocks**, both to find a fishing spot and to find a copper chest.
- **Keeps everything:** junk, treasure and pufferfish are caught and kept, not left on the ground.
- **Keeps fishing:** it doesn't run off after a fish. It keeps going until you stop it or the rod gets low.
- **Stops before the rod breaks:** when the rod has **1 durability left**, the cat stops fishing on its own, so your rod never breaks.
- **Punch to stop:** hit your own cat to make it stop fishing. This doesn't hurt the cat.
- **When it stops** (punched or low rod), it walks to the **nearest copper chest**, puts its whole catch inside, and **gives the rod back**.
- **No copper chest in range, or the chest is full:** it walks to you and drops everything at your feet.

## Open questions

- **Giving the rod back:** does the cat hand the rod to you, or put it in the copper chest with the catch?

## Version notes

- **Copper chests only exist from Minecraft 1.21.9 onwards** (they came with the copper golem). So the tamed cat behaviour needs 1.21.9 or newer. **26.2 is the natural target**, the same as Minable Mobs' main roadmap, then check 26.3 (current release).
- On 1.21.1 there are no copper chests. If it ever gets a 1.21.1 build, fall back to normal chests or a tag of chest blocks.
- Copper golems already sort items into copper chests. Copper golems put items in, while cats only drop off their catch, so check that the two don't fight over the same chest.
- Like Minable Mobs, build it with Fabric and Stonecutter so it can be ported later.

## Build order

1. Give the rod, find a spot, sit pose (no fishing yet)
2. Casting and reeling with a real bobber; enchantments; durability and breaking
3. Stray cats and ocelots: only fish count, junk left behind, drop the rod, run from you, eat the fish
4. Tamed cats: keep everything, keep fishing, stop at 1 durability, punch to stop
5. Copper chest drop-off and giving the rod back
6. Settings: tamed cat range (default 26); the stray range follows the simulation distance
