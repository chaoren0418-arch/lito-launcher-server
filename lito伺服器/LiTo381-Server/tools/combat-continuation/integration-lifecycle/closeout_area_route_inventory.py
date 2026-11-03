"""Read offline SQL text only; never opens a database or executes SQL."""
import csv
import re
from pathlib import Path
from build_development_closeout import pin


def capture(run, source):
    text = source.read_text('utf-8-sig')
    frozen = []
    tables = {}
    for table in ['mobskill', 'skills']:
        schema = re.search(r'CREATE TABLE `' + table + r'`.*?\n\) ENGINE[^;]*;', text, re.S)
        if schema is None:
            raise ValueError('Missing offline schema: ' + table)
        columns = re.findall(r'^  `([^`]+)`', schema.group(), re.M)
        frozen.append(schema.group())
        rows = []
        prefix = 'INSERT INTO `' + table + '` VALUES ('
        for line in text.splitlines():
            if not line.startswith(prefix):
                continue
            if not line.endswith(');'):
                raise ValueError('Review changed offline insert format')
            values = next(csv.reader([line[len(prefix):-2]], delimiter=',', quotechar="'", escapechar='\\', skipinitialspace=True))
            if len(values) != len(columns):
                raise ValueError('Review changed offline columns')
            rows.append(dict(zip(columns, values)))
            frozen.append(line)
        tables[table] = rows
    skills = {int(r['skill_id']): r for r in tables['skills']}
    if len(skills) != len(tables['skills']):
        raise ValueError('Duplicate offline skill')
    from closeout_skill_effect_contracts import capture as capture_effect_contracts
    effect_contract_rows, effect_contracts = capture_effect_contracts(run, skills)
    references = []
    for row in tables['skills']:
        effect = int(row['effect_skill_id'])
        if effect > 0:
            references.append({k: row[k] for k in ['skill_id', 'name', 'effect_skill_id', 'type', 'target', 'buffDuration']} |
                              {'effect_template_present': effect in skills,
                               'resolution': 'normal-cast-mode-and-generic-status-only;custom-proc-keeps-raw-id',
                               'runtime_binding': 'optional-field-reader-wired-effect-execution-review-open',
                               'data_scope': 'offline-input-only-not-live-database-validation'} |
                              effect_contract_rows[int(row['skill_id'])])
    result = []
    excluded = {2, 58, 63, 205, 210, 215, 220}
    for row in tables['mobskill']:
        if int(row['Type']) != 2 or max(int(row['AreaWidth']), int(row['AreaHeight'])) <= 0:
            continue
        skill = skills.get(int(row['SkillId']))
        if skill is None:
            branch = 'missing-skill-metadata'
        elif int(skill['skill_id']) in excluded:
            branch = 'pre-target-effect-entry-open'
        elif int(skill['type']) == 64 and skill['target'] != 'none':
            branch = 'TYPE_GMBUFF-empty-self-target-success'
        else:
            branch = 'nonempty-or-special-self-execution-open'
        result.append({k: row[k] for k in ['mobid', 'actNo', 'mobname', 'AreaWidth', 'AreaHeight', 'SkillId']} |
                      {k: skill[k] if skill else '' for k in ['name', 'effect_skill_id', 'type', 'target', 'target_to', 'area']} |
                      {'execution_branch': branch, 'runtime_binding': 'offline-input-only-not-live-database-validation'})
    extracted = run / 'inputs/offline-area-route-tables.sql'
    extracted.write_text('\n'.join(frozen) + '\n', 'utf-8')
    matrix = run / 'npc-area-routes.tsv'
    with matrix.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(result[0]), delimiter='\t')
        writer.writeheader(); writer.writerows(result)
    effects = run / 'skill-effect-references.tsv'
    with effects.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(references[0]), delimiter='\t')
        writer.writeheader(); writer.writerows(references)
    return {'source': pin(source), 'frozen_relevant_tables': pin(extracted), 'matrix': pin(matrix),
            'area_rows': len(result), 'empty_self_target_rows': sum(r['execution_branch'] == 'TYPE_GMBUFF-empty-self-target-success' for r in result),
            'effect_references': {'matrix': pin(effects), 'positive_reference_rows': len(references),
                                  'distinct_effect_ids': len({r['effect_skill_id'] for r in references}),
                                  'effect_template_not_required_for_mode_lookup': True,
                                  'Source_execution_contracts': effect_contracts},
            'offline_only': True}
