"""T020e：產生 A 階段（src/main → 正式 0.6.71）固定修補批次（設計 §7）。

用法（repo 根目錄）：
  python3 lito伺服器/LiTo381-Server/tools/semantic-diff/plan_a_batches.py \
      --gap docs/server-semantic-gap.md --owned docs/combat-merge/batches.json \
      --formal lito伺服器/LiTo381-Server/reference/base-0.6.71.jar \
      --src lito伺服器/LiTo381-Server/src/main/java/local/lito381/server \
      --max 50 --out docs/combat-merge/a-batches.json

範圍：gap 報告的 DIFF＋ONLY_RIGHT 中，外層類別不屬於 owned_by_B 者。
分組：NativeDe*（反編譯為主）、Battle*／Combat*、其他；組內依名稱排序，切成每批 ≤ max 個外層類別。
verify_entries：正式 JAR 中該批外層類別及其全部 $ 內部類別（批次輸出必須剛好是這些 entry，且與正式 SAME）。
同時以正式 JAR 常數池做相依分析（只紀錄），說明為何採暫存區驗證。純標準函式庫，結果可重現。
"""
import argparse, hashlib, json, os, re, sys, zipfile
from collections import Counter, defaultdict
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import plan_batches as pb

def category(o):
    if o.startswith('NativeDe'): return 'nativede'
    if o.startswith('Battle') or o.startswith('Combat'): return 'battle-combat'
    return 'other'
ORDER = ['nativede', 'battle-combat', 'other']

def main():
    ap = argparse.ArgumentParser()
    for k in ('--gap', '--owned', '--formal', '--src', '--out'): ap.add_argument(k, required=True)
    ap.add_argument('--max', type=int, default=50)
    a = ap.parse_args()
    md = open(a.gap, encoding='utf-8').read()
    dsec = md.split('## DIFF 類別清單')[1].split('## ONLY_LEFT')[0]
    diff = {}
    for l in dsec.splitlines():
        m = re.match(r'^\| `(local/lito381/server/[^`]+)` \| (.*) \|$', l)
        if m: diff[m.group(1)] = m.group(2)
    only_right = sorted(set(re.findall(r'(local/lito381/server/[\w$]+)', md.split('## ONLY_RIGHT')[1])))
    owned = set(json.load(open(a.owned, encoding='utf-8'))['owned_by_B'])
    targets = {e: d for e, d in diff.items() if pb.outer(e) not in owned}
    targets.update({e: 'ONLY_RIGHT（src/main缺）' for e in only_right if pb.outer(e) not in owned})
    outers = sorted({pb.outer(e) for e in targets})
    zf = zipfile.ZipFile(a.formal); formal_sha = hashlib.sha256(open(a.formal, 'rb').read()).hexdigest()
    entries = defaultdict(list); edges = defaultdict(set); S = set(outers)
    for n in zf.namelist():
        if not n.endswith('.class'): continue
        o = pb.outer(n[:-6])
        if o not in S: continue
        entries[o].append(n)
        for r in pb.class_refs(zf.read(n)):
            t = pb.outer(r)
            if t in S and t != o: edges[o].add(t)
    sccs = pb.tarjan(S, edges)
    def src_info(o):
        p = os.path.join(a.src, o + '.java')
        if not os.path.exists(p): return dict(file=None, decompiled=None)
        return dict(file=p.replace(os.sep, '/'), decompiled=open(p, encoding='utf-8', errors='ignore').read(20).startswith('// DECOMPILED'))
    groups = defaultdict(list)
    for o in outers: groups[category(o)].append(o)
    batches = []
    for cat in ORDER:
        ms = groups[cat]
        if not ms: continue
        parts = -(-len(ms) // a.max); size = -(-len(ms) // parts)
        for i in range(parts):
            chunk = ms[i*size:(i+1)*size]
            batches.append(dict(id=f'A{len(batches)+1:02d}', category=cat, size=len(chunk), classes=chunk,
                                verify_entries=sorted(e for o in chunk for e in entries[o]),
                                diff_entries={e: targets[e] for e in sorted(targets) if pb.outer(e) in chunk},
                                sources={o: src_info(o) for o in chunk}))
    assert sorted(m for b in batches for m in b['classes']) == outers
    out = dict(
        status='T020e 產生',
        generator=dict(script='lito伺服器/LiTo381-Server/tools/semantic-diff/plan_a_batches.py', gap=a.gap, owned=a.owned,
                       formal=a.formal, formal_sha256=formal_sha, max_per_batch=a.max),
        scope=dict(diff_total=len(diff), only_right_total=len(only_right), diff_owned_by_B=sum(pb.outer(e) in owned for e in diff),
                   target_entries=len(targets), target_outer=len(outers),
                   decompiled_outer=sum(1 for b in batches for v in b['sources'].values() if v['decompiled']),
                   verify_entries_total=sum(len(b['verify_entries']) for b in batches),
                   first_difference_kind=dict(Counter(d.split(' ')[0] for d in targets.values()))),
        dependency_analysis=dict(scc_count=len(sccs), largest_scc=max(len(c) for c in sccs),
                                 conclusion='A 範圍在正式 JAR 中也有大型 SCC，無法逐批在 src/main 單獨編譯；採暫存區＋正式 JAR classpath 驗證（§7-3）'),
        batches=batches)
    json.dump(out, open(a.out, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(json.dumps(dict(scope=out['scope'], sccs=len(sccs), largest=out['dependency_analysis']['largest_scc'],
                          batches=[(b['id'], b['category'], b['size'], len(b['verify_entries'])) for b in batches]), ensure_ascii=False))

if __name__ == '__main__':
    main()
