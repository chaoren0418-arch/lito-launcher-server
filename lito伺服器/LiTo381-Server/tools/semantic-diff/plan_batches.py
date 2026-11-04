"""T020b-2：從候選 JAR 產生第二階段（B）固定合併批次（設計 §6-4）。

用法（repo 根目錄）：
  python3 lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py \
      --candidate lito伺服器/LiTo381-Server/reference/candidate-50635f7e.jar \
      --owned docs/combat-merge/batches.json --diff <task/30 的 docs/combat-merge/class-diff.json> \
      --formal lito伺服器/LiTo381-Server/reference/base-0.6.71.jar --t029-out docs/combat-merge/t029-methods.txt --max 60 --out docs/combat-merge/batches.json

程序（設計 §6-4，第 3 版）：
1. 相依分析（只做紀錄）：常數池（CONSTANT_Class 與描述子中的 L...;）→ owned_by_B 之間的外層類別相依圖 → Tarjan SCC。
   結果顯示幾乎全部類別在同一個 SCC，所以「每批單獨在 src/main 編譯」不可行，改用暫存區＋候選 JAR 當 classpath 驗證（§6-5）。
2. 分組：依 class-diff.json 的分類（新類別：純 Java／混合／asm-only；既有類別：Battle*／Combat*+MaterialAction／其他／核心）
   分組，組內依名稱排序後切成每批 ≤ max 個外層類別。純標準函式庫，結果可重現。
"""
import argparse, difflib, hashlib, json, re, struct, subprocess, sys, zipfile
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


def declared_members(data):
    """Declared (name, descriptor) of fields and methods."""
    count = struct.unpack('>H', data[8:10])[0]; u = {}; i = 1; pos = 10
    while i < count:
        t = data[pos]
        if t == 1:
            n = struct.unpack('>H', data[pos+1:pos+3])[0]; u[i] = data[pos+3:pos+3+n].decode('utf-8', 'replace'); pos += 3 + n
        elif t in (7, 8, 16, 19, 20): pos += 3
        elif t == 15: pos += 4
        elif t in (3, 4, 9, 10, 11, 12, 17, 18): pos += 5
        elif t in (5, 6): pos += 9; i += 1
        i += 1
    pos += 6; ni = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2 + 2 * ni
    out = set()
    for _kind in 'fm':
        n = struct.unpack('>H', data[pos:pos+2])[0]; pos += 2
        for _ in range(n):
            _, a, b, ac = struct.unpack('>HHHH', data[pos:pos+8]); pos += 8
            out.add((u[a], u[b]))
            for _ in range(ac):
                pos += 6 + struct.unpack('>I', data[pos+2:pos+6])[0]
    return out

def member_refs(data):
    """(owner, name, descriptor) of Fieldref/Methodref/InterfaceMethodref constants."""
    count = struct.unpack('>H', data[8:10])[0]; e = {}; i = 1; pos = 10
    while i < count:
        t = data[pos]
        if t == 1:
            n = struct.unpack('>H', data[pos+1:pos+3])[0]; e[i] = ('u', data[pos+3:pos+3+n].decode('utf-8', 'replace')); pos += 3 + n
        elif t == 7: e[i] = ('c', struct.unpack('>H', data[pos+1:pos+3])[0]); pos += 3
        elif t in (8, 16, 19, 20): pos += 3
        elif t == 15: pos += 4
        elif t in (9, 10, 11): e[i] = ('m',) + struct.unpack('>HH', data[pos+1:pos+5]); pos += 5
        elif t == 12: e[i] = ('nt',) + struct.unpack('>HH', data[pos+1:pos+5]); pos += 5
        elif t in (3, 4, 17, 18): pos += 5
        elif t in (5, 6): pos += 9; i += 1
        i += 1
    out = set()
    for v in e.values():
        if v[0] == 'm':
            nt = e[v[2]]; out.add((e[e[v[1]][1]][1], e[nt[1]][1], e[nt[2]][1]))
    return out

