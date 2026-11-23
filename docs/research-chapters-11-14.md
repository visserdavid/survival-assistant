# Chapters 11-14: netherite, the Wither, the Deep Dark and raids

Game facts behind the "endgame" chapters 11 (Netherite and bastions), 12 (The Wither and the beacon),
13 (The Deep Dark) and 14 (Raids and Illagers). Checked against the Minecraft 26.3 data and code
(`minecraft-common-deobf-26.3.jar`); recipes also with `tools/recipe_check.py` (40 recipes, 0 problems).

## How goals are detected
| Goal | Detection |
|---|---|
| `ancient_debris` | advancement `nether/obtain_ancient_debris` (ancient debris in the inventory) |
| `netherite_ingot`, `upgrade_template` | the item, or any netherite gear (so using them keeps the goal) |
| `find_bastion` | advancement `nether/find_bastion` (location in `bastion_remnant`) |
| `netherite_gear` | any `netherite_*` item except the ingot, scrap, block and smithing template |
| `respawn_anchor` | advancement `nether/charge_respawn_anchor` |
| `wither_skulls`, `soul_sand` | 3 skulls / 4 soul sand, or advancement `nether/summon_wither` (they are used up) |
| `summon_wither` | advancement `nether/summon_wither` |
| `nether_star` | the item, or advancement `nether/create_beacon` |
| `beacon`, `full_beacon` | advancements `nether/create_beacon` (level ≥ 1) and `nether/create_full_beacon` (level 4) |
| `find_ancient_city` | standing in the `ancient_city` structure |
| `sneak_past` | advancement `adventure/avoid_vibration` |
| `echo_shards`, `recovery_compass` | 8 echo shards (or the compass) / a recovery compass |
| `find_outpost` | standing in `pillager_outpost`, or advancement `adventure/voluntary_exile` |
| `raid_captain` | advancement `adventure/voluntary_exile` (killing a raid captain) |
| `ominous_bottle` | the item, or advancement `adventure/hero_of_the_village` |
| `win_raid` | advancement `adventure/hero_of_the_village` |
| `totem` | a totem of undying in the inventory or hand, or advancement `adventure/totem_of_undying` |

The structure check now also runs in the Nether (bastions). A warden within 24 blocks is an urgent warning that
holds back all other tips.

## Chapter 11: netherite and bastions
- Ancient debris generates in the Nether, mostly between Y 8 and Y 22 (peak around Y 15); it needs a diamond
  pickaxe. Smelting gives netherite scrap.
- Netherite ingot (shapeless): 4 netherite scrap + 4 gold ingots.
- Netherite upgrade smithing template: found in bastion remnant chests; copied with 7 diamonds and netherrack.
- Smithing table: template + diamond item + netherite ingot; enchantments are kept. Netherite items don't burn
  in lava.
- Bastions: piglin brutes are always hostile, golden armor doesn't help. Piglins get angry when you open chests
  or mine gold blocks near them.
- Respawn anchor: 6 crying obsidian + 3 glowstone; charged with glowstone; explodes when used outside the Nether.

## Chapter 12: the Wither and the beacon
- Wither skeletons spawn in Nether fortresses; skulls are a rare drop (Looting raises the chance).
- The Wither is built from 4 soul sand or soul soil in a T with 3 wither skeleton skulls on top; it explodes on
  spawning, destroys blocks and inflicts the Wither effect (milk cures it). Below half health it becomes immune
  to arrows.
- It drops a nether star, which is immune to explosions and never despawns.
- Beacon: 5 glass, 1 nether star, 3 obsidian, on a pyramid of iron, gold, emerald, diamond or netherite blocks;
  a full pyramid has 4 levels (9 + 25 + 49 + 81 = 164 blocks) and unlocks a secondary power. Activated with an
  iron, gold, emerald, diamond or netherite payment.

## Chapter 13: the Deep Dark
- Ancient cities generate in the deep dark biome, far below Y 0.
- Sculk sensors react to vibrations; sneaking and walking on wool make none. Three sculk shrieker warnings
  summon the warden, which is blind and tracks sound and smell. Thrown projectiles (snowballs) distract it.
- Echo shards are found in ancient city chests. Recovery compass: 8 echo shards around a compass; it points to
  the last death location.

## Chapter 14: raids and Illagers
- Pillager outposts are dark-oak towers with white-and-grey (ominous) banners, often near villages.
- Raid captains carry an ominous banner; killing one gives advancement `adventure/voluntary_exile` and drops an
  ominous bottle. Drinking it gives Bad Omen; entering a village then starts a raid.
- Winning a raid gives Hero of the Village (discounts). Evokers drop a totem of undying, which saves you from one
  death when held.
