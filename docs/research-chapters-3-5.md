# Chapters 3-5: villages, the Nether, brewing and enchanting

Game facts behind the goals and tips of chapters 3 (Villages and trading), 4 (The Nether) and 5 (Brewing and
enchanting). Checked against the Minecraft 26.3 data and code (`minecraft-common-deobf-26.3.jar`). Recipes are
also checked by `tools/recipe_check.py`.

## How goals are detected
Where possible a goal uses a **vanilla advancement**, so it is detected exactly as the game counts it.
All ids exist in `data/minecraft/advancement/`:

| Goal | Detection |
|---|---|
| `first_trade` | advancement `adventure/trade` (trigger `villager_trade`), or the statistic below ≥ 1 |
| `trades` | statistic `traded_with_villager` (`Stats.TRADED_WITH_VILLAGER`) ≥ 5 |
| `iron_golem` | advancement `adventure/summon_iron_golem` (trigger `summoned_entity`, entity `iron_golem`) |
| `enter_nether` | advancement `story/enter_the_nether`, or standing in the Nether |
| `find_fortress` | advancement `nether/find_fortress` |
| `brew_potion` | advancement `nether/brew_potion` |
| `enchant` | advancement `story/enchant_item` |
| `find_village` | standing in a piece of a structure with tag `#minecraft:village` (`StructureManager.getStructureWithPieceAt`) |
| `bookshelves` | the game's own check: `EnchantingTableBlock.BOOKSHELF_OFFSETS` + `isValidBookShelf` |
| `gold_armor` | a worn item in the item tag `#minecraft:piglin_safe_armor` |

## Chapter 3: villages and trading
- Village structures (`tags/worldgen/structure/village.json`): plains, desert, savanna, snowy and taiga villages.
- Job sites are point-of-interest types (`tags/point_of_interest_type/acquirable_job_site.json`); the lectern is
  the librarian's. An unemployed villager next to a free lectern becomes a librarian, and librarians sell
  enchanted books. A villager who has never traded can lose and regain the job, which rerolls the offers.
- Trading raises a villager's level and unlocks better offers.
- Iron golem: 4 iron blocks in a T and a carved pumpkin on top (advancement icon: `carved_pumpkin`).
  A carved pumpkin is made by using shears on a pumpkin. Iron block recipe: 9 iron ingots.
- A rung bell sends villagers indoors. At night zombies attack villagers and can turn them into zombie
  villagers (how often depends on the difficulty, so the tip gives no number).

## Chapter 4: the Nether
- `dimension_type/the_nether.json`: `bed_rule` with `can_sleep: never` and `destroy_on_use: true`, so a bed
  explodes when used. `fast_lava: true`: lava flows faster.
- Piglins: item tag `piglin_safe_armor` = golden helmet, chestplate, leggings and boots. One piece is enough.
- Golden helmet recipe: 5 gold ingots (`XXX`, `X X`). Gold ore is found below Y 32 (more in badlands).
- Portal: an obsidian frame of at least 4 wide and 5 high; the corners are not needed (10 obsidian), lit with
  flint and steel. Coordinates in the Nether are scaled 1:8.
- Ghast fireballs can be hit back. Cobblestone (blast resistance 6) withstands a ghast fireball.
- Blazes spawn from spawners in Nether fortresses and drop blaze rods. Snowballs damage blazes.
- Nether wart grows on soul sand (in fortresses, often near the stairwells) and can be planted elsewhere,
  also in the overworld.

## Chapter 5: brewing and enchanting
- Brewing stand recipe: 1 blaze rod over 3 stone crafting materials (`#minecraft:stone_crafting_materials`,
  such as cobblestone).
- Blaze powder: 1 blaze rod gives 2 blaze powder. Blaze powder is the brewing stand's fuel.
- Water bottle + Nether wart = awkward potion, the base for most potions. Examples: golden carrot gives night
  vision, glistering melon slice gives healing, magma cream gives fire resistance, sugar gives swiftness.
- Bookshelf recipe: 6 planks + 3 books. Book: 3 paper + 1 leather. Paper: 3 sugar cane.
- Enchanting: bookshelves count when placed two blocks from the table with air in between; 15 give the
  maximum level of 30. The third option costs 3 lapis lazuli and 3 levels.
- Anvil recipe: 3 iron blocks and 4 iron ingots (31 ingots). An anvil repairs, renames and combines items
  with enchanted books.
- Dying drops only part of your experience, so most of it is lost.
