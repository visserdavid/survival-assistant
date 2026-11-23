"""Checks advice/Recipes.java against the recipe data in the Minecraft jar.

Usage: python tools/recipe_check.py <path to minecraft-common jar>
(the jar Loom uses lives under ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/)

For every recipe in Recipes.java it loads data/minecraft/recipe/<result>.json and checks that
- the shape (pattern with spaces) is identical, or for shapeless recipes the ingredient counts match;
- every item shown in the grid is accepted by the game for that slot (tags are expanded);
- the result count matches.
"""
import json
import re
import sys
import zipfile
from pathlib import Path

SRC = Path(__file__).resolve().parent.parent / "src/main/java/io/github/visserdavid/survivalassistant/advice/Recipes.java"


def expand_helpers(text):
    """Writes out calls to small recipe helpers such as pickaxe("minecraft:iron_pickaxe", IRON) as plain recipes."""
    helper = re.compile(r'private static Recipe (\w+)\(String (\w+), String (\w+)\) \{\s*return (new Recipe\(.*?\)\));\s*\}', re.S)
    for name, p1, p2, body in helper.findall(text):
        text = helper.sub("", text, count=1)

        def call(m, p1=p1, p2=p2, body=body):
            filled = re.sub(rf"\b{p2}\b", lambda _: m.group(2), body)
            return re.sub(rf"\b{p1}\b", lambda _: m.group(1), filled)

        text = re.sub(rf'\b{name}\(("[^"]+"|\w+), ("[^"]+"|\w+)\)', call, text)
    return text


def java_recipes(text):
    consts = dict(re.findall(r'String (\w+) = "([^"]+)";', text))
    text = expand_helpers(text)
    out = []
    for m in re.finditer(r'new Recipe\(("[^"]+"|\w+), (\d+), List\.of\(([^)]*)\),\s*Map\.of\(([^)]*)\)\)', text):
        result = m.group(1).strip('"') if m.group(1).startswith('"') else consts[m.group(1)]
        pattern = re.findall(r'"([^"]*)"', m.group(3))
        key = {}
        for ch, val in re.findall(r"'(.)', (\"[^\"]+\"|\w+)", m.group(4)):
            key[ch] = val.strip('"') if val.startswith('"') else consts[val]
        out.append((result, int(m.group(2)), pattern, key))
    return out


class Jar:
    def __init__(self, path):
        self.z = zipfile.ZipFile(path)

    def json(self, name):
        return json.loads(self.z.read(name))

    def tag(self, tag_id):
        ns, path = tag_id.split(":")
        items = set()
        for v in self.json(f"data/{ns}/tags/item/{path}.json")["values"]:
            v = v if isinstance(v, str) else v["id"]
            items |= self.tag(v[1:]) if v.startswith("#") else {v}
        return items

    def accepts(self, ingredient):
        if isinstance(ingredient, list):
            s = set()
            for i in ingredient:
                s |= self.accepts(i)
            return s
        return self.tag(ingredient[1:]) if ingredient.startswith("#") else {ingredient}


def main():
    jar = Jar(sys.argv[1])
    recipes = java_recipes(SRC.read_text(encoding="utf-8"))
    assert recipes, "no recipes parsed"
    errors = 0
    for result, count, pattern, key in recipes:
        name = result.split(":")[1]
        data = jar.json(f"data/minecraft/recipe/{name}.json")
        problems = []
        if data["result"].get("count", 1) != count:
            problems.append(f"count {count} != {data['result'].get('count', 1)}")
        if data["type"] == "minecraft:crafting_shaped":
            if data["pattern"] != pattern:
                problems.append(f"pattern {pattern} != {data['pattern']}")
            else:
                for ch in set("".join(pattern)) - {" "}:
                    if key[ch] not in jar.accepts(data["key"][ch]):
                        problems.append(f"{key[ch]} not accepted for '{ch}'")
        else:
            cells = [key[c] for row in pattern for c in row if c != " "]
            if len(cells) != len(data["ingredients"]):
                problems.append("ingredient count differs")
            for item, ing in zip(cells, data["ingredients"]):
                if item not in jar.accepts(ing):
                    problems.append(f"{item} not accepted")
        status = "OK " if not problems else "BAD"
        errors += bool(problems)
        print(f"{status} {name:18} {'; '.join(problems)}")
    print(f"{len(recipes)} recipes, {errors} problem(s)")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
