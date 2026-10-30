#!/usr/bin/env python3
"""Generates EverFest's original pixel-art textures.

Every texture is drawn procedurally from palettes and pixel maps defined here, so all art is original
and can be regenerated deterministically:  python3 tools/gen_textures.py
"""
import os
import random
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", "holidayseasons", "textures")

TEXTURES = {}


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
