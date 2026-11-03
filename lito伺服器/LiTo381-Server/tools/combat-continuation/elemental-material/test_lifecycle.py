from pathlib import Path
import hashlib, json, os, re, shutil, subprocess

R = Path(__file__).resolve().parent
S = R.parents[2]
B = S / 'tools/resource-database/build'
D = R / 'build/lifecycle'
D.mkdir(exist_ok=True)
read = lambda p: json.loads(p.read_text('utf-8'))
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
pin = lambda p: dict(path=str(p.resolve()), sha256=sha(p))
build = read(R / 'build/build.json')
jar = Path(build['runtime']['path'])
assert sha(jar) == build['runtime']['sha256']
for name in ['test.log', 'result.json', 'observations.json']:
    p = D / name
    if p.exists(): shutil.copyfile(p, D / ('previous-' + sha(p)[:12] + '-' + name))
frozen = D / 'frozen'
frozen.mkdir(exist_ok=True)
inputs = [R / 'ElementalLifecycleProbe.java', R / 'ElementalMaterialProbe.java', Path(__file__)]
for name, ids in [('LESSER_ELEMENTAL', [45306, 45303, 45304, 45305]),
                  ('GREATER_ELEMENTAL', [81053, 81050, 81051, 81052])]:
    p = R / 'build/inputs/source-reference' / (name + '.java')
    source = p.read_text('utf-8')
    assert [int(x) for x in re.findall(r'summonid = (\d+);', source)] == [0, *ids]
    assert 'pc.getCha() + 7' in source and 'petcost == 0' in source
    assert 'isRecallPets()' in source and 'S_ServerMessage(353)' in source
    inputs.append(p)
for p in inputs:
    q = frozen / (sha(p) + '-' + p.name)
    if not q.exists(): shutil.copyfile(p, q)
classes = D / 'classes'
classes.mkdir(exist_ok=True)
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
F = B / 'baseline-PC-ingress-efd4-v2'
cp = ';'.join(map(str, [classes, R / 'build/boundaries/classes', R.parent / 'build/tests/classes',
    B / 'pc-native-signed-hp-development/signed-probe-fixtures', B / 'pc-native-accept-development/fixtures',
    F, F / 'fixture-support.zip', jar, S / 'lib/*']))
env = dict(os.environ, JAVA_TOOL_OPTIONS='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 '
    '-XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8 -Dlineage.damage.commitWorkers=2 '
    '-Dlineage.damage.commitDispatchCapacity=256 -Dlineage.damage.maxTargetPending=64 -Dlineage.damage.maxGlobalPending=64')
result = dict(status='running', runtime=build['runtime'], inputs=[pin(p) for p in frozen.iterdir()],
              formal_deployment_changed=False, source_database_mutated=False)
try:
    with (D / 'test.log').open('w', encoding='utf-8') as log:
        def invoke(*args):
            subprocess.run(list(map(str, args)), cwd=S, env=env, stdout=log,
                           stderr=subprocess.STDOUT, check=True, timeout=180)
        invoke(J / 'javac.exe', '-g:none', '-encoding', 'UTF-8', '-implicit:none', '-sourcepath', '',
               '-cp', cp, '-d', classes, R / 'ElementalLifecycleProbe.java')
        invoke(J / 'java.exe', '-Xverify:all', '-cp', cp, 'local.lito381.server.ElementalLifecycleProbe',
               S / 'config/server.properties', D / 'runtime', D / 'observations.json', jar)
    observed = read(D / 'observations.json')
    assert observed['cases'] == 44 and observed['locked_writes'] == 0
    result.update(status='passed', observed=observed, output=pin(D / 'observations.json'))
except BaseException as error:
    result.update(status='failed', error=str(error))
    print((D / 'test.log').read_text('utf-8', errors='replace')[-6500:])
    raise
finally:
    (D / 'result.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'status':result['status'], 'observed':result['observed']}))
