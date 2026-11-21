#!/usr/bin/env python3
"""Regenerates the bundled walker transports in src/main/resources/walker/transports.json.

The walker plans with a pinned fork of the shortest-path plugin, so its transport data is as old
as that pin. This script reads a newer checkout of upstream shortest-path, finds the transports
the pinned fork does not have, converts the ones the bundled format can express, and keeps only
the rows that a dump of the current game cache confirms: the object has to stand beside the origin
tile and offer the menu action.

    python3 scripts/update_walker_transports.py <shortest-path checkout> --cache-dump <dump dir>

Inputs:
  <shortest-path checkout>  a clone of https://github.com/Skretzo/shortest-path, read as data only
  --cache-dump              the directory scripts/WalkerCacheDump.java writes (objects.tsv,
                            npcs.tsv, items.tsv, locs.tsv, bridges.tsv)
  --pinned-jar              the pinned fork's jar; found in the Gradle cache from build.gradle
                            when not given
  --runelite-api-jar        the RuneLite API jar, used to check quest names; found in the Gradle
                            cache from build.gradle when not given
  --output                  where to write the rows (default: the bundled resource)
  --report                  where to write why each other row was left out
                            (default: build/walker-transports-report.tsv)

Only transports that one click operates are written: a click on a scene object that moves the
player, or a teleport item whose menu the cache lists. What is left out is listed in the report
with its reason:
  * hub networks (fairy rings, spirit trees, gliders, charter ships and the like), whose rows are
    stops that the planner expands into every pair, and anything inside a player owned house
  * boats, ships and anything operated through an NPC, because they need a conversation the cache
    cannot confirm, and the cache does not say where an NPC stands
  * objects that offer several destinations from one click, which need a chooser
  * obstacles that are cleared instead of crossed (rockfalls, locks, levers), which the walker's
    single click handler cannot judge
  * rows whose requirements the bundled format cannot state (unlock tokens, cooldowns, alternatives
    with different quantities)
  * rows upstream marks as inferred or unconfirmed
  * rows in map regions the pinned fork's collision map does not cover
  * rows the cache dump does not confirm
  * item teleports the pinned fork already has under another name or a few tiles away

Every row this script writes has an id starting with "kraken:upstream-". Rows in the output file
with any other id were written by hand or by another tool, and are kept as they are.

Run ./gradlew test afterwards: BundledTransportsTest fails when a written row does not load.
"""
import argparse
import collections
import glob
import io
import json
import os
import re
import sys
import zipfile

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_OUTPUT = os.path.join(REPO_ROOT, 'src', 'main', 'resources', 'walker', 'transports.json')
DEFAULT_REPORT = os.path.join(REPO_ROOT, 'build', 'walker-transports-report.tsv')
GRADLE_MODULES = os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'modules-2', 'files-2.1')

# Upstream files whose rows are one transport each with a tile at both ends, and the category and
# shape their rows get. Anything not listed is a hub network, a player owned house file or a
# teleport file, which are handled elsewhere or not at all.
TILE_SOURCES = {
    'transports.tsv': ('GENERAL', 'SINGLE_CLICK'),
    'agility_shortcuts.tsv': ('AGILITY_SHORTCUT', 'SINGLE_CLICK'),
    'teleportation_portals.tsv': ('TELEPORTATION_PORTAL', 'SINGLE_CLICK'),
    'teleportation_levers.tsv': ('TELEPORTATION_LEVER', 'SINGLE_CLICK'),
}

# Upstream files that are read only to say why their rows are left out.
CONVERSATION_SOURCES = ('boats.tsv', 'ships.tsv')

# Starts the id of every row this script writes, which is how a rerun tells its own rows from
# rows that were added to the file some other way.
GENERATED_ID_PREFIX = 'kraken:upstream-'

# The upstream file of teleports an item gives from anywhere.
ITEM_TELEPORT_SOURCE = 'teleportation_items.tsv'

OBJECT_INFO_COLUMN = 'menuOption menuTarget objectID'

# Section comments upstream uses for rows nobody has checked in game yet.
UNCONFIRMED = re.compile(
    r'inferred|not 100% confirmed|confirm with|confirm landings|replace with captured|assumes the', re.I)

