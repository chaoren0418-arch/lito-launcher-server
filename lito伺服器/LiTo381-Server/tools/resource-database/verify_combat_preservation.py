from pathlib import Path
import subprocess, hashlib, json

R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build'
L=S.parent.parent/'lito登入器/LiTo381-Launcher'
formal=L/'app/server/LiTo381-SharedResources-candidate.jar';candidate=B/'candidate.jar'
pin=json.loads((S/'deployment/release-inputs.json').read_text(encoding='utf-8-sig'))['server_sha256']
assert hashlib.sha256(formal.read_bytes()).hexdigest()==pin,'formal deployment pin mismatch'
manifest=json.loads((B/'build-manifest.json').read_text(encoding='utf-8'))
assert hashlib.sha256(candidate.read_bytes()).hexdigest()==manifest['candidate_sha256'],'candidate manifest mismatch'
for relative, expected in manifest['builder_inputs'].items():
    source=(S/relative).resolve()
    assert source.is_relative_to(S.resolve()), 'builder input outside server workspace'
    assert hashlib.sha256(source.read_bytes()).hexdigest()==expected, 'builder input changed: '+relative
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');asm=Path.home()/'.m2/repository/org/ow2/asm'
cp=str(R/'inputs/asm-analysis-9.7.jar')+';'+';'.join(str(asm/f'{x}/9.7/{x}-9.7.jar') for x in ['asm','asm-tree'])
subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-cp',cp,'-d',str(B),str(R/'VerifyCombatPreservation.java'),str(R/'VerifyLineSpells.java'),str(R/'VerifyWorldPhysical.java'),str(R/'VerifyWorldCommit.java'),str(R/'VerifyWorldMagic.java'),str(R/'VerifyWorldMultiMagic.java'),str(R/'VerifyWorldFreeze.java'),str(R/'VerifyWorldFoe.java'),str(R/'VerifyNpcDebuffs.java'),str(R/'VerifyHealing.java'),str(R/'VerifyNpcMagic.java'),str(R/'VerifyCounterDetection.java'),str(R/'VerifyPlayerMagic.java'),str(R/'VerifyPlayerMirror.java'),str(R/'VerifyPlayerDamage.java'),str(R/'VerifyPatience.java'),str(R/'VerifyNpcPlayerPhysical.java'),str(R/'VerifyNpcPoison.java'),str(R/'VerifyIncomingSource.java'),str(R/'VerifyPoisonAtomic.java'),str(R/'VerifyPoisonParalysis.java'),str(R/'VerifyMonsterParalysis.java'),str(R/'VerifyMonsterSilence.java'),str(R/'VerifyMonsterDamage.java'),str(R/'VerifyMonsterPoisonParalysis.java'),str(R/'VerifyMonsterDamageCallback.java'),str(R/'VerifyMonsterCurseCallback.java'),str(R/'VerifyPoisonLifecycle.java'),str(R/'VerifyDeathCancellation.java')],check=True)
p=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-cp',str(B)+';'+cp,'VerifyCombatPreservation',str(formal),str(candidate),
 str(B/'combat-changed-methods.txt')],capture_output=True,encoding='utf-8',timeout=60)
(B/'combat-preservation.log').write_text(p.stdout+p.stderr,encoding='utf-8')
print(p.stdout+p.stderr);raise SystemExit(p.returncode)
