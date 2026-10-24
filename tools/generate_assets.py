#!/usr/bin/env python3
"""Generate models from the same fixed-hinge panel geometry as DoorGeometry.java."""
import json
import math
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources'
WOODS = 'oak spruce birch jungle acacia dark_oak mangrove cherry bamboo crimson warped'.split()
INSET = 3 / (2 * math.sqrt(2))
LENGTH = (16 - INSET) * math.sqrt(2)
EDGE_EXTENSION = (LENGTH - 16) / 2

def write(path, data):
    path = ROOT / path
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')

def panel(right, opened):
    hinge = (16-INSET, INSET) if right else (INSET, 16-INSET)
    tip = (0,16) if right else (16,0)
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
                hinge_side = 'right' if right else 'left'
                model = f'diagonal_{wood}_door_{half}_{"open" if opened else "closed"}_{hinge_side}'
                low, high = sorted([hinge,tip])
                dx,dz = (high[0]-low[0])/LENGTH,(high[1]-low[1])/LENGTH
                angle = 45 if dz < 0 else -45
                flipped = tip[0] < hinge[0]
                elements = []
                # Preserve the entire 16-unit vanilla design at its normal scale. Only frame
                # strips at the ends are extended; never stretch the panels, windows, or handle.
                cuts = [0, EDGE_EXTENSION, EDGE_EXTENSION+16, LENGTH]
                for segment,(start,end) in enumerate(zip(cuts,cuts[1:])):
                    mx,mz=low[0]+dx*(start+end)/2,low[1]+dz*(start+end)/2
                    width=end-start
                    if segment==1:
                        u0,u1=0,16
                    else:
                        free_edge=(segment==0)==flipped
                        u0,u1=(2,3) if free_edge else (15,16)
                    front=[u1,0,u0,16] if flipped else [u0,0,u1,16]
                    back=[u0,0,u1,16] if flipped else [u1,0,u0,16]
                    faces={side:{'uv':uv,'texture':'#door'} for side,uv in {
                        'north':front,'south':back,'up':[u0,0,u1,3],'down':[u0,13,u1,16]}.items()}
                    if segment==0: faces['west']={'uv':[0,0,3,16],'texture':'#door'}
                    if segment==2: faces['east']={'uv':[13,0,16,16],'texture':'#door'}
                    element={'from':[mx-width/2,0,mz-1.5],'to':[mx+width/2,16,mz+1.5],
                        'rotation':{'origin':[mx,8,mz],'axis':'y','angle':angle,'rescale':False},'faces':faces}
                    assert all(-16<=v<=32 for key in ['from','to'] for v in element[key]), model
                    elements.append(element)
                write(f'assets/diagonaldoors/models/block/{model}.json', {
                    'ambientocclusion':False,'render_type':'minecraft:cutout',
                    'textures':{'door':texture,'particle':texture},'elements':elements})
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
