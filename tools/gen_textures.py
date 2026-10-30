#!/usr/bin/env python3
"""Generates EverFest's original pixel-art textures.

Every texture is drawn procedurally from palettes and pixel maps defined here, so all art is original
and can be regenerated deterministically:  python3 tools/gen_textures.py
"""
import os
import random
import sys
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", "holidayseasons", "textures")

TEXTURES = {}


def _h(text):
    """Deterministic string hash (Python's hash() is randomized per process)."""
    return zlib.crc32(text.encode())


def texture(path):
    def deco(fn):
        TEXTURES[path] = fn
        return fn
    return deco


def hexc(s, a=255):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def from_map(rows, palette, size=None):
    """rows: list of strings; each char indexes palette ('.' = transparent)."""
    h = len(rows)
    w = max(len(r) for r in rows)
    img = Image.new("RGBA", size or (w, h), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "." or ch == " ":
                continue
            px[x, y] = palette[ch]
    return img


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])


def noise_fill(img, box, base, spread, seed):
    rnd = random.Random(seed)
    px = img.load()
    x0, y0, x1, y1 = box
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = shade(base, 1.0 + rnd.uniform(-spread, spread))


# ----------------------------------------------------------------------------------------------- calendar

CAL_PALETTE = {
    "P": hexc("f4ebd0"),  # paper
    "p": hexc("e2d4ad"),  # paper shade
    "R": hexc("b8322a"),  # red header
    "r": hexc("8e231d"),  # header shade
    "K": hexc("3b2a17"),  # ink
    "k": hexc("9c8865"),  # faint ink
    "M": hexc("8c8c8c"),  # metal ring
    "W": hexc("d9d9d9"),  # ring highlight
    "H": hexc("d9822b"),  # holiday mark
    "B": hexc("6b4f2a"),  # backing board
}

CAL_ROWS = [
    "................",
    "................",
    "...B.M.M.M.M.B..",
    "...BRWRWRWRWRB..",
    "...BRRRRRRRRRB..",
    "...BrrrrrrrrrB..",
    "...BPkPkPkPkPB..",
    "...BPPPPPPPPPB..",
    "...BKPKPKPKPHB..",
    "...BPPPPPPPPPB..",
    "...BKPKPHPKPKB..",
    "...BPPPPPPPPPB..",
    "...BKPKPKPKPpB..",
    "...BpppppppppB..",
    "...BBBBBBBBBBB..",
    "................",
]


@texture("block/wall_calendar.png")
def wall_calendar_block():
    # Artwork spans x 3..13, y 2..14 to match the block model's front face UVs.
    rows = [r[:3] + r[3:14] + r[14:] for r in CAL_ROWS]
    img = from_map(rows, CAL_PALETTE, (16, 16))
    return img


@texture("item/wall_calendar.png")
def wall_calendar_item():
    rows = [
        "................",
        "....M..M..M..M..",
        "..BRWRRWRRWRRWB.",
        "..BRRRRRRRRRRRB.",
        "..BrrrrrrrrrrrB.",
        "..BPkPPkPPkPPkB.",
        "..BPPPPPPPPPPPB.",
        "..BKPPKPPKPPHPB.",
        "..BPPPPPPPPPPPB.",
        "..BKPPHPPKPPKPB.",
        "..BPPPPPPPPPPPB.",
        "..BKPPKPPKPPKPB.",
        "..BPPPPPPPPPPpB.",
        "..BpppppppppppB.",
        "..BBBBBBBBBBBBB.",
        "................",
    ]
    return from_map(rows, CAL_PALETTE)


@texture("../../../everfest_logo.png")
def logo():
    # 128x128 logo: big calendar page with a pumpkin, snowflake, egg and heart.
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    small = wall_calendar_item()
    big = small.resize((128, 128), Image.NEAREST)
    img.alpha_composite(big)
    return img



# ----------------------------------------------------------------------------------------------- shared helpers

def canvas(w=16, h=16):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def rect(img, x0, y0, x1, y1, color):
    px = img.load()
    for y in range(max(0, y0), min(img.height, y1)):
        for x in range(max(0, x0), min(img.width, x1)):
            px[x, y] = color


def dots(img, box, colors, count, seed):
    rnd = random.Random(seed)
    px = img.load()
    x0, y0, x1, y1 = box
    for _ in range(count):
        px[rnd.randrange(x0, x1), rnd.randrange(y0, y1)] = rnd.choice(colors)


