"""Writes small WorldEdit schematics for in-game tests into the ModTesting instance.

Usage: python tools/test_schematics.py [name ...]      (no names: all of them)
In the game, stand where the build should go, then:  //schem load <name>   and   //paste

The builds are placed around and under the player (see each build's offset), so they work in any biome. Every build
clears the air above it. They are Sponge schematics version 3, the format WorldEdit 7.3 and later reads.
"""
import os
import sys
from pathlib import Path

import nbtlib
from nbtlib.tag import ByteArray, Compound, Int, IntArray, List, Short

DATA_VERSION = 5023  # Minecraft 26.3; WorldEdit converts older data, not newer
SCHEMATICS = Path(os.environ.get("APPDATA", "")) / "ModrinthApp" / "profiles" / "ModTesting" / "config" / "worldedit" / "schematics"


class Build:
    """A box of blocks; block(x, y, z) is relative to the box corner, offset puts the corner relative to the player."""

    def __init__(self, width, height, length, offset, fill="minecraft:air"):
        self.w, self.h, self.l, self.offset = width, height, length, offset
        self.blocks = {(x, y, z): fill for x in range(width) for y in range(height) for z in range(length)}

    def set(self, x, y, z, state):
        self.blocks[(x, y, z)] = state

    def layer(self, y, state):
        for x in range(self.w):
            for z in range(self.l):
                self.set(x, y, z, state)

    def save(self, name):
        palette = {}
        data = bytearray()
        for y in range(self.h):
            for z in range(self.l):
                for x in range(self.w):
                    index = palette.setdefault(self.blocks[(x, y, z)], len(palette))
                    while True:  # varint
                        byte = index & 0x7F
                        index >>= 7
                        data.append(byte | (0x80 if index else 0))
                        if not index:
                            break
        schematic = Compound({
            "Version": Int(3),
            "DataVersion": Int(DATA_VERSION),
            "Width": Short(self.w), "Height": Short(self.h), "Length": Short(self.l),
            "Offset": IntArray([Int(v) for v in self.offset]),
            "Blocks": Compound({
                "Palette": Compound({state: Int(i) for state, i in palette.items()}),
                "Data": ByteArray([b if b < 128 else b - 256 for b in data]),
                "BlockEntities": List[Compound](),
            }),
        })
        SCHEMATICS.mkdir(parents=True, exist_ok=True)
        path = SCHEMATICS / f"{name}.schem"
        nbtlib.File({"Schematic": schematic}).save(path, gzipped=True)
        return path


def farm_plot():
    """9x9 dirt with a water source in the middle (all of it within 4 blocks of the water), the player standing on
    it. Plain dirt, so tilling is still part of the test."""
    b = Build(9, 4, 9, (-4, -1, -4))
    b.layer(0, "minecraft:dirt")
    b.set(4, 0, 4, "minecraft:water[level=0]")
    return b


def wheat_field():
    """The farm plot, tilled and full of ripe wheat around the water: 80 plants, 9 wheat is quick to harvest."""
    b = Build(9, 4, 9, (-4, -1, -4))
    b.layer(0, "minecraft:farmland[moisture=7]")
    b.layer(1, "minecraft:wheat[age=7]")
    b.set(4, 0, 4, "minecraft:water[level=0]")
    b.set(4, 1, 4, "minecraft:air")
    return b


def pond():
    """A 5x5 pond, 2 deep, with a grass rim, the player standing on the rim at the near edge."""
    b = Build(7, 5, 7, (-3, -3, 1))
    for y in range(3):
        b.layer(y, "minecraft:grass_block" if y == 2 else "minecraft:dirt")
    for x in range(1, 6):
        for z in range(1, 6):
            b.set(x, 1, z, "minecraft:water[level=0]")
            b.set(x, 2, z, "minecraft:water[level=0]")
    return b


BUILDS = {
    "sa_farm_plot": farm_plot,
    "sa_wheat_field": wheat_field,
    "sa_pond": pond,
}


def main():
    names = sys.argv[1:] or list(BUILDS)
    for name in names:
        if name not in BUILDS:
            sys.exit(f"unknown build {name!r}; known: {', '.join(BUILDS)}")
        print(f"{BUILDS[name]().save(name)}")


if __name__ == "__main__":
    main()
