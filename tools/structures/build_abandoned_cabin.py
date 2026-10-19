"""Prints the server commands that build one Abandoned Cabin variant and set up a structure block to save it.

Usage: python3 tools/structures/build_abandoned_cabin.py <wood> <originX> <originY> <originZ>   (wood: oak or spruce)

How the templates in common/src/main/resources/data/paranormalcraft/structure were made, on the NeoForge dev server
(any loaded spot works, since everything around the house is cleared to air or structure_void; e.g. 1000 -60 1000
and 1030 -60 1000 in a flat world):
  1. /tick freeze   (so the ghost marker doesn't turn into ghosts before saving)
  2. run every printed command, e.g. over RCON
  3. /kill @e[type=item] near the site (re-placing doors and beds drops items)
  4. /paranormalcraft debug savestructure <originX-3> <originY> <originZ-3>
  5. copy <world>/generated/paranormalcraft/structure/abandoned_cabin_<wood>.nbt into the resources folder

Layout, relative to the origin: the house is x 0..8, z 0..6, with z=6 the front (porch side). Front room x1..4,
partition wall x=5 with a door, back room x6..7. The template is 15x9x14 (x -3..11, y 0..8, z -3..10): the house plus
a 2-3 block yard ring, the porch, fallen roof pieces and the broken fence. Template y=0 lands on the terrain's top
block; outside the ring everything is structure_void, so the terrain shows through. All decay is baked in; no
structure processors.

Trees: worldgen only places a tree where its sapling would survive, i.e. on #minecraft:supports_vegetation (dirt,
coarse and rooted dirt, mud, moss blocks, grass, podzol, mycelium, farmland); huge mushrooms need the same plus nylium.
So the yard ring is dirt path, gravel and packed mud (none of those), with mossy cobblestone rubble and moss carpet on
top. Grass and ferns only grow on grass blocks right under the eaves and the gable-corner overhang, where the roof is
too low for any tree to fit.

The ghost marker sits in the back room at (6, 1, 2), under the collapsed roof corner; the nearest roofed spot, and so
the cold spot, is next to it at (6, 1, 3).
"""
import sys

wood, ox, oy, oz = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
spruce = wood == "spruce"
P, LOG, STRIPPED = f"{wood}_planks", f"{wood}_log", f"stripped_{wood}_log"
DOOR, SLAB, FENCE, TRAPDOOR = f"{wood}_door", f"{wood}_slab", f"{wood}_fence", f"{wood}_trapdoor"
ROOF = "spruce" if spruce else "dark_oak"
STAIRS, ROOF_SLAB = f"{ROOF}_stairs", f"{ROOF}_slab"
cmds = []

MIN_X, MAX_X, MIN_Z, MAX_Z, MAX_Y = -3, 11, -3, 10, 8


def at(x, y, z):
    return f"{ox + x} {oy + y} {oz + z}"


def fill(x1, y1, z1, x2, y2, z2, block):
    cmds.append(f"fill {at(x1, y1, z1)} {at(x2, y2, z2)} {block}")


def setb(x, y, z, block):
    cmds.append(f"setblock {at(x, y, z)} {block}")


def plant(x, z, block, tall=False):
    """A plant on a grass block at ground level (y=0 is the terrain's top block)."""
    setb(x, 0, z, "grass_block")
    if tall:
        setb(x, 1, z, f"{block}[half=lower]")
        setb(x, 2, z, f"{block}[half=upper]")
    else:
        setb(x, 1, z, block)


# ---- clear the site: void everywhere, air inside the house ----
cmds.append(f"kill @e[type=!player,x={ox + MIN_X - 1},y={oy - 1},z={oz + MIN_Z - 1},dx=17,dy=11,dz=16]")
fill(MIN_X - 1, 0, MIN_Z - 1, MAX_X + 1, MAX_Y + 1, MAX_Z + 1, "air")
fill(MIN_X, 0, MIN_Z, MAX_X, MAX_Y, MAX_Z, "structure_void")
fill(0, 1, 0, 8, 7, 6, "air")