ORANGE = hexc("e07b1a")
ORANGE_D = hexc("b45a0e")
ORANGE_L = hexc("f29a3a")
GLOW = hexc("ffd84a")
GLOW_HOT = hexc("fff2a8")
DARK = hexc("2a1406")
WHITE = hexc("f5f5f0")
BONE = hexc("e8e2cc")
PURPLE = hexc("5b2a86")
PURPLE_D = hexc("3a1858")
BLACK = hexc("1c1a22")
STRAW = hexc("d9b44a")
STRAW_D = hexc("a8842c")
BROWN = hexc("6b4f2a")
GREEN = hexc("3a6b1f")


def ribbed(img, box, base, dark, light, seed):
    x0, y0, x1, y1 = box
    noise_fill(img, box, base, 0.06, seed)
    px = img.load()
    for x in range(x0, x1):
        if (x - x0) % 4 == 0:
            for y in range(y0, y1):
                px[x, y] = shade(dark, 1.0)
        elif (x - x0) % 4 == 2:
            for y in range(y0, y1):
                if (y + x) % 3 == 0:
                    px[x, y] = light


# ----------------------------------------------------------------------------------------------- candy and items

@texture("item/candy_corn.png")
def candy_corn():
    rows = [
        "................",
        ".......WW.......",
        "......WWWW......",
        "......WWWW......",
        ".....OOOOOO.....",
        ".....OOOOOO.....",
        "....OOOOOOOO....",
        "....OOOOOOOO....",
        "...YYYYYYYYYY...",
        "...YYYYYYYYYY...",
        "..YYYYYYYYYYYY..",
        "..yyyyyyyyyyyy..",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"W": WHITE, "O": ORANGE_L, "Y": hexc("f6c843"), "y": hexc("d8a52a")})


@texture("item/caramel_apple.png")
def caramel_apple():
    rows = [
        ".......B........",
        ".......B........",
        ".......B........",
        ".....CCBCC......",
        "....CCCCCCC.....",
        "...CCcCCCcCC....",
        "...CcCCcCCcC....",
        "...RCRRCRRCR....",
        "...RRRRRRRRR....",
        "...RRRRRRRrR....",
        "...RRRRRRrrR....",
        "....RRRRRRR.....",
        ".....RR.RR......",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"B": hexc("c8a070"), "C": hexc("c27a2c"), "c": hexc("9a5a1c"), "R": hexc("c0282a"), "r": hexc("8e1c1e")})


@texture("item/chocolate_bar.png")
def chocolate_bar():
    rows = [
        "................",
        "................",
        "...SSSSSSSSSS...",
        "...SRRRRRRRRS...",
        "...SRRRRRRRRS...",
        "...SSSSSSSSSS...",
        "...cCcCcCcCcC...",
        "...CCCCCCCCCC...",
        "...cCcCcCcCcC...",
        "...CCCCCCCCCC...",
        "...cCcCcCcCcC...",
        "...CCCCCCCCCC...",
        "...dddddddddd...",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"S": hexc("c8c8d0"), "R": hexc("b8322a"), "C": hexc("6a3d1e"), "c": hexc("82512a"), "d": hexc("4a2810")})


@texture("item/lollipop.png")
def lollipop():
    img = canvas()
    px = img.load()
    cx, cy, r = 7.5, 5.5, 5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                import math
                ang = math.atan2(y - cy, x - cx)
                band = int((ang / (2 * math.pi) * 4 + d / 2.0)) % 2
                px[x, y] = ORANGE_L if band else PURPLE
    rect(img, 7, 11, 9, 16, hexc("f0f0e8"))
    return img


@texture("item/gummy_bat.png")
def gummy_bat():
    rows = [
        "................",
        "................",
        "................",
        "......P..P......",
        "......PPPP......",
        "P....PPPPPP....P",
        "PP..PPpPPpPP..PP",
        "PPPPPPPPPPPPPPPP",
        ".PPPPPPPPPPPPPP.",
        "..PPP.PPPP.PPP..",
        "...P...PP...P...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"P": hexc("8a3cc8"), "p": hexc("f0e050")})


@texture("item/candy_bag.png")
def candy_bag():
    rows = [
        "................",
        ".....K....K.....",
        "......K..K......",
        ".....OOOOOO.....",
        "....OOOOOOOO....",
        "...OOOOOOOOOO...",
        "..OOODDOODDOOO..",
        "..OOODDOODDOOO..",
        "..OOOOOOOOOOOO..",
        "..OODOOOOOODOO..",
        "..OOODDDDDDOOO..",
        "..OOOOOOOOOOOO..",
        "...oooooooooo...",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"O": ORANGE, "o": ORANGE_D, "D": DARK, "K": BLACK})