BR = re.compile(r'^(if\w*|goto\w*|jsr\w*) \d+')
def javap_methods(jar, cls):
    """Normalized `javap -c -p` bodies: CP indices, offsets, jump targets and exception-table ranges removed."""
    txt = subprocess.run(['javap', '-c', '-p', '-cp', jar, cls.replace('/', '.')], capture_output=True, text=True, check=True).stdout
    sig = subprocess.run(['javap', '-s', '-p', '-cp', jar, cls.replace('/', '.')], capture_output=True, text=True, check=True).stdout.splitlines()
    desc = {}
    for i, l in enumerate(sig):
        if 'descriptor:' in l and i:
            head = sig[i-1].strip().rstrip(';'); m = re.search(r'([\w$<>]+)\(', head)
            if m: desc[head] = m.group(1) + l.split('descriptor: ')[1].strip()
    out = {}; cur = None
    for l in txt.splitlines():
        m = re.match(r'^  (?!\s)(.*\(.*\).*);$', l)
        if m: cur = desc.get(m.group(1), m.group(1)); out[cur] = []; continue
        if not cur or not l.strip(): continue
        x = re.sub(r'\s+', ' ', re.sub(r'^\s*\d+: ', '', re.sub(r'#\d+(:#\d+)?', '#', l))).strip()
        x = BR.sub(lambda mm: mm.group(1) + ' L', x)
        if re.match(r'^\d+ +\d+ +\d+ ', x): x = re.sub(r'^\d+ +\d+ +\d+', 'R', x)
        if re.match(r'^-?\d+: \d+$', x) or 'default:' in x: x = re.sub(r'\d+', 'N', x)
        out[cur].append(x)
    return out

