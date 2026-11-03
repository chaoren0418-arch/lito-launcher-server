from pathlib import Path
import subprocess,hashlib,json,shutil,zipfile
from combat_signature_metadata import generate
R=Path(__file__).resolve().parent;S=R.parents[1];L=S.parent.parent/'lito登入器/LiTo381-Launcher';B=R/'build';C=B/'classes';C.mkdir(parents=True,exist_ok=True)
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');base=R/'inputs/base-0.6.61.jar'
if not base.is_file():raise FileNotFoundError('Required pinned input: '+str(base))
assert hashlib.sha256(base.read_bytes()).hexdigest()=='9719f45db324206617726ce06d1a9e2184e9a3b3aefab3d50f3ebbdfbf09cfac'
# Only the verified generated class directory is replaced. A removed source must
# never leave a stale class that silently overrides a pinned-base class.
if C.resolve().parent != B.resolve() or B.resolve().parent != R.resolve():
 raise RuntimeError('Generated classes escaped the builder workspace')
shutil.rmtree(C)
C.mkdir()
values=json.loads((R/'runtime-values.json').read_text(encoding='utf-8'))
(B/'fields.tsv').write_text('\n'.join(sorted(set(r['record_type']+'\t'+r['field_name'] for r in values))),encoding='utf-8')
asm=Path.home()/'.m2/repository/org/ow2/asm'
analysis=R/'inputs/asm-analysis-9.7.jar'
assert hashlib.sha256(analysis.read_bytes()).hexdigest()=='7bc6bcbc21379948a0c8c467fb0f864206e5b818f6bc0b546872f5c9f941556f'
cp=';'.join([str(analysis),*[str(asm/f'{a}/9.7/{a}-9.7.jar') for a in ['asm','asm-tree']]])
generate(S/'src/main/java/local/lito381/server/BattleSpellCatalog.java',B/'probability-method-parameters.tsv')
def run(*a):subprocess.run(list(map(str,a)),check=True)
run(J/'javac.exe','-encoding','UTF-8','-implicit:none','-cp',base,'-d',C,*[S/'src/main/java/local/lito381/server'/f'{x}.java' for x in ['ResourceDatabase','NativeRoomRules','NativeRoomDirectoryRow','EquipmentTransitions','ForcedCombat','GmTravel','CombatDatabase','CombatRules','SourceSkillControls','CombatAccess','CombatProbability','CombatDragon','CombatAwaken','CombatBuffLeases','CombatWeaponBuffs','SourceEyeControls','SourceBlessingControls','CombatSpecialBuffs','CombatReturnNature','CombatClassDamage','CombatMagicDamage','CombatKiringku','CombatCompanionDamage','CombatCompanionSpells','CombatLineSpells','CombatWorldPhysical','CombatNpcDebuffs','CombatHealing','CombatNpcMagic','CombatCounterDetection','CombatPlayerMagic','CombatPlayerMirror','CombatPlayerDamage','CombatPlayerRecovery','CombatSourceReductions','CombatNpcPlayerDamage','CombatNpcPhysicalSource','CombatNpcPoison','CombatIncomingSource','CombatPoisonAtomic','CombatPoisonParalysis','CombatPoisonLifecycle','CombatCurseLifecycle','CombatMonsterParalysis','CombatMonsterCurseLifecycle','CombatNpcDamageQueue','CombatWorldCommit','CombatWorldMagic','CombatWorldMultiMagic','CombatWorldFreeze','CombatWorldFoe','CombatDeathCancellation','BattleChainswordWeakness']])
run(J/'javac.exe','-encoding','UTF-8','-cp',cp,'-d',B,R/'Patch.java',R/'EquipmentPatch.java',R/'MovementLockPatch.java',R/'ForcedCombatPatch.java',R/'GmTravelPatch.java',R/'CombatPatch.java',R/'DragonPatch.java',R/'AwakenPatch.java',R/'BuffPatch.java',R/'WeaponBuffPatch.java',R/'SpecialBuffPatch.java',R/'BlessingNpcPatch.java',R/'DeathPenaltyPatch.java',R/'ReturnNaturePatch.java',R/'ClassDamagePatch.java',R/'MagicDamagePatch.java',R/'KiringkuPatch.java',R/'CompanionDamagePatch.java',R/'CompanionSpellPatch.java',R/'LineSpellPatch.java',R/'WorldCompanionPhysicalPatch.java',R/'NpcDebuffPatch.java',R/'HealingPatch.java',R/'NpcMagicPatch.java',R/'CounterDetectionPatch.java',R/'PlayerMagicPatch.java',R/'PlayerMirrorPatch.java',R/'PlayerDamagePatch.java',R/'PatiencePatch.java',R/'NpcPlayerPhysicalPatch.java',R/'NpcPoisonPatch.java',R/'IncomingSourcePatch.java',R/'PoisonAtomicPatch.java',R/'PoisonParalysisPatch.java',R/'PoisonLifecyclePatch.java',R/'MonsterParalysisPatch.java',R/'MonsterSilencePatch.java',R/'MonsterDamagePatch.java',R/'MonsterPoisonParalysisPatch.java',R/'WorldCommitPatch.java',R/'WorldMagicPatch.java',R/'WorldMultiMagicPatch.java',R/'WorldFreezePatch.java',R/'WorldFoePatch.java',R/'MonsterDamageCallbackPatch.java',R/'MonsterCurseCallbackPatch.java',R/'DeathCancellationPatch.java',R/'RangedDamagePatch.java',R/'ProbabilityPatch.java',R/'ProbeProbabilityTargets.java')
pending=B/'candidate-building.jar'
try:
 run(J/'java.exe','-Xmx256m','-XX:+UseSerialGC','-cp',str(B)+';'+str(base)+';'+cp,'Patch',base,C,pending,B/'fields.tsv',B/'probability-method-parameters.tsv')
