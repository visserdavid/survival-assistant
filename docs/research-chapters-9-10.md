# Chapters 9-10: redstone and the End

Game facts behind the goals and tips of chapters 9 (Redstone) and 10 (The End). Checked against the Minecraft
26.3 data and code (`minecraft-common-deobf-26.3.jar`); recipes also with `tools/recipe_check.py`
(36 recipes, 0 problems).

## How goals are detected
| Goal | Detection |
|---|---|
| `redstone_dust` | 16 redstone dust in the inventory |
| `iron_door` | an iron door within 8 blocks with `open=true` (`BlockStateProperties.OPEN`); only redstone opens an iron door |
| `piston` | a piston or sticky piston within 8 blocks with `extended=true` (so it is powered) |
| `repeater`, `hopper` | a placed repeater or hopper within 8 blocks |
| `ender_pearls` | ender pearls plus eyes of ender ≥ 12, or advancement `story/follow_ender_eye` |
| `eyes_of_ender` | 12 eyes of ender, or advancement `story/enter_the_end` |
| `find_stronghold` | advancement `story/follow_ender_eye` (location in the `stronghold` structure) |
| `enter_end` | advancement `story/enter_the_end`, or standing in the End |
| `dragon` | advancement `end/kill_dragon` |
| `end_city` | advancement `end/find_end_city` |
| `elytra` | an elytra in the inventory or worn, or advancement `end/elytra` (trigger `inventory_changed`) |

The chapter opens after The Nether (blaze powder) and Brewing and enchanting (the strength to fight the dragon).
The structure check now also runs in the End, for the end city note.

## Chapter 9: redstone
- Redstone ore generates deep in the overworld (below Y 16, most of it near the bottom) and needs an iron
  pickaxe or better.
- Redstone dust carries a signal up to 15 blocks; a repeater restores it to full strength, adds a delay
  (right-click) and only passes the signal from back to front.
- Recipes: iron door 6 iron ingots (3 doors); piston 3 planks, 4 cobblestone, 1 iron ingot, 1 redstone;
  repeater 2 redstone torches, 1 redstone, 3 stone; hopper 5 iron ingots around a chest.
- Iron doors can't be opened by hand or broken by zombies. A sticky piston (piston + slimeball) also pulls.
- A hopper pushes items into the container it faces and pulls from the container above it, so a hopper under a
  furnace collects the output.

## Chapter 10: the End
- Endermen drop ender pearls; they are 3 blocks tall, so a 2-block-high roof keeps them out. Clerics can sell
  ender pearls.
- Eye of ender (shapeless): 1 ender pearl + 1 blaze powder. A thrown eye flies towards the nearest stronghold and
  sometimes breaks. The end portal has 12 frames; some may already hold an eye.
- Strongholds have stone brick corridors with infested blocks (silverfish) and a portal room with the end portal
  above a lava pool.
- The End: end crystals on obsidian pillars heal the dragon (some are caged in iron bars). The dragon perches on
  the exit portal; its breath leaves a purple cloud. Looking at endermen angers them. Falling into the void kills.
- After the dragon an end gateway appears; an ender pearl thrown into it takes you to the outer islands with
  end cities. Shulker bullets cause levitation; milk removes it. End ships hold an elytra in an item frame.
