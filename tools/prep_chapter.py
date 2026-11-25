"""Puts a test world's guide at a chosen goal: earlier chapters finished, earlier goals of that chapter done.

Usage: python tools/prep_chapter.py <world folder or ModTesting world name> <goal id>     (Minecraft must be closed)
Example: python tools/prep_chapter.py Stappentest brew_potion [--spawn] [--revoke husbandry/tame_an_animal ...]

With --spawn the player is also moved to the world spawn (useful when a test starts in the overworld).
With --revoke the given advancements are taken away again (a goal that an earlier test already earned would otherwise
be completed at once).

Then give gear for the test with tools/set_gear.py.

The chapter structure is read from Chapters.java and TipRegistry.java, so it never goes out of date.
Every player .dat file in the world gets the change (level.dat's Player tag and playerdata/*.dat).
A backup of every changed file is written next to it as <name>.before-prep.
"""
import json
import os
import re
import shutil
import sys
from pathlib import Path

import nbtlib
from nbtlib.tag import Byte, Compound, Double, Int, List, Long, String

from mcjar import ROOT

SRC = ROOT / "src/main/java/io/github/visserdavid/survivalassistant/advice"
KEY = "survivalassistant:progress"


def world_folder(arg):
    """A world folder, or the name of a world in the Modrinth App instance "ModTesting"."""
    p = Path(arg)
    if p.is_dir():
        return p
    saves = Path(os.environ.get("APPDATA", "")) / "ModrinthApp" / "profiles" / "ModTesting" / "saves" / arg
    if saves.is_dir():
        return saves
    sys.exit(f"no world folder {arg!r} (also not in ModTesting/saves)")


def chapters():
    """[(number, id, [goal ids], [required numbers])] in menu order."""
    reg = (SRC / "TipRegistry.java").read_text(encoding="utf-8")
    ids = dict(re.findall(r'Tip (\w+) = (?:milestone|count)\("(\w+)"', reg))
    lists = {name: [ids[t] for t in re.findall(r"\w+", body)]
             for name, body in re.findall(r"List<Tip> (CHAPTER_\w+) = List\.of\(([^;]*)\);", reg, re.S)}
    out = []
    for num, cid, lst, req in re.findall(r'new Chapter\((\d+), "(\w+)", TipRegistry\.(\w+), List\.of\(([\d, ]*)\)\)',
                                         (SRC / "Chapters.java").read_text(encoding="utf-8")):
        out.append((int(num), cid, lists[lst], [int(r) for r in re.findall(r"\d+", req)]))
    return out


def plan(goal):
    chs = {c[0]: c for c in chapters()}
    target = next((c for c in chs.values() if goal in c[2]), None)
    if target is None:
        sys.exit(f"unknown goal {goal!r}")
    done, todo = [], list(target[3])
    seen = set()
    while todo:  # every required chapter, and what those need, finished
        n = todo.pop()
        if n in seen:
            continue
        seen.add(n)
        done += chs[n][2]
        todo += chs[n][3]
    done += target[2][:target[2].index(goal)]
    return target, done


def player_tags(root):
    """(file, compound holding the player's data) for every player file in the world."""
    level = nbtlib.load(root / "level.dat")
    data = level.get("Data", level)
    if "Player" in data:
        yield root / "level.dat", level, data["Player"]
    for f in sorted((root / "playerdata").glob("*.dat")) + sorted((root / "players" / "data").glob("*.dat")):
        nbt = nbtlib.load(f)
        yield f, nbt, nbt


def spawn(root):
    """(dimension, x, y, z) of the world spawn from level.dat."""
    data = nbtlib.load(root / "level.dat")["Data"]
    s = data["spawn"]
    x, y, z = (int(v) for v in s["pos"])
    return str(s.get("dimension", "minecraft:overworld")), x + 0.5, float(y), z + 0.5


def revoke(root, names):
    """Takes the advancements away from every player of the world (backup next to each file)."""
    for f in sorted((root / "players" / "advancements").glob("*.json")) + sorted((root / "advancements").glob("*.json")):
        data = json.loads(f.read_text(encoding="utf-8"))
        gone = [n for n in names if data.pop(n if ":" in n else "minecraft:" + n, None) is not None]
        if gone:
            shutil.copy2(f, f.with_name(f.name + ".before-prep"))
            f.write_text(json.dumps(data, indent=2), encoding="utf-8")
        print(f"{f.name}: revoked {', '.join(gone) or 'nothing (not earned)'}")


def main():
    argv, names = sys.argv[1:], []
    while "--revoke" in argv:
        i = argv.index("--revoke")
        names.append(argv[i + 1])
        del argv[i:i + 2]
    args = [a for a in argv if a != "--spawn"]
    if len(args) != 2:
        sys.exit(__doc__)
    root, goal = world_folder(args[0]), args[1]
    if names:
        revoke(root, names)
    to_spawn = spawn(root) if "--spawn" in sys.argv else None
    (num, cid, goals, _), done = plan(goal)
    changed = 0
    for f, file_nbt, player in player_tags(root):
        attachments = player.setdefault("fabric:attachments", Compound())
        p = attachments.get(KEY, Compound())
        played = int(p.get("played_ticks", 0))
        p["start_tick"] = Long(max(0, int(p.get("start_tick", 0))))
        p["retired"] = Byte(0)
        p["finished"] = Byte(0)
        p["chapter_offered"] = Byte(0)
        p["reminded"] = Byte(0)
        p["chapter"] = Int(num)
        p["completed"] = List[String]([String(g) for g in done])
        p["chapter_started"] = Long(played)
        p["asked_played"] = Long(-1)
        attachments[KEY] = p
        if to_spawn:
            dim, x, y, z = to_spawn
            player["Dimension"] = String(dim)
            player["Pos"] = List[Double]([Double(x), Double(y), Double(z)])
            player["Motion"] = List[Double]([Double(0), Double(0), Double(0)])
        shutil.copy2(f, f.with_name(f.name + ".before-prep"))
        file_nbt.save()
        changed += 1
        where = f", moved to the spawn ({to_spawn[0]})" if to_spawn else ""
        print(f"{f.name}: chapter {num} ({cid}), next goal {goal}, {len(done)} goals marked done{where}")
    if not changed:
        sys.exit("no player data found: open the world once, walk around a few seconds, then quit to the title screen")


if __name__ == "__main__":
    main()
