"""Extract consumption / duration / effect columns for elf skills from
1-伺服器 db/380_db.sql `skills` (read-only).  Issue #308 (T108).

Elf skills = skill_id 129..176 (skill_level 17..22) minus the `name=none`
reserved slots (135, 136, 139..144) -> 40 rows.

usage: py -3 -I extract_elf.py <380_db.sql> <out.tsv>

Columns are identical to extract_wizard.py (16 columns, same order), so the
test can share the row parser.
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
rows = {}
for m in pat.finditer(data):
    fields = [t.strip() for t in tok.findall(m.group(1))]
    fields = [f[1:-1] if f.startswith("'") else f for f in fields]
    sid = int(fields[0])
    if 129 <= sid <= 176 and fields[2] != 'none':
        rows[sid] = fields
with open(out, 'w', encoding='utf-8', newline='\n') as w:
    w.write('# source: 1-伺服器 db/380_db.sql `skills` skill_id 129..176 minus none (read-only extract, issue #308)\n')
    w.write('\t'.join(c for c, _ in COLS) + '\n')
    for sid in sorted(rows):
        w.write('\t'.join(rows[sid][i] for _, i in COLS) + '\n')
print(len(rows))