except BaseException:
 pending.unlink(missing_ok=True)
 raise
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
with zipfile.ZipFile(base) as old,zipfile.ZipFile(pending) as new:
 old_entries={n:hashlib.sha256(old.read(n)).hexdigest() for n in old.namelist()}
 changed=[{'entry':n,'base_sha256':old_entries.get(n),'candidate_sha256':hashlib.sha256(new.read(n)).hexdigest(),
           'kind':'compiled-source' if (C/n).is_file() else 'verified-ASM-hook'}
          for n in sorted(new.namelist()) if hashlib.sha256(new.read(n)).hexdigest()!=old_entries.get(n)]
manifest={'base':str(base.relative_to(S)),'base_sha256':sha(base),'candidate_sha256':sha(pending),
 'full_source_compile':False,'changed_entries':changed,
 'builder_inputs':{str(p.relative_to(S)):sha(p) for p in sorted([Path(__file__),R/'combat_signature_metadata.py',R/'ProbeProbabilityTargets.java',analysis,S/'src/main/java/local/lito381/server/BattleSpellCatalog.java',S/'src/main/java/local/lito381/server/BattleTimedStatus.java',*R.glob('*Patch.java'),
   *[S/'src/main/java/local/lito381/server'/f'{x}.java' for x in ['ResourceDatabase','NativeRoomRules','NativeRoomDirectoryRow','EquipmentTransitions','ForcedCombat','GmTravel','CombatDatabase','CombatRules','SourceSkillControls','CombatAccess','CombatProbability','CombatDragon','CombatAwaken','CombatBuffLeases','CombatWeaponBuffs','SourceEyeControls','SourceBlessingControls','CombatSpecialBuffs','CombatReturnNature','CombatClassDamage','CombatMagicDamage','CombatKiringku','CombatCompanionDamage','CombatCompanionSpells','CombatLineSpells','CombatWorldPhysical','CombatNpcDebuffs','CombatHealing','CombatNpcMagic','CombatCounterDetection','CombatPlayerMagic','CombatPlayerMirror','CombatPlayerDamage','CombatPlayerRecovery','CombatSourceReductions','CombatNpcPlayerDamage','CombatNpcPhysicalSource','CombatNpcPoison','CombatIncomingSource','CombatPoisonAtomic','CombatPoisonParalysis','CombatPoisonLifecycle','CombatCurseLifecycle','CombatMonsterParalysis','CombatMonsterCurseLifecycle','CombatNpcDamageQueue','CombatWorldCommit','CombatWorldMagic','CombatWorldMultiMagic','CombatWorldFreeze','CombatWorldFoe','CombatDeathCancellation','BattleChainswordWeakness']]])}}
pending.replace(B/'candidate.jar')
(B/'build-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(manifest['candidate_sha256'])
