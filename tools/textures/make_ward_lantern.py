"""Writes the Ward Lantern's placeholder block texture by recolouring vanilla's soul lantern (same lantern UV layout and
animation strip), so the two can't be confused: an amethyst frame instead of dark iron, and paler cyan-tinted glass.

Usage: python3 tools/textures/make_ward_lantern.py  (reads the 26.2 client jar from the NeoForm cache)
Output: common/src/main/resources/assets/paranormalcraft/textures/block/ward_lantern.png (+ .mcmeta)
"""
import glob
import os
import struct
import zipfile
import zlib

JAR = glob.glob(os.path.expanduser("~/.gradle/caches/neoformruntime/artifacts/minecraft_26.2_client.jar"))[0]
SOURCE = "assets/minecraft/textures/block/soul_lantern.png"
OUT = os.path.join(os.path.dirname(__file__), "..", "..", "common", "src", "main", "resources", "assets", "paranormalcraft",
                   "textures", "block", "ward_lantern.png")


def decode(data):
    i, idat, pal, trns = 8, b"", b"", b""
    while i < len(data):
        n = struct.unpack(">I", data[i:i + 4])[0]
        kind, chunk = data[i + 4:i + 8], data[i + 8:i + 8 + n]
        i += 12 + n
        if kind == b"IHDR":
            w, h, depth, colour = struct.unpack(">IIBB", chunk[:10])
            assert depth == 8 and colour == 3, "expected an 8-bit palette PNG"
        elif kind == b"PLTE":
            pal = chunk
        elif kind == b"tRNS":
            trns = chunk
        elif kind == b"IDAT":
            idat += chunk
    raw, pos, prev, rows = zlib.decompress(idat), 0, [0] * w, []
    for _ in range(h):
        mode = raw[pos]
        line = list(raw[pos + 1:pos + 1 + w])
        pos += 1 + w
        out = []
        for x in range(w):
            a = out[x - 1] if x else 0
            b = prev[x]
            c = prev[x - 1] if x else 0
            v = line[x]
            if mode == 1:
                v += a
            elif mode == 2:
                v += b
            elif mode == 3:
                v += (a + b) >> 1
            elif mode == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                v += a if pa <= pb and pa <= pc else b if pb <= pc else c
            out.append(v & 255)
        rows.append(out)
        prev = out
    return w, h, [[(*pal[v * 3:v * 3 + 3], trns[v] if v < len(trns) else 255) for v in row] for row in rows]


def recolour(pixel):
    r, g, b, a = pixel
    if a == 0:
        return pixel
    if g > 140 and b > 140:  # the flame and glow: paler, cyan-tinted glass
        return (min(255, r // 2 + 110), min(255, g // 2 + 125), min(255, b // 2 + 128), a)
    luma = (r + g + b) / 3 / 90.0  # the iron frame: amethyst, keeping its shading
    return (int(min(255, 150 * luma)), int(min(255, 104 * luma)), int(min(255, 190 * luma)), a)


def encode(w, h, pixels):
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


with zipfile.ZipFile(JAR) as jar:
    w, h, pixels = decode(jar.read(SOURCE))
    meta = jar.read(SOURCE + ".mcmeta")
with open(OUT, "wb") as out:
    out.write(encode(w, h, [[recolour(p) for p in row] for row in pixels]))
with open(OUT + ".mcmeta", "wb") as out:
    out.write(meta)
print("wrote", os.path.normpath(OUT))