@texture("item/carving_knife.png")
def carving_knife():
    rows = [
        "................",
        "............SS..",
        "...........SSs..",
        "..........SSs...",
        ".........SSs....",
        "........SSs.....",
        ".......SSs......",
        "......SSs.......",
        ".....GSs........",
        "....BGG.........",
        "...BBb..........",
        "..BBb...........",
        ".BBb............",
        ".bb.............",
        "................",
        "................",
    ]
    return from_map(rows, {"S": hexc("d8dce4"), "s": hexc("8c94a0"), "G": hexc("c8a040"), "B": hexc("7a4a22"), "b": hexc("4e2e14")})


@texture("item/ectoplasm.png")
def ectoplasm():
    rows = [
        "................",
        "................",
        "......gg........",
        ".....gGGg.......",
        "....gGWWGg......",
        "...gGWWWWGg.....",
        "...gGWWWGGg.....",
        "..gGGWWGGGGg....",
        "..gGGGGGGGGgg...",
        "...gGGGGGGGGg...",
        "....ggGGGGgg....",
        "......gggg......",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"g": hexc("4fae6a", 200), "G": hexc("8ff0a8", 230), "W": hexc("e8fff0")})


# ----------------------------------------------------------------------------------------------- pumpkin faces

FACES = {
    "grin": [
        "................",
        "................",
        "................",
        "...XX......XX...",
        "..XXXX....XXXX..",
        "..XXXX....XXXX..",
        "................",
        ".......XX.......",
        "................",
        "..X..........X..",
        "..XX.X.XX.X.XX..",
        "...XXXXXXXXXX...",
        "....XXX..XXX....",
        "................",
        "................",
        "................",
    ],
    "scary": [
        "................",
        "................",
        "..X..........X..",
        "..XXX......XXX..",
        "...XXX....XXX...",
        "....XX....XX....",
        "................",
        "......X..X......",
        "................",
        "..XXXXXXXXXXXX..",
        "..X.X.XXXX.X.X..",
        "..XXXXXXXXXXXX..",
        "...X.X.XX.X.X...",
        "....XXXXXXXX....",
        "................",
        "................",
    ],
    "silly": [
        "................",
        "................",
        "..XXX......XX...",
        "..X.X.....XXXX..",
        "..XXX.....XXXX..",
        "...........XX...",
        "................",
        ".......X........",
        "................",
        "...XXXXXXXXXX...",
        "....XXXXXXXX....",
        ".....XXOOXX.....",
        "......XOOX......",
        "................",
        "................",
        "................",
    ],
    "cat": [
        "................",
        "...X........X...",
        "...XX......XX...",
        "...XXX....XXX...",
        "................",
        "...XXX....XXX...",
        "...X.X....X.X...",
        "...XXX....XXX...",
        "................",
        ".......XX.......",
        "XX.....XX.....XX",
        "..XX..X..X..XX..",
        "......X..X......",
        ".......XX.......",
        "................",
        "................",
    ],
    "ghost": [
        "................",
        "................",
        "................",
        "...XXX....XXX...",
        "..XXXXX..XXXXX..",
        "..XXXXX..XXXXX..",
        "...XXX....XXX...",
        "................",
        "................",
        "......XXXX......",
        ".....XXXXXX.....",
        ".....XXXXXX.....",
        "......XXXX......",
        "................",
        "................",
        "................",
    ],
}


def pumpkin_front(face, lit):
    img = canvas()
    ribbed(img, (0, 0, 16, 16), ORANGE, ORANGE_D, ORANGE_L, _h(face) & 0xFFFF)
    px = img.load()
    for y, row in enumerate(FACES[face]):
        for x, ch in enumerate(row):
            if ch == "X":
                px[x, y] = (GLOW_HOT if (x + y) % 3 == 0 else GLOW) if lit else DARK
            elif ch == "O":
                px[x, y] = hexc("e04040") if not lit else hexc("ff8060")
    return img


