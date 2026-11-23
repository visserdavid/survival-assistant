# Chapters 6-8: farming and animals, the ocean, exploring

Game facts behind the goals and tips of chapters 6 (Farming and animals), 7 (The ocean) and 8 (Exploring).
Checked against the Minecraft 26.3 data and code (`minecraft-common-deobf-26.3.jar`); recipes also with
`tools/recipe_check.py` (31 recipes, 0 problems).

## How goals are detected
| Goal | Detection |
|---|---|
| `tame_animal` | advancement `husbandry/tame_an_animal` (trigger `tame_animal`) |
| `fishing` | advancement `husbandry/fishy_business` (trigger `fishing_rod_hooked` with cod, salmon, pufferfish or tropical fish) |
| `honey` | advancement `husbandry/safely_harvest_honey` (a glass bottle on a smoked beehive or bee nest) |
| `horse` | riding a tamed `AbstractHorse` (horse, donkey, mule, camel) |
| `milk`, `sugar_cane`, `boat`, `treasure`, `turtle_helmet`, `map`, `spyglass` | items in the inventory (milk bucket, 16 sugar cane, any boat or riding one, heart of the sea, turtle shell, filled map, spyglass) |
| `trident` | a trident in the inventory or hand, or advancement `adventure/throw_trident` |
| `find_wreck`, `find_temple` | standing in a piece of a structure (`StructureManager.getStructureWithPieceAt`): `shipwreck`, `shipwreck_beached`, `ocean_ruin_cold`, `ocean_ruin_warm`; `desert_pyramid`, `jungle_pyramid` |
| `trial_chambers` | advancement `adventure/minecraft_trials_edition` (location in `trial_chambers`) or standing in the structure |
| `open_vault` | advancement `adventure/under_lock_and_key` (a trial key used on a non-ominous vault) |

The structure check is one call per evaluation for a fixed set of structure ids (also used for villages), and only
in the overworld.

## Chapter 6: farming and animals
- Taming: wolves with bones, cats with raw cod or salmon, parrots with seeds, horses by riding them until hearts
  appear. A tamed wolf fights alongside its owner.
- Saddle recipe (26.3): 3 leather and 1 iron ingot (` X `, `X#X`). Horses spawn on plains and savannas.
- Milk: a bucket used on a cow (or goat). Drinking milk clears all status effects.
- Sugar cane grows on sand, dirt or grass blocks next to water and regrows when the bottom block is left.
  It makes paper and sugar.
- Fishing rod recipe: 3 sticks and 2 string. Rain shortens the wait for a bite; the treasure loot includes
  enchanted books.
- Honey: a campfire under the hive or nest makes it "smokey" (the advancement checks `smokey: true`), so the
  bees stay calm. Glass bottle: 3 glass (from smelted sand). Campfire recipe: 3 sticks, coal or charcoal, 3 logs.

## Chapter 7: the ocean
- Boat recipe: 5 planks of one wood type. Boats are fast on water and very fast on ice.
- Shipwrecks generate in oceans and on beaches (`shipwreck_beached`); ocean ruins under water, with suspicious
  sand or gravel for archaeology. Shipwreck map chests contain buried treasure maps.
- Buried treasure: the chest always contains a heart of the sea.
- Turtles breed with seagrass and lay eggs on the beach; a baby turtle drops a turtle scute when it grows up.
  Turtle shell recipe: 5 turtle scutes. Wearing it gives extra time underwater.
- Tridents drop only from drowned that hold one; such drowned throw it at the player.

## Chapter 8: exploring
- Map recipe: 8 paper around a compass; compass: 4 iron ingots around redstone dust.
- Spyglass recipe: 1 amethyst shard over 2 copper ingots. Amethyst grows in geodes underground.
- Desert pyramid: the hidden room has a pressure plate wired to TNT. Jungle pyramid: tripwires with arrow
  dispensers and a lever puzzle with a hidden chest.
- Trial chambers: underground structures of copper and tuff with trial spawners. Beating a trial spawner gives
  trial keys; a key opens a vault once per player. The breeze is a mob found there. Cartographers can sell a
  trial explorer map (`on_buried_trial_chambers_maps`).
