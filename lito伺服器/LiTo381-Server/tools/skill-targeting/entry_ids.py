"""List the 181 entry skills from the closeout skills.tsv and cross-check targeting columns vs SQL extract.
usage: py -3 -I t305_entry_ids.py <closeout skills.tsv> <t305_skills.tsv>
"""
import sys
rows = [l.rstrip('\n').split('\t') for l in open(sys.argv[1], encoding='utf-8')]
h = rows[0]
ix = {n: i for i, n in enumerate(h)}
ids = []
for r in rows[1:]:
    if r[ix['category']] == 'player-selector-skill' and r[ix['static_path_state']] in ('symbolic-dispatch-path-located',):
        ids.append(int(r[ix['skill_id']]))
cats = {}
for r in rows[1:]:
    k = (r[ix['category']], r[ix['static_path_state']])
    cats[k] = cats.get(k, 0) + 1
print(cats)
print(len(ids))
print(','.join(map(str, sorted(ids))))
sql = {}
for l in open(sys.argv[2], encoding='utf-8'):
    if l[0] in '#s':
        continue
    f = l.rstrip('\n').split('\t')
    sql[int(f[0])] = f
mism = 0
for r in rows[1:]:
    sid = int(r[ix['skill_id']])
    if sid in sql:
        s = sql[sid]
        a = (r[ix['target']], r[ix['target_to']], r[ix['range']], r[ix['area']], '1' if r[ix['through']] == 'true' else '0', r[ix['source_type']])
        b = (s[1], s[2], s[4], s[5], s[6], s[3])
        if a != b:
            mism += 1
            print('MISMATCH', sid, a, b)
print('mismatches', mism)
