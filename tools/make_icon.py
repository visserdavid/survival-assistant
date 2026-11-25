"""Builds the mod icons from the guide's default skin: the hooded Keeper of the Ancients on a copper medallion.

Everything is drawn on a 128x128 pixel grid and scaled up without smoothing, so the icon stays crisp pixel art
like the skin itself. The bust is rendered in 3D from guide.png by the renderer in make_guide_skin.py, so the
icon follows the skin when it changes (run make_guide_skin.py first).

Usage (needs Pillow):
    uv run --with pillow python tools/make_icon.py
Outputs:
    src/main/resources/assets/survivalassistant/icon.png  (256x256, shown in Minecraft mod menus)
    docs/images/icon-512.png                                (512x512, for Modrinth/GitHub)
"""
import argparse
import math
from pathlib import Path

from PIL import Image

from make_guide_skin import COPPER, CLOAK, ROBE, OUT, hexc, render

ROOT = Path(__file__).resolve().parent.parent
GRID = 128
CENTRE = (GRID - 1) / 2
RIM_OUT, RIM_IN = 62, 55  # radii of the copper rim
TEXEL = 5                 # icon pixels per skin texel for the bust
YAW = -0.5                # turn the keeper a little toward the viewer


def medallion():
	"""A verdigris copper rim, lit from the top left, around a dark disc that fades to black at the edge."""
	img = Image.new("RGBA", (GRID, GRID), (0, 0, 0, 0))
	px = img.load()
	for y in range(GRID):
		for x in range(GRID):
			dx, dy = x - CENTRE, y - CENTRE
			r = math.hypot(dx, dy)
			light = (-dx - dy) / (math.sqrt(2) * max(r, 1))  # 1 at the top left, -1 at the bottom right
			if r <= RIM_IN:
				# the inner disc: midnight cloak blue, a faint teal glow in the middle
				t = r / RIM_IN
				ramp = CLOAK[1:4]
				px[x, y] = ramp[2] if t < 0.45 else ramp[1] if t < 0.8 else ramp[0]
			elif r <= RIM_OUT:
				band = (r - RIM_IN) / (RIM_OUT - RIM_IN)
				i = 1 + round(light * 1.4) - (1 if band > 0.7 else 0) + (1 if band < 0.15 else 0)
				px[x, y] = COPPER[max(0, min(3, i))]
	# a faded glyph of the ancient builders behind the keeper, like the one on his cloak
	glyph = ["..yyyyyy..",
			 ".y......y.",
			 "y..yyyy..y",
			 "y..y..y..y",
			 "y..y..y..y",
			 "y..yyyy..y",
			 ".y......y.",
			 "..yy..yy.."]
	g = 6
	gx0, gy0 = round(CENTRE - len(glyph[0]) * g / 2), 12
	for row, line in enumerate(glyph):
		for col, ch in enumerate(line):
			if ch == "y":
				for yy in range(gy0 + row * g, gy0 + row * g + g):
					for xx in range(gx0 + col * g, gx0 + col * g + g):
						px[xx, yy] = ROBE[2]
	return img


def bust(skin):
	"""A full 3D render of the skin, cropped to its outline; the medallion cuts it off below the chest."""
	full = render(skin, YAW, scale=TEXEL)
	return full.crop(full.getbbox())


def compose(skin):
	icon = medallion()
	b = bust(skin)
	x0 = round(CENTRE - b.width / 2)
	y0 = 17
	px, bp = icon.load(), b.load()
	for y in range(b.height):
		for x in range(b.width):
			c = bp[x, y]
			X, Y = x0 + x, y0 + y
			# opaque pixels only (no soft edges in pixel art), and only inside the rim
			if c[3] >= 128 and 0 <= X < GRID and 0 <= Y < GRID and math.hypot(X - CENTRE, Y - CENTRE) <= RIM_IN:
				px[X, Y] = c[:3] + (255,)
	return icon


def main():
	argparse.ArgumentParser(description="Build the mod icons from the guide's default skin.").parse_args()
	icon = compose(Image.open(OUT / "guide.png").convert("RGBA"))
	for size, path in ((256, ROOT / "src/main/resources/assets/survivalassistant/icon.png"),
			(512, ROOT / "docs/images/icon-512.png")):
		icon.resize((size, size), Image.NEAREST).save(path, optimize=True)
		print("wrote", path)


if __name__ == "__main__":
	main()
