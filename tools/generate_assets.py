#!/usr/bin/env python3
"""Generate models from the same fixed-hinge panel geometry as DoorGeometry.java."""
import json
import math
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
WOODS = 'oak spruce birch jungle acacia dark_oak mangrove cherry bamboo crimson warped'.split()
INSET = 3 / (2 * math.sqrt(2))
LENGTH = (16 - 2 * INSET) * math.sqrt(2)

def write(path, data):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')

def panel(right, opened):
    a, b = (INSET, 16-INSET), (16-INSET, INSET)
    hinge, tip = (b, a) if right else (a, b)
    if opened:
        dx, dz = tip[0]-hinge[0], tip[1]-hinge[1]
        tip = (hinge[0]+dz, hinge[1]-dx) if right else (hinge[0]-dz, hinge[1]+dx)
    return hinge, tip

for wood in WOODS:
    variants = {}
    for half in ['lower', 'upper']:
        texture = f'minecraft:block/{wood}_door_' + ('bottom' if half == 'lower' else 'top')
        for right in [False, True]:
            for opened in [False, True]:
                hinge, tip = panel(right, opened)
                mx, mz = (hinge[0]+tip[0])/2, (hinge[1]+tip[1])/2
                # Model rotations support +/-45 degrees; blockstate rotations supply the cardinal facing.
                angle = -45 if opened else 45
                hinge_side = 'right' if right else 'left'
                model = f'diagonal_{wood}_door_{half}_{"open" if opened else "closed"}_{hinge_side}'
                # Mirror UVs for the opposite hinge so the handle remains at the free edge.
                front = [16, 0, 0, 16] if right and not opened else [0, 0, 16, 16]
                back = [0, 0, 16, 16] if right and not opened else [16, 0, 0, 16]
                faces = {side: {'uv': uv, 'texture': '#door'} for side, uv in {
                    'north': front, 'south': back, 'east': [13,0,16,16],
                    'west': [0,0,3,16], 'up': [0,0,16,3], 'down': [0,13,16,16]}.items()}
                write(f'assets/diagonaldoors/models/block/{model}.json', {
                    'ambientocclusion': False, 'render_type': 'minecraft:cutout',
                    'textures': {'door': texture, 'particle': texture},
                    'elements': [{'from': [mx-LENGTH/2,0,mz-1.5], 'to': [mx+LENGTH/2,16,mz+1.5],
                        'rotation': {'origin': [mx,8,mz], 'axis': 'y', 'angle': angle, 'rescale': False}, 'faces': faces}]})
                for turns, facing in enumerate(['north','east','south','west']):
                    key = f'facing={facing},half={half},hinge={hinge_side},open={str(opened).lower()}'
                    variants[key] = {'model': f'diagonaldoors:block/{model}', 'y': turns*90}
    write(f'assets/diagonaldoors/blockstates/diagonal_{wood}_door.json', {'variants': variants})
    write(f'data/diagonaldoors/loot_table/blocks/diagonal_{wood}_door.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item',
        'name': f'diagonaldoors:diagonal_{wood}_door'}], 'conditions': [
            {'condition': 'minecraft:block_state_property', 'block': f'diagonaldoors:diagonal_{wood}_door', 'properties': {'half':'lower'}},
            {'condition': 'minecraft:survives_explosion'}]}]})
for tag in ['doors', 'wooden_doors', 'mineable/axe']:
    write(f'data/minecraft/tags/block/{tag}.json', {'replace': False, 'values': [f'diagonaldoors:diagonal_{w}_door' for w in WOODS]})
for tag in ['doors', 'wooden_doors']:
    write(f'data/minecraft/tags/item/{tag}.json', {'replace': False, 'values': [f'diagonaldoors:diagonal_{w}_door' for w in WOODS]})
# Remove the prototype's unreferenced, center-pivot models.
for path in (ROOT/'assets/diagonaldoors/models/block').glob('*_closed.json'):
    path.unlink()
print('Generated 88 hinged models, 11 blockstates, 11 loot tables, and door/axe tags.')
