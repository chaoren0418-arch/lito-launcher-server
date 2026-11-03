"""Read-only per-property Source references and explicit dynamic alias evidence.

This is an inventory, not semantic qualification. No Source code is executed.
"""
from pathlib import Path
from datetime import datetime, timezone
import collections
import csv
import json
import re
import subprocess
import zipfile
from build_development_closeout import ROOT, SERVER, checked, pin, write

SOURCE = Path('D:/lineage開發/1-伺服器/src')


def main():
    old_ref = json.loads((ROOT / 'build/source-completion-inventory-current.json').read_text('utf-8'))
    old = json.loads(checked(old_ref['manifest']).read_text('utf-8'))
    refs = {Path(x['path']).name: x for x in old['artifacts']}
    with checked(refs['controls.tsv']).open(encoding='utf-8') as f:
        rows = list(csv.DictReader(f, delimiter='\t'))
    source_inputs = {Path(x['path']).stem: x for x in old['Source_configuration_inputs']}
    source_text = {name: checked(ref).read_text('utf-8') for name, ref in source_inputs.items()}
    pattern = r'\b(?:' + '|'.join(source_inputs) + r')\.'
    found = subprocess.run(['rg', '-l', pattern, str(SOURCE), '-g', '*.java'], capture_output=True, text=True, encoding='utf-8', check=False)
    if found.returncode not in (0, 1): raise RuntimeError(found.stderr)
    uses = collections.defaultdict(list)
    consumers = []
    for name in found.stdout.splitlines():
        path = Path(name)
        if path.stem in source_inputs: continue
        text = path.read_text('utf-8-sig')
        consumers.append(pin(path))
        for line_no, line in enumerate(text.splitlines(), 1):
            for m in re.finditer(r'\b(Config\w+)\.([A-Za-z_][A-Za-z_0-9]*)', line):
                if m[1] in source_inputs:
                    uses[m[1], m[2]].append(str(path.relative_to(SOURCE)).replace('\\', '/') + ':' + str(line_no))
    current = json.loads((ROOT / 'build/development-skill-effects-current.json').read_text('utf-8'))
    checked(current['runtime'])
    # Dynamic aliases below are backed by both source and retained candidate class bytes.
    alias_sources = [SERVER / 'src/main/java/local/lito381/server/CombatClassDamage.java',
                     SERVER / 'src/main/java/local/lito381/server/CombatMagicDamage.java', ROOT / 'NativeSourceDisplayPlan.java']
    with zipfile.ZipFile(checked(old_ref['runtime'])) as before, zipfile.ZipFile(current['runtime']['path']) as after:
        for cls in ['CombatClassDamage', 'CombatMagicDamage', 'NativeSourceDisplayPlan$Settings']:
            entry = 'local/lito381/server/' + cls + '.class'
            if before.read(entry) != after.read(entry): raise ValueError('Alias implementation changed: ' + cls)
        templates = {
            'CombatClassDamage': [b'Other_To_is\x01_pc', b'Other_To_is\x01_npc'],
            'CombatMagicDamage': [b'Other_ADD_is\x01_MagicPC'],
            'NativeSourceDisplayPlan$Settings': [b'DmgSprPhysical\x01', b'DmgSprMagic\x01'],
        }
        for cls, recipes in templates.items():
            data = after.read('local/lito381/server/' + cls + '.class')
            if any(recipe not in data for recipe in recipes): raise ValueError('Dynamic alias recipe changed: ' + cls)
    classes = ['Crown', 'Knight', 'Elf', 'Wizard', 'Darkelf', 'DragonKnight', 'Illusionist']
    aliases = {f'Other_To_is{job}_{kind}': 'CombatClassDamage.keys/key/scale → CombatRules.number'
               for job in classes for kind in ['pc', 'npc']}
    aliases.update({f'Other_ADD_is{job}_MagicPC': 'CombatMagicDamage.keys/finish → CombatRules.number' for job in classes})
    aliases.update({f'DmgSpr{kind}{place}': 'NativeSourceDisplayPlan.Settings.read → physical/magic → digits'
                    for kind in ['Physical', 'Magic'] for place in ['Unit', 'Ten', 'Hundred', 'Thousand', 'TenThousand']})
    for row in rows:
        text = source_text[row['source_file']]
        lines = text.splitlines()
        offset = sum(len(x) + 1 for x in lines[:int(row['line']) - 1])
        key_at = text.find('"' + row['key'] + '"', max(0, offset - 5))
        start = text.rfind(';', 0, key_at) + 1
        prefix = text[start:key_at]
        assignments = list(re.finditer(r'\b([A-Za-z_][A-Za-z_0-9]*)\s*(?:\[[^\]]*\])?\s*=\s*', prefix))
        field = assignments[-1][1] if assignments else ''
        row['source_assigned_symbol'] = field
        row['source_references'] = ';'.join(sorted(set(uses[row['source_file'], field]))) if field else ''
        row['dynamic_alias_consumer'] = aliases.get(row['key'], '')
        row['development_status'] = ('dynamic-reader-consumer-located-tests-deferred' if row['dynamic_alias_consumer']
                                     else 'literal-reader-located-consumer-review-open' if row['candidate_literal_locators']
                                     else 'source-consumer-located-applicability-review-open' if row['source_references']
                                     else 'no-qualified-Source-reference-found-review-open')
        row['semantic_qualified'] = 'False'
    run = ROOT / 'build/development-control-inventory' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    run.mkdir(parents=True)
    with (run / 'controls.tsv').open('w', encoding='utf-8', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(rows[0]), delimiter='\t'); writer.writeheader(); writer.writerows(rows)
    manifest = {
        'status': '252-rows-Source-references-and31-dynamic-aliases-inventoried-no-runtime-qualification',
        'runtime': current['runtime'], 'source_inventory': old_ref['manifest'],
        'source_configurations': list(source_inputs.values()), 'source_consumers': consumers,
        'candidate_alias_sources': [pin(p) for p in alias_sources],
        'alias_count': sum(bool(r['dynamic_alias_consumer']) for r in rows),
        'row_count': len(rows), 'statuses': dict(collections.Counter(r['development_status'] for r in rows)),
        'matrix': pin(run / 'controls.tsv'), 'tool': pin(Path(__file__)),
        'limits': ['Static reference presence is not runtime reachability or equivalence.', 'Local temporary variable assignments and computed array keys require manual review.', 'No unreferenced property is automatically declared obsolete or installed.'],
        'tests_executed': False, 'database_connected': False, 'all_Source_consumers_wired': False,
    }
    write(run / 'manifest.json', manifest)
    result = {'status': manifest['status'], 'manifest': pin(run / 'manifest.json'), 'matrix': manifest['matrix']}
    write(ROOT / 'build/development-control-inventory-current.json', result)
    print(json.dumps({'status': manifest['status'], 'rows': len(rows), 'aliases': manifest['alias_count'], 'statuses': manifest['statuses'], 'manifest': result['manifest']}, ensure_ascii=False))


if __name__ == '__main__': main()
