"""Extract skills targeting columns (skill_id 1..220) from 1-伺服器 db/380_db.sql (read-only).

usage: py -3 -I extract_skills.py <380_db.sql> <out.tsv>
Columns follow CREATE TABLE `skills` order: skill_id(0) effect_skill_id(1) name(2) skill_level(3)
skill_number(4) mpConsume(5) hpConsume(6) itemConsumeId(7) itemConsumeCount(8) reuseDelay(9)
buffDuration(10) target(11) target_to(12) damage_value(13) damage_dice(14) damage_dice_count(15)
probability_value(16) probability_dice(17) attr(18) type(19) lawful(20) ranged(21) area(22) through(23) id(24)
"""
import re
import sys

src, out = sys.argv[1], sys.argv[2]
data = open(src, 'rb').read().decode('cp950', errors='replace')
pat = re.compile(r"INSERT INTO `skills` VALUES \((.*?)\);\r?\n")
tok = re.compile(r"'(?:[^'\\]|\\.)*'|[^,]+")
rows = {}
for m in pat.finditer(data):
    fields = [t.strip() for t in tok.findall(m.group(1))]
    fields = [f[1:-1] if f.startswith("'") else f for f in fields]
    sid = int(fields[0])
    if 1 <= sid <= 220:
        rows[sid] = fields
with open(out, 'w', encoding='utf-8', newline='\n') as w:
    w.write('# source: 1-伺服器 db/380_db.sql `skills` (read-only extract, issue #305)\n')
    w.write('skill_id\ttarget\ttarget_to\ttype\tranged\tarea\tthrough\tskill_level\tattr\tid\n')
    for sid in sorted(rows):
        f = rows[sid]
        w.write('\t'.join([f[0], f[11], f[12], f[19], f[21], f[22], f[23], f[3], f[18], f[24]]) + '\n')
print(len(rows))
