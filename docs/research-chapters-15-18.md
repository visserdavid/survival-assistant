# Chapters 15-18: archaeology, trials, the happy ghast and automation

Game facts behind chapters 15 (Archaeology and trims), 16 (Trial challenges), 17 (The happy ghast) and
18 (Automation). Checked against the Minecraft 26.3 data and code (`minecraft-common-deobf-26.3.jar`); recipes
also with `tools/recipe_check.py` (48 recipes, 0 problems).

## How goals are detected
| Goal | Detection |
|---|---|
| `brush` | a brush, or advancement `adventure/salvage_sherd` |
| `find_trail_ruins` | standing in the `trail_ruins` structure |
| `sherd` | advancement `adventure/salvage_sherd` (brushing archaeology loot, or a sherd in the inventory) |
| `decorated_pot` | a decorated pot in the inventory or placed nearby |
| `armor_trim` | advancement `adventure/trim_with_any_armor_pattern` |
| `sniffer_egg` | advancement `husbandry/obtain_sniffer_egg` |
| `breeze_rods` | 2 breeze rods, or a mace |
| `wind_charge`, `heavy_core`, `mace` | the items (the heavy core also counts when it is in the mace) |
| `ominous_vault` | advancement `adventure/revaulting` (an ominous trial key used on an ominous vault) |
| `dried_ghast` | a dried ghast, or advancement `husbandry/place_dried_ghast_in_water` |
| `hydrate_ghast` | advancement `husbandry/place_dried_ghast_in_water` (a waterlogged dried ghast placed) |
| `ghastling` | a baby happy ghast within 16 blocks, or riding a happy ghast |
| `harness` | an item in the tag `#minecraft:harnesses`, or riding a happy ghast |
| `ride_happy_ghast` | the player's vehicle is a `HappyGhast` |
| `observer`, `crafter` | placed within 8 blocks (the crafter also by its advancement) |
| `auto_smelter` | a furnace, smoker or blast furnace with a hopper above it pointing down and a hopper below it |
| `copper_bulb` | a `CopperBulbBlock` (any oxidation stage, waxed or not) with `lit=true` nearby |
| `crafter_crafts` | advancement `adventure/crafters_crafting_crafters` |

## Chapter 15: archaeology and trims
- Brush recipe: feather, copper ingot and stick in a column.
- Suspicious sand and gravel are found in trail ruins, desert temples, desert wells and ocean ruins; mining them
  destroys the block and its loot. Pottery sherds are part of the archaeology loot.
- Decorated pot: 4 sherds or bricks (`#minecraft:decorated_pot_ingredients`) in a diamond; it stores one stack.
- Armor trims are applied at a smithing table with a trim template and a material; templates are copied with
  7 diamonds.
- Sniffer eggs come from suspicious sand in warm ocean ruins; they hatch faster on moss.

## Chapter 16: trial challenges
- Breezes spawn in trial chambers, deflect projectiles and drop breeze rods. Wind charge (shapeless): 1 breeze rod
  gives 4.
- An ominous bottle gives Bad Omen; entering trial chambers turns it into Trial Omen and makes trial spawners
  ominous. Ominous trial keys open ominous vaults (advancement `adventure/revaulting`); the heavy core is rare
  loot from them.
- Mace: heavy core on a breeze rod. Its smash attack scales with fall distance and cancels fall damage.

## Chapter 17: the happy ghast
- Dried ghast: found in Nether fossils in soul sand valleys, or crafted from 8 ghast tears around soul sand.
- Placed in water (waterlogged), it rehydrates and turns into a ghastling (baby happy ghast); the ghastling grows
  up and grows faster when fed snowballs.
- Harness (white): 3 leather, 2 glass and 1 white wool; other wool colors give other harness colors. A harnessed
  happy ghast can be ridden and carries up to four players.

## Chapter 18: automation
- Observer: 6 cobblestone, 2 redstone and nether quartz; it pulses when the block it faces changes.
- Hoppers push items into the container they face and pull from the container above; a hopper above a furnace
  feeds it and one below takes the output.
- Copper bulb: 3 copper blocks, a blaze rod and redstone dust (gives 4); every redstone pulse toggles it.
- Crafter: 5 iron ingots, a crafting table, 2 redstone and a dropper; it crafts on a redstone pulse and slots can
  be disabled. Advancement `adventure/crafters_crafting_crafters`: a crafter crafting a crafter.
- The chapter opens after Redstone and The Nether, because the observer needs quartz and the copper bulb a blaze rod.