# ---- foundation: cobblestone ring with moss, plank floor ----
fill(0, 0, 0, 8, 0, 6, "mossy_cobblestone")
for x, z in [(1, 0), (4, 0), (8, 3), (3, 6), (6, 6)]:
    setb(x, 0, z, "cobblestone")
for x, z in [(0, 1), (0, 2), (0, 4), (2, 6), (7, 6), (8, 5)]:
    setb(x, 0, z, "moss_block")
fill(1, 0, 1, 7, 0, 5, P)

# ---- frame: log sill beam, corner and partition posts, plank walls ----
fill(1, 1, 0, 7, 1, 0, f"{LOG}[axis=x]")
fill(1, 1, 6, 7, 1, 6, f"{LOG}[axis=x]")
fill(0, 1, 1, 0, 1, 5, f"{LOG}[axis=z]")
fill(8, 1, 1, 8, 1, 5, f"{LOG}[axis=z]")
fill(1, 2, 0, 7, 3, 0, P)
fill(1, 2, 6, 7, 3, 6, P)
fill(0, 2, 1, 0, 3, 5, P)
fill(8, 2, 1, 8, 3, 5, P)
for x, z in [(0, 0), (8, 0), (0, 6), (8, 6), (5, 0), (5, 6)]:
    fill(x, 1, z, x, 3, z, f"{LOG}[axis=y]")
# Weathered section of the west wall: bleached (stripped) boards and a missing board.
fill(0, 2, 2, 0, 2, 4, f"{STRIPPED}[axis=z]")
setb(0, 3, 2, f"{STRIPPED}[axis=z]")
setb(0, 3, 4, f"{STRIPPED}[axis=z]")
setb(0, 3, 3, "air")
# Partition with a door between the rooms; its gable closes the attic between them.
fill(5, 1, 1, 5, 3, 5, P)
setb(5, 1, 3, f"{DOOR}[facing=east,half=lower,hinge=left]")
setb(5, 2, 3, f"{DOOR}[facing=east,half=upper,hinge=left]")
# Front door.
setb(2, 1, 6, f"{DOOR}[facing=north,half=lower,hinge=right]")
setb(2, 2, 6, f"{DOOR}[facing=north,half=upper,hinge=right]")

# ---- windows: two per long wall ----
# Front: A (x3..4) intact; B (x6..7) half-broken, one pane gone.
setb(3, 2, 6, "glass_pane")
setb(4, 2, 6, "glass_pane")
setb(6, 2, 6, "glass_pane")
setb(7, 2, 6, "air")
# Back: C (x1..2) panes missing; D (x6..7) intact, half covered by a shut shutter.
setb(1, 2, 0, "air")
setb(2, 2, 0, "air")
setb(6, 2, 0, "glass_pane")
setb(7, 2, 0, "glass_pane")
# Shutters: one hanging open beside window A, one shut over half of window D.
setb(5, 2, 7, f"{TRAPDOOR}[facing=east,half=bottom,open=true]")
setb(7, 2, -1, f"{TRAPDOOR}[facing=north,half=bottom,open=true]")

# ---- roof: gable along the long side, stairs from the eaves to a log ridge, overhanging the gable ends ----
rows = [(-1, 3, "south"), (0, 4, "south"), (1, 5, "south"), (2, 6, "south"),
        (4, 6, "north"), (5, 5, "north"), (6, 4, "north"), (7, 3, "north")]
for z, y, facing in rows:
    fill(-1, y, z, 9, y, z, f"{STAIRS}[facing={facing},half=bottom]")
fill(-1, 6, 3, 9, 6, 3, f"{LOG}[axis=x]")
for x in (0, 5, 8):
    fill(x, 4, 1, x, 4, 5, P)
    fill(x, 5, 2, x, 5, 4, P)