# The walker looks for a transport's object by id this close to the origin tile, then by name
# within the wider radius. See TransportEntityResolver.
ID_SEARCH_RADIUS = 4
NAME_SEARCH_RADIUS = 10

# The menu actions ItemTeleportHandler tries on a teleport item when a row names none.
ITEM_TELEPORT_MENUS = ('Rub', 'Teleport', 'Operate', 'Break', 'Invoke', 'Play')

# Menu options that clear an obstacle and leave the player standing where they were. The walker's
# single click handler waits for the player to move, so over a short distance it would time out.
CLEARING_OPTIONS = {normalised for normalised in ('mine', 'picklock', 'pullout', 'pull', 'fireat', 'chopdown', 'dodge')}

# How far a crossing has to carry the player before the walker counts it as a teleport, which
# needs no judgement about what happened to an obstacle. See TransportArrival.
TELEPORT_DISTANCE = 16

# How close two landing tiles have to be to count as the same teleport written twice.
SAME_LANDING_TILES = 10

# Bounds of the player owned house area, in the pinned fork and in upstream, which moved it. The
# planner treats the house specially.
POH_X = (1856, 2047)
POH_Y_RANGES = ((5696, 5767), (7040, 7111))

VAR_PATTERN = re.compile(r'^\s*(\d+)\s*([=<>&@])\s*(-?\d+)\s*$')


class Skip(Exception):
    """Raised with the reason a row is left out of the bundled file."""


