#!/usr/bin/env python3
"""Writes GameTest structure templates (gzipped NBT) without external libraries:  python3 tools/gen_structures.py"""
import gzip
import os
import struct

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "holidayseasons", "structure")
DATA_VERSION = 3955  # Minecraft 1.21.1

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def w_str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def payload(tag, value):
    if tag == TAG_INT:
        return struct.pack(">i", value)
    if tag == TAG_STRING:
        return w_str(value)
    if tag == TAG_LIST:
        elem_tag, items = value
        out = struct.pack(">bi", elem_tag if items else TAG_END, len(items))
        for it in items:
            out += payload(elem_tag, it)
        return out
    if tag == TAG_COMPOUND:
        out = b""
        for name, (t, v) in value.items():
            out += struct.pack(">b", t) + w_str(name) + payload(t, v)
        return out + struct.pack(">b", TAG_END)
    raise ValueError(tag)


def structure(size, blocks, palette):
    root = {
        "DataVersion": (TAG_INT, DATA_VERSION),
        "size": (TAG_LIST, (TAG_INT, list(size))),
        "palette": (TAG_LIST, (TAG_COMPOUND, [{"Name": (TAG_STRING, p)} for p in palette])),
        "blocks": (TAG_LIST, (TAG_COMPOUND, [{"pos": (TAG_LIST, (TAG_INT, list(pos))), "state": (TAG_INT, st)} for pos, st in blocks])),
        "entities": (TAG_LIST, (TAG_COMPOUND, [])),
    }
    return struct.pack(">b", TAG_COMPOUND) + w_str("") + payload(TAG_COMPOUND, root)


def floor(w, h, d, block="minecraft:smooth_stone"):
    palette = ["minecraft:air", block]
    blocks = []
    for x in range(w):
        for z in range(d):
            blocks.append(((x, 0, z), 1))
            for y in range(1, h):
                blocks.append(((x, y, z), 0))
    return structure((w, h, d), blocks, palette)


class Template:
    """A sparse block template: positions not set are left untouched when the structure is placed."""

    def __init__(self, w, h, d):
        self.size = (w, h, d)
        self.blocks = {}

    def set(self, x, y, z, name, nbt=None, **props):
        w, h, d = self.size
        if 0 <= x < w and 0 <= y < h and 0 <= z < d:
            self.blocks[(x, y, z)] = (name if ":" in name else "minecraft:" + name, tuple(sorted((k, str(v).lower()) for k, v in props.items())), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, name, **props)

    def chest(self, x, y, z, loot, facing="north"):
        self.set(x, y, z, "chest", nbt={"id": "minecraft:chest", "LootTable": loot}, facing=facing)

    def build(self):
        palette, index, blocks = [], {}, []
        for pos in sorted(self.blocks):
            name, props, nbt = self.blocks[pos]
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": (TAG_STRING, name)}
                if props:
                    entry["Properties"] = (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in props})
                palette.append(entry)
            b = {"pos": (TAG_LIST, (TAG_INT, list(pos))), "state": (TAG_INT, index[key])}
            if nbt:
                b["nbt"] = (TAG_COMPOUND, {k: (TAG_STRING, v) for k, v in nbt.items()})
            blocks.append(b)
        root = {
            "DataVersion": (TAG_INT, DATA_VERSION),
            "size": (TAG_LIST, (TAG_INT, list(self.size))),
            "palette": (TAG_LIST, (TAG_COMPOUND, palette)),
            "blocks": (TAG_LIST, (TAG_COMPOUND, blocks)),
            "entities": (TAG_LIST, (TAG_COMPOUND, [])),
        }
        return struct.pack(">b", TAG_COMPOUND) + w_str("") + payload(TAG_COMPOUND, root)


def rng(seed):
    import random
    return random.Random(seed)