T029_REF = re.compile(r'GmPolyMe|SelectionRecovery|SharedPresentationPackets')
T029_NAMES = {'sendRoomParticipant', 'litoRoomParticipant', 'magicDollIcon', 'magicDollCustomIcon'}
def t029(formal, candidate):
    """Methods of ClientSession/Protocol/GameHub that must be taken whole from the formal JAR."""
    rows = []
    for c in sorted(PKG + x for x in CORE):
        f, k = javap_methods(formal, c), javap_methods(candidate, c)
        for m, body in sorted(f.items()):
            name = m.split('(')[0]
            if not (T029_REF.search('\n'.join(body)) or name in T029_NAMES or m not in k): continue
            if m not in k:
                kind = 'formal-only-method'
            elif k[m] == body:
                continue  # 兩邊相同，不需特別處理
            else:
                kind = 'whole-formal'
            delta = [l for l in difflib.unified_diff(k.get(m, []), body, lineterm='', n=0) if l[:1] in '+-' and l[:3] not in ('+++', '---')]
            rows.append(dict(method=f'{c}#{m}', kind=kind, formal_minus_candidate=delta[:40], delta_lines=len(delta)))
    return rows

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
    ap.add_argument('--formal', required=True)
    ap.add_argument('--t029-out', required=True)
    ap.add_argument('--max', type=int, default=60)
    ap.add_argument('--out', required=True)
    a = ap.parse_args()
    formal_sha = hashlib.sha256(open(a.formal, 'rb').read()).hexdigest()
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
    edges = defaultdict(set); cand_entries = defaultdict(list)
    zc, zf = zipfile.ZipFile(a.candidate), zipfile.ZipFile(a.formal)
    formal_names = set(zf.namelist())
    for n in zc.namelist():
        if not n.endswith('.class'): continue
        o = outer(n[:-6])
        if o not in owned: continue
        cand_entries[o].append(n)
        for ref in class_refs(zc.read(n)):
            t = outer(ref)
            if t in owned and t != o: edges[o].add(t)
    formal_only_entries = defaultdict(list)
    for n in formal_names:
        if n.endswith('.class') and outer(n[:-6]) in owned and n not in zc.namelist():
            formal_only_entries[outer(n[:-6])].append(n)
    # 既有類別在候選中新增的成員，以及用到這些新成員的類別
    new_members = {}
    for n in zc.namelist():
        if n.endswith('.class') and n in formal_names and outer(n[:-6]) in owned:
            extra_m = declared_members(zc.read(n)) - declared_members(zf.read(n))
            if extra_m: new_members[n[:-6]] = extra_m
    users = set()
    for n in zc.namelist():
        o = outer(n[:-6]) if n.endswith('.class') else None
        if o in owned and any((nm, d) in new_members.get(cls, ()) for cls, nm, d in member_refs(zc.read(n))): users.add(o)
    t029_rows = t029(a.formal, a.candidate)
    with open(a.t029_out, 'w', encoding='utf-8') as f:
        f.write(''.join(r['method'] + '\n' for r in t029_rows))
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
                                    verify_entries=sorted(e for o in chunk for e in cand_entries[o]),
                                    history_entries=sorted(e for o in chunk for e in info[o]['entries']),
                                    remove_entries=sorted(e for o in chunk for e in formal_only_entries[o])))
    assert sorted(m for b in batches for m in b['classes']) == sorted(owned)
    plan.update(
        status='batches 已產生（T020b-2，§6-4 第 3 版）',
        generator=dict(script='lito伺服器/LiTo381-Server/tools/semantic-diff/plan_batches.py', candidate=a.candidate,
                       candidate_sha256=cand_sha, formal=a.formal, formal_sha256=formal_sha, diff=a.diff, max_per_batch=a.max),
        entry_rules=dict(verify_entries='候選 JAR 中該批外層類別及其全部 $ 內部類別；批次輸出必須剛好是這些 entry，全部與候選 SAME（無缺漏、無多出）',
                         history_entries='class-diff.json 中該批的歷史差異列（僅供參考，不是驗收範圍）',
                         remove_entries='正式 JAR 有、候選沒有的同批 entry；B-land 時必須消失'),
        t029=dict(file=a.t029_out, rule='ClientSession／Protocol／GameHub 中，正式版引用 GmPolyMe／SelectionRecovery／SharedPresentationPackets、名稱為 sendRoomParticipant／litoRoomParticipant／magicDollIcon／magicDollCustomIcon、或只存在正式版，且與候選不同的方法；正規化 javap（去常數池編號、偏移、跳轉目標、例外表範圍）',
                  methods=t029_rows),
        dependency_analysis=dict(nodes=len(owned), edges=sum(len(v) for v in edges.values()),
                                 scc_count=len(sccs), largest_scc=max(len(c) for c in sccs),
                                 largest_scc_without_core=max(len(c) for c in no_core),
                                 existing_classes_with_new_members=len(new_members),
                                 existing_outer_classes_with_new_members=len({outer(c) for c in new_members}),
                                 new_member_count=sum(len(v) for v in new_members.values()),
                                 classes_using_new_members=len(users),
                                 candidate_entries_total=sum(len(v) for v in cand_entries.values()),
                                 conclusion='幾乎全部類別互相依賴，無法逐批在 src/main 單獨編譯；改以暫存區＋候選 JAR classpath 驗證（§6-5）'),
        batches=batches)
    json.dump(plan, open(a.out, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(json.dumps(dict(sha=cand_sha, sccs=len(sccs), largest=plan['dependency_analysis']['largest_scc'],
                          largest_no_core=plan['dependency_analysis']['largest_scc_without_core'],
                          entries=plan['dependency_analysis']['candidate_entries_total'],
                          new_members=[plan['dependency_analysis'][k] for k in ('existing_classes_with_new_members','existing_outer_classes_with_new_members','new_member_count','classes_using_new_members')],
                          t029=[(r['method'].split('#')[1][:40], r['kind'], r['delta_lines']) for r in t029_rows],
                          batches=[(b['id'], b['category'], b['size'], len(b['verify_entries']), len(b['remove_entries'])) for b in batches]), ensure_ascii=False))

if __name__ == '__main__':
    main()
