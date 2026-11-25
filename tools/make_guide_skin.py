"""Draws the guide's built-in skins, "Keeper of the Ancients" and his route outfits, pixel by pixel.

An old guardian of the builders who left the ancient cities: a deep midnight hood and cloak, a long grey beard,
a verdigris copper clasp, a sculk-teal robe with faded runes, and soul-blue eyes that glow softly in the dark.
On a route he dresses for it, with the same face, beard and eyes:
- fighter: a chainmail hood and cloak over a crimson tabard with a gold sword;
- farmer: a straw hat over a moss-green hood, a linen smock and a wheat stalk;
- explorer: a weathered ochre cloak, a red scarf, a compass and a map on his back;
- researcher: an amethyst cloak with goggles pushed up, redstone lines on an indigo robe and a book.

Design rules (see the research in the 0.42 plan): one light source at the top front left (the entity's right side),
3 to 6 tones per material with hue shifting (shadows lean blue, highlights warm), no black outlines, no noise, detail
only in the focal points (face, beard, clasp). The base layer is fully opaque; the second layer adds depth (hood,
beard, cloak, sleeves, frayed hem).

Usage (needs Pillow):
    uv run --with pillow python tools/make_guide_skin.py [--preview DIR]
Outputs:
    src/main/resources/assets/survivalassistant/textures/entity/guide.png         the keeper (64x64)
    src/main/resources/assets/survivalassistant/textures/entity/guide_<route>.png  per route: fighter, farmer, ...
    src/main/resources/assets/survivalassistant/textures/entity/guide_eyes.png    only the eyes, for the glow layer
    with --preview: DIR/guide_preview.png, front, back and night views and the flat texture per skin
    with --docs: docs/images/guide-<name>.png per guide (also the classic one) and guides.png with all of them
"""
import argparse
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/survivalassistant/textures/entity"


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Ramps from dark to light, hue shifted: the dark ends lean blue/purple, the light ends warmer.
CLOAK = [hexc(c) for c in ("#0b0f19", "#151c2b", "#1e293b", "#29384e", "#3a4f67", "#526c86")]
ROBE = [hexc(c) for c in ("#0a1f26", "#0f3940", "#155058", "#1d6a70", "#2f8a88")]
SKIN = [hexc(c) for c in ("#6b4639", "#8c604b", "#ad7d62", "#c79a79")]
BEARD = [hexc(c) for c in ("#6f6c68", "#97938a", "#c2bdb0", "#e7e3d6")]
COPPER = [hexc(c) for c in ("#2a5d55", "#3d8a79", "#5eac94", "#8fd1b5")]
LEATHER = [hexc(c) for c in ("#291b17", "#3d2921", "#58392c", "#785034")]
PARCH = [hexc(c) for c in ("#8d7d57", "#b6a477", "#d7c89f")]
EYES = [hexc(c) for c in ("#2aa9b8", "#6af0e4")]
EYES_GLOW = [hexc(c) for c in ("#155a62", "#2f9e9a")]  # dimmer: the glow layer is additive and full-bright

GOLD = [hexc(c) for c in ("#6b4a12", "#9c7020", "#d0a338", "#f2d36b")]
RED = [hexc(c) for c in ("#5c1a1a", "#8c2a26", "#b8443a")]


def ramp(*colors):
	return [hexc(c) for c in colors]


def make_palette(o):
	"""Palette letters: the outfit decides the cloak (0-5, H), robe (q r t y u, trims) and metal (X x c C)."""
	cloak, robe, metal = o["cloak"], o["robe"], o["metal"]
	trim = o.get("trim", robe[3:5])
	return {
		**BASE_PALETTE,
		"H": cloak[0],
		**{str(i): cloak[i] for i in range(6)},
		"q": robe[0], "r": robe[1], "t": robe[2], "y": trim[0], "u": trim[1],
		"X": metal[0], "x": metal[1], "c": metal[2], "C": metal[3],
	}


