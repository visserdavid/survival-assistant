"""Digs a 3x3 staircase down from where the player stands, in the direction they look, for in-game tests.

Usage: python tools/test_stair.py <world folder or ModTesting world name> [floor Y, default -58]     (Minecraft closed)

It reads the player's position and facing from the save and writes a small data pack into the world. When the world
is opened, the pack waits until a player is there and the whole staircase is loaded, then carves it once with /fill:
only stone, dirt, gravel, sand and the like become air. Ores, water, lava, chests and anything built stay. Torches
light every 6th step, and a short flat tunnel ends the stairs. The pack remembers that it ran (command storage
sa:stair), so opening the world again changes nothing; delete datapacks/sa_stair to remove it.
"""
import glob
import json
import os
import shutil
import sys
from pathlib import Path

import nbtlib

PACK_FORMAT = 121  # Minecraft 26.3 data packs
TORCH_EVERY = 6
LANDING = 4  # flat blocks at the bottom

DIGGABLE = [
    "#minecraft:base_stone_overworld", "#minecraft:dirt", "#minecraft:sand", "#minecraft:terracotta",
    "#minecraft:leaves", "#minecraft:logs", "#minecraft:flowers",
    "minecraft:grass_block", "minecraft:podzol", "minecraft:mycelium", "minecraft:moss_block", "minecraft:mud",
    "minecraft:gravel", "minecraft:sandstone", "minecraft:red_sandstone", "minecraft:clay", "minecraft:cobblestone",
    "minecraft:mossy_cobblestone", "minecraft:cobbled_deepslate", "minecraft:calcite", "minecraft:dripstone_block",
    "minecraft:pointed_dripstone", "minecraft:smooth_basalt", "minecraft:infested_stone",
    "minecraft:infested_deepslate", "minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow",
    "minecraft:ice", "minecraft:packed_ice", "minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern",
    "minecraft:large_fern", "minecraft:vine", "minecraft:glow_lichen", "minecraft:moss_carpet",
    "minecraft:cave_vines", "minecraft:cave_vines_plant",
]

# yaw → (forward dx, dz) and the sideways axis; Minecraft: 0 south (+Z), 90 west (-X), 180 north (-Z), 270 east (+X)
DIRECTIONS = {0: ("south", 0, 1), 90: ("west", -1, 0), 180: ("north", 0, -1), 270: ("east", 1, 0)}


def world_folder(arg):
    p = Path(arg)
    if p.is_dir():
        return p
    saves = Path(os.environ.get("APPDATA", "")) / "ModrinthApp" / "profiles" / "ModTesting" / "saves" / arg
    if saves.is_dir():
        return saves
    sys.exit(f"no world folder {arg!r} (also not in ModTesting/saves)")


def main():
    if len(sys.argv) not in (2, 3):
        sys.exit(__doc__)
    root = world_folder(sys.argv[1])
    floor = int(sys.argv[2]) if len(sys.argv) == 3 else -58
    files = glob.glob(str(root / "players" / "data" / "*.dat"))
    if not files:
        sys.exit("no player data: open the world once first")
    player = nbtlib.load(files[0])
    if str(player["Dimension"]) != "minecraft:overworld":
        sys.exit("the player is not in the overworld")
    px, py, pz = (int(float(v) // 1) for v in player["Pos"])
    yaw = float(player["Rotation"][0]) % 360
    name, dx, dz = DIRECTIONS[min(DIRECTIONS, key=lambda a: min(abs(yaw - a), 360 - abs(yaw - a)))]
    steps = py - 1 - floor  # the block under the feet is py - 1; every step goes one lower
    if steps < 1:
        sys.exit(f"the player stands at Y {py}, not above floor Y {floor}")

    def at(n, side):  # n blocks forward, side blocks to the right-hand axis
        return px + dx * n + (side if dz else 0), pz + dz * n + (side if dx else 0)

    lines = []
    for n in range(1, steps + LANDING + 1):
        bottom = py - min(n, steps)  # floor below this step is bottom - 1
        (x1, z1), (x2, z2) = at(n, -1), at(n, 1)
        lines.append(f"fill {x1} {bottom} {z1} {x2} {bottom + 2} {z2} air replace #sa:diggable")
        if n % TORCH_EVERY == 0:
            tx, tz = at(n, -1)
            lines.append(f"setblock {tx} {bottom} {tz} torch keep")
    end_x, end_z = at(steps + LANDING, 0)
    mid_x, mid_z = at((steps + LANDING) // 2, 0)
    xs = [px + dx * n + s for n in (0, steps + LANDING) for s in (-1, 1)] if dz == 0 else [px - 1, px + 1]
    zs = [pz + dz * n + s for n in (0, steps + LANDING) for s in (-1, 1)] if dx == 0 else [pz - 1, pz + 1]
    area = f"{min(xs)} {min(zs)} {max(xs)} {max(zs)}"
    lines += ["data modify storage sa:stair done set value 1b", f"forceload remove {area}",
              f'tellraw @a {{"text":"Staircase dug: {steps} steps {name} to floor Y {floor}.","color":"gray"}}']

    pack = root / "datapacks" / "sa_stair"
    if pack.exists():
        shutil.rmtree(pack)
    (pack / "data" / "sa" / "function").mkdir(parents=True)
    (pack / "data" / "sa" / "tags" / "block").mkdir(parents=True)
    (pack / "data" / "minecraft" / "tags" / "function").mkdir(parents=True)
    (pack / "pack.mcmeta").write_text(json.dumps({"pack": {
        "description": "Survival Assistant test staircase", "min_format": PACK_FORMAT, "max_format": PACK_FORMAT}}),
        encoding="utf-8")
    (pack / "data" / "sa" / "tags" / "block" / "diggable.json").write_text(json.dumps({"values": [
        {"id": v, "required": False} for v in DIGGABLE]}, indent=1), encoding="utf-8")
    (pack / "data" / "minecraft" / "tags" / "function" / "load.json").write_text(
        json.dumps({"values": ["sa:stair_load"]}), encoding="utf-8")
    (pack / "data" / "minecraft" / "tags" / "function" / "tick.json").write_text(
        json.dumps({"values": ["sa:stair_tick"]}), encoding="utf-8")
    fn = pack / "data" / "sa" / "function"
    (fn / "stair_load.mcfunction").write_text(
        f"execute unless data storage sa:stair {{done:1b}} run forceload add {area}\n", encoding="utf-8")
    (fn / "stair_tick.mcfunction").write_text(
        f"execute unless data storage sa:stair {{done:1b}} if entity @a if loaded {px} {py} {pz} "
        f"if loaded {mid_x} {py} {mid_z} if loaded {end_x} {floor} {end_z} run function sa:stair_build\n",
        encoding="utf-8")
    (fn / "stair_build.mcfunction").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{pack}: {steps} steps {name} from ({px}, {py}, {pz}) to floor Y {floor}, then {LANDING} flat; "
          f"{len(lines)} commands")


if __name__ == "__main__":
    main()
