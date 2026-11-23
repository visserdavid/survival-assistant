"""Finds the Minecraft jar (with the game's data files) that Loom downloaded, for the tools that read game data."""
import glob
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PATTERNS = (
    # Loom 1.19 keeps the game in the Gradle user cache ...
    os.path.join(os.path.expanduser("~"), ".gradle", "caches", "fabric-loom", "minecraftMaven", "net", "minecraft",
                 "minecraft-common-*", "*", "minecraft-common-*.jar"),
    # ... older Loom versions kept it in the project
    str(ROOT / ".gradle" / "loom-cache" / "minecraftMaven" / "net" / "minecraft" / "minecraft-common-*" / "*"
        / "minecraft-common-*.jar"),
)


def minecraft_version():
    """minecraft_version from gradle.properties."""
    text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    return re.search(r"^minecraft_version=(.+)$", text, re.M).group(1).strip()


def find_jar(explicit=None):
    """The jar given on the command line, or the one for this project's Minecraft version; exits when none is found."""
    if explicit:
        return Path(explicit)
    version = minecraft_version()
    jars = [Path(j) for pattern in PATTERNS for j in glob.glob(pattern) if not j.endswith("-sources.jar")]
    matching = [j for j in jars if j.parent.name == version]
    if not matching:
        sys.exit(f"No Minecraft {version} jar found in the Loom cache; run ./gradlew build first, or pass --jar.")
    return sorted(matching)[-1]
