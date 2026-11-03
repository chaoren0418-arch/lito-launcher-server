"""Classify all retained rows and locate current archive paths; no game or DB execution."""
from collections import Counter, defaultdict, deque
from datetime import datetime, timezone
from pathlib import Path
import csv
import json
import os
import re
import shutil
import subprocess
from build_development_closeout import ROOT, SERVER, JAVA, SEED, checked, pin, write


def table(path):
    with path.open(encoding='utf-8', newline='') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


def main():
    current = json.loads((ROOT / 'build/development-skill-effects-current.json').read_text('utf-8'))
    base = checked(current['runtime']); checked(current['manifest'])
    historical = json.loads((ROOT / 'build/all-targeted-source-paths-current.json').read_text('utf-8'))
    history = json.loads(checked(historical['manifest']).read_text('utf-8'))
    rows = table(checked(history['matrix']))
    if len(rows) != 807: raise ValueError('Review changed Source row inventory')
    run = ROOT / 'build/development-skill-inventory' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    inputs = run / 'inputs'; inputs.mkdir(parents=True)
    for p in [Path(__file__), ROOT / 'CloseoutSkillCensus.java', ROOT / 'build_development_closeout.py']:
        shutil.copyfile(p, inputs / p.name)
    source = []
    for entry in history['Source']:
        original = Path(entry['original']['path'])
        if original.name not in ['C_UseSkill.java', 'L1SkillId.java', 'L1SkillUse.java', 'L1SkillMode.java']: continue
        frozen = inputs / original.name; shutil.copyfile(original, frozen)
        source.append({'original': pin(original), 'frozen': pin(frozen), 'matches_historical': pin(original)['sha256'] == entry['original']['sha256']})
    client = (inputs / 'C_UseSkill.java').read_text('utf-8')
    for name in ['L1MobSkillUse.java', 'L1MagicNpc.java']:
        original = Path('D:/lineage開發/1-伺服器/src/com/lineage/server/model') / name
        frozen = inputs / name; shutil.copyfile(original, frozen)
        source.append({'original': pin(original), 'frozen': pin(frozen)})
    if not re.search(r'if\s*\(skillId\s*>\s*220\)\s*\{\s*return;', client):
        raise ValueError('Re-review Source client upper skill boundary')
    seed = json.loads(SEED.read_text('utf-8')); asm = [checked(p) for p in seed['asm']]
    cp = ';'.join(map(str, [run / 'tools', *asm])); commands = []
    with (run / 'audit.log').open('w', encoding='utf-8') as log:
        for args in [[JAVA / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-cp', cp, '-d', run / 'tools', inputs / 'CloseoutSkillCensus.java'],
                     [JAVA / 'java.exe', '-cp', cp, 'CloseoutSkillCensus', base, run / 'methods.tsv', run / 'edges.tsv', run / 'npc-definition-fields.tsv']]:
            args = list(map(str, args)); commands.append(args)
            subprocess.run(args, cwd=SERVER, stdout=log, stderr=subprocess.STDOUT, timeout=180, check=True,
                           env=dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'))
    methods = table(run / 'methods.tsv'); edges = table(run / 'edges.tsv')
    graph = defaultdict(set); by_name = defaultdict(list)
    for m in methods: by_name[(m['owner'], m['method'])].append((m['owner'], m['method'], m['descriptor']))
    for e in edges: graph[(e['owner'], e['method'], e['descriptor'])].add((e['callee_owner'], e['callee'], e['callee_descriptor']))
    client_owner = 'local/lito381/server/ClientSession'
    start = (client_owner, 'useSpell', '([B)V')
    if start not in graph: raise ValueError('Missing original packet dispatch')
    previous = {start: None}; queue = deque([start])
    while queue:
        node = queue.popleft()
        for target in sorted(graph.get(node, ())):
            if target not in previous: previous[target] = node; queue.append(target)
    def path(node):
        result = []
        while node is not None:
            result.append(node[0].split('/')[-1] + '.' + node[1] + node[2]); node = previous[node]
        return ' -> '.join(reversed(result))
    result = []
    for row in rows:
        skill = int(row['skill_id']); roots = row['native_entry_locators'].strip('[]').split(', ')
        roots = [x for x in roots if x]
        special = skill == 5
        if special: roots = ['useTeleportSpell']
        found = [m for name in roots for m in by_name.get((client_owner, name), [])]
        reachable = [m for m in found if m in previous]
        kind = 'placeholder-row' if row['name'] == 'none' else 'player-selector-skill' if skill <= 220 else 'non-player-data-row'
        wiring = ('current-special-teleport-entry' if special else 'current-model-entry') if found else 'no-current-player-entry-locator'
        if kind != 'player-selector-skill': wiring = 'data-consumer-review-open'
        result.append({
            'skill_id': row['skill_id'], 'name': row['name'], 'category': kind,
            'source_type': row['source_type'], 'target': row['target'], 'target_to': row['target_to'],
            'range': row['range'], 'area': row['area'], 'through': row['through'],
            'Source_symbols': row['Source_symbols'], 'Source_executors': row['Source_registry_executors'],
            'historical_native_models': row['native_models'], 'current_entry_locators': ';'.join(roots),
            'located_entries': ';'.join(m[1] + m[2] for m in found),
            'static_path_state': 'symbolic-dispatch-path-located' if reachable else 'no-symbolic-dispatch-path-proved',
            'shortest_static_path': min((path(m) for m in reachable), key=len, default=''),
            'development_state': wiring, 'semantics_complete': False, 'runtime_validation': 'deferred-by-user',
        })
    with (run / 'skills.tsv').open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(result[0]), delimiter='\t'); writer.writeheader(); writer.writerows(result)
    counts = {'rows': len(result), 'categories': dict(Counter(r['category'] for r in result)),
              'player_entry_located': sum(r['category'] == 'player-selector-skill' and bool(r['located_entries']) for r in result),
              'player_symbolic_path_located': sum(r['category'] == 'player-selector-skill' and r['static_path_state'] == 'symbolic-dispatch-path-located' for r in result),
              'methods': len(methods), 'edges': len(edges), 'implementation_handles': sum(e['kind'] == 'implementation-handle' for e in edges),
              'exact_reflection_bridges': sum(e['kind'] == 'verified-field-reflection' for e in edges)}
    manifest = {
        'status': 'current-archive-skill-routing-classified-semantic-review-open', 'runtime': current['runtime'],
        'historical_data_inventory': history['matrix'], 'Source': source, 'counts': counts,
        'matrix': pin(run / 'skills.tsv'), 'methods': pin(run / 'methods.tsv'), 'edges': pin(run / 'edges.tsv'),
        'npc_numeric_seams': pin(run / 'npc-definition-fields.tsv'),
        'npc_numeric_review': 'Definition getters remain stored values to preserve record identity. Quest/Time Attack consumers now read a captured Source skills row for HP/MP and each original damage loop. The separate NPC values build records exact mutations and inverse audit. Birth-captured mobskill reuse now governs the existing typed row selection and successful scheduling; two target attribute reads use loaded Source metadata. Visual and Ground magic now share Source HP/MP costs per readiness/payment. Source NPC INT uses 1..254; self-cost refreshes regeneration rather than sending HPBAR. Natural L1Monster recovery now uses captured Source intervals/amounts on the existing Hub executor. SP consumer coverage and other NPC classes remain under review.',
        'inputs': [pin(p) for p in sorted(inputs.iterdir())], 'asm': [pin(p) for p in asm], 'commands': commands, 'log': pin(run / 'audit.log'),
        'limits': ['All 807 rows retained. Non-player and placeholder categories do not remove them from required data-consumer review.',
                   'Symbolic call paths include lambda implementation handles and one bytecode-matched Dragon LEGACY reflection bridge. They do not prove skill-id-specific branches, virtual overrides, other reflection, runtime admission or effects.',
                   'The 807 row definitions are frozen historical read evidence; no new SQL read or equivalence claim.',
                   'Teleport has an independent current entry; its absence from historical model reflection was not an implementation gap.'],
        'tests_executed': False, 'database_connected': False, 'deployed': False, 'full_goal_achieved': False,
    }
    write(run / 'manifest.json', manifest)
    receipt = {'status': manifest['status'], 'runtime': current['runtime'], 'manifest': pin(run / 'manifest.json'), 'matrix': manifest['matrix'], 'counts': counts}
    write(ROOT / 'build/development-skill-inventory-current.json', receipt)
    print(json.dumps({'status': manifest['status'], 'counts': counts, 'manifest': receipt['manifest']}, ensure_ascii=False))


if __name__ == '__main__': main()
