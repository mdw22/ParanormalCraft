"""Prints the server commands that build one Abandoned Cabin variant and set up a structure block to save it.

Usage: python3 tools/structures/build_abandoned_cabin.py <wood> <originX> <originY> <originZ>   (wood: oak or spruce)

How the templates in common/src/main/resources/data/paranormalcraft/structure were made, on the NeoForge dev server
(a flat world, origin on the grass surface, e.g. 1000 -60 1000 and 1020 -60 1000):
  1. /tick freeze   (so the ghost marker doesn't turn into ghosts before saving)
  2. run every printed command except the last (redstone_block) line, e.g. over RCON
  3. /kill @e[type=item] near the site (re-placing doors and beds drops items)
  4. /paranormalcraft debug savestructure <originX-1> <originY> <originZ-1>
  5. copy <world>/generated/paranormalcraft/structure/abandoned_cabin_<wood>.nbt into the resources folder

Layout (relative x 0..8, z 0..6; z=6 is the front): front room x1..4, partition wall x=5 with a door, back room x6..7.
The ghost marker sits in the back room at (6, 1, 2); that room is roofed, so cabin ghosts' cold spot lands there.
"""
import sys

wood, ox, oy, oz = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), int(sys.argv[4])
P, LOG, DOOR, SLAB = f"{wood}_planks", f"{wood}_log", f"{wood}_door", f"{wood}_slab"
cmds = []


def at(x, y, z):
    return f"{ox + x} {oy + y} {oz + z}"


def fill(x1, y1, z1, x2, y2, z2, block):
    cmds.append(f"fill {at(x1, y1, z1)} {at(x2, y2, z2)} {block}")


def setb(x, y, z, block):
    cmds.append(f"setblock {at(x, y, z)} {block}")


# Clear the site (and anything left from a previous build).
cmds.append(f"kill @e[type=!player,x={ox - 2},y={oy - 1},z={oz - 2},dx=13,dy=10,dz=11]")
fill(-1, 0, -1, 9, 7, 7, "air")
# Foundation: mossy cobblestone ring with plank floor.
fill(0, 0, 0, 8, 0, 6, "mossy_cobblestone")
for x, z in [(1, 0), (4, 0), (8, 3), (0, 5), (6, 6), (3, 6)]:
    setb(x, 0, z, "cobblestone")
fill(1, 0, 1, 7, 0, 5, P)
# Walls, logs at the corners and at the partition ends.
fill(0, 1, 0, 8, 3, 6, P)
fill(1, 1, 1, 7, 3, 5, "air")
for x, z in [(0, 0), (8, 0), (0, 6), (8, 6), (5, 0), (5, 6)]:
    fill(x, 1, z, x, 3, z, f"{LOG}[axis=y]")
# Partition with a door between the rooms.
fill(5, 1, 1, 5, 3, 5, P)
setb(5, 1, 3, f"{DOOR}[facing=east,half=lower,hinge=left]")
setb(5, 2, 3, f"{DOOR}[facing=east,half=upper,hinge=left]")
# Front door.
setb(2, 1, 6, f"{DOOR}[facing=north,half=lower,hinge=right]")
setb(2, 2, 6, f"{DOOR}[facing=north,half=upper,hinge=right]")
# Back room windows: broken glass panes in the east wall (one gone entirely).
setb(8, 2, 2, "glass_pane")
setb(8, 2, 3, "air")
setb(8, 2, 4, "glass_pane")
# Roof, then the collapsed back corner (north-east).
fill(0, 4, 0, 8, 4, 6, P)
fill(0, 5, 2, 8, 5, 4, f"{SLAB}[type=bottom]")
fill(7, 4, 0, 8, 5, 1, "air")
fill(8, 3, 0, 8, 3, 1, "air")
setb(7, 3, 0, "air")
# Front room: table with two lit candles, crafting table, loot chest, wall torches by the door.
setb(1, 1, 1, f"{SLAB}[type=top]")
setb(2, 1, 1, f"{SLAB}[type=top]")
setb(1, 2, 1, "candle[candles=2,lit=true]")
setb(2, 2, 1, "candle[candles=1,lit=true]")
setb(4, 1, 1, "crafting_table")
setb(1, 1, 4, 'chest[facing=east]{LootTable:"paranormalcraft:chests/abandoned_cabin"}')
setb(1, 2, 5, "wall_torch[facing=north]")
setb(3, 2, 5, "wall_torch[facing=north]")
# Back room: bed with cobwebs, a hanging lantern, rubble under the collapsed corner, and the ghost marker.
setb(7, 1, 4, "red_bed[facing=south,part=foot]")
setb(7, 1, 5, "red_bed[facing=south,part=head]")
setb(7, 2, 5, "cobweb")
setb(6, 3, 1, "cobweb")
setb(7, 1, 1, "cobweb")
setb(6, 3, 4, "lantern[hanging=true]")
setb(6, 1, 2, "paranormalcraft:ghost_marker")
# Painting on the back wall of the front room, item frame with a flower pot on the west wall.
cmds.append(f'summon painting {at(3, 2, 1)} {{facing:0b,variant:"minecraft:kebab"}}')
cmds.append(f'summon item_frame {at(1, 2, 3)} {{Facing:5b,Item:{{id:"minecraft:flower_pot",count:1}}}}')
# The structure block that saves the template (step 4 above). The redstone block only saves to memory, not disk.
sb = f"{ox - 1} {oy} {oz - 1}"
cmds.append(f'setblock {sb} structure_block[mode=save]{{mode:"SAVE",name:"paranormalcraft:abandoned_cabin_{wood}",'
            f'posX:1,posY:0,posZ:1,sizeX:9,sizeY:6,sizeZ:7,ignoreEntities:0b,showboundingbox:1b}}')
cmds.append(f"setblock {ox - 2} {oy} {oz - 1} redstone_block")
print("\n".join(cmds))
