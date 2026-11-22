# Survival Assistant

A Fabric mod for **Minecraft 26.3** that helps new players get through their first days in survival.
A friendly guide walks with you and tells you what to do next. A compact task card shows your progress,
and an environment card explains your current biome and which ores are worth digging for at your height.

All advice follows a researched beginner path. Recipes, mob spawns, ore heights and other facts were
**checked against the game's own data files** ([research](docs/research-first-days.md),
[biome and ore facts](docs/biomes-and-heights.md)). There are no speedrun tricks: it's aimed at an average player.

<!-- Screenshots: put images in docs/images/ and link them here. -->

## Features

### The guide
- A guide character spawns next to you in a new world. It follows you, can't be hurt, and monsters ignore it.
- It gives tips in chat at the right moment, praises you when you reach a goal, and looks at the nearest tree
  or stone when it tells you to gather them.
- Right-click the guide for a tip and your progress.
- After 3 in-game days it says goodbye. You can call it back with `/guide call`.
- Don't want a character walking around? `/guide character off` removes it. The task card stays, and you
  only get urgent warnings in chat.

### Task card (top-left by default)
```
┌─ Guide · Day 1/3 ────── ☀ ▮▮▮▮▯▯ ┐
│ [log] Chop wood            6/10  │   current task: icon, live counter, progress bar
│       ██████░░░░                 │
│  · Make a crafting table         │   next two tasks
│  · Wooden pickaxe                │
└──────────────────────────────────┘
```
- 15 goals in order: wood → crafting table → wooden pickaxe → stone tools → furnace → food → bed →
  torches → shelter → chest → iron → shield → iron pickaxe → iron armor → bucket of water.
- Live counters (for example logs 6/10 or wool 2/3), with a check-mark animation when a goal is done.
- A sun/moon clock that counts down to sunset, turning orange at dusk and red at night.
- **Urgent mode**: the border pulses red, and the current-task line becomes a warning (creeper nearby,
  low health, hunger, dusk or night outside, thunderstorm, underground without torches).
- Press **J** to switch between compact, the full list of all 15 goals, and hidden.

### Environment card (below the task card)
```
┌ [sand] Desert ───────────── Y 64 ┐
│ + Sand for glass, cactus, temples │   biome pro
│ − Only rabbits and camels; husks  │   biome con
│ ⛏ here: Iron · Copper · Coal     │   ores at this height (★ = best height)
│ ↓ Iron: best around Y 16          │   pointer for your current task
│ ⚠ Below Y −54: lava lakes         │   depth hazard, when relevant
└───────────────────────────────────┘
```
- A pro and a con for all 67 biomes (grouped into 25 families).
- Ores at your current Y-level, most valuable first. Emerald only shows in mountain biomes, and the extra
  surface gold only in badlands.
- A pointer to the best height for the ore your current task needs.
- One-time chat notes the first time you reach deepslate, the lava level, the deep dark or high mountains.
- Press **K** to show or hide it.

## Commands

| Command | What it does |
|---|---|
| `/guide tip` | Get the most important tip right now |
| `/guide call` | Bring the guide to you (or back after it said goodbye) |
| `/guide dismiss` | Send the guide away |
| `/guide mute <minutes>` | No unsolicited tips for a while (0–120) |
| `/guide character on\|off` | Show or hide the guide character (saved per player) |

## Configuration

**Server**: `config/survivalassistant.json`

| Option | Default | Meaning |
|---|---|---|
| `maxDays` | `3` | In-game days the guide stays |
| `tipIntervalSeconds` | `2` | How often the situation is checked |
| `milestoneCooldownSeconds` | `180` | Minimum time between repeats of the same task tip |
| `urgentCooldownSeconds` | `60` | Minimum time between repeats of the same warning |
| `allowRecall` | `true` | Allow `/guide call` after the guide has left |
| `npcEnabledByDefault` | `true` | Default for the guide character (players can override) |
| `depthNotices` | `true` | One-time chat notes about depth and mountains |
| `spawnOnJoin` | `true` | Give new players a guide automatically |
| `pointAtResources` | `true` | Guide looks at nearby trees/stone when giving those tips |

**Client**: `config/survivalassistant-client.json`

| Option | Default | Meaning |
|---|---|---|
| `corner` | `TOP_LEFT` | `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT` or `BOTTOM_RIGHT` |
| `mode` | `COMPACT` | Task card mode: `COMPACT`, `FULL` or `HIDDEN` (J) |
| `showEnvironment` | `true` | Show the environment card (K) |
| `envLines` | `FULL` | `COMPACT` shows only the biome header and ore line |
| `scale` | `1.0` | Extra scale on top of the GUI scale (0.5–2.0) |
| `margin` | `4` | Distance from the screen edge |

## Languages
English and Dutch (Nederlands). The game language decides which one is used.

## Requirements and installation
- Minecraft **26.3**
- [Fabric Loader](https://fabricmc.net/) **0.20.1** or newer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java **25**

Put the mod jar and Fabric API in your `mods` folder. The mod is needed on the server (or in singleplayer)
for the guide and advice, and on the client for the cards.

## Building from source
```sh
./gradlew build                 # builds the jar and runs the unit tests
./gradlew runClient             # starts a development client
./gradlew installToModrinth     # copies the jar into a Modrinth App instance ("Mod Testing" by default)
./gradlew installToModrinth -PmodrinthInstance="My Instance"
```
Build output goes to `%LOCALAPPDATA%/gradle-builds/survivalassistant` when that variable exists (Windows),
because sync tools like OneDrive lock files in `build/`. On other systems it uses the normal `build/` folder.

Developer tools:
- `tools/make_lang.py` generates `en_us.json` and `nl_nl.json` from one table, so both languages always have the same keys.
- `tools/biome_facts.py` regenerates [docs/biomes-and-heights.md](docs/biomes-and-heights.md) from the Minecraft jar.

## How the advice was verified
See [docs/research-first-days.md](docs/research-first-days.md) for the sources, the consensus and a table of
claims checked against recipes, loot tables and game code. See [docs/biomes-and-heights.md](docs/biomes-and-heights.md)
for ore placement and mob spawns per biome, extracted from the 26.3 data. The pure advice logic
(`advice` package) has no Minecraft dependencies and is covered by unit tests.

## License
[MIT](LICENSE) © 2026 David Visser
