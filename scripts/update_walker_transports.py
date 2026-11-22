#!/usr/bin/env python3
"""Regenerates the walker's transport dataset and collision map from upstream shortest-path.

The walker plans over data Kraken ships itself: src/main/resources/walker/dataset.json and
src/main/resources/walker/collision-map.zip. Both come from https://github.com/Skretzo/shortest-path,
which keeps its transports as tab separated files and is maintained in public. This script reads
a checkout of that repository and writes every transport it lists in Kraken's row format.

    python3 scripts/update_walker_transports.py <shortest-path checkout> [--cache-dump <dump dir>]

Inputs:
  <shortest-path checkout>  a clone of https://github.com/Skretzo/shortest-path, read as data only
  --cache-dump              the directory scripts/WalkerCacheDump.java writes (objects.tsv,
                            npcs.tsv, items.tsv, locs.tsv, bridges.tsv). Optional, see below
  --runelite-api-jar        the RuneLite API jar, used to check quest and skill names; found in
                            the Gradle cache from build.gradle when not given
  --output                  where to write the dataset (default: the bundled resource)
  --collision-map           where to copy upstream's collision map (default: the bundled resource)
  --report                  where to write the notes on single rows
                            (default: build/walker-transports-report.tsv)

What is written:
  * every row that has a tile at both ends, as one transport
  * every row of a teleport file (items, spells, minigames), as a teleport usable from anywhere
  * hub networks (fairy rings, spirit trees, gliders and the like) as their stops, which the
    loader pairs up, so 55 fairy rings stay 110 rows instead of 2,968

A row names what to click the way upstream does, as one string such as "Open Door 9398". With a
cache dump, a row whose object the cache confirms beside the origin tile is written with the menu
option, the target name and the id the object is placed under instead, which is what the walker
searches the scene for. Without a dump every row keeps upstream's string.

What is left out, each with its reason in the report:
  * stops and rows the walker's fixed settings never plan: fairy rings, spirit trees and obelisks
    inside a player owned house, the mounted amulet of glory, and spirit trees the player has to
    grow first
  * rows gated on an account unlock the client does not expose (a shown fire cape, Xeric's Honour)
  * the Prifddinas respawn rows, which the client cannot tell from the Lumbridge default
  * item teleports that name no item, and teleports that state no wilderness level

The report also notes rows that are written but may not work as one click: rows upstream marks as
unconfirmed, one click that leads to several places, obstacles that are cleared instead of
crossed, and click targets the cache does not confirm.

Run ./gradlew test afterwards: TransportDatasetTest fails when a written row does not load.
"""
import argparse
import collections
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESOURCES = os.path.join(REPO_ROOT, 'src', 'main', 'resources', 'walker')
DEFAULT_OUTPUT = os.path.join(RESOURCES, 'dataset.json')
DEFAULT_COLLISION_MAP = os.path.join(RESOURCES, 'collision-map.zip')
DEFAULT_REPORT = os.path.join(REPO_ROOT, 'build', 'walker-transports-report.tsv')
GRADLE_MODULES = os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'modules-2', 'files-2.1')

UPSTREAM_URL = 'https://github.com/Skretzo/shortest-path'

# Starts the id of every row this script writes.
GENERATED_ID_PREFIX = 'kraken:upstream-'

OBJECT_INFO_COLUMN = 'menuOption menuTarget objectID'

# Upstream files whose rows have a tile at both ends, and the category their rows get.
TILE_SOURCES = {
    'transports.tsv': 'GENERAL',
    'agility_shortcuts.tsv': 'AGILITY_SHORTCUT',
    'boats.tsv': 'BOAT',
    'canoes.tsv': 'CANOE',
    'charter_ships.tsv': 'CHARTER_SHIP',
    'ships.tsv': 'SHIP',
    'magic_carpets.tsv': 'MAGIC_CARPET',
    'minecarts.tsv': 'MINECART',
    'teleportation_boxes.tsv': 'TELEPORTATION_BOX',
    'teleportation_levers.tsv': 'TELEPORTATION_LEVER',
    'teleportation_portals.tsv': 'TELEPORTATION_PORTAL',
    'teleportation_portals_poh.tsv': 'TELEPORTATION_PORTAL_POH',
}

# Upstream files whose rows are stops: a row with only an origin can travel to every row with
# only a destination. The number is how far apart two stops have to be, in tiles, to be joined,
# which keeps a stop from being paired with itself.
HUB_SOURCES = {
    'fairy_rings.tsv': ('FAIRY_RING', 6),
    'gnome_gliders.tsv': ('GNOME_GLIDER', 6),
    'hot_air_balloons.tsv': ('HOT_AIR_BALLOON', 7),
    'magic_mushtrees.tsv': ('MAGIC_MUSHTREE', 5),
    'minecarts.tsv': ('MINECART', 0),
    'quetzals.tsv': ('QUETZAL', 5),
    'spirit_trees.tsv': ('SPIRIT_TREE', 5),
    'wilderness_obelisks.tsv': ('WILDERNESS_OBELISK', 0),
}

# Upstream files of teleports usable from anywhere, which have no origin column.
TELEPORT_SOURCES = {
    'teleportation_items.tsv': 'TELEPORTATION_ITEM',
    'quetzal_whistle.tsv': 'QUETZAL_WHISTLE',
    'seasonal_transports.tsv': 'SEASONAL',
    'teleportation_spells.tsv': 'TELEPORTATION_SPELL',
    'teleportation_spells_home.tsv': 'TELEPORTATION_SPELL_HOME',
    'teleportation_minigames.tsv': 'TELEPORTATION_MINIGAME',
}