for _face in FACES:
    TEXTURES["block/carved_pumpkin_" + _face + ".png"] = (lambda f: (lambda: pumpkin_front(f, False)))(_face)
    TEXTURES["block/jack_o_lantern_" + _face + ".png"] = (lambda f: (lambda: pumpkin_front(f, True)))(_face)


@texture("block/pumpkin_side_ribbed.png")
def pumpkin_side_ribbed():
    img = canvas()
    ribbed(img, (0, 0, 16, 16), ORANGE, ORANGE_D, ORANGE_L, 77)
    return img


@texture("block/pumpkin_top_stem.png")
def pumpkin_top_stem():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), ORANGE, 0.06, 78)
    for i in range(16):
        img.putpixel((i, 7), ORANGE_D)
        img.putpixel((7, i), ORANGE_D)
    rect(img, 6, 6, 10, 10, hexc("4a6a22"))
    rect(img, 7, 7, 9, 9, hexc("6a8a32"))
    return img


# ----------------------------------------------------------------------------------------------- blocks

@texture("block/candy_bowl_side.png")
def candy_bowl_side():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("e8e0d0"), 0.04, 5)
    rect(img, 0, 3, 16, 6, ORANGE)
    rect(img, 0, 8, 16, 9, PURPLE)
    for x in range(0, 16, 4):
        img.putpixel((x + 1, 4), BLACK)
    return img


@texture("block/candy_bowl_inside.png")
def candy_bowl_inside():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("d8d0c0"), 0.05, 6)
    return img


@texture("block/candy_pile.png")
def candy_pile():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("f6c843"), 0.1, 7)
    dots(img, (0, 0, 16, 16), [ORANGE_L, WHITE, hexc("c0282a"), PURPLE, hexc("6a3d1e"), hexc("8a3cc8"), GLOW], 90, 8)
    return img


@texture("block/doorbell.png")
def doorbell():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("c8a040"), 0.08, 9)
    rect(img, 5, 5, 11, 11, hexc("8a6a20"))
    rect(img, 6, 6, 10, 10, hexc("f0e0a0"))
    rect(img, 7, 7, 9, 9, hexc("fff8d8"))
    return img


