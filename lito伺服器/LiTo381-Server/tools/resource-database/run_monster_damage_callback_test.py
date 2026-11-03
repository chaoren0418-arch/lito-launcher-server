"""Hash-bound native WORLD damage poison callback regression; no deployment or SQL."""
from pathlib import Path
import subprocess,hashlib,json,os,sys,re
R=Path(__file__).resolve().parent;S=R.parents[1];B=R/'build';L=S.parent.parent/'lito登入器/LiTo381-Launcher'
J=Path('C:/Program Files/Microsoft/jdk-17.0.17.10-hotspot/bin');cp=str(B/'candidate.jar')+';'+str(S/'lib/*');source=S.parents[2]/'1-伺服器/target/classes'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
bind=lambda p:{'path':str(p.resolve()),'sha256':sha(p)}
env=dict(os.environ)
if not env.get('JAVA_TOOL_OPTIONS'):env['JAVA_TOOL_OPTIONS']='-Xms16m -Xmx256m -XX:+UseSerialGC -XX:CICompilerCount=2 -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=64m'
subprocess.run([sys.executable,'-B','-X','utf8',str(R/'run_monster_damage_source_test.py')],cwd=S,env=env,check=True)
reference=json.loads((B/'monster-damage-source-oracle-current.json').read_text('utf-8'));assert reference['status']=='passed'
pins=reference['source_files']+[bind(p) for name in ['model/L1AttackPc','model/skill/L1SkillUse'] for p in [source/('com/lineage/server/'+name+'.class'),source.parents[1]/('src/com/lineage/server/'+name+'.java')]]
sources=[R/(n+'.java') for n in ['RangedDamageRegression','KiringkuRegression','CombatRegression','CounterDetectionRegression','PlayerMagicRegression','PlayerMirrorRegression','PlayerDamageRegression','PatienceRegression','NpcPhysicalRegression','NpcPoisonRegression','PoisonAtomicRegression','PoisonParalysisRegression','PoisonCallbackRegression','SilenceCallbackRegression','ParalysisCallbackRegression','CurseCallbackRegression','MonsterParalysisRegression','MonsterCurseCallbackRegression','MonsterSilenceCallbackRegression','WorldCompanionPhysicalRegression','CompanionDamageRegression','ClassMagicRegression','ClassDamageRegression','WeaponBuffRegression','AwakenCombatRegression']]
sources.extend([S/'tools/durable-settlement/FlowRegression.java',S/'tools/dragon-combat/Regression.java',S/'tools/native-room-state/NativeDirectoryRegression.java',S/'tools/shared-database/SourceLoginRegression.java'])
sources.append(R/'MonsterDamageCallbackRegression.java')
inputs=[bind(p) for p in [Path(__file__),*sources]]
candidate=bind(B/'candidate.jar');formal=bind(L/'app/server/LiTo381-SharedResources-candidate.jar')
receipt_path=B/'monster-damage-callback-oracle-current.json'
receipt=dict(status='running',candidate_sha256=candidate['sha256'],source_files=pins,verification_inputs=inputs,generated_executable_files=reference['generated_executable_files'],source_monster_damage_reference_receipt=bind(B/'monster-damage-source-oracle-current.json'),source_monster_damage_contract=reference['source_monster_damage_contract'],full_behavior_parity_verified=False,formal_deployment_changed=False)
save=lambda:receipt_path.write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
save()
try:
 subprocess.run([str(J/'javac.exe'),'-encoding','UTF-8','-implicit:none','-cp',cp,'-d',str(B/'tests'),*map(str,sources)],cwd=S,env=env,check=True)
 with (B/'MonsterDamageCallbackRegression.log').open('w',encoding='utf-8') as log:
  result=subprocess.run([str(J/'java.exe'),'-Dfile.encoding=UTF-8','-Dlito.resourceProfile=tw13081901-v1','-Dlito.commonCatalog='+str(L/'app/common_catalog.bin'),'-Xverify:all','-cp',str(B/'tests')+';'+cp,'local.lito381.server.MonsterDamageCallbackRegression',str(S/'config/server.properties'),str(S/'runtime/combat-monster-damage-callback-regression')],cwd=S,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=100)
 output=(B/'MonsterDamageCallbackRegression.log').read_text('utf-8');print(output[-6000:]);assert result.returncode==0,'native damage callback regression failed'
 for item in [candidate,formal,*pins,*inputs]:assert sha(Path(item['path']))==item['sha256'],'input changed: '+item['path']
 fields={
  'Damage':['final long expires;','final long interval;','final long attacker;','final long player;','long next;','final local.lito381.server.ClassicWorldMonsterPopulation$ContributorKind contributor;','final local.lito381.server.CombatMonsterCurseLifecycle$Recipient owner;'],
  'Recipient':['private final long id;','private final java.lang.String source;','private final java.lang.String account;','private final java.lang.String name;'],
  'Curse':['final local.lito381.server.BattleTimedStatus slot;','final long duration;','final long epoch;','final boolean poison;','int phase;','long initialWall;','long initialLogical;','local.lito381.server.CombatMonsterCurseLifecycle$Removal removal;','local.lito381.server.CombatMonsterCurseLifecycle$Damage damage;','local.lito381.server.CombatMonsterCurseLifecycle$Paralysis paralysis;']}
 receipt['open_lease_value_fields']={}
 for suffix,expected_fields in fields.items():
  inspected=subprocess.run([str(J/'javap.exe'),'-private','-classpath',str(B/'candidate.jar'),'local.lito381.server.CombatMonsterCurseLifecycle$'+suffix],capture_output=True,encoding='utf-8',env=env,check=True)
  actual=[line.strip() for line in inspected.stdout.splitlines() if line.strip().endswith(';') and '(' not in line]
  assert actual==expected_fields,(suffix,actual)
  receipt['open_lease_value_fields'][suffix]=actual
 receipt['native_WORLD_damage_callback_boundary_port_verified']=True
 receipt.update(status='passed',native_regression_checks=int(re.search(r'PASS WORLD monster damage callback checks=(\d+);',output)[1]))
except BaseException as error:
 receipt.update(status='failed',failure=str(error));raise
finally:save()
