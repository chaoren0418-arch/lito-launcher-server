#!/usr/bin/env python3
"""唯讀掃描兩個 reference JAR；輸出可重現的 record 額外欄位與批次清單。"""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True
from as_record import build, jdk

SERVER = Path(__file__).resolve().parents[2]
REPO = SERVER.parents[1]
DOCS = REPO / 'docs/combat-merge'


def generate():
    jars = [SERVER / 'reference' / name for name in
            ('candidate-50635f7e.jar', 'base-0.6.71.jar')]
    cp = build()
    result = subprocess.run([jdk('java'), '-cp', cp,
                             'local.lito381.asrecord.ScanRecordFields', *map(str, jars)],
                            check=True, capture_output=True, encoding='utf-8')
    rows = json.loads(result.stdout)
    owners = {}
    batch_inputs = []
    for filename in ('batches.json', 'a-batches.json'):
        path = DOCS / filename
        batch_inputs.append(dict(path=path.relative_to(REPO).as_posix(),
                                 sha256=hashlib.sha256(path.read_bytes()).hexdigest()))
        for batch in json.loads(path.read_text(encoding='utf-8'))['batches']:
            for outer in batch['classes']:
                key = 'local/lito381/server/' + outer
                owners.setdefault(key, []).append(dict(id=batch['id'], source=filename))
    for row in rows:
        outer = row['class'].split('$', 1)[0]
        row['batches'] = sorted(owners.get(outer, []), key=lambda b: b['id'])
        if not row['batches']:
            raise ValueError(f'找不到批次：{row["jar"]}:{row["entry"]}')
    raw_counts = dict(sorted(Counter(row['jar'] for row in rows).items()))
    grouped = {}
    for row in rows:
        grouped.setdefault(row['class'], []).append(row)
    selected = []
    for name, variants in sorted(grouped.items()):
        stages = {b['id'][0] for b in variants[0]['batches']}
        if len(stages) != 1:
            raise ValueError(f'A/B 歸屬不唯一：{name}: {stages}')
        preferred = 'candidate-50635f7e.jar' if stages == {'B'} else 'base-0.6.71.jar'
        matches = [r for r in variants if r['jar'] == preferred]
        if len(matches) != 1:
            raise ValueError(f'批次指定版本不存在或重複：{name}: {preferred}')
        chosen = matches[0]
        chosen['other_jar_variants'] = [r for r in variants if r is not chosen]
        selected.append(chosen)
    counts = dict(sorted(Counter(row['jar'] for row in selected).items()))
    return dict(schema_version=1,
                selection='superName == java/lang/Record && instance fields > record components',
                access_scope='同一 JAR 中實際 GETFIELD/PUTFIELD 指令；包含宣告類別本身，排除反射與 method handle',
                batch_rule='以外層類別對照 classes；B 選候選，A 選正式；另一 JAR 的完整證據保留在 other_jar_variants',
                inputs=[dict(path=p.relative_to(REPO).as_posix(),
                             sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in jars],
                batch_inputs=batch_inputs, raw_counts=raw_counts, raw_total=len(rows),
                counts=counts, total=len(selected), records=selected)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='只核對已提交輸出，不寫入')
    parser.add_argument('--output', type=Path, default=DOCS / 'record-asm-fields.json')
    args = parser.parse_args()
    data = generate()
    expected = {'candidate-50635f7e.jar': 38, 'base-0.6.71.jar': 1}
    rendered = (json.dumps(data, ensure_ascii=False, indent=2) + '\n').encode('utf-8')
    if args.check:
        if args.output.read_bytes() != rendered:
            raise ValueError('掃描輸出與提交檔不一致，請重新產生')
    else:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_bytes(rendered)
    print(f'{"CHECK" if args.check else "GENERATED"}: total={data["total"]} counts={data["counts"]}')
    print(f'RAW: total={data["raw_total"]} counts={data["raw_counts"]}; 重複類別的版本證據保留於 other_jar_variants')
    if data['counts'] != expected:
        raise ValueError(f'筆數與預期不符：expected={expected}；實際清單已產生，請核對輸入雜湊與 selection')


if __name__ == '__main__':
    main()