@texture("block/hearth_stone.png")
def hearth_stone():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("8a4a3a"), 0.08, 10)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 4 == 3 or (x + (4 if (y // 4) % 2 else 0)) % 8 == 7:
                px[x, y] = hexc("5a2e22")
    return img


@texture("block/hearth_front.png")
def hearth_front():
    img = hearth_stone()
    rect(img, 3, 5, 13, 16, hexc("1a0c08"))
    flame = [(6, 9), (7, 8), (8, 8), (9, 9), (5, 11), (6, 10), (7, 10), (8, 10), (9, 10), (10, 11), (6, 12), (7, 12), (8, 12), (9, 12)]
    for x, y in flame:
        img.putpixel((x, y), GLOW if y < 11 else hexc("ff8a20"))
    rect(img, 4, 13, 12, 15, hexc("7a3a12"))
    rect(img, 5, 14, 11, 15, hexc("ff5a10"))
    return img


@texture("block/hearth_top.png")
def hearth_top():
    img = hearth_stone()
    rect(img, 3, 3, 13, 13, hexc("2a140c"))
    dots(img, (3, 3, 13, 13), [hexc("ff5a10"), hexc("ff8a20"), GLOW, hexc("7a3a12")], 30, 11)
    return img


@texture("block/tombstone.png")
def tombstone():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("8c8c90"), 0.08, 12)
    rows = ["R.I.P"]
    px = img.load()
    # Engraved cross.
    rect(img, 7, 2, 9, 9, hexc("5a5a60"))
    rect(img, 5, 4, 11, 6, hexc("5a5a60"))
    for x in range(3, 13, 2):
        px[x, 11] = hexc("5a5a60")
    dots(img, (0, 12, 16, 16), [hexc("4f7a3a"), hexc("3a5a2a")], 14, 13)
    return img


@texture("block/hanging_ghost.png")
def hanging_ghost():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("f2f4f8"), 0.04, 14)
    rect(img, 4, 4, 6, 7, BLACK)
    rect(img, 10, 4, 12, 7, BLACK)
    rect(img, 6, 9, 10, 12, hexc("3a3a48"))
    for x in range(0, 16, 3):
        img.putpixel((x, 15), (0, 0, 0, 0))
    return img


# ----------------------------------------------------------------------------------------------- costume textures

def flat(color, seed, spread=0.05):
    def make():
        img = canvas()
        noise_fill(img, (0, 0, 16, 16), color, spread, seed)
        return img
    return make


TEXTURES["item/costume/pumpkin_side.png"] = pumpkin_side_ribbed
TEXTURES["item/costume/pumpkin_top.png"] = pumpkin_top_stem
TEXTURES["item/costume/pumpkin_face.png"] = lambda: pumpkin_front("grin", True)
TEXTURES["item/costume/ghost_cloth.png"] = flat(hexc("f4f6fa"), 20, 0.03)
TEXTURES["item/costume/bone.png"] = flat(BONE, 21, 0.05)
TEXTURES["item/costume/witch_purple.png"] = flat(PURPLE, 22, 0.08)
TEXTURES["item/costume/witch_band.png"] = flat(ORANGE, 23, 0.05)
TEXTURES["item/costume/vampire_black.png"] = flat(BLACK, 24, 0.06)
TEXTURES["item/costume/vampire_red.png"] = flat(hexc("9a1020"), 25, 0.06)
TEXTURES["item/costume/straw.png"] = flat(STRAW, 26, 0.12)
TEXTURES["item/costume/straw_band.png"] = flat(BROWN, 27, 0.06)
TEXTURES["item/costume/bat.png"] = flat(hexc("2e2238"), 28, 0.08)
TEXTURES["item/costume/cat.png"] = flat(BLACK, 29, 0.05)
TEXTURES["item/costume/cat_inner.png"] = flat(hexc("e88aa8"), 30, 0.05)


@texture("item/costume/ghost_face.png")
def ghost_face():
    img = flat(hexc("f4f6fa"), 31, 0.03)()
    rect(img, 3, 4, 6, 9, BLACK)
    rect(img, 10, 4, 13, 9, BLACK)
    rect(img, 6, 10, 10, 14, hexc("2a2a34"))
    return img


@texture("item/costume/skull_face.png")
def skull_face():
    img = flat(BONE, 32, 0.05)()
    rect(img, 3, 4, 7, 8, BLACK)
    rect(img, 9, 4, 13, 8, BLACK)
    rect(img, 7, 9, 9, 11, hexc("3a3428"))
    for x in range(4, 12):
        img.putpixel((x, 12), BLACK if x % 2 else BONE)
        img.putpixel((x, 13), hexc("a8a290"))
    return img


# ----------------------------------------------------------------------------------------------- entities

def box_faces(u, v, w, h, d):
    """Texture regions of a model box: dict face -> (x0, y0, x1, y1)."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def paint_box(img, u, v, w, h, d, color, spread=0.05, seed=0):
    for face, (x0, y0, x1, y1) in box_faces(u, v, w, h, d).items():
        noise_fill(img, (x0, y0, x1, y1), color, spread, seed + _h(face) % 100)


@texture("entity/ghost.png")
def ghost_entity():
    img = canvas(64, 64)
    cloth = hexc("eef2ff", 205)
    paint_box(img, 0, 0, 8, 8, 8, cloth, 0.03, 40)
    paint_box(img, 0, 16, 10, 10, 10, cloth, 0.03, 41)
    for i in range(3):
        paint_box(img, 40, i * 5, 4, 5, 1, cloth, 0.03, 42 + i)
    paint_box(img, 40, 16, 2, 7, 2, cloth, 0.03, 45)
    fx0, fy0, _, _ = box_faces(0, 0, 8, 8, 8)["front"]
    rect(img, fx0 + 1, fy0 + 2, fx0 + 3, fy0 + 5, hexc("101018", 255))
    rect(img, fx0 + 5, fy0 + 2, fx0 + 7, fy0 + 5, hexc("101018", 255))
    rect(img, fx0 + 3, fy0 + 6, fx0 + 5, fy0 + 8, hexc("20202c", 255))
    return img


@texture("entity/gourdling.png")
def gourdling_entity():
    img = canvas(64, 32)
    faces = box_faces(0, 0, 10, 8, 10)
    for name, (x0, y0, x1, y1) in faces.items():
        ribbed(img, (x0, y0, x1, y1), ORANGE, ORANGE_D, ORANGE_L, 50 + len(name))
    fx0, fy0, _, _ = faces["front"]
    face = ["..........", ".XX....XX.", ".XX....XX.", "..........", "....XX....", "X.XXXXXX.X", ".XX.XX.XX.", ".........."]
    for y, row in enumerate(face):
        for x, ch in enumerate(row):
            if ch == "X":
                img.putpixel((fx0 + x, fy0 + y), GLOW)
    paint_box(img, 40, 0, 2, 3, 2, hexc("4a6a22"), 0.05, 51)
    paint_box(img, 40, 5, 4, 1, 2, GREEN, 0.08, 52)
    paint_box(img, 0, 18, 2, 2, 2, hexc("2e5a18"), 0.05, 53)
    return img


@texture("entity/scarecrow.png")
def scarecrow_entity():
    img = canvas(64, 64)
    head = box_faces(0, 0, 8, 8, 8)
    for name, (x0, y0, x1, y1) in head.items():
        ribbed(img, (x0, y0, x1, y1), hexc("c86a1a"), hexc("9a4a0e"), hexc("e08a3a"), 60 + len(name))
    fx0, fy0, _, _ = head["front"]
    stitched = ["........", ".X....X.", "XXX..XXX", ".X....X.", "........", "X.X.X.X.", ".X.X.X.X", "........"]
    for y, row in enumerate(stitched):
        for x, ch in enumerate(row):
            if ch == "X":
                img.putpixel((fx0 + x, fy0 + y), DARK)
    paint_box(img, 32, 0, 6, 4, 6, hexc("5a4a32"), 0.08, 61)
    paint_box(img, 56, 0, 2, 30, 2, hexc("6b4f2a"), 0.08, 62)
    paint_box(img, 0, 16, 8, 12, 4, hexc("8a5a3a"), 0.1, 63)
    bx0, by0, bx1, by1 = box_faces(0, 16, 8, 12, 4)["front"]
    for y in range(by0, by1):
        img.putpixel((bx0 + 3, y), hexc("5a3a20"))
    rect(img, bx0 + 1, by0 + 2, bx0 + 3, by0 + 4, hexc("4a6a8a"))
    paint_box(img, 24, 16, 3, 4, 3, STRAW, 0.15, 64)
    paint_box(img, 24, 24, 6, 3, 5, STRAW_D, 0.12, 65)
    paint_box(img, 0, 32, 24, 2, 2, hexc("6b4f2a"), 0.08, 66)
    paint_box(img, 0, 40, 12, 1, 12, hexc("5a4a32"), 0.1, 67)
    return img




# ----------------------------------------------------------------------------------------------- christmas

NEEDLE = hexc("2f6b3a")
NEEDLE_D = hexc("1e4a28")
NEEDLE_L = hexc("4a8f4f")
RED = hexc("c0392b")
GOLD = hexc("d4af37")
SNOW = hexc("f4f8ff")


@texture("block/present_paper.png")
def present_paper():
    # Greyscale so the wrapping colour (tint) shows through; subtle star pattern.
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("e8e8e8"), 0.03, 90)
    for x, y in ((3, 3), (11, 5), (6, 10), (13, 12), (2, 13)):
        img.putpixel((x, y), hexc("ffffff"))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            img.putpixel((x + dx, y + dy), hexc("f8f8f8"))
    return img


@texture("block/present_ribbon.png")
def present_ribbon():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), GOLD, 0.06, 91)
    for y in range(0, 16, 3):
        img.putpixel((7, y), hexc("f0d070"))
    return img


@texture("block/present_open.png")
def present_open():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("b88a58"), 0.06, 92)
    rect(img, 0, 0, 16, 1, hexc("8a6038"))
    rect(img, 0, 15, 16, 16, hexc("8a6038"))
    return img


@texture("block/tree_needles.png")
def tree_needles():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), NEEDLE, 0.12, 93)
    dots(img, (0, 0, 16, 16), [NEEDLE_D, NEEDLE_L, hexc("26593a")], 70, 94)
    return img


def ornaments(level, seed):
    img = canvas()
    colors = [RED, GOLD, hexc("2e6ec0"), hexc("e84a7f"), hexc("e8e8e8"), hexc("8e44ad")]
    rnd = random.Random(seed)
    spots = [(2, 3), (11, 2), (6, 7), (13, 9), (3, 12), (9, 13), (7, 1), (1, 8), (14, 14)]
    for i, (x, y) in enumerate(spots[: level * 3]):
        c = colors[rnd.randrange(len(colors))]
        rect(img, x, y, x + 2, y + 2, c)
        img.putpixel((x, y), shade(c, 1.35))
    if level >= 2:
        for x in range(16):
            y = 5 + int(2 * __import__("math").sin(x / 2.0))
            if img.getpixel((x, y))[3] == 0:
                img.putpixel((x, y), hexc("e8e0c8"))
    return img


for _lvl in (1, 2, 3):
    TEXTURES[f"block/tree_ornaments_{_lvl}.png"] = (lambda l: (lambda: ornaments(l, 95 + l)))(_lvl)


@texture("block/tree_star.png")
def tree_star():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), GOLD, 0.08, 96)
    return img


@texture("block/tree_star_lit.png")
def tree_star_lit():
    img = canvas()
    noise_fill(img, (0, 0, 16, 16), hexc("fff2a8"), 0.04, 97)
    return img


@texture("block/tree_lights.png")
def tree_lights():
    img = canvas()
    rnd = random.Random(98)
    for i in range(18):
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), rnd.choice([hexc("ffe680"), hexc("ff8080"), hexc("80c0ff"), hexc("a0ff a0".replace(" ", ""))]))
    return img


@texture("block/wreath.png")
def wreath():
    img = canvas()
    import math
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 3.5 <= d <= 7.2:
                img.putpixel((x, y), NEEDLE_L if (x * 3 + y) % 5 == 0 else (NEEDLE_D if (x + y) % 3 == 0 else NEEDLE))
    for x, y in ((3, 4), (12, 6), (5, 12), (11, 11), (2, 9), (13, 2)):
        img.putpixel((x, y), RED)
    rect(img, 6, 12, 10, 15, RED)
    img.putpixel((5, 15), RED)
    img.putpixel((10, 15), RED)
    rect(img, 7, 13, 9, 14, hexc("e85848"))
    return img


@texture("block/string_lights.png")
def string_lights():
    img = canvas()
    import math
    colors = [hexc("ff5050"), hexc("ffd84a"), hexc("50c0ff"), hexc("60e070"), hexc("ff80e0")]
    for x in range(16):
        y = 3 + int(round(2 * math.sin(x / 2.5)))
        img.putpixel((x, y), hexc("2a3a2a"))
        if x % 3 == 1:
            c = colors[(x // 3) % len(colors)]
            rect(img, x, y + 1, x + 2, y + 3, c)
    for x in range(16):
        y = 10 + int(round(2 * math.sin(x / 2.5 + 1.5)))
        img.putpixel((x, y), hexc("2a3a2a"))
        if x % 3 == 0:
            c = colors[(x // 3 + 2) % len(colors)]
            rect(img, x, y + 1, x + 2, y + 3, c)
    return img


@texture("item/christmas_tree.png")
def christmas_tree_item():
    rows = [
        ".......Y........",
        "......YYY.......",
        ".......G........",
        "......GGG.......",
        ".....GGRGG......",
        "......GGG.......",
        ".....GGGGG......",
        "....GBGGGGG.....",
        "...GGGGGYGGG....",
        ".....GGGGG......",
        "....GGGGRGGG....",
        "...GGYGGGGBGG...",
        "..GGGGGGGGGGGG..",
        ".......TT.......",
        ".......TT.......",
        "................",
    ]
    return from_map(rows, {"Y": GOLD, "G": NEEDLE, "R": RED, "B": hexc("2e6ec0"), "T": BROWN})


@texture("item/ornament.png")
def ornament_item():
    rows = [
        "................",
        "................",
        ".......GG.......",
        "......G..G......",
        ".......GG.......",
        ".....RRRRRR.....",
        "....RWRRRRRR....",
        "...RWRRRRRRRR...",
        "...RRRRRRRRRR...",
        "...RRRRRRRRRr...",
        "...RRRRRRRRrr...",
        "....RRRRRRrr....",
        ".....rrrrrr.....",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"G": GOLD, "R": RED, "r": hexc("8e231d"), "W": hexc("f0a090")})


TEXTURES["item/costume/santa_red.png"] = flat(RED, 99, 0.06)
TEXTURES["item/costume/fur.png"] = flat(SNOW, 100, 0.04)
TEXTURES["item/costume/elf_green.png"] = flat(hexc("2e8b57"), 101, 0.06)
TEXTURES["item/costume/elf_trim.png"] = flat(hexc("c0392b"), 102, 0.06)
TEXTURES["item/costume/bell.png"] = flat(GOLD, 103, 0.05)
TEXTURES["item/costume/antler.png"] = flat(hexc("8a6a42"), 104, 0.08)
TEXTURES["item/costume/headband.png"] = flat(hexc("6b3a1e"), 105, 0.06)


@texture("item/costume/beanie.png")
def beanie():
    img = canvas()
    for y in range(16):
        c = hexc("2e6ec0") if (y // 3) % 2 == 0 else SNOW
        rect(img, 0, y, 16, y + 1, c)
    for x in range(0, 16, 2):
        for y in range(16):
            px = img.getpixel((x, y))
            img.putpixel((x, y), shade(px, 0.9))
    return img



# ----------------------------------------------------------------------------------------------- spring and feasts

EGG_ROWS = [
    "................",
    "................",
    "......AAAA......",
    ".....AAAAAA.....",
    "....AHAAAAAA....",
    "....AHAAAAAA....",
    "...BBBBBBBBBB...",
    "...AAAAAAAAAA...",
    "...ACACACACAA...",
    "...AAAAAAAAAA...",
    "...BBBBBBBBBB...",
    "....AAAAAAAA....",
    "....AAAAAAAA....",
    ".....aaaaaa.....",
    "................",
    "................",
]


def egg(base, band, dot):
    def make():
        return from_map(EGG_ROWS, {"A": base, "a": shade(base, 0.8), "H": shade(base, 1.25), "B": band, "C": dot})
    return make


TEXTURES["item/easter_egg_pink.png"] = egg(hexc("f4a6c8"), hexc("ffffff"), hexc("8e44ad"))
TEXTURES["item/easter_egg_blue.png"] = egg(hexc("8cc8f0"), hexc("f6c843"), hexc("ffffff"))
TEXTURES["item/easter_egg_yellow.png"] = egg(hexc("f6e070"), hexc("e07b1a"), hexc("4fae6a"))
TEXTURES["item/easter_egg_green.png"] = egg(hexc("a8e08a"), hexc("e84a7f"), hexc("ffffff"))
TEXTURES["item/golden_egg.png"] = egg(hexc("f0c040"), hexc("fff2a8"), hexc("c08a20"))


@texture("item/easter_basket.png")
def easter_basket():
    rows = [
        "................",
        "....HHHHHHHH....",
        "...H........H...",
        "...H........H...",
        "...H.PB..YG.H...",
        "..WWWWWWWWWWWW..",
        "..WwWwWwWwWwWW..",
        "..WWWWWWWWWWWW..",
        "...WwWwWwWwWW...",
        "...WWWWWWWWWW...",
        "....WwWwWwWW....",
        "....wwwwwwww....",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"H": hexc("a8784a"), "W": hexc("c8955a"), "w": hexc("8a6038"), "P": hexc("f4a6c8"), "B": hexc("8cc8f0"),
                           "Y": hexc("f6e070"), "G": hexc("a8e08a")})


@texture("item/chocolate_box.png")
def chocolate_box():
    rows = [
        "................",
        "................",
        "......RR.RR.....",
        ".....RRRRRRR....",
        "....RRRRRRRRR...",
        "....RRRPPRRRR...",
        "....RRRPPRRRR...",
        ".....RRRRRRR....",
        "......RRRRR.....",
        ".......RRR......",
        "........R.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"R": hexc("c0283a"), "P": hexc("f4c0d0")})


@texture("item/harvest_pie.png")
def harvest_pie():
    rows = [
        "................",
        "................",
        "................",
        "................",
        "....CCCCCCCC....",
        "...CFFFFFFFFC...",
        "..CFFcFFFcFFFC..",
        "..CFFFFcFFFFFC..",
        "..CCCCCCCCCCCC..",
        "..cccccccccccc..",
        "...cccccccccc...",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    return from_map(rows, {"C": hexc("d8a050"), "c": hexc("a8743a"), "F": hexc("c86a1a"), "f": hexc("a8541a")})


def main():
    only = set(sys.argv[1:])
    for path, fn in TEXTURES.items():
        if only and not any(o in path for o in only):
            continue
        out = os.path.normpath(os.path.join(TEX, path))
        os.makedirs(os.path.dirname(out), exist_ok=True)
        img = fn()
        img.save(out)
        print("wrote", os.path.relpath(out, ROOT))


if __name__ == "__main__":
    main()