BASE_PALETTE = {
	"K": SKIN[0], "k": SKIN[1], "s": SKIN[2], "S": SKIN[3],
	"n": BEARD[0], "v": BEARD[1], "V": BEARD[2], "W": BEARD[3],
	"p": hexc("#8a5a3c"), "R": RED[2], "Q": RED[0], "A": hexc("#b48ce0"), "a": hexc("#7a52b0"),
	"M": LEATHER[0], "m": LEATHER[1], "l": LEATHER[2], "L": LEATHER[3],
	"O": PARCH[0], "o": PARCH[1], "P": PARCH[2],
	"e": EYES[0], "E": EYES[1],
	" ": (0, 0, 0, 0),
}

# Boxes of the player model: texture origin (u, v) and size (width, height, depth).
BOXES = {
	"head": ((0, 0), (8, 8, 8)), "hat": ((32, 0), (8, 8, 8)),
	"body": ((16, 16), (8, 12, 4)), "jacket": ((16, 32), (8, 12, 4)),
	"rarm": ((40, 16), (4, 12, 4)), "rsleeve": ((40, 32), (4, 12, 4)),
	"larm": ((32, 48), (4, 12, 4)), "lsleeve": ((48, 48), (4, 12, 4)),
	"rleg": ((0, 16), (4, 12, 4)), "rpants": ((0, 32), (4, 12, 4)),
	"lleg": ((16, 48), (4, 12, 4)), "lpants": ((0, 48), (4, 12, 4)),
}
BASE = ("head", "body", "rarm", "larm", "rleg", "lleg")
FACES = ("top", "bottom", "right", "front", "left", "back")
# Light from the top front left: the entity's right side is lit, its left side and back are in shadow.
FACE_LIGHT = {"top": 1, "bottom": -2, "right": 0, "front": 0, "left": -1, "back": -1}


def face_rect(box, face):
	(u, v), (w, h, d) = BOXES[box]
	return {
		"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
		"right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
		"left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
	}[face]


