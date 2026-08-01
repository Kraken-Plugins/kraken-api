#!/usr/bin/env python3
"""Regenerates the bundled DPS data in src/main/resources/dps/ from the osrs-dps-calc
project's cdn/json files.

Every equipment piece and monster is kept; only fields the Java calculator never reads
are dropped (item images/weights, monster images, UI-only max_hit/level) to keep the
jar small. Run this after pulling a newer osrs-dps-calc to pick up new items/monsters:

    python3 scripts/update_dps_data.py [path-to-osrs-dps-calc]

The source data itself is maintained by the wiki team and regenerated from the game
cache via osrs-dps-calc's scripts/generateEquipment.py and scripts/generateMonsters.py. You
can clone the DPS calc repo here but it is not bundled with the API by default: https://github.com/weirdgloop/osrs-dps-calc
"""
import json
import os
import sys

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CALC_ROOT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO_ROOT, 'osrs-dps-calc')
SRC = os.path.join(CALC_ROOT, 'cdn', 'json')
DST = os.path.join(REPO_ROOT, 'src', 'main', 'resources', 'dps')

VALID_STYLES = {'stab', 'slash', 'crush', 'magic', 'ranged'}


def main():
    os.makedirs(DST, exist_ok=True)

    # Equipment: drop image/weight, keep everything the calculator needs
    equipment = json.load(open(os.path.join(SRC, 'equipment.json')))
    eq_out = []
    for e in equipment:
        eq_out.append({
            'id': e['id'], 'name': e['name'], 'version': e.get('version', ''),
            'slot': e['slot'], 'speed': e.get('speed', 0), 'category': e.get('category', ''),
            'isTwoHanded': e.get('isTwoHanded', False),
            'bonuses': e['bonuses'], 'offensive': e['offensive'], 'defensive': e['defensive'],
        })
    json.dump(eq_out, open(os.path.join(DST, 'equipment.json'), 'w'), separators=(',', ':'))

    # Monsters: drop image/max_hit/level; style list -> single lowercase string or null
    # (mirrors the TS project's Monsters.ts parsing)
    monsters = json.load(open(os.path.join(SRC, 'monsters.json')))
    mo_out = []
    for m in monsters:
        style = ','.join(m.get('style') or []).lower() if m.get('style') else None
        if style not in VALID_STYLES:
            style = None
        mo_out.append({
            'id': m['id'], 'name': m['name'], 'version': m.get('version', ''),
            'size': m['size'], 'speed': m['speed'], 'style': style,
            'skills': m['skills'], 'offensive': m['offensive'], 'defensive': m['defensive'],
            'attributes': m.get('attributes', []),
            'weakness': m.get('weakness'),
            'burnImmunity': (m.get('immunities') or {}).get('burn'),
            'slayerMonster': m.get('is_slayer_monster', False),
        })
    json.dump(mo_out, open(os.path.join(DST, 'monsters.json'), 'w'), separators=(',', ':'))

    # Aliases (variant item id -> canonical id): straight copy, minified
    aliases = json.load(open(os.path.join(SRC, 'equipment_aliases.json')))
    json.dump(aliases, open(os.path.join(DST, 'equipment_aliases.json'), 'w'), separators=(',', ':'))

    # Spells: rename fields to the Java model's names
    spells = json.load(open(os.path.join(SRC, 'spells.json')))
    sp_out = [{'name': s['name'], 'maxHit': s['max_hit'], 'spellbook': s['spellbook'],
               'element': s.get('element')} for s in spells]
    json.dump(sp_out, open(os.path.join(DST, 'spells.json'), 'w'), separators=(',', ':'))

    print(f'Wrote {len(eq_out)} equipment, {len(mo_out)} monsters, '
          f'{len(aliases)} aliases, {len(sp_out)} spells to {DST}')


if __name__ == '__main__':
    main()
