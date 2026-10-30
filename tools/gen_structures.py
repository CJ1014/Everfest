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


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, data in {"empty_5x5": floor(5, 5, 5), "plot_16x16": floor(16, 8, 16)}.items():
        path = os.path.join(OUT, name + ".nbt")
        with gzip.open(path, "wb") as f:
            f.write(data)
        print("wrote", path)


if __name__ == "__main__":
    main()