def gable_roof(t, x0, x1, z0, z1, y0, stairs, slab, planks):
    """A roof along x: stairs rising from both z edges to a slab ridge, gable ends filled with planks."""
    depth = z1 - z0
    for i in range(depth // 2 + 1):
        y = y0 + i
        za, zb = z0 + i, z1 - i
        for x in range(x0, x1 + 1):
            if za < zb:
                t.set(x, y, za, stairs, facing="south", half="bottom", shape="straight", waterlogged=False)
                t.set(x, y, zb, stairs, facing="north", half="bottom", shape="straight", waterlogged=False)
            elif za == zb:
                t.set(x, y, za, slab, type="bottom", waterlogged=False)
        # gable ends just inside the overhang
        for z in range(za + 1, zb):
            t.set(x0 + 1, y, z, planks)
            t.set(x1 - 1, y, z, planks)


# Ground for yards and graveyards: nothing here is in #minecraft:dirt, so the biome's trees and flowers (placed after
# structures) cannot take root inside the landmark.
GROUND = ["dirt_path", "dirt_path", "gravel", "packed_mud", "dirt_path"]


def seasonal_cabin():
    t = Template(11, 13, 11)
    loot = "holidayseasons:chests/seasonal_cabin"
    # foundation and floor (walls stand on x 1..9, z 1..9)
    t.fill(1, 0, 1, 9, 0, 9, "cobblestone")
    t.fill(2, 0, 2, 8, 0, 8, "spruce_planks")
    # clear the inside
    t.fill(2, 1, 2, 8, 3, 8, "air")
    # log walls
    for y in range(1, 4):
        for x in range(1, 10):
            t.set(x, y, 1, "spruce_log", axis="x")
            t.set(x, y, 9, "spruce_log", axis="x")
        for z in range(2, 9):
            t.set(1, y, z, "spruce_log", axis="z")
            t.set(9, y, z, "spruce_log", axis="z")
        for (x, z) in ((1, 1), (9, 1), (1, 9), (9, 9)):
            t.set(x, y, z, "stripped_spruce_log", axis="y")
    # ceiling
    t.fill(1, 4, 1, 9, 4, 9, "spruce_planks")
    gable_roof(t, 0, 10, 0, 10, 4, "spruce_stairs", "spruce_slab", "spruce_planks")
    # door, windows, wreath over the door
    t.set(5, 1, 1, "spruce_door", facing="south", half="lower", hinge="left", open=False, powered=False)
    t.set(5, 2, 1, "spruce_door", facing="south", half="upper", hinge="left", open=False, powered=False)
    for x in (3, 7):
        t.set(x, 2, 1, "glass_pane", east=True, west=True, north=False, south=False, waterlogged=False)
    for z in (4, 6):
        t.set(1, 2, z, "glass_pane", north=True, south=True, east=False, west=False, waterlogged=False)
        t.set(9, 2, z, "glass_pane", north=True, south=True, east=False, west=False, waterlogged=False)
    t.set(5, 3, 0, "holidayseasons:wreath", facing="north")
    t.set(4, 1, 0, "lantern", hanging=False, waterlogged=False)
    # fireplace with a claimable Holiday Hearth, stone chimney with a smoking campfire
    t.fill(4, 1, 9, 6, 3, 9, "cobblestone")
    t.set(5, 1, 8, "holidayseasons:holiday_hearth", facing="north")
    t.fill(5, 4, 9, 5, 11, 9, "cobblestone")
    t.fill(5, 1, 10, 5, 11, 10, "cobblestone")
    t.set(5, 12, 10, "campfire", lit=True, signal_fire=False, facing="north", waterlogged=False)
    # inside: decorated tree, bed, chest, table, rug, lantern
    t.set(2, 1, 7, "holidayseasons:christmas_tree", half="lower", decoration=3, lit=True)
    t.set(2, 2, 7, "holidayseasons:christmas_tree", half="upper", decoration=3, lit=True)
    t.set(8, 1, 3, "red_bed", part="foot", facing="north", occupied=False)
    t.set(8, 1, 2, "red_bed", part="head", facing="north", occupied=False)
    t.chest(8, 1, 7, loot, facing="west")
    t.set(2, 1, 2, "crafting_table")
    t.set(3, 1, 2, "barrel", facing="up", open=False)
    t.fill(4, 1, 5, 6, 1, 6, "red_carpet")
    t.set(5, 3, 5, "lantern", hanging=True, waterlogged=False)
    t.set(2, 3, 4, "holidayseasons:string_lights", facing="east")
    t.set(8, 3, 5, "holidayseasons:string_lights", facing="west")
    return t


def haunted_cottage():
    t = Template(9, 8, 11)
    r = rng(1031)
    loot = "holidayseasons:chests/haunted_cottage"
    # yard (z 0..1) and house (x 0..8, z 2..10)
    for x in range(9):
        for z in range(0, 2):
            t.set(x, 0, z, r.choice(GROUND))
    for x in range(9):
        for z in range(2, 11):
            t.set(x, 0, z, r.choice(["cobblestone", "mossy_cobblestone", "cobblestone"]) if x in (0, 8) or z in (2, 10)
                  else r.choice(["dark_oak_planks", "dark_oak_planks", "dark_oak_planks", "cracked_stone_bricks"]))
    t.fill(1, 1, 3, 7, 3, 9, "air")
    for y in range(1, 4):
        for x in range(9):
            for z in (2, 10):
                t.set(x, y, z, "dark_oak_planks")
        for z in range(3, 10):
            for x in (0, 8):
                t.set(x, y, z, "dark_oak_planks")
        for (x, z) in ((0, 2), (8, 2), (0, 10), (8, 10)):
            t.set(x, y, z, "dark_oak_log", axis="y")
    # decay: a few holes and broken boards
    for (x, y, z) in ((2, 3, 2), (8, 3, 5), (0, 1, 8), (6, 3, 10), (8, 2, 8)):
        t.set(x, y, z, "air")
    for (x, y, z) in ((1, 3, 2), (8, 1, 4), (3, 3, 10)):
        t.set(x, y, z, "stripped_dark_oak_log", axis="y")
    # door and broken windows
    t.set(4, 1, 2, "dark_oak_door", facing="south", half="lower", hinge="right", open=True, powered=False)
    t.set(4, 2, 2, "dark_oak_door", facing="south", half="upper", hinge="right", open=True, powered=False)
    t.set(6, 2, 2, "black_stained_glass_pane", east=True, west=True, north=False, south=False, waterlogged=False)
    t.set(0, 2, 6, "black_stained_glass_pane", north=True, south=True, east=False, west=False, waterlogged=False)
    t.set(8, 2, 6, "black_stained_glass_pane", north=True, south=True, east=False, west=False, waterlogged=False)
    # sagging slab roof with a hole and a mossy chimney
    for x in range(-0, 9):
        for z in range(2, 11):
            if (x, z) not in ((3, 6), (4, 6), (3, 7)):
                t.set(x, 4, z, "dark_oak_slab", type="bottom", waterlogged=False)
    for x in range(1, 8):
        for z in range(4, 9):
            if (x + z) % 5 != 0 and (x, z) not in ((3, 6), (4, 6), (3, 7)):
                t.set(x, 5, z, "dark_oak_slab", type="bottom", waterlogged=False)
    t.fill(7, 1, 9, 7, 6, 9, "mossy_cobblestone")
    t.set(7, 7, 9, "cobweb")
    # inside: cobwebs, cauldron, scary jack o'lantern, hanging ghost, soul lantern, bookshelf, chest
    for (x, y, z) in ((1, 3, 3), (7, 3, 3), (1, 3, 9), (6, 3, 9), (1, 1, 9), (4, 3, 5)):
        t.set(x, y, z, "cobweb")
    t.set(2, 1, 8, "cauldron")
    t.set(4, 1, 9, "holidayseasons:jack_o_lantern_scary", facing="north")
    t.set(5, 3, 6, "holidayseasons:hanging_ghost", facing="north")
    t.set(2, 3, 5, "soul_lantern", hanging=True, waterlogged=False)
    t.set(1, 1, 5, "bookshelf")
    t.set(1, 2, 5, "bookshelf")
    t.set(1, 1, 6, "bookshelf")
    t.chest(6, 1, 9, loot, facing="north")
    # yard: carved pumpkins, a dead bush, a broken fence
    t.set(2, 1, 0, "holidayseasons:carved_pumpkin_grin", facing="north")
    t.set(6, 1, 1, "holidayseasons:carved_pumpkin_cat", facing="north")
    for (x, z) in ((1, 1), (7, 0)):
        t.set(x, 0, z, "brown_terracotta")
        t.set(x, 1, z, "dead_bush")
    t.set(0, 1, 0, "dark_oak_fence", east=True, west=False, north=False, south=False, waterlogged=False)
    t.set(1, 1, 0, "dark_oak_fence", east=False, west=True, north=False, south=False, waterlogged=False)
    t.set(8, 1, 0, "dark_oak_fence", east=False, west=False, north=False, south=False, waterlogged=False)
    return t


def forgotten_graveyard():
    t = Template(11, 5, 9)
    r = rng(1102)
    loot = "holidayseasons:chests/forgotten_graveyard"
    for x in range(11):
        for z in range(9):
            t.set(x, 0, z, r.choice(GROUND))
        for z in range(9):
            t.set(x, 1, z, "air")
    # fence ring with a gate in the middle of the north side and gaps where it has rotted away
    def fence(x, z):
        t.set(x, 1, z, "dark_oak_fence", waterlogged=False,
              east=(z in (0, 8) and x < 10), west=(z in (0, 8) and x > 0),
              north=(x in (0, 10) and z > 0), south=(x in (0, 10) and z < 8))
    for x in range(11):
        for z in (0, 8):
            if x != 5 and (x, z) not in ((8, 8), (2, 0)):
                fence(x, z)
    for z in range(1, 8):
        for x in (0, 10):
            if (x, z) != (10, 3):
                fence(x, z)
    t.set(5, 1, 0, "dark_oak_fence_gate", facing="north", open=False, powered=False, in_wall=False)
    for (x, z) in ((0, 0), (10, 0), (0, 8), (10, 8)):
        t.set(x, 2, z, "soul_lantern", hanging=False, waterlogged=False)
    # three rows of graves facing the gate
    for z in (3, 5):
        for x in (2, 4, 6, 8):
            if (x, z) == (8, 5):
                t.set(x, 1, z, "mossy_stone_brick_wall", up=True, north="none", south="none", east="none", west="none", waterlogged=False)
            else:
                t.set(x, 1, z, "holidayseasons:tombstone", facing="north")
            t.set(x, 0, z + 1, "packed_mud")
    t.set(2, 1, 7, "holidayseasons:tombstone", facing="north")
    t.set(4, 1, 7, "cobweb")
    for (x, z) in ((3, 2), (7, 6)):
        t.set(x, 0, z, "brown_terracotta")
        t.set(x, 1, z, "dead_bush")
    t.set(6, 1, 7, "candle", candles=3, lit=False, waterlogged=False)
    # a dead tree in the corner
    t.fill(9, 1, 7, 9, 4, 7, "dark_oak_log", axis="y")
    t.set(8, 4, 7, "dark_oak_log", axis="x")
    t.set(9, 4, 6, "dark_oak_log", axis="z")
    # a buried chest under the oldest grave
    t.chest(5, 0, 6, loot, facing="north")
    return t


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, data in {"empty_5x5": floor(5, 5, 5), "plot_16x16": floor(16, 8, 16)}.items():
        path = os.path.join(OUT, name + ".nbt")
        if os.path.exists(path):
            continue  # GameTest plots are fixed; keep the committed files byte-identical
        with gzip.open(path, "wb") as f:
            f.write(data)
        print("wrote", path)
    landmarks = os.path.join(OUT, "landmark")
    os.makedirs(landmarks, exist_ok=True)
    for name, fn in (("seasonal_cabin", seasonal_cabin), ("haunted_cottage", haunted_cottage), ("forgotten_graveyard", forgotten_graveyard)):
        path = os.path.join(landmarks, name + ".nbt")
        t = fn()
        with open(path, "wb") as raw, gzip.GzipFile(fileobj=raw, mode="wb", mtime=0) as f:
            f.write(t.build())
        print("wrote", path, t.size, len(t.blocks), "blocks")


if __name__ == "__main__":
    main()
