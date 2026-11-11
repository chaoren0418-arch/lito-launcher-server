"""Extract consumption / duration / effect columns for wizard skills 1..80 from
1-伺服器 db/380_db.sql `skills` (read-only).  Issue #307 (T107).

usage: py -3 -I extract_wizard.py <380_db.sql> <out.tsv>

Column indexes follow CREATE TABLE `skills` (see extract_skills.py):
skill_id(0) name(2) skill_level(3) mpConsume(5) hpConsume(6) itemConsumeId(7)
itemConsumeCount(8) reuseDelay(9) buffDuration(10) damage_value(13)
damage_dice(14) damage_dice_count(15) probability_value(16)
probability_dice(17) attr(18) type(19) lawful(20)
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
    if 1 <= sid <= 80 and fields[2] != 'none':
        rows[sid] = fields
with open(out, 'w', encoding='utf-8', newline='\n') as w:
    w.write('# source: 1-伺服器 db/380_db.sql `skills` skill_id 1..80 (read-only extract, issue #307)\n')
    w.write('\t'.join(c for c, _ in COLS) + '\n')
    for sid in sorted(rows):
        w.write('\t'.join(rows[sid][i] for _, i in COLS) + '\n')
print(len(rows))
