"""T020b-2：從候選 JAR 產生第二階段（B）固定合併批次（設計 §6-4）。

用法（repo 根目錄）：
  python3 lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py \
      --candidate lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
      --owned docs/combat-merge/batches.json --diff <task/30 的 docs/combat-merge/class-diff.json> --max 60 --out docs/combat-merge/batches.json

程序（設計 §6-4，第 3 版）：
1. 相依分析（只做紀錄）：常數池（CONSTANT_Class 與描述子中的 L...;）→ owned_by_B 之間的外層類別相依圖 → Tarjan SCC。
   結果顯示幾乎全部類別在同一個 SCC，所以「每批單獨在 src/main 編譯」不可行，改用暫存區＋候選 JAR 當 classpath 驗證（§6-5）。
2. 分組：依 class-diff.json 的分類（新類別：純 Java／混合／asm-only；既有類別：Battle*／Combat*+MaterialAction／其他／核心）
   分組，組內依名稱排序後切成每批 ≤ max 個外層類別。純標準函式庫，結果可重現。
"""
import argparse, hashlib, json, re, struct, sys, zipfile
from collections import defaultdict

PKG = 'local/lito381/server/'
CORE = {'ClientSession', 'Protocol', 'GameHub'}
REF = re.compile(r'L(local/lito381/server/[\w$]+);')

def outer(internal):
    return internal[len(PKG):].split('$', 1)[0] if internal.startswith(PKG) else None

def class_refs(data):
    """Return internal class names referenced by a class file's constant pool."""
    if data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('not a class file')
    count = struct.unpack('>H', data[8:10])[0]
    utf8, classes, i, pos = {}, [], 1, 10
    while i < count:
        tag = data[pos]
        if tag == 1:
            n = struct.unpack('>H', data[pos+1:pos+3])[0]
            utf8[i] = data[pos+3:pos+3+n].decode('utf-8', 'replace'); pos += 3 + n
        elif tag in (7, 8, 16, 19, 20):
            if tag == 7: classes.append(struct.unpack('>H', data[pos+1:pos+3])[0])
            pos += 3
        elif tag == 15: pos += 4
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): pos += 5
        elif tag in (5, 6): pos += 9; i += 1
        else: raise ValueError(f'bad constant tag {tag}')
        i += 1
    names = set()
    for c in classes:
        n = utf8.get(c, '')
        names.add(n.lstrip('[').removeprefix('L').removesuffix(';'))
    for s in utf8.values():
        names.update(REF.findall(s))
    return names

def tarjan(nodes, edges):
    index, low, stack, on, out, counter = {}, {}, [], set(), [], [0]
    sys.setrecursionlimit(10000)
    def visit(v):
        index[v] = low[v] = counter[0]; counter[0] += 1; stack.append(v); on.add(v)
        for w in sorted(edges[v]):
            if w not in index: visit(w); low[v] = min(low[v], low[w])
            elif w in on: low[v] = min(low[v], index[w])
        if low[v] == index[v]:
            comp = []
            while True:
                w = stack.pop(); on.discard(w); comp.append(w)
                if w == v: break
            out.append(sorted(comp))
    for v in sorted(nodes):
        if v not in index: visit(v)
    return out  # Tarjan emits SCCs in reverse topological order (dependencies first)

def category(name, kinds, classes):
    if 'CHANGED' in classes:
        if name in CORE: return 'existing-core'
        if name.startswith('Battle'): return 'existing-battle'
        if name.startswith('Combat') or name == 'ForcedCombat' or name.endswith('MaterialAction'): return 'existing-combat-material'
        return 'existing-native'
    if kinds == {'asm-only'}: return 'new-asm'
    return 'new-java'  # 純 Java 或「外層 Java＋內部 ASM」

ORDER = ['new-asm', 'new-java', 'existing-battle', 'existing-combat-material', 'existing-native', 'existing-core']

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--candidate', required=True)
    ap.add_argument('--owned', required=True)
    ap.add_argument('--diff', required=True)
    ap.add_argument('--max', type=int, default=60)
    ap.add_argument('--out', required=True)
    a = ap.parse_args()
    raw = open(a.candidate, 'rb').read()
    cand_sha = hashlib.sha256(raw).hexdigest()
    plan = json.load(open(a.owned, encoding='utf-8'))
    owned = set(plan['owned_by_B'])
    diff = json.load(open(a.diff, encoding='utf-8'))
    rows = [r for r in next(c for c in diff['comparisons'] if c['id'] == 'candidate_5063_vs_8d26')['rows']
            if r['classification'] != 'SAME']
    info = defaultdict(lambda: dict(kinds=set(), classes=set(), entries=[]))
    for r in rows:
        o = outer(r['class_entry'][:-6])
        info[o]['kinds'].add(r['source_kind']); info[o]['classes'].add(r['classification'])
        info[o]['entries'].append(r['class_entry'])
    if set(info) != owned: raise SystemExit('owned_by_B 與 class-diff.json 不一致')
    edges = defaultdict(set)
    with zipfile.ZipFile(a.candidate) as z:
        for n in z.namelist():
            if not n.endswith('.class'): continue
            o = outer(n[:-6])
            if o not in owned: continue
            for ref in class_refs(z.read(n)):
                t = outer(ref)
                if t in owned and t != o: edges[o].add(t)
    sccs = tarjan(owned, edges)
    no_core = tarjan(owned - CORE, defaultdict(set, {k: {t for t in v if t not in CORE} for k, v in edges.items() if k not in CORE}))
    groups = defaultdict(list)
    for o in sorted(owned): groups[category(o, info[o]['kinds'], info[o]['classes'])].append(o)
    batches = []
    for cat in ORDER:
        members = groups[cat]
        parts = max(1, -(-len(members) // a.max))
        size = -(-len(members) // parts)
        for i in range(parts):
            chunk = members[i*size:(i+1)*size]
            if chunk:
                batches.append(dict(id=f'B{len(batches)+1:02d}', category=cat, size=len(chunk), classes=chunk,
                                    class_entries=sorted(e for o in chunk for e in info[o]['entries'])))
    assert sorted(m for b in batches for m in b['classes']) == sorted(owned)
    plan.update(
        status='batches 已產生（T020b-2，§6-4 第 3 版）',
        generator=dict(script='lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py', candidate=a.candidate,
                       candidate_sha256=cand_sha, diff=a.diff, max_per_batch=a.max),
        dependency_analysis=dict(nodes=len(owned), edges=sum(len(v) for v in edges.values()),
                                 scc_count=len(sccs), largest_scc=max(len(c) for c in sccs),
                                 largest_scc_without_core=max(len(c) for c in no_core),
                                 conclusion='幾乎全部類別互相依賴，無法逐批在 src/main 單獨編譯；改以暫存區＋候選 JAR classpath 驗證（§6-5）'),
        batches=batches)
    json.dump(plan, open(a.out, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(json.dumps(dict(sha=cand_sha, sccs=len(sccs), largest=plan['dependency_analysis']['largest_scc'],
                          largest_no_core=plan['dependency_analysis']['largest_scc_without_core'],
                          batches=[(b['id'], b['category'], b['size']) for b in batches]), ensure_ascii=False))

if __name__ == '__main__':
    main()
