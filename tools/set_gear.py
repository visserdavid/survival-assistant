"""Empty the player's inventory in a world and give gear for the next test.

Usage: python tools/set_gear.py <world folder or ModTesting world name> <goal>

Minecraft must be closed. The items a test is about are never handed out (they would skip its steps); the test
instructions give those with /give. Tests that need crafting get a crafting table. Goals without a kit get the
default one; add a kit to KITS for every new goal with steps.
"""
import os
import sys
from pathlib import Path

import nbtlib
from nbtlib.tag import Byte, Compound, Int, List, String


def world_folder(arg):
    """A world folder, or the name of a world in the Modrinth App instance "ModTesting"."""
    p = Path(arg)
    if p.is_dir():
        return p
    saves = Path(os.environ.get("APPDATA", "")) / "ModrinthApp" / "profiles" / "ModTesting" / "saves" / arg
    if saves.is_dir():
        return saves
    sys.exit(f"no world folder {arg!r} (also not in ModTesting/saves)")


def item(id, count=1, ench=None, **components):
    c = Compound({"id": String("minecraft:" + id), "count": Int(count)})
    comp = {}
    if ench:
        comp["minecraft:enchantments"] = Compound({"minecraft:" + k: Int(v) for k, v in ench.items()})
    comp.update(components)
    if comp:
        c["components"] = Compound(comp)
    return c


ARMOR = {"protection": 4, "unbreaking": 3, "mending": 1}


def armor(material="netherite", legs_extra=None, feet_extra=None):
    return {
        "head": item(f"{material}_helmet", ench=ARMOR | {"respiration": 3}),
        "chest": item(f"{material}_chestplate", ench=ARMOR),
        "legs": item(f"{material}_leggings", ench=ARMOR | (legs_extra or {})),
        "feet": item(f"{material}_boots", ench=ARMOR | {"feather_falling": 4} | (feet_extra or {})),
    }


DIAMOND_SWORD = item("diamond_sword", ench={"sharpness": 5, "unbreaking": 3, "mending": 1, "looting": 3})
DIAMOND_PICK = item("diamond_pickaxe", ench={"efficiency": 5, "unbreaking": 3, "mending": 1, "fortune": 3})
SWORD = item("netherite_sword", ench={"sharpness": 5, "unbreaking": 3, "mending": 1, "looting": 3})
PICK = item("netherite_pickaxe", ench={"efficiency": 5, "unbreaking": 3, "mending": 1, "fortune": 3})
FOOD = item("cooked_beef", 32)
GOLDEN = item("golden_apple", 8)
TORCHES = item("torch", 64)
BLOCKS = item("cobblestone", 64)
SHIELD = item("shield", ench={"unbreaking": 3, "mending": 1})
TABLE = item("crafting_table")

# per goal: (hotbar items in order, armor, off hand)
KITS = {
    # ocean, drowned: a ranged trident is the thing to test, so no trident here
    "trident": ([SWORD, item("bow", ench={"power": 5, "infinity": 1}), item("arrow"), FOOD, GOLDEN, BLOCKS],
                armor(feet_extra={"depth_strider": 3}), SHIELD),
    # deep dark: sneak fast and quietly; no echo shards, compass, iron or redstone
    "recovery_compass": ([SWORD, PICK, FOOD, GOLDEN, TORCHES, BLOCKS, item("white_wool", 32), TABLE],
                         armor(legs_extra={"swift_sneak": 3}), SHIELD),
    # trial chambers: breezes deflect arrows, so melee; no breeze rod, heavy core or mace
    "mace": ([SWORD, PICK, FOOD, GOLDEN, TORCHES, BLOCKS, item("trial_key", 4), item("ominous_trial_key", 4), TABLE],
             armor(), SHIELD),
    # beach: shears to cut seagrass yourself; no seagrass, scutes or turtle shell
    "turtle_helmet": ([SWORD, item("shears", ench={"efficiency": 5, "unbreaking": 3}), FOOD, GOLDEN, TORCHES, BLOCKS,
                       item("oak_boat"), TABLE], armor(feet_extra={"depth_strider": 3}), SHIELD),
    # Nether, ghasts: a strong bow; no ghast tears or soul sand
    "dried_ghast": ([item("bow", ench={"power": 5, "punch": 2, "flame": 1, "infinity": 1, "unbreaking": 3}), item("arrow"),
                     SWORD, PICK, FOOD, GOLDEN, BLOCKS, item("potion", 3, **{"minecraft:potion_contents": Compound({"potion": String("minecraft:long_fire_resistance")})}),
                     TABLE], armor(), SHIELD),
    # bastion: piglin brutes always attack; gold keeps the other piglins calm. No upgrade template, and no netherite
    # at all: netherite gear completes the goal too
    "upgrade_template": ([DIAMOND_SWORD, DIAMOND_PICK, item("bow", ench={"power": 5, "infinity": 1}), item("arrow"), FOOD, GOLDEN, BLOCKS,
                          item("potion", 3, **{"minecraft:potion_contents": Compound({"potion": String("minecraft:long_fire_resistance")})})],
                         armor("diamond") | {"head": item("golden_helmet", ench={"protection": 4, "unbreaking": 3})}, SHIELD),
    # crafter: buttons for the pulse, a table to place first; no crafter, iron, dropper or redstone
    "crafter_crafts": ([PICK, item("stone_button", 4), item("hopper", 2), item("chest"), BLOCKS, TORCHES, FOOD, TABLE],
                       armor(), SHIELD),
    # early game, chapter 2: simple stone tools; no hoe, seeds, string, flint, feathers, bow or arrows
    "farm": ([item("stone_sword"), item("stone_pickaxe"), item("stone_shovel"), item("water_bucket"), FOOD, TORCHES,
              item("dirt", 32), TABLE], armor("iron"), None),
    "bow": ([item("stone_sword"), item("stone_shovel"), item("stick", 8), item("gravel", 32), FOOD, TORCHES, TABLE],
            armor("iron"), SHIELD),
    # redstone: a lever and buttons for the pulse; no copper, blaze rod, redstone or bulbs
    "copper_bulb": ([PICK, item("lever", 4), item("stone_button", 4), BLOCKS, TORCHES, FOOD, TABLE], armor(), SHIELD),
}
DEFAULT = ([SWORD, PICK, FOOD, GOLDEN, TORCHES, BLOCKS, TABLE], armor(), SHIELD)


def main():
    root, goal = world_folder(sys.argv[1]), sys.argv[2]
    hotbar, worn, offhand = KITS.get(goal, DEFAULT)
    for f in (root / "players" / "data").glob("*.dat"):
        nbt = nbtlib.load(f)
        inv = List[Compound]()
        for slot, it in enumerate(hotbar):
            c = Compound(it)
            c["Slot"] = Byte(slot)
            inv.append(c)
        nbt["Inventory"] = inv
        eq = Compound({k: Compound(v) for k, v in worn.items()})
        if offhand is not None:
            eq["offhand"] = Compound(offhand)
        nbt["equipment"] = eq
        nbt["SelectedItemSlot"] = Int(0)
        nbt.save()
        print(f"{f.name}: inventory emptied, gear for {goal}: " + ", ".join(str(i["id"])[10:] for i in hotbar)
              + " + " + ", ".join(str(v["id"])[10:] for v in worn.values()))


if __name__ == "__main__":
    main()