# Ceiling joists: one across the front room, a broken one in the back room (the lantern hangs from it).
fill(3, 4, 1, 3, 4, 5, f"{LOG}[axis=z]")
fill(6, 4, 3, 6, 4, 5, f"{LOG}[axis=z]")
# Moss on the front slope and the ridge.
setb(0, 4, 6, "moss_block")
setb(1, 4, 6, "moss_block")
setb(1, 5, 5, "moss_block")
for x in (1, 2, 3):
    setb(x, 7, 3, "moss_carpet")

# ---- porch: slab step, two fence posts holding a slab awning ----
# The step is a bottom slab sunk into the ground layer right outside the door: half a block below the floor, so the
# awning (bottom of y=3) is 2.5 blocks above it and 2 above the ground in front. A step at y=1 left 1.5 blocks under
# the awning and blocked the door.
setb(2, 0, 7, f"{SLAB}[type=bottom]")
for x in (1, 3):
    setb(x, 0, 8, "cobblestone")
    fill(x, 1, 8, x, 2, 8, FENCE)
fill(1, 3, 7, 3, 3, 8, f"{SLAB}[type=bottom]")

# ---- collapse: the back (north-east) corner. The ridge and top front stairs are gone too, so the hole shows from
# the front as a notch in the ridge line ----
fill(6, 4, 0, 9, 6, 2, "air")       # north slope over the back room, and the east gable's north half
fill(7, 3, -1, 9, 3, -1, "air")     # eave
fill(7, 6, 3, 9, 6, 3, "air")       # ridge
fill(7, 6, 4, 9, 6, 4, "air")       # the front slope's top two rows, so the hole shows from the front
fill(8, 5, 5, 9, 5, 5, "air")
setb(9, 4, 6, "air")
fill(8, 4, 3, 8, 5, 3, "air")       # more of the east gable
setb(8, 3, 0, "air")                # top of the corner post
setb(8, 3, 1, "air")
setb(7, 3, 0, "air")
setb(7, 4, 1, "cobweb")             # cobweb in the gap
# Fallen roof pieces on the ground nearby, and one inside.
setb(10, 1, -1, f"{STAIRS}[facing=west,half=bottom]")
setb(9, 1, -2, f"{ROOF_SLAB}[type=bottom]")
setb(10, 1, 1, f"{STAIRS}[facing=north,half=top]")
setb(10, 1, 6, f"{STAIRS}[facing=east,half=bottom]")  # one more in front, seen on the approach
setb(6, 1, 1, f"{ROOF_SLAB}[type=bottom]")

# ---- interior (unchanged): table with lit candles, crafting table, loot chest, torches, bed, lantern, marker ----
setb(1, 1, 1, f"{SLAB}[type=top]")
setb(2, 1, 1, f"{SLAB}[type=top]")
setb(1, 2, 1, "candle[candles=2,lit=true]")
setb(2, 2, 1, "candle[candles=1,lit=true]")
setb(4, 1, 1, "crafting_table")
setb(1, 1, 4, 'chest[facing=east]{LootTable:"paranormalcraft:chests/abandoned_cabin"}')
setb(1, 2, 5, "wall_torch[facing=north]")
setb(4, 2, 1, "wall_torch[facing=south]")
setb(7, 1, 4, "red_bed[facing=south,part=foot]")
setb(7, 1, 5, "red_bed[facing=south,part=head]")
setb(7, 2, 5, "cobweb")
setb(6, 3, 1, "cobweb")
setb(7, 1, 1, "cobweb")
setb(6, 3, 4, "lantern[hanging=true]")
setb(6, 1, 2, "paranormalcraft:ghost_marker")
cmds.append(f'summon painting {at(3, 2, 1)} {{facing:0b,variant:"minecraft:kebab"}}')
cmds.append(f'summon item_frame {at(1, 2, 3)} {{Facing:5b,Item:{{id:"minecraft:flower_pot",count:1}}}}')

# ---- outside decay: vines, cobwebs under the eaves, coarse dirt, grass and ferns, broken fence ----
for z, ys in [(1, (1, 2, 3)), (2, (2, 3)), (5, (2, 3)), (4, (3,))]:
    for y in ys:
        setb(-1, y, z, "vine[east=true]")
