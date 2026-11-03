"""Execute source NPC silence bodies in isolation; no native callback parity claim."""
from pathlib import Path
import hashlib
import json
import os
import subprocess

R = Path(__file__).resolve().parent
S = R.parents[1]
B = R / 'build'
J = Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin')
source = S.parents[2] / '1-伺服器/target/classes'
output = B / 'monster-silence-source-oracle'
output.mkdir(exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
bind = lambda p: {'path': str(p.resolve()), 'sha256': sha(p)}
receipt_path = B / 'monster-silence-source-oracle-current.json'
names = ['model/L1Character', 'model/L1Paralysis', 'model/L1CurseParalysis',
         'model/poison/L1Poison', 'model/poison/L1DamagePoison',
         'model/poison/L1SilencePoison', 'model/poison/L1ParalysisPoison',
         'model/skill/L1SkillId']
pins = [bind(p) for name in names for p in
        [source / ('com/lineage/server/' + name + '.class'),
         source.parents[1] / ('src/com/lineage/server/' + name + '.java')]]
pins.extend(bind(source / ('com/lineage/server/' + name + '.class')) for name in
            ['model/L1Character$PoisonTransition', 'model/L1Character$ParalysisTransition',
             'model/poison/L1Poison$CleanupPlan', 'model/L1Paralysis$CleanupPlan'])
assert len(pins) == 20
existing = {str(p): sha(p) for p in B.glob('*oracle-current.json') if p != receipt_path}
candidate = bind(B / 'candidate.jar')
inputs = [bind(R / name) for name in ['BuildPoisonLeaseOracle.java',
          'BuildDeathCancellationOracle.java', 'BuildMonsterSilenceOracle.java', Path(__file__).name]]
receipt = dict(status='running', candidate_sha256=candidate['sha256'], source_files=pins,
               verification_inputs=inputs, generated_executable_files=[],
               native_NPC_silence_callback_port_verified=False,
               full_behavior_parity_verified=False, formal_deployment_changed=False)
save = lambda: receipt_path.write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
save()
asm = Path.home() / '.m2/repository/org/ow2/asm'
cp = ';'.join(str(asm / f'{n}/9.7/{n}-9.7.jar') for n in ['asm', 'asm-tree'])
environment = dict(os.environ)
if not environment.get('JAVA_TOOL_OPTIONS'):
    environment['JAVA_TOOL_OPTIONS'] = '-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m'
try:
    with (B / 'MonsterSilenceSourceOracle.log').open('w', encoding='utf-8') as log:
        def run(*args):
            subprocess.run([str(a) for a in args], cwd=S, env=environment, check=True,
                           stdout=log, stderr=subprocess.STDOUT, timeout=90)
        run(J / 'javac.exe', '-encoding', 'UTF-8', '-cp', cp, '-d', output,
            R / 'BuildPoisonLeaseOracle.java', R / 'BuildDeathCancellationOracle.java',
            R / 'BuildMonsterSilenceOracle.java')
        run(J / 'java.exe', '-Xverify:all', '-cp', str(output) + ';' + cp,
            'BuildPoisonLeaseOracle', source, output, output / 'source-lease-contract.json')
        run(J / 'java.exe', '-Xverify:all', '-cp', str(output) + ';' + cp,
            'BuildMonsterSilenceOracle', source, output, output / 'source-monster-silence-contract.json')
    for item in [candidate, *pins, *inputs]:
        assert sha(Path(item['path'])) == item['sha256'], 'input changed: ' + item['path']
    assert {str(p): sha(p) for p in B.glob('*oracle-current.json') if p != receipt_path} == existing, 'independent oracle receipt changed'
    generated = sorted((output / 'local/lito381/server').glob('*.class'))
    assert len(generated) == 21
    receipt.update(status='passed', source_lease_contract=json.loads((output / 'source-lease-contract.json').read_text('utf-8')),
                   source_monster_silence_contract=json.loads((output / 'source-monster-silence-contract.json').read_text('utf-8')),
                   generated_executable_files=[bind(p) for p in generated],
                   executable_contracts=[bind(output / name) for name in ['source-lease-contract.json', 'source-monster-silence-contract.json']])
except BaseException as error:
    receipt.update(status='failed', failure=str(error))
    raise
finally:
    save()
    print((B / 'MonsterSilenceSourceOracle.log').read_text('utf-8', errors='replace')[-5000:])