# The teleport categories an item carries. Their rows name the item in the Items column.
ITEM_TELEPORT_CATEGORIES = ('TELEPORTATION_ITEM', 'QUETZAL_WHISTLE', 'SEASONAL')

# Requirements upstream applies to a whole kind of transport in code rather than in its file.
# FAIRY2_QUEENCURE_QUEST (2326) has to be past 39 before any fairy ring works.
CATEGORY_REQUIREMENTS = {
    'FAIRY_RING': {'varbits': [{'id': 2326, 'comparison': 'AT_LEAST', 'value': 40}]},
    'GNOME_GLIDER': {'quests': ['THE_GRAND_TREE']},
    'MAGIC_MUSHTREE': {'quests': ['BONE_VOYAGE']},
    'SPIRIT_TREE': {'quests': ['TREE_GNOME_VILLAGE']},
}

# A fairy ring needs a dramen or lunar staff until the Lumbridge elite diary is done
# (LUMBRIDGE_DIARY_ELITE_COMPLETE, 4498).
FAIRY_RING_STAFF = {'waivedByVarbit': {'id': 4498, 'comparison': 'EQUAL', 'value': 1}}

# Account state the client reports for an unlock token. A token that is not listed here is
# something only the player knows, such as an axe left at a canoe station.
DETECTABLE_UNLOCKS = {
    # KARAMJA_DIARY_ELITE_COMPLETE
    'UNLOCK_KARAMJA_ELITE': {'id': 4566, 'comparison': 'EQUAL', 'value': 1},
}

# Section comments upstream uses for rows nobody has checked in game yet.
UNCONFIRMED = re.compile(
    r'inferred|not 100% confirmed|confirm with|confirm landings|replace with captured|assumes the', re.I)

# The walker looks for a transport's object by id this close to the origin tile, then by name
# within the wider radius. See TransportEntityResolver.
ID_SEARCH_RADIUS = 4
NAME_SEARCH_RADIUS = 10

# Menu options that clear an obstacle and leave the player standing where they were.
CLEARING_OPTIONS = ('mine', 'picklock', 'pullout', 'pull', 'fireat', 'chopdown', 'dodge')

# How far a crossing has to carry the player before the walker counts it as a teleport, which
# needs no judgement about what happened to an obstacle. See TransportArrival.
TELEPORT_DISTANCE = 16

# The player owned house area and the tile every transport into it is planned to. A house is an
# instance, so one tile stands for wherever inside it the player really is.
POH_X = (1856, 2047)
POH_Y = (7040, 7111)
POH_LANDING = (1858, 7051, 0)

# Where the Prifddinas respawn lands. Every respawn point but this one and Lumbridge has a varbit,
# so the client cannot tell the two apart, and the Lumbridge rows are the ones kept.
PRIFDDINAS_RESPAWN = (3265, 6077, 0)

VAR_PATTERN = re.compile(r'^\s*(\d+)\s*([=<>&@])\s*(-?\d+)\s*$')

# The order BundledTransports.toJson writes keys in, which generated rows follow so that they read
# like rows written by hand.
ROW_KEYS = ('id', 'origin', 'destination', 'category', 'shape', 'menuOption', 'menuTarget', 'entityId',
            'objectInfo', 'itemIds', 'displayInfo', 'duration', 'maxWildernessLevel', 'consumable',
            'bidirectional', 'replaces', 'requirements')
REQUIREMENT_KEYS = ('skills', 'combatLevel', 'totalLevel', 'quests', 'items', 'varbits', 'varPlayers', 'membersOnly')
ITEM_KEYS = ('anyOf', 'quantity', 'staves', 'offhands', 'or', 'waivedByVarbit')


class Skip(Exception):
    """Raised with the reason a row is left out of the dataset."""


def read_tsv(lines, file_name):
    """Reads a shortest-path transport file into (header, rows), one dict per row."""
    header = None
    section = None
    rows = []
    for number, line in enumerate(lines, 1):
        line = line.rstrip('\r\n')
        if not line.strip():
            continue
        if line.startswith('#'):
            if header is None:
                header = [cell.strip() for cell in line[1:].lstrip(' ').split('\t')]
            else:
                section = line[1:].strip().split('\t')[0].strip()
            continue
        if header is None:
            header = [cell.strip() for cell in line.split('\t')]
            continue
        cells = line.split('\t')
        row = {header[i]: (cells[i].strip() if i < len(cells) else '') for i in range(len(header))}
        row['_file'] = file_name
        row['_line'] = number
        row['_section'] = section or ''
        rows.append(row)
    return header or [], rows


def read_text_lines(path):
    with open(path, encoding='utf-8') as handle:
        return handle.read().splitlines()


def gradle_setting(pattern):
    with open(os.path.join(REPO_ROOT, 'build.gradle'), encoding='utf-8') as handle:
        match = re.search(pattern, handle.read())
    return match.group(1) if match else None


def find_in_gradle_cache(group, artifact, version):
    found = glob.glob(os.path.join(GRADLE_MODULES, group, artifact, version, '*', f'{artifact}-{version}.jar'))
    if not found:
        raise SystemExit(f'{artifact}-{version}.jar is not in the Gradle cache; run ./gradlew compileJava '
                         f'or pass its path')
    return found[0]


