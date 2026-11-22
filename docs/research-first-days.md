# Research: the most effective start in survival (average player)

*Researched on 22 Nov 2026 for Minecraft Java 26.3. Speedrun strategies are deliberately left out (e.g. diving for diamonds right away, strip-mining at Y −40 on day 1, Fortune farming).*

## Method and source verification

1. **Collect sources**: the Minecraft Wiki (community-maintained reference), Wikibooks, the Minecraft Help Center and gaming sites (GameSpot, Windows Central, Sportskeeda, TechTimes and others).
2. **Find consensus**: a piece of advice only counts when several independent sources give it.
3. **Check facts against the 26.3 game files themselves** (recipes, loot tables and decompiled code). This turned out to be necessary: Wikibooks gives outdated or wrong numbers (8 sticks, 6 fences, 5 planks for a shield).

| Claim | Source | Verified in 26.3 | Result |
|---|---|---|---|
| 1 log → 4 planks, 2 planks → 4 sticks | Wiki, Wikibooks | `recipe/oak_planks.json`, `stick.json` | ✅ (Wikibooks' "8 sticks" ❌) |
| Crafting table = 4 planks | all | `crafting_table.json` | ✅ |
| Stone pickaxe = 3 stone + 2 sticks; sword = 2 stone + 1 stick | Wiki | `stone_pickaxe.json`, `stone_sword.json` | ✅ |
| Furnace = 8 cobblestone | all | `furnace.json` | ✅ |
| Torch: 1 coal/charcoal + 1 stick → 4 torches | Wiki | `torch.json` | ✅ |
| Charcoal from smelting wood | Wiki, Wikibooks | `charcoal.json` | ✅ |
| Bed = 3 wool + 3 planks | all | `white_bed.json` | ✅ |
| Killing a sheep → 1 wool, shearing → 1–3 wool | Wikibooks ("3 sheep") | `loot_table/entities/sheep/white.json`, `shearing/sheep/white.json` | ✅ (so kill 3 sheep, or shear 1–3) |
| Shield = 6 planks + 1 iron | Wiki | `shield.json` | ✅ (Wikibooks' "5 planks" ❌) |
| Monsters only spawn at block light 0 | Wiki (torches) | `dimension_type/overworld.json`: `monster_spawn_block_light_limit: 0` | ✅ |
| No natural spawns within 24 blocks of the player | Wiki | `NaturalSpawner`: `distSqr <= 576` | ✅ |
| Food ≤ 6: can't sprint. Food 0: damage (Normal: down to ½ heart) | Wiki | `FoodData`: `> 6.0F`, starvation rules | ✅ |
| Natural regeneration only at food ≥ 18 | — | `FoodData`: `foodLevel >= 18` | ✅ (extra) |
| Phantoms after 3 nights without sleep | Wiki | `PhantomSpawner`: `TIME_SINCE_REST >= 72000` | ✅ |
| A creeper explodes ~1.5 s after hissing | Wiki | `Creeper`: `maxSwell = 30` ticks | ✅ |
| A game day lasts 20 minutes | Wikibooks | 24000 ticks / 20 tps | ✅ (the source saying "10 min" ❌) |

**Not verified**: the official minecraft.net articles ("How to survive your first day/night") could not be reached during the research (timeouts). Their content is only known from search results and matches the consensus below.

Biome and ore facts (heights, mob spawns per biome, lava level) are in [biomes-and-heights.md](biomes-and-heights.md), generated directly from the game data.

## Consensus: the order nearly every source follows

**Wood → crafting table → wooden pickaxe → stone → stone tools and furnace → food → bed and shelter before sunset → light.** Days 2–3 follow with **iron → shield, tools, armor** and **renewable food**.

Sources mainly differ on quantities. Wood advice, for example, ranges from "± 4 logs" (Wiki, as a minimum) to "a stack of 64" (TechTimes). For an average player we pick a practical middle ground.

## Task list

### Day 1 – morning (time 0–6000)
1. **Chop wood**: ± 10–16 logs. Don't leave floating tree tops behind.
2. **Make planks, a crafting table and sticks.**
3. **Make only a wooden pickaxe.** Other wooden tools are a waste (Wiki).
4. **Mine ± 15–20 cobblestone.** Dig in steps, not straight down.
5. **Make a stone pickaxe and a stone sword (or axe)**, plus a **furnace**.

### Day 1 – afternoon (6000–11000)
6. **Gather food**: kill cows, pigs, sheep or chickens, until you have ± half a stack of raw meat. **Cook it** in the furnace.
7. **Collect 3 wool** (kill 3 sheep, or shear them) and **make a bed**.
8. **Find coal** (visible in rock faces). No coal? Smelt wood into **charcoal**. Make **torches**.

### Day 1 – evening (from 11000; the sun sets at 12000)
9. **Build or dig a closed shelter** (digging into a hill is fastest). Seal every gap and put torches inside.
10. **Place the bed and sleep.** This skips the night and makes the bed your respawn point.
11. **No bed?** Stay inside and safely mine some stone or coal until it gets light.

### Day 2
12. **Note your base coordinates** (F3) or mark your base with a tall pillar.
13. **Make a chest** for your belongings.
14. **Find iron** (in cave entrances or cliff faces; bring torches and blocks) and **smelt it**.
15. **Make a shield** (6 planks + 1 iron), according to the Wiki "the single biggest survivability upgrade". Then an **iron pickaxe**.
16. **Start renewable food**: seeds from grass, farmland within 4 blocks of water.

### Day 3
17. **Iron armor**, chestplate first (8 iron), and an **iron sword**.
18. **Bucket of water** (3 iron): puts out fire, makes lava harmless and breaks your fall.
19. **Improve your base**: walls 2+ blocks high, torches everywhere, a fence around animals and crops.
20. **Sleep every night**: after 3 nights without sleep, phantoms come.

### Always – warnings (situational)
- **Never dig straight down** (lava, holes). The number one beginner mistake according to every source.
- **Creeper nearby** (hissing): run, at least ± 16 blocks. It explodes within 1.5 s.
- **Food ≤ 6**: you can't sprint. Preferably eat to keep food ≥ 18, because only then do you heal by yourself.
- **Low health and monsters nearby**: retreat and put blocks between you and the monster.
- **Night and outside**: find cover. **Thunderstorm**: monsters spawn during the day too.
- **Don't get lost**: don't go far without coordinates or landmarks.
- **Water**: watch the air bubbles. **Sand and gravel**: can fall down.
- **Don't attack or look at endermen.**

## How this maps to the mod

| Task list | In the mod |
|---|---|
| 1–3 | milestones `wood`, `crafting_table`, `wooden_pickaxe` |
| 4–5 | `stone_tools` (pickaxe and sword), `furnace` |
| 6 | `food` (≥ 5 edible items) |
| 7, 10 | `bed`, placed before the shelter |
| 8 | `torches` |
| 9 | `shelter` |
| 12–13 | `chest` (with a hint to note coordinates) |
| 14–15 | `iron` (smelting), `shield`, `iron_pickaxe` |
| 17 | `iron_armor` |
| 18 | `water_bucket` |
| Warnings | urgent tips: `creeper`, `danger`, `hungry_*`, `dusk`, `night_outside`; warnings: `thunder`, `underground` (don't dig straight down, bring torches) |

## Sources
- [Minecraft Wiki – Tutorial: Beginner's guide](https://minecraft.wiki/w/Tutorial:Beginner%27s_guide) (edited 16 Nov 2026)
- [Minecraft Wiki – Tutorial: Things not to do](https://minecraft.wiki/w/Tutorial:Things_not_to_do)
- [Wikibooks – How to Survive in Minecraft/Day 1](https://en.wikibooks.org/wiki/How_to_Survive_in_Minecraft/Day_1) (edited 21 May 2025; some numbers wrong, see table)
- [Wikibooks – Simple Guide to Minecraft/Survival Tutorials](https://en.wikibooks.org/wiki/Simple_Guide_to_Minecraft/Survival_Tutorials) (the "skilled survival" section contains speedrun-style advice, left out)
- [Minecraft Help Center – How to Survive your First Night](https://help-redesign.zendesk.com/hc/en-us/articles/34348587958541-How-to-Survive-your-First-Night-in-Minecraft)
- [minecraft.net – How to survive your first night](https://www.minecraft.net/article/how-survive-your-first-night-minecraft) (unreachable during research)
- [GameSpot – Surviving the First Night](https://www.gamespot.com/articles/minecraft-guide-surviving-the-first-night/1100-6496575/%253C%2525=/)
- [Windows Central – How to survive your first night](https://www.windowscentral.com/how-survive-your-first-night-minecraft)
- [TechTimes – 10-day beginner survival tips (Feb 2026)](https://www.techtimes.com/articles/314709/20260218/10-day-minecraft-beginner-survival-tips-mine-your-way-noob-pro.htm)
- [Sportskeeda – Things to avoid as a beginner](https://sportskeeda.com/minecraft/things-avoid-minecraft-beginner)
- Minecraft 26.3 game files (`data/minecraft/recipe`, `loot_table`, `dimension_type`, decompiled classes `FoodData`, `NaturalSpawner`, `PhantomSpawner`, `Creeper`)
