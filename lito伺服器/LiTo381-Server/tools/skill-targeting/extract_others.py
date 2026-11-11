"""Extract consumption / duration / effect columns for knight / dark elf /
royal / dragon knight / illusionist skills from 1-伺服器 db/380_db.sql
`skills` (read-only).  Issue #309 (T109).

Ranges follow combat-coverage-current.md SQL grouping:
  knight 87..91 (5), dark elf 97..112 (16), royal 113..118 (6),
  dragon knight 181..192 (12), illusionist 193..195 + 201..220 (23) -> 62.
`name=none` reserved slots are skipped.

usage: py -3 -I extract_others.py <380_db.sql> <out.tsv>

Columns are identical to extract_wizard.py / extract_elf.py (16 columns).
"""
import re
import sys

src, out = sys.argv[1], sys.argv[2]
data = open(src, 'rb').read().decode('cp950', errors='replace')
pat = re.compile(r"INSERT INTO `skills` VALUES \((.*?)\);\r?\n")
tok = re.compile(r"'(?:[^'\\]|\\.)*'|[^,]+")
COLS = [('skill_id', 0), ('skill_level', 3), ('mp', 5), ('hp', 6), ('item_id', 7),
        ('item_count', 8), ('reuse_ms', 9), ('buff_s', 10), ('dmg_value', 13),
        ('dmg_dice', 14), ('dmg_dice_count', 15), ('prob_value', 16), ('prob_dice', 17),
        ('attr', 18), ('type', 19), ('lawful', 20)]
IDS = (list(range(87, 92)) + list(range(97, 113)) + list(range(113, 119))
       + list(range(181, 193)) + list(range(193, 196)) + list(range(201, 221)))
rows = {}
for m in pat.finditer(data):
    fields = [t.strip() for t in tok.findall(m.group(1))]
    fields = [f[1:-1] if f.startswith("'") else f for f in fields]
    sid = int(fields[0])
    if sid in IDS and fields[2] != 'none':
        rows[sid] = fields
with open(out, 'w', encoding='utf-8', newline='\n') as w:
    w.write('# source: 1-server db/380_db.sql `skills` 87..91, 97..118, 181..195, 201..220 minus none'
            ' (read-only extract, issue #309)\n')
    w.write('\t'.join(c for c, _ in COLS) + '\n')
    for sid in sorted(rows):
        w.write('\t'.join(rows[sid][i] for _, i in COLS) + '\n')
print(len(rows))