for x, y, z in [(1, 2, -1), (4, 2, -1), (6, 2, 7), (-1, 5, 3), (0, 2, 7)]:
    setb(x, y, z, "cobweb")
short, tall = ("fern", "large_fern") if spruce else ("short_grass", "tall_grass")
# Plants only under the long eaves (z=-1, z=7) and the gable-corner overhang, where the roof is 3-4 blocks up.
PLANTS = {(-1, 0): short, (-1, 6): short, (2, -1): short, (4, -1): short, (8, 7): short,
          (3, -1): (tall, True), (-1, -1): (tall, True), (4, 7): "fern", (-1, 7): "fern", (5, -1): "fern"}
if not spruce:
    PLANTS.update({(0, 7): "poppy", (7, 7): "dandelion"})
for (x, z), plant_spec in PLANTS.items():
    if isinstance(plant_spec, tuple):
        plant(x, z, plant_spec[0], tall=True)
    else:
        plant(x, z, plant_spec)

# The yard ring: 2 blocks all round, a third block in irregular patches. None of it supports vegetation.
def ring_distance(x, z):
    dx = -x if x < 0 else (x - 8 if x > 8 else 0)
    dz = -z if z < 0 else (z - 6 if z > 6 else 0)
    return max(dx, dz)


def noise(x, z):
    """A fixed pseudo-random value for a column (an integer hash), so the ring looks irregular but rebuilds the same."""
    h = (x * 374761393 + z * 668265263 + 0x5BD1E995) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return (h ^ (h >> 16)) >> 8


KEEP = set(PLANTS) | {(1, 8), (3, 8), (2, 7)} | ({(-1, 3)} if spruce else set())
TRAIL = {(2, 8), (2, 9), (2, 10), (3, 10)}
RUBBLE = {(-2, 5): "mossy_cobblestone", (10, 4): "mossy_cobblestone", (5, -2): "mossy_cobblestone_slab[type=bottom]",
          (-3, 1): "mossy_cobblestone_slab[type=bottom]", (11, 7): "mossy_cobblestone"}
for x in range(MIN_X, MAX_X + 1):
    for z in range(MIN_Z, MAX_Z + 1):
        d = ring_distance(x, z)
        if (x, z) in TRAIL:
            setb(x, 0, z, "dirt_path")
            continue
        if d == 0 or (x, z) in KEEP or not (1 <= d <= 2 or (d == 3 and noise(x, z) % 2 == 0)):
            continue
        n = noise(x, z) % 100
        ground = "gravel" if n < 40 else ("packed_mud" if n < 78 else "dirt_path")
        setb(x, 0, z, ground)
        if (x, z) in RUBBLE:
            setb(x, 1, z, RUBBLE[(x, z)])
        elif ground == "packed_mud" and noise(z, x) % 4 == 0:
            setb(x, 1, z, "moss_carpet")
for x in (-1, 0, 1, 4):
    setb(x, 0, 10, "grass_block")
    setb(x, 1, 10, FENCE)

# ---- spruce: a mossy cobblestone chimney stub on the west gable end ----
if spruce:
    fill(-1, 0, 3, -1, 6, 3, "mossy_cobblestone")
    setb(-1, 7, 3, "mossy_cobblestone_wall")
    setb(-1, 5, 3, "mossy_cobblestone")

# The structure block that saves the template (step 4 above).
sb = at(MIN_X - 1, 0, MIN_Z - 1)
cmds.append(f'setblock {sb} structure_block[mode=save]{{mode:"SAVE",name:"paranormalcraft:abandoned_cabin_{wood}",'
            f'posX:1,posY:0,posZ:1,sizeX:{MAX_X - MIN_X + 1},sizeY:{MAX_Y + 1},sizeZ:{MAX_Z - MIN_Z + 1},'
            f'ignoreEntities:0b,showboundingbox:1b}}')
print("\n".join(cmds))
