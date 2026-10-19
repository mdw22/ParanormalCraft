"""Writes the placeholder textures for the Milestone 2 tools (16x16 PNGs, no dependencies).

Usage: python3 tools/textures/make_placeholders.py
Output: common/src/main/resources/assets/paranormalcraft/textures/{item,block}/...
These are stand-ins until the art pass; rerunning overwrites them.
"""
import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "paranormalcraft", "textures")
CLEAR = (0, 0, 0, 0)


def png(path, pixels):
    """pixels: 16 rows of 16 (r, g, b, a) tuples."""
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as out:
        out.write(data)


def canvas(fill=CLEAR):
    return [[fill for _ in range(16)] for _ in range(16)]


def rect(img, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img[y][x] = colour


def noise(x, y):
    return ((x * 73 + y * 151) ^ (x * y * 17)) % 7


IRON = (176, 180, 186, 255)
IRON_DARK = (110, 114, 122, 255)
COPPER = (192, 108, 74, 255)
COPPER_DARK = (140, 72, 50, 255)
AMETHYST = (170, 120, 220, 255)
CYAN = (111, 231, 240, 255)
CYAN_DIM = (60, 150, 160, 255)
TINT = (52, 40, 70, 255)
SHADOW = (24, 22, 30, 255)

# Tether Wand: a copper handle, a lightning-rod shaft and an amethyst tip, diagonal like a tool.
wand = canvas()
for i in range(12):
    x, y = 2 + i, 13 - i
    colour = COPPER if i < 4 else (IRON if i < 10 else AMETHYST)
    dark = COPPER_DARK if i < 4 else (IRON_DARK if i < 10 else (120, 80, 170, 255))
    wand[y][x] = colour
    if y + 1 < 16:
        wand[y + 1][x] = dark
wand[1][14] = CYAN
wand[2][13] = CYAN_DIM
wand[0][15] = CYAN_DIM
png(os.path.join(ROOT, "item", "tether_wand.png"), wand)


def trap_icon(window):
    img = canvas()
    rect(img, 2, 6, 13, 13, IRON)
    rect(img, 2, 13, 13, 13, IRON_DARK)
    rect(img, 1, 4, 14, 6, COPPER)
    rect(img, 1, 6, 14, 6, COPPER_DARK)
    rect(img, 5, 8, 10, 11, window)
    rect(img, 5, 11, 10, 11, SHADOW if window == TINT else CYAN_DIM)
    for x in (3, 12):
        img[9][x] = IRON_DARK
    return img


png(os.path.join(ROOT, "item", "ghost_trap.png"), trap_icon(TINT))
png(os.path.join(ROOT, "item", "filled_ghost_trap.png"), trap_icon(CYAN))

# Block faces.
side = canvas()
for y in range(16):
    for x in range(16):
        side[y][x] = IRON if noise(x, y) else IRON_DARK
rect(side, 0, 0, 15, 2, COPPER)
rect(side, 0, 2, 15, 2, COPPER_DARK)
rect(side, 0, 13, 15, 15, IRON_DARK)
png(os.path.join(ROOT, "block", "ghost_trap_side.png"), side)


def top(window, glow):
    img = canvas()
    for y in range(16):
        for x in range(16):
            img[y][x] = COPPER if noise(x, y) else COPPER_DARK
    rect(img, 4, 4, 11, 11, IRON_DARK)
    rect(img, 5, 5, 10, 10, window)
    if glow:
        rect(img, 7, 7, 8, 8, (200, 250, 255, 255))
    return img


png(os.path.join(ROOT, "block", "ghost_trap_top.png"), top(TINT, False))
png(os.path.join(ROOT, "block", "filled_ghost_trap_top.png"), top(CYAN, True))
inside = canvas(SHADOW)
for y in range(16):
    for x in range(16):
        if noise(x, y) == 0:
            inside[y][x] = CYAN_DIM
png(os.path.join(ROOT, "block", "ghost_trap_inside.png"), inside)
print("wrote placeholder textures under", os.path.normpath(ROOT))