def read_tsv(lines, file_name):
    """Reads a shortest-path transport file into one dict per row, keyed by the header's names."""
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
        cells = line.split('\t')
        row = {header[i]: (cells[i].strip() if i < len(cells) else '') for i in range(len(header))}
        row['_file'] = file_name
        row['_line'] = number
        row['_section'] = section or ''
        rows.append(row)
    return rows


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

        self.items = {}
        for cells in self._rows(directory, 'items.tsv'):
            sub_actions = []
            for group in filter(None, cells[7].split('|') if len(cells) > 7 else []):
                sub_actions.extend(filter(None, group.split(':', 1)[1].split(';')))
            self.items[int(cells[0])] = {
                'name': cells[1],
                'actions': [action for action in cells[2].split('|') if action],
                'subActions': sub_actions,
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
    the walker's id search radius. When nothing with that id stands there, an object within the
    same radius that carries the row's target name and offers its option is taken instead. That is
    the object the walker would fall back to in game, and the row is written with its id.

    An object that only matches further out, up to the walker's name search radius, is reported as
    "far" so the caller can leave the row out: at that distance another object of the same name is
    too easy to mistake for it.
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


def parse_skills(text):
    skills = {}
    for part in filter(None, (part.strip() for part in text.split(';'))):
        match = re.match(r'^(\d+)\s+(.+)$', part)
        if not match:
            raise Skip(f'skill requirement "{part}" is not a level and a skill')
        name = match.group(2).strip().upper()
        if not re.match(r'^[A-Z]+$', name) or name in ('TOTAL', 'COMBAT', 'QUEST'):
            raise Skip(f'"{part}" is not a level in one skill')
        skills[name] = int(match.group(1))
    return skills


def quest_constant(name):
    """Turns a quest's display name into the name of its constant in RuneLite's Quest enum."""
    stripped = re.sub(r"['&\-.!,()]", '', name.upper())
    return re.sub(r'[^A-Z0-9]', '_', stripped)


def parse_quests(text, quest_class):
    quests = []
    for name in filter(None, (part.strip() for part in text.split(';'))):
        constant = quest_constant(name)
        if constant.encode() not in quest_class or name.encode() not in quest_class:
            raise Skip(f'quest "{name}" is not in RuneLite\'s Quest enum as {constant}')
        quests.append(constant)
    return quests


def parse_items(text, variations):
    """Reads an Items cell into a list of {anyOf, quantity} requirements that must all hold."""
    requirements = []
    cleaned = text.replace(' ', '').replace('&&', '&').replace('||', '|')
    for group in filter(None, cleaned.split('&')):
        ids = []
        quantities = set()
        for alternative in group.split('|'):
            if '=' not in alternative:
                raise Skip(f'item requirement "{alternative}" has no quantity')
            name, quantity = alternative.split('=', 1)
            if name.upper().startswith('UNLOCK_'):
                raise Skip(f'needs an account unlock ({name}), which the bundled format cannot state')
            if name.isdigit():
                ids.append(int(name))
            elif name.upper() in variations:
                ids.extend(variations[name.upper()])
            else:
                raise Skip(f'item name "{name}" is not in upstream item-variations.tsv')
            quantities.add(int(quantity))
        if len(quantities) != 1:
            raise Skip(f'alternatives in "{group}" need different quantities')
        quantity = quantities.pop()
        if quantity < 1:
            raise Skip(f'"{group}" requires an item to be absent')
        requirement = {'anyOf': list(dict.fromkeys(ids))}
        if quantity != 1:
            requirement['quantity'] = quantity
        requirements.append(requirement)
    return requirements


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
            # Upstream tests a mask, "any of these bits is set". The bundled format names one bit
            # by its index, so only a mask with a single bit can be stated.
            if value <= 0 or value & (value - 1):
                raise Skip(f'"{part}" tests several bits at once, which the bundled format cannot state')
            requirements.append({'id': var_id, 'comparison': 'BIT_SET', 'value': value.bit_length() - 1})
        else:
            raise Skip('needs a cooldown check, which the bundled format cannot state')
    return requirements


def parse_requirements(row, variations, quest_class, skip_items=False):
    requirements = {}
    skills = parse_skills(row.get('Skills', ''))
    if skills:
        requirements['skills'] = skills
    quests = parse_quests(row.get('Quests', ''), quest_class)
    if quests:
        requirements['quests'] = quests
    if not skip_items:
        items = parse_items(row.get('Items', ''), variations)
        if items:
            requirements['items'] = items
    varbits = parse_vars(row.get('Varbits', ''))
    if varbits:
        requirements['varbits'] = varbits
    var_players = parse_vars(row.get('VarPlayers', ''))
    if var_players:
        requirements['varPlayers'] = var_players
    return requirements


def inside_house(point):
    return POH_X[0] <= point[0] <= POH_X[1] and any(low <= point[1] <= high for low, high in POH_Y_RANGES)


def tiles_apart(first, second):
    return max(abs(first[0] - second[0]), abs(first[1] - second[1]))


def as_point(point):
    return {'x': point[0], 'y': point[1], 'plane': point[2]}


def convert_tile_row(row, env):
    """Turns one upstream row with a tile at both ends into a bundled row, or raises Skip."""
    if row['_file'] in CONVERSATION_SOURCES:
        raise Skip('needs a conversation, which the cache cannot confirm')

    cache = env['cache']
    category, shape = TILE_SOURCES[row['_file']]
    origin = parse_point(row['Origin'])
    destination = parse_point(row['Destination'])

    if UNCONFIRMED.search(row['_section']):
        raise Skip('upstream marks this section as inferred or unconfirmed')
    if inside_house(origin) or inside_house(destination):
        raise Skip('inside a player owned house')
    for point in (origin, destination):
        if (point[0] >> 6, point[1] >> 6) not in env['regions']:
            raise Skip(f'the pinned collision map has no region for {point}')

    info = row.get(OBJECT_INFO_COLUMN, '')
    match = re.match(r'^(.*\S)\s+(\d+)$', info)
    if not match:
        raise Skip('names nothing to click' if not info else 'object info has no id')
    remainder, entity_id = match.group(1).strip(), int(match.group(2))

    if len(env['destinations_by_click'][(row['Origin'], info)]) > 1:
        raise Skip('the same click leads to several destinations, so it needs a chooser')

    requirements = parse_requirements(row, env['variations'], env['quest_class'])

    resolved = resolve_object(cache, origin, remainder, entity_id)
    if resolved is None:
        npc = cache.npcs.get(entity_id)
        if npc and (remainder.lower() == npc['name'].lower() or split_object_info(remainder, npc['name'])):
            raise Skip('operated through an NPC, which the cache cannot place on the map')
        if entity_id not in cache.objects:
            raise Skip(f'object {entity_id} is not in the cache')
        raise Skip(f'no object matching "{remainder}" ({entity_id}) offers that action within '
                   f'{NAME_SEARCH_RADIUS} tiles of the origin')
    option, target, placed_id, how = resolved
    if how == 'far':
        raise Skip(f'the nearest object matching "{remainder}" is more than {ID_SEARCH_RADIUS} tiles from the origin')
    if (normalise(option) in CLEARING_OPTIONS and origin[2] == destination[2]
            and tiles_apart(origin, destination) <= TELEPORT_DISTANCE):
        raise Skip(f'"{option}" clears an obstacle instead of moving the player across it')

    if category == 'AGILITY_SHORTCUT' and any(skill in requirements.get('skills', {}) for skill in ('RANGED', 'STRENGTH')):
        category = 'GRAPPLE_SHORTCUT'

    bundled = {'origin': as_point(origin), 'destination': as_point(destination)}
    if category != 'GENERAL':
        bundled['category'] = category
    bundled['shape'] = shape
    if option:
        bundled['menuOption'] = option
    bundled['menuTarget'] = target
    bundled['entityId'] = placed_id
    if row.get('Display info'):
        bundled['displayInfo'] = row['Display info']
    duration = row.get('Duration', '')
    if duration.isdigit() and int(duration) > 1:
        bundled['duration'] = int(duration)
    if requirements:
        bundled['requirements'] = requirements
    return bundled, how


def item_teleport_menu(cache, item_ids, stop):
    """Checks that ItemTeleportHandler can work a teleport from the menus the cache lists.

    Returns the menu option to state in the row, or None when the handler's own candidates do.
    Every listed item has to offer the stop directly, or one of the usual teleport actions.
    """
    def among(wanted, actions):
        return any(normalise(action) == normalise(wanted) for action in actions)

    for item_id in item_ids:
        item = cache.items.get(item_id)
        if item is None:
            raise Skip(f'item {item_id} is not in the cache')
        has_menu = any(among(menu, item['actions']) for menu in ITEM_TELEPORT_MENUS)
        if not stop:
            if not has_menu:
                raise Skip(f'item {item_id} ({item["name"]}) offers none of the usual teleport actions')
        elif not among(stop, item['actions']) and not (has_menu and among(stop, item['subActions'])):
            raise Skip(f'item {item_id} ({item["name"]}) does not list a stop called "{stop}"')
    return None


def item_ids_named(text, variations):
    """Lists every item id an Items cell mentions, whatever the syntax around them."""
    ids = set()
    for token in re.split(r'[&|]', text.replace(' ', '')):
        name = token.split('=', 1)[0].upper()
        if name.isdigit():
            ids.add(int(name))
        else:
            ids.update(variations.get(name, ()))
    return ids


def already_pinned_teleport(row, env):
    """Reports whether the pinned fork has an item teleport that lands where this row does.

    Upstream renames teleports and moves their landing tiles by a tile or two. A row that shares
    an item with a pinned teleport and lands beside it is that teleport again, and writing it
    would only put an unproven copy in front of the dataset's own.
    """
    if len(row.get('Destination', '').split()) != 3:
        return False
    destination = parse_point(row['Destination'])
    return any(tiles_apart(destination, landing) <= SAME_LANDING_TILES
               for item_id in item_ids_named(row.get('Items', ''), env['variations'])
               for landing in env['pinned_landings'].get(item_id, ()))


def convert_item_teleport(row, env):
    """Turns one upstream item teleport into a bundled row usable from anywhere, or raises Skip."""
    cache, variations = env['cache'], env['variations']
    destination = parse_point(row['Destination'])
    if UNCONFIRMED.search(row['_section']):
        raise Skip('upstream marks this section as inferred or unconfirmed')
    if inside_house(destination):
        raise Skip('lands inside a player owned house')
    if (destination[0] >> 6, destination[1] >> 6) not in env['regions']:
        raise Skip(f'the pinned collision map has no region for {destination}')

    items = parse_items(row.get('Items', ''), variations)
    if len(items) != 1 or items[0].get('quantity', 1) != 1:
        raise Skip('needs more than the teleport item itself, which a bundled item teleport cannot state apart')
    item_ids = items[0]['anyOf']

    display = row.get('Display info', '')
    if not display:
        raise Skip('has no display info to name the stop')
    stop = display.split(':', 1)[1].strip() if ':' in display else ''
    if not stop and any(tiles_apart(destination, landing) > SAME_LANDING_TILES
                        for item_id in item_ids for landing in env['upstream_landings'][(item_id, '')]):
        raise Skip('the item has several destinations and the row names no stop to choose')
    item_teleport_menu(cache, item_ids, stop)

    requirements = parse_requirements(row, variations, env['quest_class'], skip_items=True)
    wilderness = row.get('Wilderness level', '')
    if not wilderness.isdigit():
        raise Skip('states no wilderness level')

    bundled = {'destination': as_point(destination), 'shape': 'ITEM_SUBOP', 'itemIds': item_ids}
    if stop:
        bundled['displayInfo'] = stop
    duration = row.get('Duration', '')
    if duration.isdigit() and int(duration) > 1:
        bundled['duration'] = int(duration)
    if int(wilderness) != 20:
        bundled['maxWildernessLevel'] = int(wilderness)
    if requirements:
        bundled['requirements'] = requirements
    return bundled, 'item'


def slug(text):
    return re.sub(r'[^a-z0-9]+', '-', text.lower()).strip('-')


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('upstream', help='a checkout of upstream shortest-path')
    parser.add_argument('--cache-dump', required=True, help='directory written by scripts/WalkerCacheDump.java')
    parser.add_argument('--pinned-jar')
    parser.add_argument('--runelite-api-jar')
    parser.add_argument('--output', default=DEFAULT_OUTPUT)
    parser.add_argument('--report', default=DEFAULT_REPORT)
    arguments = parser.parse_args()

    pinned_jar = arguments.pinned_jar or find_in_gradle_cache(
        'com.github.Kraken-Plugins', 'shortest-path',
        gradle_setting(r"com\.github\.Kraken-Plugins:shortest-path:([0-9a-f]+)"))
    api_jar = arguments.runelite_api_jar or find_in_gradle_cache(
        'net.runelite', 'runelite-api', gradle_setting(r"runeLiteVersion\s*=\s*'([^']+)'"))

    with zipfile.ZipFile(api_jar) as jar:
        quest_class = jar.read('net/runelite/api/Quest.class')

    resources = os.path.join(arguments.upstream, 'src', 'main', 'resources')
    variations = {}
    for line in read_text_lines(os.path.join(resources, 'item-variations.tsv')):
        if line and not line.startswith('#'):
            cells = line.split('\t')
            variations[cells[0].upper()] = [int(value) for value in cells[1].split(',') if value]

    # What the pinned fork already has: every pair of tiles, and where each teleport item lands.
    pinned_edges = set()
    pinned_landings = collections.defaultdict(list)
    with zipfile.ZipFile(pinned_jar) as jar:
        for name in jar.namelist():
            if name.startswith('transports/') and name.endswith('.tsv'):
                for row in read_tsv(jar.read(name).decode('utf-8').splitlines(), os.path.basename(name)):
                    pinned_edges.add((row.get('Origin', ''), row.get('Destination', '')))
                    if not row.get('Origin') and len(row.get('Destination', '').split()) == 3:
                        for item_id in item_ids_named(row.get('Items', ''), variations):
                            pinned_landings[item_id].append(parse_point(row['Destination']))
        with zipfile.ZipFile(io.BytesIO(jar.read('collision-map.zip'))) as collision:
            regions = {tuple(int(part) for part in name.split('_')) for name in collision.namelist()}

    def upstream_rows(file_name):
        return read_tsv(read_text_lines(os.path.join(resources, 'transports', file_name)), file_name)

    # Every destination one click on one object leads to, and every landing an item has per stop,
    # counted over all upstream rows so that a choice is noticed even when only one branch is new.
    destinations_by_click = collections.defaultdict(set)
    for file_name in TILE_SOURCES:
        for row in upstream_rows(file_name):
            destinations_by_click[(row.get('Origin', ''), row.get(OBJECT_INFO_COLUMN, ''))].add(row.get('Destination', ''))
    upstream_landings = collections.defaultdict(list)
    for row in upstream_rows(ITEM_TELEPORT_SOURCE):
        if len(row.get('Destination', '').split()) != 3:
            continue
        display = row.get('Display info', '')
        stop = display.split(':', 1)[1].strip() if ':' in display else ''
        for item_id in item_ids_named(row.get('Items', ''), variations):
            upstream_landings[(item_id, stop)].append(parse_point(row['Destination']))

    env = {
        'cache': Cache(arguments.cache_dump),
        'variations': variations,
        'quest_class': quest_class,
        'regions': regions,
        'destinations_by_click': destinations_by_click,
        'pinned_landings': pinned_landings,
        'upstream_landings': upstream_landings,
    }

    bundled_rows = []
    report = []
    counts = collections.Counter()
    used_ids = collections.Counter()

    def consider(row, convert, key):
        try:
            bundled, how = convert(row, env)
        except Skip as skip:
            counts[f'{row["_file"]}: left out'] += 1
            report.append((row['_file'], row['_line'], row.get('Origin', ''), row.get('Destination', ''),
                           row.get(OBJECT_INFO_COLUMN, '') or row.get('Display info', ''), str(skip)))
            return
        used_ids[key] += 1
        suffix = '' if used_ids[key] == 1 else f'-{used_ids[key]}'
        bundled_rows.append({'id': f'{GENERATED_ID_PREFIX}{key}{suffix}', **bundled})
        counts[f'{row["_file"]}: written ({how})'] += 1

    for file_name in list(TILE_SOURCES) + list(CONVERSATION_SOURCES):
        for row in upstream_rows(file_name):
            if not row.get('Origin') or not row.get('Destination'):
                continue
            if (row['Origin'], row['Destination']) in pinned_edges:
                continue
            stem = file_name[:-len('.tsv')].replace('_', '-')
            key = f'{stem}-{row["Origin"].replace(" ", "-")}-to-{row["Destination"].replace(" ", "-")}'
            consider(row, convert_tile_row, key)

    for row in upstream_rows(ITEM_TELEPORT_SOURCE):
        if already_pinned_teleport(row, env):
            continue
        key = f'item-{slug(row.get("Display info", ""))}-{row.get("Destination", "").replace(" ", "-")}'
        consider(row, convert_item_teleport, key)

    kept_rows = []
    if os.path.exists(arguments.output):
        with open(arguments.output, encoding='utf-8') as handle:
            kept_rows = [row for row in json.load(handle)
                         if not str(row.get('id', '')).startswith(GENERATED_ID_PREFIX)]
    all_rows = kept_rows + bundled_rows

    os.makedirs(os.path.dirname(arguments.output), exist_ok=True)
    with open(arguments.output, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('[\n')
        handle.write(',\n'.join('  ' + json.dumps(row, separators=(', ', ': ')) for row in all_rows))
        handle.write('\n]\n' if all_rows else ']\n')

    os.makedirs(os.path.dirname(arguments.report), exist_ok=True)
    with open(arguments.report, 'w', encoding='utf-8', newline='\n') as handle:
        handle.write('# file\tline\torigin\tdestination\tobject or display info\treason\n')
        for entry in report:
            handle.write('\t'.join(str(cell) for cell in entry) + '\n')

    for label in sorted(counts):
        print(f'{counts[label]:6d}  {label}')
    reasons = collections.Counter(re.sub(r'\d+', 'N', entry[5])[:90] for entry in report)
    print('reasons rows were left out:')
    for reason, count in reasons.most_common(25):
        print(f'{count:6d}  {reason}')
    print(f'wrote {len(bundled_rows)} rows to {arguments.output}, beside {len(kept_rows)} kept rows')
    print(f'wrote {len(report)} left out rows to {arguments.report}')


if __name__ == '__main__':
    sys.exit(main())