def parse_point(text):
    parts = text.split()
    if len(parts) != 3:
        raise Skip(f'tile "{text}" is not three numbers')
    return tuple(int(part) for part in parts)


def normalise(text):
    """Compares menu text the way the walker does: without case, hyphens, spaces or underscores."""
    return re.sub(r'[-_ ]', '', text or '').lower()


class Cache:
    """The parts of the game cache dump the checks need."""

    def __init__(self, directory):
        self.objects = {}
        for cells in self._rows(directory, 'objects.tsv'):
            transforms = [int(value) for value in cells[7].split(',') if value and value != '-1']
            self.objects[int(cells[0])] = {
                'name': cells[1],
                'actions': [action for action in cells[2].split('|') if action],
                'size': (int(cells[3]), int(cells[4])),
                'transforms': transforms,
            }

        self.npcs = {}
        for cells in self._rows(directory, 'npcs.tsv'):
            self.npcs[int(cells[0])] = {
                'name': cells[1],
                'actions': [action for action in cells[2].split('|') if action],
            }

        bridges = {(int(cells[0]), int(cells[1])) for cells in self._rows(directory, 'bridges.tsv')}

        # Placed objects, bucketed by 16 tile squares so the ones near a tile are quick to find.
        self.placed = collections.defaultdict(list)
        for cells in self._rows(directory, 'locs.tsv'):
            object_id, orientation = int(cells[0]), int(cells[2])
            x, y, plane = int(cells[3]), int(cells[4]), int(cells[5])
            # A bridge tile is stored one plane above the plane the game shows it on.
            if plane > 0 and (x, y) in bridges:
                plane -= 1
            definition = self.objects.get(object_id)
            if definition is None:
                continue
            width, height = definition['size']
            if orientation % 2 == 1:
                width, height = height, width
            # RuneLite reports a game object's location as the tile under its centre.
            centre = (x + width // 2, y + height // 2)
            self.placed[(x >> 4, y >> 4, plane)].append((object_id, x, y, centre))

    @staticmethod
    def _rows(directory, name):
        with open(os.path.join(directory, name), encoding='utf-8') as handle:
            for line in handle:
                if not line.startswith('#'):
                    yield line.rstrip('\r\n').split('\t')

    def forms(self, object_id):
        """Lists the definitions an object can show as: itself and whatever it transforms into."""
        definition = self.objects.get(object_id)
        if definition is None:
            return []
        forms = [(object_id, definition)]
        for other in definition['transforms']:
            if other in self.objects:
                forms.append((other, self.objects[other]))
        return forms

    def placed_near(self, point, radius):
        """Yields (placed id, distance) for every object within a radius of a tile, nearest first."""
        x, y, plane = point
        found = []
        for bucket_x in range((x - radius - 8) >> 4, ((x + radius + 8) >> 4) + 1):
            for bucket_y in range((y - radius - 8) >> 4, ((y + radius + 8) >> 4) + 1):
                for object_id, corner_x, corner_y, centre in self.placed.get((bucket_x, bucket_y, plane), ()):
                    distance = min(
                        max(abs(corner_x - x), abs(corner_y - y)),
                        max(abs(centre[0] - x), abs(centre[1] - y)))
                    if distance <= radius:
                        found.append((distance, object_id))
        found.sort()
        return [(object_id, distance) for distance, object_id in found]


def offers(definition, option):
    wanted = normalise(option)
    return any(normalise(action) == wanted for action in definition['actions'])


def split_object_info(info, name):
    """Splits "<option> <target>" for a target name, or returns None when the text does not end with it."""
    if not name or name == 'null':
        return None
    if info.lower().endswith(' ' + name.lower()):
        return info[:len(info) - len(name) - 1].strip(), name
    return None


def match_forms(info, forms):
    """Finds the form of a placed object that a row's "<option> <target>" text describes.

    The form has to carry the target's name and offer the option, so the split between the two is
    never a guess. Longer names are tried first, so "Open Castle door" is not read as a "door".
    """
    for _, definition in sorted(forms, key=lambda form: len(form[1]['name']), reverse=True):
        split = split_object_info(info, definition['name'])
        if split and split[0] and offers(definition, split[0]):
            return split
    return None


def resolve_object(cache, origin, info, object_id):
    """Finds the placed object a row means and returns (option, target, placed id, how).

    The id upstream records is looked for first, as itself or as a form of a placed object, within
    the walker's name search radius. When nothing with that id stands there, an object that carries
    the row's target name and offers its option is taken instead, which is the object the walker
    would fall back to in game.

    "how" is "id" or "name" when the object stands within the walker's id search radius, and "far"
    when it only matches further out, where another object of the same name is too easy to
    mistake for it.
    """
    for placed_id, distance in cache.placed_near(origin, NAME_SEARCH_RADIUS):
        forms = cache.forms(placed_id)
        if object_id not in [form_id for form_id, _ in forms]:
            continue
        split = match_forms(info, forms)
        if split:
            return split[0], split[1], placed_id, 'id' if distance <= ID_SEARCH_RADIUS else 'far'

    for placed_id, distance in cache.placed_near(origin, NAME_SEARCH_RADIUS):
        split = match_forms(info, cache.forms(placed_id))
        if split:
            return split[0], split[1], placed_id, 'name' if distance <= ID_SEARCH_RADIUS else 'far'

    return None


def parse_skills(text, skill_names, notes):
    """Reads a Skills cell into the skill, combat level and total level requirements it states."""
    requirements = {}
    skills = {}
    for part in filter(None, (part.strip() for part in text.split(';'))):
        match = re.match(r'^(\S+)\s+(.+)$', part)
        if not match:
            raise Skip(f'skill requirement "{part}" is not a level and a skill')
        level, name = match.group(1), match.group(2).strip().lower()
        maximum = level == 'Max'
        if not maximum and not level.isdigit():
            raise Skip(f'skill requirement "{part}" has no level')

        if name.startswith('total'):
            # Zero or less means every skill at its highest level, whatever the number of skills.
            requirements['totalLevel'] = -1 if maximum else int(level)
        elif name.startswith('combat'):
            if maximum:
                raise Skip(f'"{part}" asks for the highest combat level, which the format cannot state')
            requirements['combatLevel'] = int(level)
        elif name.startswith('quest') or name.startswith('music'):
            # The cape these rows need is already required as an item, and the game refuses its
            # teleport while the player is behind on quests or music.
            notes.append(f'"{part}" is not checked; the row still requires its item')
        elif name.upper() in skill_names:
            if maximum:
                raise Skip(f'"{part}" asks for the highest level in one skill, which the format cannot state')
            skills[name.upper()] = int(level)
        else:
            raise Skip(f'"{part}" names no skill RuneLite knows')
    if skills:
        requirements['skills'] = skills
    return requirements


def quest_constant(name):
    """Turns a quest's display name into the name of its constant in RuneLite's Quest enum."""
    stripped = re.sub(r"['&\-.!,()]", '', name.upper())
    return re.sub(r'[^A-Z0-9]', '_', stripped)


def parse_quests(text, quest_names):
    quests = []
    for name in filter(None, (part.strip() for part in text.split(';'))):
        constant = quest_constant(name)
        if constant not in quest_names:
            raise Skip(f'quest "{name}" is not in RuneLite\'s Quest enum as {constant}')
        quests.append(constant)
    return quests


def parse_alternative(token, variations):
    """Reads one "NAME=quantity" into an alternative, or into the name of an unlock token."""
    if '=' not in token:
        raise Skip(f'item requirement "{token}" has no quantity')
    name, quantity = token.split('=', 1)
    name = name.upper()
    if not re.match(r'^\d+$', quantity):
        raise Skip(f'item requirement "{token}" has no quantity')
    if name.startswith('UNLOCK_'):
        return name

    alternative = {}
    if name.isdigit():
        alternative['anyOf'] = [int(name)]
    elif name in variations:
        ids, staves, offhands = variations[name]
        alternative['anyOf'] = list(ids)
        if staves:
            alternative['staves'] = list(staves)
        if offhands:
            alternative['offhands'] = list(offhands)
    else:
        raise Skip(f'item name "{name}" is not in upstream item-variations.tsv')
    if int(quantity) != 1:
        alternative['quantity'] = int(quantity)
    return alternative


def parse_items(text, variations, notes):
    """Reads an Items cell into a list of requirements that must all hold.

    Returns (item requirements, varbit requirements). Each item requirement is its first
    alternative, with any further ones under "or". An unlock token the client reports becomes
    "waivedByVarbit", or a varbit requirement when no item stands beside it. One the client does not
    report is dropped when an item can stand in for it, and otherwise makes the whole row unusable.
    """
    requirements = []
    waivers = []
    cleaned = text.replace(' ', '').replace('&&', '&').replace('||', '|')
    for group in filter(None, cleaned.split('&')):
        alternatives = []
        waiver = None
        for token in group.split('|'):
            parsed = parse_alternative(token, variations)
            if isinstance(parsed, str):
                if parsed in DETECTABLE_UNLOCKS:
                    waiver = DETECTABLE_UNLOCKS[parsed]
                else:
                    notes.append(f'{parsed} is something the client does not report, so it is not counted')
                continue
            alternatives.append(parsed)

        if not alternatives and waiver is None:
            raise Skip(f'"{group}" needs an account unlock the client does not report')

        if not alternatives:
            # Only account state meets this, so it is a varbit requirement and not an item one.
            waivers.append(waiver)
            continue
        requirement = dict(alternatives[0])
        if len(alternatives) > 1:
            requirement['or'] = alternatives[1:]
        if waiver is not None:
            requirement['waivedByVarbit'] = waiver
        requirements.append(requirement)
    return requirements, waivers


def parse_vars(text):
    requirements = []
    for part in filter(None, (part.strip() for part in text.split(';'))):
        match = VAR_PATTERN.match(part)
        if not match:
            raise Skip(f'var requirement "{part}" could not be read')
        var_id, operator, value = int(match.group(1)), match.group(2), int(match.group(3))
        if operator == '=':
            requirements.append({'id': var_id, 'comparison': 'EQUAL', 'value': value})
        elif operator == '>':
            requirements.append({'id': var_id, 'comparison': 'AT_LEAST', 'value': value + 1})
        elif operator == '<':
            requirements.append({'id': var_id, 'comparison': 'AT_MOST', 'value': value - 1})
        elif operator == '&':
            # Upstream tests a mask: any of these bits is set. A mask of one bit is written as
            # that bit's index, which is the form rows written by hand use.
            if value <= 0:
                raise Skip(f'"{part}" tests no bit')
            if value & (value - 1):
                requirements.append({'id': var_id, 'comparison': 'ANY_BIT_OF', 'value': value})
            else:
                requirements.append({'id': var_id, 'comparison': 'BIT_SET', 'value': value.bit_length() - 1})
        else:
            requirements.append({'id': var_id, 'comparison': 'COOLDOWN_MINUTES', 'value': value})
    return requirements


def parse_requirements(row, env, notes):
    """Reads every requirement column of a row. Items come back apart, under "items"."""
    requirements = parse_skills(row.get('Skills', ''), env['skill_names'], notes)
    quests = parse_quests(row.get('Quests', ''), env['quest_names'])
    if quests:
        requirements['quests'] = quests
    items, waivers = parse_items(row.get('Items', ''), env['variations'], notes)
    if items:
        requirements['items'] = items
    varbits = parse_vars(row.get('Varbits', '')) + waivers
    if varbits:
        requirements['varbits'] = varbits
    var_players = parse_vars(row.get('VarPlayers', ''))
    if var_players:
        requirements['varPlayers'] = var_players
    return requirements


def merge_requirements(first, second):
    """Joins two sets of requirements into one that asks for both."""
    merged = {}
    for source in (first, second):
        for key, value in source.items():
            if key == 'skills':
                skills = merged.setdefault('skills', {})
                for skill, level in value.items():
                    skills[skill] = max(skills.get(skill, 0), level)
            elif key in ('combatLevel', 'totalLevel'):
                merged[key] = value if key not in merged else (-1 if -1 in (merged[key], value) else max(merged[key], value))
            else:
                merged.setdefault(key, [])
                merged[key] = merged[key] + [entry for entry in value if entry not in merged[key]]
    return merged


def ordered(row):
    """Returns a row, a stop or a hub's requirements with its keys in the order the loader writes them."""
    def in_order(value, keys):
        unknown = set(value) - set(keys)
        if unknown:
            raise SystemExit(f'no place in the key order for {sorted(unknown)}')
        return {key: value[key] for key in keys if key in value}

    def item(value):
        value = in_order(value, ITEM_KEYS)
        if 'or' in value:
            value['or'] = [in_order(alternative, ITEM_KEYS) for alternative in value['or']]
        return value

    def requirements(value):
        value = in_order(value, REQUIREMENT_KEYS)
        if 'items' in value:
            value['items'] = [item(entry) for entry in value['items']]
        return value

    row = in_order(row, ROW_KEYS)
    if 'requirements' in row:
        row['requirements'] = requirements(row['requirements'])
    return row


def inside_house(point):
    return POH_X[0] <= point[0] <= POH_X[1] and POH_Y[0] <= point[1] <= POH_Y[1]


def tiles_apart(first, second):
    return max(abs(first[0] - second[0]), abs(first[1] - second[1]))


def as_point(point):
    return {'x': point[0], 'y': point[1], 'plane': point[2]}


def planted_patch(point, patches):
    """Names the spirit tree patch a tile lies on, or None."""
    for name, (x1, y1, x2, y2) in patches.items():
        if x1 <= point[0] <= x2 and y1 <= point[1] <= y2:
            return name
    return None


def duration_of(row, teleport):
    text = row.get('Duration', '')
    duration = int(text) if text.isdigit() else 0
    return max(duration, 1) if teleport else duration


def add_common(bundled, row, duration, requirements):
    """Adds the fields every kind of row writes the same way, leaving out the loader's defaults."""
    if duration != 1:
        bundled['duration'] = duration
    if row.get('Consumable', '') == 'T' or row.get('Consumable', '').lower() == 'yes':
        bundled['consumable'] = True
    if requirements:
        bundled['requirements'] = requirements


def click_target(row, origin, destination, env, notes):
    """Returns the fields that say what a row clicks.

    The row keeps upstream's joined string unless the cache dump confirms an object beside the
    origin that carries the target's name and offers the option. Then the row names the option,
    the target and the id the object is placed under.
    """
    info = row.get(OBJECT_INFO_COLUMN, '')
    if not info:
        # Two tiles the game joins by walking where the collision map does not.
        notes.append('names nothing to click, so it is walked across')
        return {'shape': 'WALK'}

    cache = env['cache']
    match = re.match(r'^(.*\S)\s+(\d+)$', info)
    if cache is None or not match or origin is None:
        if not match:
            notes.append('object info has no id')
        return {'objectInfo': info}

    remainder, entity_id = match.group(1).strip(), int(match.group(2))
    resolved = resolve_object(cache, origin, remainder, entity_id)
    if resolved is None:
        npc = cache.npcs.get(entity_id)
        if npc and (remainder.lower() == npc['name'].lower() or split_object_info(remainder, npc['name'])):
            return {'objectInfo': info}
        notes.append(f'the cache has no object matching "{remainder}" ({entity_id}) within '
                     f'{NAME_SEARCH_RADIUS} tiles of the origin')
        return {'objectInfo': info}

    option, target, placed_id, how = resolved
    if (destination is not None and normalise(option) in CLEARING_OPTIONS and origin[2] == destination[2]
            and tiles_apart(origin, destination) <= TELEPORT_DISTANCE):
        notes.append(f'"{option}" clears an obstacle instead of moving the player across it')
    if how == 'far':
        notes.append(f'the nearest object matching "{remainder}" is more than {ID_SEARCH_RADIUS} tiles from the origin')
        return {'objectInfo': info}

    target_fields = {}
    if option:
        target_fields['menuOption'] = option
    target_fields['menuTarget'] = target
    target_fields['entityId'] = placed_id
    return target_fields


def house_rule(category, row, points):
    """Raises Skip for a row the walker's fixed settings for a player owned house never plan."""
    if not any(point is not None and inside_house(point) for point in points):
        return
    if category in ('FAIRY_RING', 'SPIRIT_TREE', 'WILDERNESS_OBELISK'):
        raise Skip('a fairy ring, spirit tree or obelisk inside a player owned house, which the walker does not plan')
    if category == 'TELEPORTATION_BOX' and 'amulet of glory' in row.get(OBJECT_INFO_COLUMN, '').lower():
        raise Skip('the mounted amulet of glory, which the ornate jewellery box already covers')
    if category == 'TELEPORTATION_BOX' and 'Honour' in row.get('Display info', ''):
        raise Skip('needs Xeric\'s Honour, an account unlock the client does not report')


def landing(point):
    """Moves a tile inside a player owned house to the tile the house is planned through."""
    return POH_LANDING if point is not None and inside_house(point) else point


def respawn_rule(row, destination):
    display = row.get('Display info', '')
    if 'Ring of returning: Prifddinas' in display or (
            destination == PRIFDDINAS_RESPAWN and ('Respawn' in display or 'Ring of returning:' in display)):
        raise Skip('the Prifddinas respawn, which the client cannot tell from the Lumbridge default')


def convert_tile_row(row, category, env, notes):
    """Turns one upstream row with a tile at both ends into a dataset row, or raises Skip."""
    origin = parse_point(row['Origin'])
    destination = parse_point(row['Destination'])
    house_rule(category, row, (origin, destination))
    respawn_rule(row, destination)
    for point in (origin, destination):
        patch = planted_patch(point, env['patches'])
        if patch and category in ('SPIRIT_TREE', 'SEASONAL', 'TELEPORTATION_ITEM'):
            raise Skip(f'the {patch} spirit tree, which the player has to grow first')
    if UNCONFIRMED.search(row['_section']):
        notes.append('upstream marks this section as inferred or unconfirmed')

    requirements = parse_requirements(row, env, notes)
    if category == 'AGILITY_SHORTCUT' and any(
            requirements.get('skills', {}).get(skill, 0) > 1 for skill in ('RANGED', 'STRENGTH')):
        category = 'GRAPPLE_SHORTCUT'
    requirements = merge_requirements(CATEGORY_REQUIREMENTS.get(category, {}), requirements)

    target = click_target(row, origin, destination, env, notes)
    origin, destination = landing(origin), landing(destination)
    if origin == destination:
        raise Skip('starts and ends on the same tile')

    bundled = {'origin': as_point(origin), 'destination': as_point(destination)}
    if category != 'GENERAL':
        bundled['category'] = category
    bundled.update(target)
    if row.get('Display info'):
        bundled['displayInfo'] = row['Display info']
    add_common(bundled, row, duration_of(row, False), requirements)
    return bundled


def convert_teleport(row, category, env, notes):
    """Turns one upstream teleport into a dataset row usable from anywhere, or raises Skip."""
    destination = parse_point(row['Destination'])
    respawn_rule(row, destination)
    patch = planted_patch(destination, env['patches'])
    if patch and category in ('SEASONAL', 'TELEPORTATION_ITEM'):
        raise Skip(f'the {patch} spirit tree, which the player has to grow first')
    if UNCONFIRMED.search(row['_section']):
        notes.append('upstream marks this section as inferred or unconfirmed')

    wilderness = row.get('Wilderness level', '')
    if not wilderness.isdigit():
        raise Skip('states no wilderness level, so it works nowhere')

    display = row.get('Display info', '')
    requirements = parse_requirements(row, env, notes)
    bundled = {'destination': as_point(landing(destination)), 'category': category}

    if category in ITEM_TELEPORT_CATEGORIES:
        # The first item requirement names the item that carries the teleport. A definition
        # requires one of its item ids to be held by itself, so that requirement is only written
        # out when it asks for more than that.
        items = requirements.pop('items', [])
        if not items or not items[0]['anyOf']:
            raise Skip('names no item that carries the teleport')
        carrier = items[0]
        bundled['itemIds'] = [item_id for alternative in [carrier] + carrier.get('or', [])
                              for item_id in alternative['anyOf']]
        if set(carrier) - {'anyOf', 'or'} or any(set(alternative) - {'anyOf'} for alternative in carrier.get('or', [])):
            requirements['items'] = items
        elif len(items) > 1:
            requirements['items'] = items[1:]
        # Display info reads "<item>: <stop>". Only the stop is something to choose.
        stop = display.split(':', 1)[1].strip() if ':' in display else ''
        if stop:
            bundled['displayInfo'] = stop
    elif display:
        bundled['displayInfo'] = display
    elif category in ('TELEPORTATION_SPELL', 'TELEPORTATION_SPELL_HOME'):
        raise Skip('names no spell')

    if int(wilderness) != 20:
        bundled['maxWildernessLevel'] = int(wilderness)
    add_common(bundled, row, duration_of(row, True), requirements)
    return bundled


def convert_stop(row, category, env, notes):
    """Turns one stop of a hub network into (kind, stop), or raises Skip."""
    is_origin = bool(row.get('Origin'))
    point = parse_point(row['Origin'] if is_origin else row['Destination'])
    house_rule(category, row, (point,))
    patch = planted_patch(point, env['patches'])
    if patch and category == 'SPIRIT_TREE':
        raise Skip(f'the {patch} spirit tree, which the player has to grow first')
    if UNCONFIRMED.search(row['_section']):
        notes.append('upstream marks this section as inferred or unconfirmed')

    requirements = parse_requirements(row, env, notes)
    stop = {('origin' if is_origin else 'destination'): as_point(landing(point))}
    if is_origin:
        target = click_target(row, point, None, env, notes)
        if 'shape' in target:
            raise Skip('a stop that names nothing to click cannot be travelled from')
        stop.update(target)
    if row.get('Display info'):
        stop['displayInfo'] = row['Display info']
    add_common(stop, row, duration_of(row, False), requirements)
    return ('origins' if is_origin else 'destinations'), stop


def slug(text):
    return re.sub(r'[^a-z0-9]+', '-', text.lower()).strip('-')


def class_constants(javap, jar, class_name):
    """Lists the names of a class's public static final fields of its own type, such as enum constants."""
    output = subprocess.run([javap, '-cp', jar, class_name], check=True, capture_output=True, text=True).stdout
    return set(re.findall(r'public static final ' + re.escape(class_name) + r' (\w+);', output))


def find_javap():
    home = os.environ.get('JAVA_HOME')
    if home and os.path.exists(os.path.join(home, 'bin', 'javap' + ('.exe' if os.name == 'nt' else ''))):
        return os.path.join(home, 'bin', 'javap')
    found = shutil.which('javap')
    if not found:
        raise SystemExit('javap is not on the path; set JAVA_HOME to a JDK')
    return found


def write_dataset(path, source, transports, hubs):
    """Writes the dataset with one row per line, so a regenerated file diffs row by row."""
    def line(value):
        return json.dumps(value, separators=(', ', ': '))

    with open(path, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('{\n')
        handle.write(f'  "source": {line(source)},\n')
        handle.write('  "transports": [\n')
        handle.write(',\n'.join('    ' + line(row) for row in transports))
        handle.write('\n  ],\n')
        handle.write('  "hubs": [\n')
        for index, hub in enumerate(hubs):
            handle.write('    {\n')
            for key in ('id', 'category', 'minDistance', 'requirements'):
                if key in hub:
                    handle.write(f'      {line(key)}: {line(hub[key])},\n')
            for key in ('origins', 'destinations'):
                handle.write(f'      {line(key)}: [\n')
                handle.write(',\n'.join('        ' + line(stop) for stop in hub[key]))
                handle.write('\n      ]' + (',\n' if key == 'origins' else '\n'))
            handle.write('    }' + (',\n' if index < len(hubs) - 1 else '\n'))
        handle.write('  ]\n')
        handle.write('}\n')


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('upstream', help='a checkout of upstream shortest-path')
    parser.add_argument('--cache-dump', help='directory written by scripts/WalkerCacheDump.java')
    parser.add_argument('--runelite-api-jar')
    parser.add_argument('--output', default=DEFAULT_OUTPUT)
    parser.add_argument('--collision-map', default=DEFAULT_COLLISION_MAP)
    parser.add_argument('--report', default=DEFAULT_REPORT)
    arguments = parser.parse_args()

    api_jar = arguments.runelite_api_jar or find_in_gradle_cache(
        'net.runelite', 'runelite-api', gradle_setting(r"runeLiteVersion\s*=\s*'([^']+)'"))
    javap = find_javap()
    quest_names = class_constants(javap, api_jar, 'net.runelite.api.Quest')
    skill_names = class_constants(javap, api_jar, 'net.runelite.api.Skill') - {'OVERALL'}
    if not quest_names or not skill_names:
        raise SystemExit(f'could not read the Quest and Skill enums from {api_jar}')

    resources = os.path.join(arguments.upstream, 'src', 'main', 'resources')
    variations = {}
    for line in read_text_lines(os.path.join(resources, 'item-variations.tsv')):
        if line and not line.startswith('#'):
            cells = line.split('\t') + ['', '']
            variations[cells[0].upper()] = tuple(
                [int(value) for value in cell.split(',') if value] for cell in cells[1:4])

    patches = {}
    for line in read_text_lines(os.path.join(resources, 'spirit-tree-patches.tsv')):
        if line and not line.startswith('#'):
            cells = line.split('\t')
            patches[cells[0]] = tuple(int(value) for value in cells[3:7])

    def upstream_rows(file_name):
        return read_tsv(read_text_lines(os.path.join(resources, 'transports', file_name)), file_name)

    # Every destination one click on one object leads to under the same requirements, counted
    # over all rows of a file. One click that leads to two places needs something to choose with.
    def ambiguous_clicks(rows):
        destinations = collections.defaultdict(set)
        for row in rows:
            key = (row.get('Origin', ''), row.get(OBJECT_INFO_COLUMN, ''),
                   tuple(row.get(column, '') for column in ('Skills', 'Items', 'Quests', 'Varbits', 'VarPlayers')))
            destinations[key].add(row.get('Destination', ''))
        return {key for key, found in destinations.items() if len(found) > 1}

    env = {
        'cache': Cache(arguments.cache_dump) if arguments.cache_dump else None,
        'variations': variations,
        'patches': patches,
        'quest_names': quest_names,
        'skill_names': skill_names,
    }

    transports = []
    hubs = []
    report = []
    counts = collections.Counter()
    used_ids = collections.Counter()

    def describe(row):
        return row.get(OBJECT_INFO_COLUMN, '') or row.get('Display info', '')

    def record(row, outcome, text):
        report.append((row['_file'], row['_line'], outcome, row.get('Origin', ''), row.get('Destination', ''),
                       describe(row), text))

    def consider(row, convert, category):
        """Converts one row, and returns it or None when it is left out."""
        notes = []
        try:
            converted = convert(row, category, env, notes)
        except Skip as skip:
            counts[f'{row["_file"]}: left out'] += 1
            record(row, 'left out', str(skip))
            return None
        for note in notes:
            record(row, 'note', note)
        return converted

    def unique_id(key):
        used_ids[key] += 1
        return f'{GENERATED_ID_PREFIX}{key}' + ('' if used_ids[key] == 1 else f'-{used_ids[key]}')

    for file_name in sorted(set(TILE_SOURCES) | set(HUB_SOURCES) | set(TELEPORT_SOURCES)):
        header, rows = upstream_rows(file_name)
        stem = file_name[:-len('.tsv')].replace('_', '-')

        if file_name in TELEPORT_SOURCES:
            category = TELEPORT_SOURCES[file_name]
            for row in rows:
                bundled = consider(row, convert_teleport, category)
                if bundled is None:
                    continue
                # The first teleport file written kept this shorter key, and ids stay as they were.
                key_stem = 'item' if file_name == 'teleportation_items.tsv' else stem
                key = f'{key_stem}-{slug(row.get("Display info", ""))}-{row["Destination"].replace(" ", "-")}'
                transports.append(ordered({'id': unique_id(key), **bundled}))
                counts[f'{file_name}: written'] += 1
            continue

        ambiguous = ambiguous_clicks(rows)
        stops = {'origins': [], 'destinations': []}
        for row in rows:
            has_origin, has_destination = bool(row.get('Origin')), bool(row.get('Destination'))
            if has_origin and has_destination:
                category = TILE_SOURCES.get(file_name) or HUB_SOURCES[file_name][0]
                bundled = consider(row, convert_tile_row, category)
                if bundled is None:
                    continue
                click = (row['Origin'], row.get(OBJECT_INFO_COLUMN, ''),
                         tuple(row.get(column, '') for column in ('Skills', 'Items', 'Quests', 'Varbits', 'VarPlayers')))
                if click in ambiguous and not row.get('Display info'):
                    record(row, 'note', 'the same click leads to several destinations and the row names none to choose')
                key = f'{stem}-{row["Origin"].replace(" ", "-")}-to-{row["Destination"].replace(" ", "-")}'
                transports.append(ordered({'id': unique_id(key), **bundled}))
                counts[f'{file_name}: written ({"object checked" if "entityId" in bundled else "as upstream"})'] += 1
            elif file_name in HUB_SOURCES and (has_origin or has_destination):
                converted = consider(row, convert_stop, HUB_SOURCES[file_name][0])
                if converted is not None:
                    stops[converted[0]].append(ordered(converted[1]))
                    counts[f'{file_name}: stops'] += 1
            else:
                counts[f'{file_name}: left out'] += 1
                record(row, 'left out', 'has neither an origin nor a destination')

        if stops['origins'] and stops['destinations']:
            category, min_distance = HUB_SOURCES[file_name]
            hub = {'id': f'{GENERATED_ID_PREFIX}{stem}', 'category': category}
            if min_distance:
                hub['minDistance'] = min_distance
            requirements = dict(CATEGORY_REQUIREMENTS.get(category, {}))
            if category == 'FAIRY_RING':
                requirements['items'] = [{'anyOf': list(variations['DRAMEN_STAFF'][0]), **FAIRY_RING_STAFF}]
            if requirements:
                hub['requirements'] = ordered({'requirements': requirements})['requirements']
            hub.update(stops)
            hubs.append(hub)

    commit = subprocess.run(['git', '-C', arguments.upstream, 'rev-parse', 'HEAD'],
                            capture_output=True, text=True).stdout.strip()
    source = UPSTREAM_URL + (f' at {commit}' if commit else '')

    os.makedirs(os.path.dirname(arguments.output), exist_ok=True)
    write_dataset(arguments.output, source, transports, hubs)
    shutil.copyfile(os.path.join(resources, 'collision-map.zip'), arguments.collision_map)

    os.makedirs(os.path.dirname(arguments.report), exist_ok=True)
    with open(arguments.report, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('# file\tline\toutcome\torigin\tdestination\tobject or display info\ttext\n')
        for entry in report:
            handle.write('\t'.join(str(cell) for cell in entry) + '\n')

    for label in sorted(counts):
        print(f'{counts[label]:6d}  {label}')
    for outcome in ('left out', 'note'):
        print(f'{outcome}:')
        reasons = collections.Counter(re.sub(r'"[^"]*"|\(\d+\)|\d+', 'N', entry[6])[:100]
                                      for entry in report if entry[2] == outcome)
        for reason, count in reasons.most_common(20):
            print(f'{count:6d}  {reason}')
    pairs = sum(len(hub['origins']) * len(hub['destinations']) for hub in hubs)
    print(f'wrote {len(transports)} transports and {len(hubs)} hub networks (at most {pairs} pairs) to {arguments.output}')
    print(f'copied the collision map to {arguments.collision_map}')
    print(f'wrote {len(report)} notes to {arguments.report}')


if __name__ == '__main__':
    sys.exit(main())