class Skin:
	def __init__(self, palette):
		self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
		self.px = self.img.load()
		self.palette = palette

	def put(self, box, face, x, y, color):
		fx, fy, fw, fh = face_rect(box, face)
		assert 0 <= x < fw and 0 <= y < fh, (box, face, x, y)
		self.px[fx + x, fy + y] = color

	def fill(self, box, face, ramp, index, shade_bottom=True, rows=None):
		"""Flat colour with the face's light, one tone darker on the lowest quarter (ambient occlusion)."""
		_, _, fw, fh = face_rect(box, face)
		for y in range(fh) if rows is None else rows:
			i = index + FACE_LIGHT[face] - (1 if shade_bottom and fh >= 8 and y >= fh * 3 // 4 else 0)
			color = ramp[max(0, min(len(ramp) - 1, i))]
			for x in range(fw):
				self.put(box, face, x, y, color)

	def fill_box(self, box, ramp, index, faces=FACES, **kw):
		for face in faces:
			self.fill(box, face, ramp, index, **kw)

	def grid(self, box, face, rows):
		"""Hand-placed pixels: a palette letter per pixel, '.' keeps what is there."""
		_, _, fw, fh = face_rect(box, face)
		assert len(rows) == fh and all(len(r) == fw for r in rows), (box, face, rows)
		for y, row in enumerate(rows):
			for x, ch in enumerate(row):
				if ch != ".":
					self.put(box, face, x, y, self.palette[ch])


def paint(s, o):
	"""The keeper in the outfit o: the face, beard and eyes are the same in every outfit."""
	# --- head: an old face deep in the hood ---
	s.fill_box("head", o["cloak"], 1)
	s.fill("head", "bottom", BEARD, 2)
	s.grid("head", "front", [
		"HKkkkkKH",
		"HksSSskH",
		"HWVskVvH",  # bushy brows
		"HeEskEeH",  # soul-blue eyes (lit nose bridge between them)
		"HkKSKKkH",  # nose, wrinkles under the eyes
		"HWWVVVvH",  # moustache
		"HWVnnVvH",  # mouth in the beard
		"HVWVVvvH",
	])
	# hood: lit brim, open face, beard volume in front
	s.fill_box("hat", o["cloak"], 3, faces=("top", "right", "left", "back"))
	s.grid("hat", "front", [
		"54444333",
		"43    32",
		"3      2",
		"3      1",
		"3      1",
		"3      1",
		"3 WVVv 1",
		"2 VVvn 0",
	])
	s.grid("hat", "back", [
		"33333332",
		"32222222",
		"22222222",
		"22232222",  # a seam
		"22232222",
		"22222221",
		"21111111",
		"1y1yy1y1",  # rune trim on the hood's edge
	])

	# --- body: sculk-teal robe, beard, sash with copper buckle and a pouch ---
	s.fill_box("body", o["robe"], 2)
	s.grid("body", "front", [
		"tyVWWVyt",
		"tyvWVvyt",
		"tyyVVyyt",
		"ttyvvytt",
		"tttuyttt",
		"tttuyttt",
		"rttuytrr",
		"lLLcxlmm",  # sash with a verdigris buckle
		"mllxXlmM",
		"rtLLmtrr",  # pouch
		"rrlmMrrq",
		"qrrrrrqq",
	])
	for face in ("right", "left", "back"):
		s.fill("body", face, LEATHER, 2, rows=(7,))
		s.fill("body", face, LEATHER, 1, rows=(8,))

	# cloak over the shoulders, open at the front, a copper clasp chain over the beard
	s.fill_box("jacket", o["cloak"], 3, faces=("top", "right", "left"))
	s.grid("jacket", "front", [
		"443  322",
		"43C  c21",
		"43 xX 21",
		"43    21",
		"43    21",
		"43    21",
		"43    21",
		"43    21",
		"43    21",
		"32    11",
		"32    10",
		"3y    y0",
	])
	s.grid("jacket", "back", [
		"22222222",
		"22222222",
		"22yyyy22",  # a faded glyph of the ancient builders
		"2y2222y2",
		"2y2uu2y2",
		"2y2uu2y2",
		"2y2222y2",
		"2yy22yy2",
		"22222222",
		"21222212",
		"11211211",
		"1y1yy1y1",
	])
	s.grid("jacket", "right", [  # a rolled old scroll tucked under the cloak
		"....", "....", "....", "....", "....",
		".OO.", ".Po.", ".Po.", ".Po.", ".OO.",
		"....", "....",
	])

	# --- arms: aged hands under wide sleeves with copper cuffs ---
	for arm, sleeve in (("rarm", "rsleeve"), ("larm", "lsleeve")):
		s.fill_box(arm, o["robe"], 1, shade_bottom=False)
		for face in ("right", "front", "left", "back"):
			s.fill(arm, face, SKIN, 2, shade_bottom=False, rows=(10, 11))
		s.fill(arm, "bottom", SKIN, 2)
		s.grid(arm, "front", ["...."] * 10 + ["sSss", "kssk"])
		s.fill_box(sleeve, o["cloak"], 3, faces=("top", "right", "front", "left", "back"), shade_bottom=False, rows=None)
		for face in ("right", "front", "left", "back"):
			_, _, fw, _ = face_rect(sleeve, face)
			lit = 1 + FACE_LIGHT[face]
			s.grid(sleeve, face, ["." * fw] * 8 + [
				"".join("cx"[(x + (face == "back")) % 2] if lit > 0 else "xX"[(x + 1) % 2] for x in range(fw)),
				"".join("p" if x % 3 == 1 else ("x" if lit > 0 else "X") for x in range(fw)),
				" " * fw, " " * fw])
		s.grid(sleeve, "front", ["4333"] + ["." * 4] * 11)

	# --- legs: the robe goes down to worn boots; the frayed hem is on the second layer ---
	for leg, pants in (("rleg", "rpants"), ("lleg", "lpants")):
		s.fill_box(leg, o["robe"], 1, faces=("top", "right", "front", "left", "back"), shade_bottom=False)
		for face in ("right", "front", "left", "back"):
			_, _, fw, _ = face_rect(leg, face)
			s.fill(leg, face, LEATHER, 2, shade_bottom=False, rows=(9, 10))
			s.fill(leg, face, LEATHER, 1, shade_bottom=False, rows=(11,))
			s.grid(leg, face, ["." * fw] * 6 + ["".join("u" if x % 2 == 0 else "y" for x in range(fw))] + ["." * fw] * 5)
		s.fill(leg, "bottom", LEATHER, 0)
		s.grid(leg, "front", ["." * 4] * 9 + ["lLLl", "mlLm", "MmmM"])
		for face in ("right", "front", "left", "back"):
			_, _, fw, _ = face_rect(pants, face)
			dark = FACE_LIGHT[face] < 0
			s.grid(pants, face, [" " * fw] * 7 + [
				"".join("q" if dark else "r" for _ in range(fw)),
				"".join(("q" if dark else "r") if (x + (leg == "lleg")) % 2 == 0 else " " for x in range(fw)),
				" " * fw, " " * fw, " " * fw])
	o["details"](s)


def no_details(s):
	pass


def fighter(s):
	s.grid("body", "front", [  # a gold sword on the tabard, under the beard
		"........", "........", "........", "........",
		"..xCcx..",
		"...Cc...",
		"...Cc...",
		"........", "........", "........", "........", "........",
	])
	s.grid("hat", "front", ["5CcCCc33"] + ["." * 8] * 7)  # a gold band on the mail hood


def farmer(s):
	straw = ["CcCCcCCc", "cCCcCCcC"]
	s.grid("hat", "front", ["CCcCCcCc", "pm    mp"] + ["." * 8] * 6)
	for face in ("right", "left", "back"):
		s.grid("hat", face, straw + ["pmpmpmpm" if face == "back" else "mpmpmpmp"] + ["." * 8] * 5)
	s.grid("hat", "top", ["cCCcCCcC", "CcCCcCCc", "CCcCCcCC", "cCCCCcCc", "CcCcCCcC", "CCcCcCCc", "cCCcCCcC", "CcCCcCCc"])
	s.grid("jacket", "right", [  # a wheat stalk instead of the scroll
		"....", "..C.", ".cC.", "..c.", ".Cc.", "..c.", "..x.", "..x.", "..x.", "....", "....", "....",
	])


def explorer(s):
	s.grid("hat", "front", ["." * 8] * 7 + ["RQ.....Q"[:2] + "....." + "Q"])  # the scarf under the hood
	s.grid("jacket", "front", ["RRQ  QQR"] + ["." * 8] * 11)
	s.grid("body", "front", ["........"] * 9 + ["rtxCxtrr", "rrxRxrrq", "........"])  # a compass on the sash
	s.grid("jacket", "back", [  # a map strapped to his back
		"........", "........",
		".PPPPPP.",
		".PoPPRP.",
		".PPoPPP.",
		".oPPoPP.",
		".PoPPPo.",
		".PPPPPP.",
		"........", "........", "........", "........",
	])


def researcher(s):
	s.grid("hat", "front", [".......3", "4xAxxAx2"] + ["." * 8] * 6)  # goggles pushed up on the hood
	s.grid("jacket", "right", [  # a book instead of the scroll
		"....", "....", "....", "....",
		".pp.", ".pP.", ".pP.", ".pP.", ".pp.",
		"....", "....", "....",
	])


OUTFITS = {
	"default": {"cloak": CLOAK, "robe": ROBE, "metal": COPPER, "details": no_details},
	"fighter": {
		"cloak": ramp("#16181f", "#23262f", "#343842", "#4b505b", "#6a707a", "#8f959c"),
		"robe": ramp("#2a0b12", "#4a1220", "#6b1a2a", "#8c2734", "#b0413f"),
		"metal": GOLD, "details": fighter,
	},
	"farmer": {
		"cloak": ramp("#141d12", "#1f2b19", "#2c3c22", "#3d5230", "#566e40", "#748c54"),
		"robe": ramp("#2b2016", "#45331f", "#5f4a2c", "#7d6540", "#a08758"),
		"metal": ramp("#7a5a1c", "#a88430", "#d4b04a", "#f0d77a"), "details": farmer,
	},
	"explorer": {
		"cloak": ramp("#1f140d", "#33221a", "#4d3424", "#6b4a30", "#8d6641", "#b08a58"),
		"robe": ramp("#0c1d1a", "#13302a", "#1c4538", "#285c47", "#3a7a5a"),
		"metal": ramp("#5a2e1c", "#8a4a2c", "#c06b3e", "#e8996a"), "details": explorer,
	},
	"researcher": {
		"cloak": ramp("#160f22", "#231734", "#33214a", "#472f63", "#614383", "#8460a8"),
		"robe": ramp("#0d0f24", "#161a3a", "#202650", "#2d3668", "#414d8a"),
		"trim": ramp("#a3201c", "#e0402e"),  # redstone lines
		"metal": GOLD, "details": researcher,
	},
}


def eyes_texture(skin):
	"""Only the eye pixels, in dimmer colours: the eyes render type adds them to the scene at full brightness."""
	img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
	fx, fy, _, _ = face_rect("head", "front")
	for x in range(8):
		color = skin.px[fx + x, fy + 3]
		if color in EYES:
			img.putpixel((fx + x, fy + 3), EYES_GLOW[EYES.index(color)])
	return img


def check(img):
	"""Every base face is opaque and nothing is painted outside the template."""
	used = set()
	for box in BOXES:
		for face in FACES:
			fx, fy, fw, fh = face_rect(box, face)
			for y in range(fy, fy + fh):
				for x in range(fx, fx + fw):
					used.add((x, y))
					if box in BASE and img.getpixel((x, y))[3] != 255:
						raise SystemExit(f"{box} {face}: transparent base pixel at {x},{y}")
	stray = [(x, y) for y in range(64) for x in range(64) if (x, y) not in used and img.getpixel((x, y))[3] != 0]
	if stray:
		raise SystemExit(f"pixels outside the template: {stray[:5]}")


# --- preview: a small software renderer of the player model ---

# Model space in texture pixels: x to the viewer's right, y up from the feet, z toward the viewer.
PARTS = {
	"head": ((-4, 24, -4), (4, 32, 4), "hat", 0.5),
	"body": ((-4, 12, -2), (4, 24, 2), "jacket", 0.25),
	"rarm": ((-8, 12, -2), (-4, 24, 2), "rsleeve", 0.25),
	"larm": ((4, 12, -2), (8, 24, 2), "lsleeve", 0.25),
	"rleg": ((-4, 0, -2), (0, 12, 2), "rpants", 0.25),
	"lleg": ((0, 0, -2), (4, 12, 2), "lpants", 0.25),
}


def box_faces(lo, hi):
	(x0, y0, z0), (x1, y1, z1) = lo, hi
	# texture origin corner, end of the u axis, end of the v axis, outward normal
	return {
		"front": ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1), (0, 0, 1)),
		"back": ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0), (0, 0, -1)),
		"right": ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0), (-1, 0, 0)),
		"left": ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1), (1, 0, 0)),
		"top": ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1), (0, 1, 0)),
	}


