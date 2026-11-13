"""#138 B-land semantic verification.

Usage (from worktree root, after `mvn -DskipTests package`):
  py -I scratch/issue-138/verify.py <run-label>

Runs semantic-diff (rebuilt from tools/semantic-diff/src with ASM 9.7) in four modes:
  b-approved : Maven JAR vs candidate-50635f7e, --classes = B 1,384 verify_entries, T029 allow-right base,
               only approvals already in docs/combat-merge/*.txt
  b-pending  : same + docs/combat-merge/exceptions-exempt-pending-138.txt (NOT approved; request in #138)
  a          : Maven JAR vs base-0.6.71, --classes = A 692 verify_entries (re-run after rebase on A-land)
  full       : Maven JAR vs base-0.6.71, every entry, classified B / SOURCE_ONLY / OTHER
"""
import collections, hashlib, json, subprocess, sys, zipfile
from pathlib import Path

r = Path.cwd()
s = r / 'lito伺服器/LiTo381-Server'
w = r / 'scratch/issue-138' / sys.argv[1]
w.mkdir(parents=True, exist_ok=False)
tools = r / 'scratch/issue-138/tools'
j = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
cm = r / 'docs/combat-merge'
commands = []


def run(name, args):
    args = list(map(str, args))
    p = subprocess.run(args, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (w / (name + '.log')).write_bytes(p.stdout)
    commands.append(dict(name=name, argv=args, exit=p.returncode))
    (w / 'commands.json').write_text(json.dumps(commands, ensure_ascii=False, indent=2), encoding='utf8')
    print(name, 'exit', p.returncode, flush=True)
    return p.returncode


expected = {'asm': 'adf46d5e34940bdf148ecdd26a9ee8eea94496a72034ff7141066b3eea5c4e9d',
            'asm-tree': '62f4b3bc436045c1acb5c3ba2d8ec556ec3369093d7f5d06c747eb04b56d52b1',
            'asm-analysis': '7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'}
deps = []
for n, h in expected.items():
    p = tools / f'{n}-9.7.jar'
    assert hashlib.sha256(p.read_bytes()).hexdigest() == h, p
    deps.append(p)
cp = ';'.join(map(str, deps))
tool = w / 'tool'
tool.mkdir()
assert run('tool-javac', [j / 'javac.exe', '--release', '17', '-encoding', 'UTF-8', '-cp', cp, '-d', tool,
                          *sorted((s / 'tools/semantic-diff/src/main/java').rglob('*.java'))]) == 0

left = s / 'target/LiTo381-Server.jar'
cand = s / 'reference/candidate-50635f7e.jar'
base = s / 'reference/base-0.6.71.jar'


def entries(jar):
    with zipfile.ZipFile(jar) as z:
        return {n.removesuffix('.class') for n in z.namelist() if n.endswith('.class')}


le, ce, be = entries(left), entries(cand), entries(base)
bb = json.loads((cm / 'batches.json').read_text(encoding='utf8'))
b = {n.removesuffix('.class') for x in bb['batches'] for n in x['verify_entries']}
removes = [n for x in bb['batches'] for n in x.get('remove_entries', [])]
a = {n.removesuffix('.class') for x in json.loads((cm / 'a-batches.json').read_text(encoding='utf8'))['batches']
     for n in x['verify_entries']}
(w / 'b-entries.txt').write_text('\n'.join(sorted(b)) + '\n', encoding='utf8')
(w / 'a-entries.txt').write_text('\n'.join(sorted(a)) + '\n', encoding='utf8')
summary = dict(left_sha256=hashlib.sha256(left.read_bytes()).hexdigest(),
               candidate_sha256=hashlib.sha256(cand.read_bytes()).hexdigest(),
               base_sha256=hashlib.sha256(base.read_bytes()).hexdigest(),
               b_entries=len(b), a_entries=len(a),
               b_missing_in_left=sorted(b - le), b_extra_vs_candidate=sorted(n for n in le - ce if n.split('$')[0] in {x.split('$')[0] for x in b}),
               remove_entries_declared=removes, remove_entries_present=[n for n in removes if n.removesuffix('.class') in le])

APPROVALS = [('effects-methods', 'effects-approved.txt'), ('access-widen', 'access-widen-approved.txt'),
             ('deviations', 'deviations.txt'), ('exceptions-exempt', 'exceptions-exempt.txt'),
             ('extra-methods', 'extra-methods-approved.txt'), ('metadata-exempt', 'metadata-exempt.txt'),
             ('layout-methods', 'layout-approved.txt')]


def trimmed(label, selection, right_entries, extra=()):
    flags, skipped = [], {}
    groups = collections.defaultdict(list)
    for flag, file in list(APPROVALS) + list(extra):
        skipped[file] = []
        for line in (cm / file).read_text(encoding='utf-8-sig').splitlines():
            key = line.split(' #')[0].strip()
            if not key or key.startswith('#'):
                continue
            owner = next((v for v in key.split() if '#' in v), key.split()[0]).split('#')[0].removesuffix('.class')
            if owner in selection and owner in le and owner in right_entries:
                groups[flag].append(line)
            else:
                skipped[file].append(line)
    for flag, lines in groups.items():
        p = w / f'{label}-{flag}.txt'
        p.write_text('\n'.join(lines) + '\n', encoding='utf8')
        flags += ['--' + flag, p]
    (w / f'{label}-unused-approvals.json').write_text(json.dumps(skipped, ensure_ascii=False, indent=2), encoding='utf8')
    return flags


def compare(label, right, selection, classes_file, right_entries, extra_flags=(), extra_lists=(), approval_scope=None):
    args = [j / 'java.exe', '-Xmx3g', '-Dfile.encoding=UTF-8', '-cp', str(tool) + ';' + cp,
            'local.lito381.semanticdiff.SemanticDiff', '--left', left, '--right', right,
            *trimmed(label, selection if approval_scope is None else approval_scope, right_entries, extra_lists), *extra_flags,
            '--json', w / (label + '.json'), '--md', w / (label + '.md')]
    if classes_file:
        args += ['--classes', classes_file]
    code = run('semantic-' + label, args)
    res = json.loads((w / (label + '.json')).read_text(encoding='utf8')) if (w / (label + '.json')).exists() else None
    if res:
        sel = res.get('classSelection', {})
        summary[label] = dict(exit=code, counts=res['counts'], missingLeft=sel.get('missingLeft'), extraLeft=sel.get('extraLeft'),
                              bad=[dict(name=c['name'], status=c['status'], firstDiff=c.get('firstDiff'))
                                   for c in res['classes'] if c['status'] in ('DIFF', 'ONLY_LEFT', 'ONLY_RIGHT')])
        print(label, res['counts'], flush=True)
    return res


t029 = ['--allow-methods', cm / 't029-methods.txt', '--allow-right', base]
compare('b-approved', cand, b, w / 'b-entries.txt', ce | be, t029)
compare('a', base, a, w / 'a-entries.txt', be)
full = compare('full', base, le | be, None, be, approval_scope=(le | be) - b)
if full:
    groups = collections.defaultdict(list)
    for c in full['classes']:
        if c['status'] not in ('DIFF', 'ONLY_LEFT', 'ONLY_RIGHT'):
            continue
        n = c['name']
        g = 'B' if n in b else 'SOURCE_ONLY' if c['status'] == 'ONLY_LEFT' else 'OTHER'
        groups[g].append(dict(name=n, status=c['status']))
    summary['full_classification'] = {k: dict(collections.Counter(x['status'] for x in v)) for k, v in groups.items()}
    summary['full_other'] = groups.get('OTHER', [])
(w / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf8')
print(json.dumps({k: v for k, v in summary.items() if k not in ('b-approved', 'b-pending', 'a', 'full')}, ensure_ascii=False)[:3000])
