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
    """pixels: rows of (r, g, b, a) tuples, square (16x16 for items and blocks, 18x18 for mob effects)."""
    size = len(pixels)
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(kind, data):
        body = kind + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as out:
        out.write(data)


def canvas(fill=CLEAR, size=16):
    return [[fill for _ in range(size)] for _ in range(size)]


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
# ---- Update 9: Containment Unit, Ward Lantern, ectoplasm, residue, journal, Restful ----
STEEL = (120, 126, 136, 255)
STEEL_DARK = (72, 76, 86, 255)
GLASS_DARK = (30, 34, 44, 255)
RED = (230, 60, 40, 255)


def cabinet_side():
    img = canvas()
    for y in range(16):
        for x in range(16):
            img[y][x] = STEEL if noise(x, y) else STEEL_DARK
    rect(img, 0, 0, 15, 1, IRON)
    rect(img, 0, 14, 15, 15, IRON_DARK)
    rect(img, 0, 0, 1, 15, IRON_DARK)
    rect(img, 14, 0, 15, 15, IRON_DARK)
    return img


png(os.path.join(ROOT, "block", "containment_unit_side.png"), cabinet_side())
top_img = cabinet_side()
rect(top_img, 5, 5, 10, 10, COPPER)
rect(top_img, 6, 6, 9, 9, COPPER_DARK)
png(os.path.join(ROOT, "block", "containment_unit_top.png"), top_img)


def cabinet_front(glass, glow):
    img = cabinet_side()
    rect(img, 3, 2, 12, 12, IRON_DARK)
    rect(img, 4, 3, 11, 11, glass)
    if glow is not None:
        for y in range(4, 11):
            for x in range(5, 11):
                if (x + y) % 3 == 0:
                    img[y][x] = glow
    rect(img, 6, 13, 9, 13, COPPER)
    return img


png(os.path.join(ROOT, "block", "containment_unit_front.png"), cabinet_front(GLASS_DARK, None))
png(os.path.join(ROOT, "block", "containment_unit_front_lit.png"), cabinet_front(CYAN_DIM, CYAN))
png(os.path.join(ROOT, "block", "containment_unit_front_unstable.png"), cabinet_front((90, 20, 16, 255), RED))

# The Ward Lantern icon: an amethyst frame round pale cyan glass (the block texture is make_ward_lantern.py).
WARD_FRAME = (150, 104, 190, 255)
WARD_FRAME_DARK = (100, 68, 130, 255)
ward = canvas()
rect(ward, 7, 0, 8, 2, WARD_FRAME_DARK)
rect(ward, 4, 3, 11, 4, WARD_FRAME)
rect(ward, 4, 5, 11, 12, WARD_FRAME_DARK)
rect(ward, 5, 6, 10, 11, (150, 235, 245, 255))
rect(ward, 6, 7, 9, 10, (220, 252, 255, 255))
rect(ward, 4, 13, 11, 13, WARD_FRAME)
png(os.path.join(ROOT, "item", "ward_lantern.png"), ward)

ecto = canvas()
for y in range(16):
    for x in range(16):
        d = (x - 7.5) ** 2 / 30 + (y - 9) ** 2 / 20
        if d < 1.0:
            ecto[y][x] = (200, 240, 230, 210) if d > 0.6 else (230, 255, 245, 170)
for x, y in ((6, 6), (9, 8), (7, 11)):
    ecto[y][x] = (255, 255, 255, 230)
png(os.path.join(ROOT, "item", "lingering_ectoplasm.png"), ecto)

residue = canvas()
for x, y in ((7, 4), (8, 5), (6, 6), (9, 7), (7, 8), (8, 9), (6, 10), (9, 11), (7, 12)):
    residue[y][x] = (150, 110, 210, 255)
    residue[y][x + 1 if x < 15 else x] = (90, 220, 230, 200)
png(os.path.join(ROOT, "item", "spectral_residue.png"), residue)

journal = canvas()
rect(journal, 3, 2, 12, 13, (92, 60, 38, 255))
rect(journal, 4, 3, 11, 12, (120, 82, 52, 255))
rect(journal, 12, 3, 12, 12, (236, 226, 200, 255))
rect(journal, 6, 6, 9, 8, COPPER)
rect(journal, 7, 7, 8, 7, CYAN)
png(os.path.join(ROOT, "item", "field_journal.png"), journal)

restful = canvas(size=18)
for y in range(18):
    for x in range(18):
        if (x - 8.5) ** 2 + (y - 8.5) ** 2 < 49 and (x - 11) ** 2 + (y - 6.5) ** 2 > 30:
            restful[y][x] = (200, 230, 255, 255)
png(os.path.join(os.path.dirname(ROOT), "textures", "mob_effect", "restful.png"), restful)
print("wrote placeholder textures under", os.path.normpath(ROOT))

# ---- Update 10: Spirit Vessel (empty and bound), Spectral Step, the Wisp Familiar's glow ----
GLASS = (200, 230, 236, 150)
GLASS_EDGE = (150, 190, 200, 220)


def vessel(bound):
    img = canvas()
    rect(img, 6, 1, 9, 2, COPPER)            # copper cap
    rect(img, 6, 3, 9, 3, COPPER_DARK)
    rect(img, 7, 4, 8, 5, GLASS_EDGE)        # neck
    rect(img, 4, 6, 11, 14, GLASS_EDGE)      # body
    rect(img, 5, 7, 10, 13, GLASS)
    rect(img, 5, 14, 10, 14, COPPER_DARK)    # base band
    if bound:
        rect(img, 6, 8, 9, 12, CYAN_DIM)
        rect(img, 7, 9, 8, 11, CYAN)
        img[9][7] = (230, 255, 255, 255)
    return img


png(os.path.join(ROOT, "item", "spirit_vessel.png"), vessel(False))
png(os.path.join(ROOT, "item", "spirit_vessel_bound.png"), vessel(True))

# Spectral Step: an amethyst charm on a thread, with a cyan swirl through it.
step = canvas()
for x, y in [(7, 0), (8, 1), (8, 2), (7, 3)]:
    step[y][x] = (200, 200, 210, 255)
rect(step, 5, 4, 10, 12, (120, 80, 170, 255))
rect(step, 6, 5, 9, 11, AMETHYST)
rect(step, 7, 13, 8, 14, (120, 80, 170, 255))
for x, y in [(6, 10), (7, 9), (8, 8), (9, 7), (8, 6), (7, 7)]:
    step[y][x] = CYAN
png(os.path.join(ROOT, "item", "spectral_step.png"), step)

# The Wisp Familiar's billboard: a soft white glow, 32x32, tinted by the renderer.
glow = canvas(size=32)
for y in range(32):
    for x in range(32):
        d = ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5 / 16.0
        if d < 1.0:
            core = max(0.0, 1.0 - d / 0.35)
            alpha = (1.0 - d) ** 2.2
            value = int(200 + 55 * core)
            glow[y][x] = (value, value, value, int(255 * min(1.0, alpha + core)))
png(os.path.join(ROOT, "entity", "wisp_familiar.png"), glow)