def render(img, yaw, scale=12, size=(34, 44)):
	cos, sin = math.cos(yaw), math.sin(yaw)

	def rot(p):
		x, y, z = p
		return (x * cos + z * sin, y, -x * sin + z * cos)

	def proj(p):
		x, y, z = rot(p)
		near = (z - x) / math.sqrt(2)
		sx = (x + z) / math.sqrt(2)
		return ((sx + size[0] / 2) * scale, (size[1] - 4 - y * 0.85 + near * 0.45) * scale)

	view = (-1 / math.sqrt(2), 0.45, 1 / math.sqrt(2))
	faces = []
	for base, (lo, hi, overlay, grow) in PARTS.items():
		for box, g in ((base, 0), (overlay, grow)):
			l = tuple(a - g for a in lo)
			h = tuple(a + g for a in hi)
			for name, (o, ue, ve, n) in box_faces(l, h).items():
				rn = rot(n)
				if sum(a * b for a, b in zip(rn, view)) <= 0:
					continue
				centre = rot(tuple((a + b) / 2 for a, b in zip(ue, ve)))
				depth = (centre[2] - centre[0]) / math.sqrt(2) + centre[1] * 0.3
				faces.append((depth, g, box, name, o, ue, ve))
	canvas = Image.new("RGBA", (size[0] * scale, size[1] * scale), (0, 0, 0, 0))
	for depth, g, box, name, o, ue, ve in sorted(faces, key=lambda f: (f[0], f[1])):
		fx, fy, fw, fh = face_rect(box, name)
		tex = img.crop((fx, fy, fx + fw, fy + fh))
		p0, pu, pv = proj(o), proj(ue), proj(ve)
		ax, ay = (pu[0] - p0[0]) / fw, (pu[1] - p0[1]) / fw
		bx, by = (pv[0] - p0[0]) / fh, (pv[1] - p0[1]) / fh
		det = ax * by - ay * bx
		if abs(det) < 1e-9:
			continue
		# inverse affine: output (X, Y) -> texture (u, v)
		ia, ib = by / det, -bx / det
		ic, id_ = -ay / det, ax / det
		coeffs = (ia, ib, -(ia * p0[0] + ib * p0[1]), ic, id_, -(ic * p0[0] + id_ * p0[1]))
		layer = tex.transform(canvas.size, Image.AFFINE, coeffs, resample=Image.NEAREST, fillcolor=(0, 0, 0, 0))
		canvas.alpha_composite(layer)
	return canvas


