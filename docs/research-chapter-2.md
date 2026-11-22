# Chapter 2: after the first days

Chapter 1 (see `research-first-days.md`) gets a new player safely through the first nights: tools, food,
bed, shelter, iron. Chapter 2 is the step from *surviving* to *settling in*: steady food, full protection,
ranged combat, diamonds, and the preparations for the Nether and enchanting. It deliberately stops before
actually entering the Nether, which is a big jump in danger for an average player.

All facts below are checked against the Minecraft 26.3 game data (recipes, tags, loot tables, stats) in the
jar Loom uses; the recipes are also checked by `tools/recipe_check.py`.

## Goals (in order)

| # | Id | Goal | Done when | Why / verified facts |
|---|---|---|---|---|
| 1 | `farm` | Start a farm | 6 crops planted near you | Endless food without hunting. Seeds drop from short grass (12.5 %, loot table `blocks/short_grass`). Hoe: 2 planks + 2 sticks. Crops = block tag `crops` (wheat, carrots, potatoes, beetroots, ...). Plant next to water. |
| 2 | `bread` | Bake bread | 3 bread | Bread = 3 wheat in a row (recipe `bread`), no furnace needed. |
| 3 | `breed` | Breed animals | stat `animals_bred` >= 2 | A renewable source of meat, leather and wool. Cows and sheep eat wheat, pigs carrots/potatoes/beetroot, chickens seeds (item tags `cow_food`, `sheep_food`, `pig_food`, `chicken_food`). |
| 4 | `full_iron_armor` | Full iron armor | 4 pieces (iron or better), worn or carried | Chapter 1 asked for one piece. Helmet 5, chestplate 8, leggings 7, boots 4 iron = 24 iron. |
| 5 | `bow` | Bow and arrows | a bow (or crossbow) and 16 arrows | Fight skeletons and creepers from a distance. Bow = 3 sticks + 3 string (spiders); 4 arrows = flint + stick + feather (gravel, chickens). |
| 6 | `diamonds` | Find diamonds | 3 diamonds (or a diamond pickaxe) | Diamond ore is most common deep down (see `biomes-and-heights.md`); it needs an iron pickaxe (block tag `needs_iron_tool`). |
| 7 | `diamond_pickaxe` | Diamond pickaxe | a diamond pickaxe or better | The only way to mine obsidian (block tag `needs_diamond_tool`). |
| 8 | `obsidian` | Collect obsidian | 10 obsidian | Water on still lava makes obsidian. 10 is the smallest Nether portal frame (4 x 5 without corners). |
| 9 | `flint_and_steel` | Flint and steel | in your inventory | Lights the portal. Shapeless: 1 iron ingot + 1 flint. |
| 10 | `enchanting_table` | Enchanting table | carried or placed nearby | Book + 2 diamonds + 4 obsidian. Book = 3 paper (sugar cane) + 1 leather (cows). |

## Design choices
- The chapter starts only when the player wants it: after all 15 chapter-1 goals the guide congratulates
  and offers chapter 2 with clickable buttons; "no" (or no answer within a day) means goodbye.
- Chapter 2 gets its own period (`chapter2Days`, default 4 days) and can be extended like chapter 1.
- Completed goals stay completed (they are never taken away when items are used up), like in chapter 1.
- Recipes on the recipe card: wooden hoe, bread, bow, diamond pickaxe, flint and steel, enchanting table.
