"""Builds the mod icons from the source artwork (docs/images/SurvivalAssistant.jpg).

The white background around the badge is made transparent by flood-filling near-white pixels from the image
border, so white details inside the badge are kept.

Usage (needs Pillow):
    uv run --with pillow python tools/make_icon.py
Outputs:
    src/main/resources/assets/survivalassistant/icon.png  (256x256, shown in Minecraft mod menus)
    docs/images/icon-512.png                                (512x512, for Modrinth/GitHub)
"""
from collections import deque

from PIL import Image, ImageFilter

SOURCE = "docs/images/SurvivalAssistant.jpg"
THRESHOLD = 228  # channels above this count as "background white" (JPEG noise included)

img = Image.open(SOURCE).convert("RGBA")
w, h = img.size
px = img.load()


def is_bg(x, y):
    r, g, b, _ = px[x, y]
    return r >= THRESHOLD and g >= THRESHOLD and b >= THRESHOLD


# Flood fill from every border pixel through near-white pixels.
mask = Image.new("L", (w, h), 255)  # 255 = keep, 0 = transparent
m = mask.load()
queue = deque()
for x in range(w):
    queue.extend([(x, 0), (x, h - 1)])
for y in range(h):
    queue.extend([(0, y), (w - 1, y)])
seen = set()
while queue:
    x, y = queue.popleft()
    if (x, y) in seen or not (0 <= x < w and 0 <= y < h):
        continue
    seen.add((x, y))
    if not is_bg(x, y):
        continue
    m[x, y] = 0
    queue.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])

# Soften the cut edge slightly so it doesn't look jagged after downscaling.
mask = mask.filter(ImageFilter.GaussianBlur(1.2))
img.putalpha(mask)

for size, path in ((256, "src/main/resources/assets/survivalassistant/icon.png"), (512, "docs/images/icon-512.png")):
    img.resize((size, size), Image.LANCZOS).save(path, optimize=True)
    print("wrote", path)