def night_view(img, eyes):
	"""The skin dimmed as at night, with the glowing eyes added at full brightness."""
	night = img.copy()
	npx, eye_px = night.load(), eyes.load()
	for y in range(64):
		for x in range(64):
			r, g, b, a = npx[x, y]
			r, g, b = r // 5, g // 5, b // 4
			if eye_px[x, y][3]:
				er, eg, eb, _ = eye_px[x, y]
				r, g, b = min(255, r + er), min(255, g + eg), min(255, b + eb)
			npx[x, y] = (r, g, b, a)
	return night


def preview(skins, eyes, out_dir):
	"""One row per skin: front, back, night, and the flat texture."""
	rows = []
	for img in skins.values():
		rows.append([render(img, 0), render(img, math.pi), render(night_view(img, eyes), 0),
				img.resize((64 * 5, 64 * 5), Image.NEAREST)])
	w = sum(v.width for v in rows[0]) + 50
	row_h = max(v.height for v in rows[0]) + 10
	sheet = Image.new("RGBA", (w, row_h * len(rows) + 10), (196, 204, 196, 255))
	for r, views in enumerate(rows):
		x, y = 10, 10 + r * row_h
		for i, v in enumerate(views):
			if i == 2:
				ImageDraw.Draw(sheet).rectangle((x, y, x + v.width, y + v.height), fill=(14, 16, 26, 255))
			sheet.alpha_composite(v, (x, y))
			x += v.width + 10
	out_dir.mkdir(parents=True, exist_ok=True)
	path = out_dir / "guide_preview.png"
	sheet.save(path)
	return path


DOCS = ROOT / "docs/images"


def docs_images(skins):
	"""A picture of every guide for the documentation (3/4 front view, transparent), and all of them in a row."""
	classic = Image.open(OUT / "guide_classic.png").convert("RGBA")
	pictures = {**skins, "classic": classic}
	renders = {}
	for name, img in pictures.items():
		r = render(img, -0.35, scale=8)
		r = r.crop(r.getbbox())
		r.save(DOCS / f"guide-{name}.png", optimize=True)
		renders[name] = r
	gap = 24
	w = sum(r.width for r in renders.values()) + gap * (len(renders) - 1)
	h = max(r.height for r in renders.values())
	row = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	x = 0
	for r in renders.values():
		row.alpha_composite(r, (x, h - r.height))
		x += r.width + gap
	row.save(DOCS / "guides.png", optimize=True)
	return [f"guide-{n}.png" for n in renders] + ["guides.png"]


def main():
	parser = argparse.ArgumentParser(description="Draw the guide's built-in skins and their glowing eyes.")
	parser.add_argument("--preview", type=Path, help="also write guide_preview.png into this folder")
	parser.add_argument("--docs", action="store_true", help="also write the pictures of every guide in docs/images")
	args = parser.parse_args()
	OUT.mkdir(parents=True, exist_ok=True)
	skins = {}
	eyes = None
	for name, outfit in OUTFITS.items():
		skin = Skin(make_palette(outfit))
		paint(skin, outfit)
		check(skin.img)
		file = "guide.png" if name == "default" else f"guide_{name}.png"
		skin.img.save(OUT / file)
		skins[name] = skin.img
		# every outfit has the same eyes, so one glow texture serves them all
		e = eyes_texture(skin)
		assert eyes is None or e.tobytes() == eyes.tobytes(), name
		eyes = e
		print(f"wrote {file}")
	eyes.save(OUT / "guide_eyes.png")
	print("wrote guide_eyes.png")
	if args.preview:
		print(f"preview: {preview(skins, eyes, args.preview)}")
	if args.docs:
		print("wrote docs/images/" + ", ".join(docs_images(skins)))


if __name__ == "__main__":
	main()
